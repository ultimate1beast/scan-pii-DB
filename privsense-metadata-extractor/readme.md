# PrivSense Metadata Extractor

## Overview
The PrivSense Metadata Extractor module is responsible for extracting database schema metadata from various database management systems. This module is a critical component of the PrivSense CGI platform that provides detailed information about database structures, including tables, columns, relationships, and metadata such as comments. The extracted metadata is essential for other components of the system to understand the structure and context of the data they're analyzing.

## Features
- Extracts comprehensive database schema metadata including:
  - Tables and views
  - Columns with their data types and constraints
  - Primary keys
  - Foreign key relationships
  - Table and column comments
- Database-specific implementations for:
  - MySQL
  - PostgreSQL
  - Extensible architecture for adding support for additional database types
- Integration with Spring framework
- Standard JDBC-based extraction with enhanced database-specific metadata capabilities

## Architecture

### Core Components

#### MetadataExtractor Interface
The core service interface implemented by this module. It provides methods to extract database metadata through a JDBC connection.

#### JdbcMetadataExtractorImpl
Main implementation of the MetadataExtractor interface, using JDBC DatabaseMetaData and enhanced with database-specific scanners. This service:
1. Connects to databases using JDBC
2. Extracts basic metadata using standard JDBC DatabaseMetaData
3. Detects the database type
4. Applies database-specific scanners for enhanced metadata extraction
5. Returns structured schema information

#### Database-Specific Scanners
The module follows the Strategy pattern with database-specific scanners that implement the DbSpecificMetadataScanner interface:

1. **DbSpecificMetadataScanner**: Interface defining methods for database-specific metadata extraction
2. **MySqlMetadataScanner**: MySQL-specific implementation that extracts comments and relationships from MySQL's information_schema
3. **PostgreSqlMetadataScanner**: PostgreSQL-specific implementation that extracts comments and relationships from PostgreSQL's system catalogs and information schema

### Data Models
The module uses data models defined in the privsense-core module:

- **SchemaInfo**: Represents a database schema containing tables
- **TableInfo**: Represents a database table with columns, primary keys, and relationships
- **ColumnInfo**: Represents a table column with metadata (type, constraints, comments)
- **RelationshipInfo**: Represents foreign key relationships between tables

## Dependencies

### Internal Dependencies
- privsense-core: Core models and interfaces
- privsense-db-connector: Database connectivity components

### External Dependencies
- Spring Framework: For dependency injection and component management
- Apache Commons Lang3: Utility functions
- JDBC drivers (provided at runtime)

## Integration

### Spring Integration
The module integrates with Spring framework by using the following annotations:
- @Service: Marking service implementations
- @Component: Marking database-specific scanners
- @Autowired: For dependency injection

### Usage Example
```java
// Get the metadata extractor service
@Autowired
private MetadataExtractor metadataExtractor;

// Get a database connection
Connection connection = databaseConnector.getConnection();

// Extract complete schema metadata
SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);

// Or extract metadata for specific tables only
List<String> tableNames = Arrays.asList("customers", "orders");
SchemaInfo specificSchemaInfo = metadataExtractor.extractMetadataForTables(connection, tableNames);

// Get columns for a specific table
List<String> columns = metadataExtractor.getTableColumns(connection, "customers");
```

## Key Functionality Details

### MySQL Metadata Extraction
- Table comments: Extracted from `information_schema.TABLES.TABLE_COMMENT`
- Column comments: Extracted from `information_schema.COLUMNS.COLUMN_COMMENT`
- Foreign key relationships: Extracted by joining `information_schema.KEY_COLUMN_USAGE` with `REFERENTIAL_CONSTRAINTS`

### PostgreSQL Metadata Extraction
- Table comments: Extracted from `pg_description` joined with `pg_class` and `pg_namespace`
- Column comments: Extracted from `pg_description` joined with `pg_attribute` and related catalog tables
- Foreign key relationships: Extracted from `information_schema.referential_constraints` and `key_column_usage`

## Extensibility
The module is designed for extensibility in the following ways:

1. **Support for New Database Types**: Implement the `DbSpecificMetadataScanner` interface for a new database type and register it as a Spring component
2. **Enhanced Metadata Extraction**: Existing scanners can be extended to extract additional database-specific metadata

## Error Handling
- Uses a custom `MetadataExtractionException` for error reporting
- Comprehensive logging using SLF4J
- Detailed debug logs for troubleshooting extraction issues

## Best Practices Implemented
- Interface-based design for flexibility and testability
- Strategy pattern for database-specific implementations
- Comprehensive logging
- Proper resource management with try-with-resources
- SQL injection prevention using parameterized queries