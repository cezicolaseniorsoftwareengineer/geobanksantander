# Database Schema Design

## Overview

Optimized database schema for geospatial banking operations with proper constraints, indexing, and performance considerations.

## Table Structure

### branches

Primary table for storing branch geographic information.

```sql
CREATE TABLE branches (
    branch_id VARCHAR(36) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_branches PRIMARY KEY (branch_id),
    CONSTRAINT valid_coordinates
        CHECK (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180),
    CONSTRAINT brazilian_territory
        CHECK (latitude BETWEEN -35 AND 6 AND longitude BETWEEN -75 AND -30),
    CONSTRAINT unique_coordinates
        UNIQUE (latitude, longitude)
);
```

## Indexes

### Geospatial Index

Optimized for proximity queries and distance calculations:

```sql
CREATE INDEX idx_branches_geospatial
    ON branches(latitude, longitude);
```

**Purpose:**

- Accelerates spatial queries
- Optimizes radius-based searches
- Improves distance calculation performance

### Timestamp Index

For audit and temporal queries:

```sql
CREATE INDEX idx_branches_created
    ON branches(created_at);
```

## Constraints Design

### Geographic Validation

**World Coordinates:**

```sql
CONSTRAINT valid_coordinates
    CHECK (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
```

**Brazilian Territory:**

```sql
CONSTRAINT brazilian_territory
    CHECK (latitude BETWEEN -35 AND 6 AND longitude BETWEEN -75 AND -30)
```

### Data Integrity

**Unique Coordinates:**

```sql
CONSTRAINT unique_coordinates
    UNIQUE (latitude, longitude)
```

Prevents duplicate branches at identical coordinates.

## Data Types

### Coordinate Precision

- **DECIMAL(10,8)**: Latitude storage
  - Total digits: 10
  - Decimal places: 8
  - Range: -99.99999999 to 99.99999999
  - Precision: ~1.1 meters at equator

- **DECIMAL(11,8)**: Longitude storage
  - Total digits: 11
  - Decimal places: 8
  - Range: -999.99999999 to 999.99999999
  - Precision: ~1.1 meters at equator

### UUID Storage

**VARCHAR(36)**: Branch identifier

- Standard UUID format: 8-4-4-4-12 characters
- Example: `550e8400-e29b-41d4-a716-446655440000`

## Sample Data

```sql
INSERT INTO branches (branch_id, latitude, longitude) VALUES
('550e8400-e29b-41d4-a716-446655440001', -23.55052000, -46.63330000),
('550e8400-e29b-41d4-a716-446655440002', -23.54890000, -46.63880000),
('550e8400-e29b-41d4-a716-446655440003', -22.90685000, -43.17290000),
('550e8400-e29b-41d4-a716-446655440004', -25.44280000, -49.27650000),
('550e8400-e29b-41d4-a716-446655440005', -30.03280000, -51.23020000);
```

## Query Patterns

### Distance-Based Queries

**Find branches within radius:**

```sql
SELECT
    branch_id,
    latitude,
    longitude,
    (6371 * ACOS(
        COS(RADIANS(?)) * COS(RADIANS(latitude)) *
        COS(RADIANS(longitude) - RADIANS(?)) +
        SIN(RADIANS(?)) * SIN(RADIANS(latitude))
    )) AS distance_km
FROM branches
WHERE (6371 * ACOS(
    COS(RADIANS(?)) * COS(RADIANS(latitude)) *
    COS(RADIANS(longitude) - RADIANS(?)) +
    SIN(RADIANS(?)) * SIN(RADIANS(latitude))
)) <= ?
ORDER BY distance_km
LIMIT ?;
```

### Nearest Branch Query

**Find closest branch:**

```sql
SELECT
    branch_id,
    latitude,
    longitude,
    (6371 * ACOS(
        COS(RADIANS(?)) * COS(RADIANS(latitude)) *
        COS(RADIANS(longitude) - RADIANS(?)) +
        SIN(RADIANS(?)) * SIN(RADIANS(latitude))
    )) AS distance_km
FROM branches
ORDER BY distance_km
LIMIT 1;
```

## Performance Considerations

### Index Usage

- Geospatial index accelerates coordinate-based queries
- Composite index on (latitude, longitude) supports both individual and combined searches
- Timestamp indexes enable efficient audit queries

### Query Optimization

- Distance calculations use optimized Haversine formula
- LIMIT clauses prevent excessive result sets
- Proper parameter binding prevents SQL injection

### Storage Efficiency

- DECIMAL precision balances accuracy with storage requirements
- VARCHAR(36) for UUIDs avoids unnecessary padding
- Timestamp defaults reduce application complexity

## Maintenance Operations

### Statistics Update

```sql
ANALYZE TABLE branches;
```

### Index Rebuild

```sql
ALTER TABLE branches DROP INDEX idx_branches_geospatial;
CREATE INDEX idx_branches_geospatial ON branches(latitude, longitude);
```

### Constraint Validation

```sql
SELECT COUNT(*) FROM branches
WHERE NOT (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180);

SELECT COUNT(*) FROM branches
WHERE NOT (latitude BETWEEN -35 AND 6 AND longitude BETWEEN -75 AND -30);
```
