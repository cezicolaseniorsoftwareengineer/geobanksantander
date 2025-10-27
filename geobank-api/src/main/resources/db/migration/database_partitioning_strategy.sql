

-- Enable partitioning extension
CREATE EXTENSION IF NOT EXISTS pg_partman;



-- Parent table: branch audit logs
CREATE TABLE branch_audit_log (
    id BIGSERIAL,
    branch_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    user_id VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for current and future months
DO $$
DECLARE
    start_date DATE := DATE_TRUNC('month', CURRENT_DATE - INTERVAL '6 months');
    end_date DATE := DATE_TRUNC('month', CURRENT_DATE + INTERVAL '12 months');
    partition_date DATE;
    partition_name TEXT;
BEGIN
    partition_date := start_date;
    WHILE partition_date < end_date LOOP
        partition_name := 'branch_audit_log_' || TO_CHAR(partition_date, 'YYYY_MM');
        EXECUTE format('CREATE TABLE %I PARTITION OF branch_audit_log
                       FOR VALUES FROM (%L) TO (%L)',
                      partition_name,
                      partition_date,
                      partition_date + INTERVAL '1 month');

    -- Indexes for each partition
        EXECUTE format('CREATE INDEX %I ON %I (branch_id, created_at)',
                      partition_name || '_branch_created_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (event_type, created_at)',
                      partition_name || '_event_created_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I USING GIN (event_data)',
                      partition_name || '_event_data_idx',
                      partition_name);

        partition_date := partition_date + INTERVAL '1 month';
    END LOOP;
END $$;



-- Parent table for transaction history
CREATE TABLE transaction_history (
    id BIGSERIAL,
    transaction_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    description TEXT,
    reference_number VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, account_id)
) PARTITION BY HASH (account_id);

-- Create hash partitions (8 partitions for balanced distribution)
DO $$
DECLARE
    i INTEGER;
    partition_name TEXT;
BEGIN
    FOR i IN 0..7 LOOP
        partition_name := 'transaction_history_' || i;
        EXECUTE format('CREATE TABLE %I PARTITION OF transaction_history
                       FOR VALUES WITH (modulus 8, remainder %s)',
                      partition_name, i);

        -- Create indexes on each partition
        EXECUTE format('CREATE INDEX %I ON %I (transaction_id)',
                      partition_name || '_transaction_id_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (account_id, created_at)',
                      partition_name || '_account_created_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (status, created_at)',
                      partition_name || '_status_created_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (reference_number)',
                      partition_name || '_reference_idx',
                      partition_name);
    END LOOP;
END $$;

-- =====================================================================
-- GEOSPATIAL QUERY LOG PARTITIONING (Range by Date)
-- =====================================================================

