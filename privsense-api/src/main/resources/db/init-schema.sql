-- Create database if it doesn't exist (PostgreSQL)
-- This needs to be run manually as a superuser
-- CREATE DATABASE privsense;

-- Schema initialization script
-- This will be automatically executed if you set spring.sql.init.mode=always in application.yml
-- Make sure to create the database first before running this script

-- Tables for metadata (schemas, tables, columns)
CREATE TABLE IF NOT EXISTS schemas (
    id UUID PRIMARY KEY,
    schema_name VARCHAR(255),
    catalog_name VARCHAR(255),
    scan_id UUID
);

CREATE TABLE IF NOT EXISTS tables (
    id UUID PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    table_type VARCHAR(50),
    remarks TEXT,
    schema_id UUID REFERENCES schemas(id)
);

CREATE TABLE IF NOT EXISTS columns (
    id UUID PRIMARY KEY,
    column_name VARCHAR(255) NOT NULL,
    jdbc_type INTEGER,
    database_type_name VARCHAR(100),
    comments TEXT,
    size INTEGER,
    precision INTEGER,
    scale INTEGER,
    nullable BOOLEAN,
    primary_key BOOLEAN,
    table_id UUID REFERENCES tables(id)
);

-- Relationships between tables
CREATE TABLE IF NOT EXISTS relationships (
    id UUID PRIMARY KEY,
    relationship_name VARCHAR(255),
    constraint_name VARCHAR(255),
    update_rule VARCHAR(50),
    delete_rule VARCHAR(50),
    deferrable SMALLINT,
    initially_deferred SMALLINT,
    source_table_id UUID REFERENCES tables(id),
    source_column_id UUID REFERENCES columns(id),
    target_table_id UUID REFERENCES tables(id),
    target_column_id UUID REFERENCES columns(id)
);

-- Scan tracking tables
CREATE TABLE IF NOT EXISTS scans (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    database_name VARCHAR(255),
    database_product_name VARCHAR(100),
    database_product_version VARCHAR(100),
    total_tables_scanned INTEGER,
    total_columns_scanned INTEGER,
    total_pii_columns_found INTEGER,
    error_message TEXT
);

-- Detection results tables
CREATE TABLE IF NOT EXISTS detection_results (
    id UUID PRIMARY KEY,
    highest_confidence_pii_type VARCHAR(100),
    highest_confidence_score DECIMAL(5,4),
    scan_id UUID REFERENCES scans(id),
    column_id UUID REFERENCES columns(id)
);

CREATE TABLE IF NOT EXISTS detection_methods (
    result_id UUID REFERENCES detection_results(id),
    method_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (result_id, method_name)
);

-- PII findings
CREATE TABLE IF NOT EXISTS pii_candidates (
    id UUID PRIMARY KEY,
    pii_type VARCHAR(100) NOT NULL,
    confidence_score DECIMAL(5,4) NOT NULL,
    detection_method VARCHAR(50) NOT NULL,
    evidence TEXT,
    result_id UUID REFERENCES detection_results(id),
    column_id UUID REFERENCES columns(id)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_scans_connection_id ON scans(connection_id);
CREATE INDEX IF NOT EXISTS idx_scans_status ON scans(status);
CREATE INDEX IF NOT EXISTS idx_detection_results_scan_id ON detection_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_detection_results_confidence ON detection_results(highest_confidence_score DESC);
CREATE INDEX IF NOT EXISTS idx_pii_candidates_pii_type ON pii_candidates(pii_type);
CREATE INDEX IF NOT EXISTS idx_pii_candidates_detection_method ON pii_candidates(detection_method);
CREATE INDEX IF NOT EXISTS idx_pii_candidates_result_id ON pii_candidates(result_id);