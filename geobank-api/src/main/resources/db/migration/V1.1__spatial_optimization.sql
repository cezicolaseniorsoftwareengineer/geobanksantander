-- ============================================================================
-- GeoBank Spatial Optimization Migration
-- Purpose: Enhance geospatial query performance using PostGIS indexes
-- Database: PostgreSQL 16+ with PostGIS 3.4+
-- Author: Banking Engineering Team
-- Version: 1.1.0
-- ============================================================================

-- Enable PostGIS extension if not already enabled
CREATE EXTENSION IF NOT EXISTS postgis;

-- Add geometry column for optimized spatial operations
ALTER TABLE branches
ADD COLUMN IF NOT EXISTS geom GEOMETRY(POINT, 4326);

-- Populate geometry column from existing lat/lon
UPDATE branches
SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE geom IS NULL;

-- Create spatial index using GIST (Generalized Search Tree)
-- This dramatically improves proximity queries from O(n) to O(log n)
CREATE INDEX IF NOT EXISTS idx_branches_geom_gist
ON branches USING GIST(geom);

-- Create covering index for common query patterns
-- Includes frequently accessed columns to avoid table lookups
CREATE INDEX IF NOT EXISTS idx_branches_geom_status_type
ON branches USING GIST(geom)
INCLUDE (branch_name, branch_code, city_name, state_code);

-- Add functional index for distance calculations
-- Pre-computes geography cast for faster distance operations
CREATE INDEX IF NOT EXISTS idx_branches_geog
ON branches USING GIST(CAST(geom AS geography));

-- Create partial index for active branches only
-- Reduces index size and improves query speed for active branch lookups
CREATE INDEX IF NOT EXISTS idx_branches_active_geom
ON branches USING GIST(geom)
WHERE status = 'ACTIVE';

-- Add B-tree indexes for non-spatial filtering
CREATE INDEX IF NOT EXISTS idx_branches_status
ON branches(status)
WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_branches_type
ON branches(type);

CREATE INDEX IF NOT EXISTS idx_branches_created_at
ON branches(created_at DESC);

-- Create composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_branches_type_status
ON branches(type, status)
WHERE status = 'ACTIVE';

-- Add constraint to ensure geometry consistency
ALTER TABLE branches
ADD CONSTRAINT IF NOT EXISTS check_geom_not_null
CHECK (geom IS NOT NULL);

-- Create trigger to auto-update geometry on lat/lon changes
CREATE OR REPLACE FUNCTION update_branch_geometry()
RETURNS TRIGGER AS $$
BEGIN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_branch_geometry ON branches;
CREATE TRIGGER trigger_update_branch_geometry
    BEFORE INSERT OR UPDATE OF latitude, longitude ON branches
    FOR EACH ROW
    EXECUTE FUNCTION update_branch_geometry();

-- Create materialized view for branch density analysis
CREATE MATERIALIZED VIEW IF NOT EXISTS branch_density_grid AS
SELECT
    ST_SnapToGrid(geom, 0.1) AS grid_cell,
    COUNT(*) AS branch_count,
    ARRAY_AGG(branch_id) AS branch_ids,
    AVG(ST_X(geom)) AS center_lon,
    AVG(ST_Y(geom)) AS center_lat
FROM branches
WHERE status = 'ACTIVE'
GROUP BY grid_cell;

CREATE INDEX IF NOT EXISTS idx_branch_density_grid
ON branch_density_grid USING GIST(grid_cell);

-- Create function for optimized proximity search
CREATE OR REPLACE FUNCTION find_nearest_branches(
    search_lat DECIMAL,
    search_lon DECIMAL,
    radius_km DECIMAL DEFAULT 10.0,
    max_results INTEGER DEFAULT 10
)
RETURNS TABLE (
    branch_id VARCHAR,
    branch_name VARCHAR,
    distance_km DECIMAL,
    latitude DECIMAL,
    longitude DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        b.branch_id,
        b.branch_name,
        ROUND(
            ST_Distance(
                b.geom::geography,
                ST_SetSRID(ST_MakePoint(search_lon, search_lat), 4326)::geography
            ) / 1000.0,
            2
        ) AS distance_km,
        b.latitude,
        b.longitude
    FROM branches b
    WHERE
        b.status = 'ACTIVE'
        AND ST_DWithin(
            b.geom::geography,
            ST_SetSRID(ST_MakePoint(search_lon, search_lat), 4326)::geography,
            radius_km * 1000
        )
    ORDER BY b.geom <-> ST_SetSRID(ST_MakePoint(search_lon, search_lat), 4326)
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql STABLE PARALLEL SAFE;

-- Create function for batch proximity analysis
CREATE OR REPLACE FUNCTION analyze_branch_coverage(
    min_lat DECIMAL,
    max_lat DECIMAL,
    min_lon DECIMAL,
    max_lon DECIMAL,
    grid_size DECIMAL DEFAULT 0.1
)
RETURNS TABLE (
    cell_lat DECIMAL,
    cell_lon DECIMAL,
    branch_count BIGINT,
    avg_distance_km DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    WITH grid AS (
        SELECT
            generate_series(min_lat, max_lat, grid_size) AS lat,
            generate_series(min_lon, max_lon, grid_size) AS lon
    ),
    grid_points AS (
        SELECT
            lat,
            lon,
            ST_SetSRID(ST_MakePoint(lon, lat), 4326) AS point
        FROM grid
    )
    SELECT
        gp.lat AS cell_lat,
        gp.lon AS cell_lon,
        COUNT(b.branch_id) AS branch_count,
        ROUND(AVG(
            ST_Distance(
                gp.point::geography,
                b.geom::geography
            ) / 1000.0
        ), 2) AS avg_distance_km
    FROM grid_points gp
    LEFT JOIN branches b ON
        b.status = 'ACTIVE'
        AND ST_DWithin(
            gp.point::geography,
            b.geom::geography,
            10000  -- 10km radius
        )
    GROUP BY gp.lat, gp.lon
    ORDER BY cell_lat, cell_lon;
END;
$$ LANGUAGE plpgsql STABLE;

-- Analyze tables for query planner optimization
ANALYZE branches;
ANALYZE branch_density_grid;

-- Create statistics for better query planning
CREATE STATISTICS IF NOT EXISTS branches_geom_stats (dependencies)
ON geom, status, type FROM branches;

ANALYZE branches;

-- Performance verification query
-- Expected: sub-10ms for proximity search within 10km radius
EXPLAIN (ANALYZE, BUFFERS)
SELECT branch_id, branch_name,
       ST_Distance(geom::geography,
                   ST_SetSRID(ST_MakePoint(-46.633308, -23.550520), 4326)::geography) / 1000.0 AS distance_km
FROM branches
WHERE status = 'ACTIVE'
  AND ST_DWithin(geom::geography,
                 ST_SetSRID(ST_MakePoint(-46.633308, -23.550520), 4326)::geography,
                 10000)
ORDER BY geom <-> ST_SetSRID(ST_MakePoint(-46.633308, -23.550520), 4326)
LIMIT 10;

-- ============================================================================
-- Performance Benchmarks (Expected Results)
-- ============================================================================
-- Before optimization:
--   - Proximity search: ~150ms for 10,000 branches
--   - Full table scan with Haversine formula
--   - Query cost: ~2000 units
--
-- After optimization:
--   - Proximity search: <10ms for 10,000 branches
--   - Index scan with GIST
--   - Query cost: ~50 units
--
-- Improvement: 15x faster queries, 40x lower cost
-- ============================================================================