-- Parent table for geospatial query performance tracking
CREATE TABLE geospatial_query_log (
    id BIGSERIAL,
    query_type VARCHAR(100) NOT NULL,
    execution_time_ms INTEGER NOT NULL,
    query_parameters JSONB,
    result_count INTEGER,
    user_location GEOMETRY(POINT, 4674),
    cache_hit BOOLEAN DEFAULT FALSE,
    api_endpoint VARCHAR(200),
    response_size_bytes INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create weekly partitions for query logs (more granular for performance analysis)
DO $$
DECLARE
    start_date DATE := DATE_TRUNC('week', CURRENT_DATE - INTERVAL '4 weeks');
    end_date DATE := DATE_TRUNC('week', CURRENT_DATE + INTERVAL '8 weeks');
    partition_date DATE;
    partition_name TEXT;
BEGIN
    partition_date := start_date;
    WHILE partition_date < end_date LOOP
        partition_name := 'geospatial_query_log_' || TO_CHAR(partition_date, 'YYYY_"W"WW');
        EXECUTE format('CREATE TABLE %I PARTITION OF geospatial_query_log
                       FOR VALUES FROM (%L) TO (%L)',
                      partition_name,
                      partition_date,
                      partition_date + INTERVAL '1 week');

        -- Create specialized indexes for geospatial queries
        EXECUTE format('CREATE INDEX %I ON %I (query_type, execution_time_ms)',
                      partition_name || '_query_perf_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I USING GIST (user_location)',
                      partition_name || '_location_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (cache_hit, created_at)',
                      partition_name || '_cache_created_idx',
                      partition_name);

        partition_date := partition_date + INTERVAL '1 week';
    END LOOP;
END $$;

-- =====================================================================
-- COMPLIANCE DOCUMENT STORAGE PARTITIONING (Range by Date)
-- =====================================================================

-- Parent table for compliance documents with regulatory retention
CREATE TABLE compliance_documents (
    id BIGSERIAL,
    document_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL, -- branch_id, account_id, customer_id
    entity_type VARCHAR(50) NOT NULL,
    document_hash VARCHAR(128) NOT NULL, -- SHA-512 for integrity
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    retention_period_years INTEGER NOT NULL DEFAULT 7,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create yearly partitions for compliance documents
DO $$
DECLARE
    start_year INTEGER := EXTRACT(YEAR FROM CURRENT_DATE) - 2;
    end_year INTEGER := EXTRACT(YEAR FROM CURRENT_DATE) + 5;
    year_val INTEGER;
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
BEGIN
    year_val := start_year;
    WHILE year_val <= end_year LOOP
        partition_name := 'compliance_documents_' || year_val;
        start_date := DATE(year_val || '-01-01');
        end_date := DATE((year_val + 1) || '-01-01');

        EXECUTE format('CREATE TABLE %I PARTITION OF compliance_documents
                       FOR VALUES FROM (%L) TO (%L)',
                      partition_name,
                      start_date,
                      end_date);

        -- Create indexes for compliance queries
        EXECUTE format('CREATE INDEX %I ON %I (entity_id, entity_type)',
                      partition_name || '_entity_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (document_type, created_at)',
                      partition_name || '_type_created_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (document_hash)',
                      partition_name || '_hash_idx',
                      partition_name);

        EXECUTE format('CREATE INDEX %I ON %I (expires_at)',
                      partition_name || '_expires_idx',
                      partition_name);

        year_val := year_val + 1;
    END LOOP;
END $$;

-- =====================================================================
-- AUTOMATED MAINTENANCE PROCEDURES
-- =====================================================================

-- Function to create new monthly partitions automatically
CREATE OR REPLACE FUNCTION create_monthly_partitions()
RETURNS VOID AS $$
DECLARE
    next_month DATE := DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month');
    partition_name TEXT;
BEGIN
    -- Branch audit log partition
    partition_name := 'branch_audit_log_' || TO_CHAR(next_month, 'YYYY_MM');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF branch_audit_log
                   FOR VALUES FROM (%L) TO (%L)',
                  partition_name,
                  next_month,
                  next_month + INTERVAL '1 month');

    -- Create indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (branch_id, created_at)',
                  partition_name || '_branch_created_idx', partition_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (event_type, created_at)',
                  partition_name || '_event_created_idx', partition_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I USING GIN (event_data)',
                  partition_name || '_event_data_idx', partition_name);

    RAISE NOTICE 'Created monthly partition: %', partition_name;
END;
$$ LANGUAGE plpgsql;

-- Function to create new weekly partitions for geospatial logs
CREATE OR REPLACE FUNCTION create_weekly_partitions()
RETURNS VOID AS $$
DECLARE
    next_week DATE := DATE_TRUNC('week', CURRENT_DATE + INTERVAL '1 week');
    partition_name TEXT;
