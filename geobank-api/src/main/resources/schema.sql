



DROP TABLE IF EXISTS branches;



CREATE TABLE branches (
    branch_id VARCHAR(36) PRIMARY KEY,
    branch_code VARCHAR(10) UNIQUE NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    full_address VARCHAR(500) NOT NULL,
    city_name VARCHAR(100) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Coordinate validation
    CONSTRAINT valid_latitude CHECK (latitude BETWEEN -90.0 AND 90.0),
    CONSTRAINT valid_longitude CHECK (longitude BETWEEN -180.0 AND 180.0),
    -- Brazilian territory constraint
    CONSTRAINT brazilian_territory CHECK (
        latitude BETWEEN -35.0 AND 6.0 AND
        longitude BETWEEN -75.0 AND -30.0
    ),
    -- State and branch code uppercase enforcement
    CONSTRAINT valid_state_code CHECK (state_code = UPPER(state_code)),
    CONSTRAINT valid_branch_code CHECK (branch_code = UPPER(branch_code))
);



CREATE INDEX idx_branches_geospatial ON branches(latitude, longitude);
CREATE INDEX idx_branches_city ON branches(city_name);
CREATE INDEX idx_branches_state ON branches(state_code);
CREATE INDEX idx_branches_code ON branches(branch_code);