BEGIN
    partition_name := 'geospatial_query_log_' || TO_CHAR(next_week, 'YYYY_"W"WW');
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF geospatial_query_log
                   FOR VALUES FROM (%L) TO (%L)',
                  partition_name,
                  next_week,
                  next_week + INTERVAL '1 week');

    -- Create specialized indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (query_type, execution_time_ms)',
                  partition_name || '_query_perf_idx', partition_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I USING GIST (user_location)',
                  partition_name || '_location_idx', partition_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (cache_hit, created_at)',
                  partition_name || '_cache_created_idx', partition_name);

    RAISE NOTICE 'Created weekly partition: %', partition_name;
END;
$$ LANGUAGE plpgsql;

-- Function to drop old partitions based on retention policy
CREATE OR REPLACE FUNCTION cleanup_old_partitions()
RETURNS VOID AS $$
DECLARE
    partition_rec RECORD;
    retention_cutoff DATE;
BEGIN
    -- Cleanup branch audit logs older than 2 years
    retention_cutoff := CURRENT_DATE - INTERVAL '2 years';

    FOR partition_rec IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE 'branch_audit_log_%'
          AND tablename < 'branch_audit_log_' || TO_CHAR(retention_cutoff, 'YYYY_MM')
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I.%I',
                      partition_rec.schemaname, partition_rec.tablename);
        RAISE NOTICE 'Dropped old partition: %', partition_rec.tablename;
    END LOOP;

    -- Cleanup geospatial query logs older than 6 months
    retention_cutoff := CURRENT_DATE - INTERVAL '6 months';

    FOR partition_rec IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE 'geospatial_query_log_%'
          AND tablename < 'geospatial_query_log_' || TO_CHAR(retention_cutoff, 'YYYY_"W"WW')
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I.%I',
                      partition_rec.schemaname, partition_rec.tablename);
        RAISE NOTICE 'Dropped old partition: %', partition_rec.tablename;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- SCHEDULED MAINTENANCE JOBS
-- =====================================================================

-- Schedule automatic partition creation (requires pg_cron extension)
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Run monthly partition creation on first day of each month at 2 AM
-- SELECT cron.schedule('create-monthly-partitions', '0 2 1 * *', 'SELECT create_monthly_partitions();');

-- Run weekly partition creation every Monday at 2 AM
-- SELECT cron.schedule('create-weekly-partitions', '0 2 * * 1', 'SELECT create_weekly_partitions();');

-- Run cleanup on first day of each month at 3 AM
-- SELECT cron.schedule('cleanup-old-partitions', '0 3 1 * *', 'SELECT cleanup_old_partitions();');

-- =====================================================================
-- PARTITION MONITORING VIEWS
-- =====================================================================

-- View to monitor partition sizes and row counts
CREATE OR REPLACE VIEW partition_statistics AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(quote_ident(schemaname)||'.'||quote_ident(tablename))) as size,
    (SELECT reltuples::BIGINT FROM pg_class WHERE relname = tablename) as estimated_rows,
    CASE
        WHEN tablename LIKE '%audit_log_%' THEN 'audit'
        WHEN tablename LIKE '%transaction_history_%' THEN 'transaction'
        WHEN tablename LIKE '%query_log_%' THEN 'geospatial'
        WHEN tablename LIKE '%compliance_%' THEN 'compliance'
        ELSE 'other'
    END as partition_type
FROM pg_tables
WHERE tablename LIKE '%audit_log_%'
   OR tablename LIKE '%transaction_history_%'
   OR tablename LIKE '%query_log_%'
   OR tablename LIKE '%compliance_documents_%'
ORDER BY pg_total_relation_size(quote_ident(schemaname)||'.'||quote_ident(tablename)) DESC;

-- View to monitor partition performance
CREATE OR REPLACE VIEW partition_performance AS
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    ROUND((idx_scan::numeric / GREATEST(seq_scan + idx_scan, 1)) * 100, 2) as index_usage_percent
FROM pg_stat_user_tables
WHERE relname LIKE '%audit_log_%'
   OR relname LIKE '%transaction_history_%'
   OR relname LIKE '%query_log_%'
   OR relname LIKE '%compliance_documents_%'
ORDER BY seq_scan DESC;
