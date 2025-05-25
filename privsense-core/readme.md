# PrivSense Core Module

## Overview

The privsense-core module serves as the foundation of the PrivSense system, providing essential domain models, interfaces, and utilities that are shared across other modules. It establishes the core business logic, data structures, and contracts that define the functionality of the PrivSense PII (Personally Identifiable Information) detection and management system.

## Architecture

The privsense-core module follows a clean architecture approach with clear separation of concerns:

- **Domain Models**: Entity objects representing the core business concepts
- **Service Interfaces**: Contracts defining system behaviors and operations
- **Repository Interfaces**: Data access abstractions following the Repository pattern
- **Exception Classes**: Structured exception hierarchy for robust error handling
- **Utility Classes**: Helper components for common operations

## Key Components

### Domain Models

#### Entity Models

* **ScanMetadata**: Represents metadata for a database scan operation, including status, timing information, and summary statistics. Tracks the lifecycle of a scan from initiation to completion.

* **DetectionResult**: Contains information about detected PII within database columns, including confidence scores and detection method.

* **DatabaseConnectionInfo**: Stores connection details for databases, with security features for handling credentials. Supports different database types.

* **ComplianceReport**: Final output of the PII scanning process with detailed findings and statistics. Used for regulatory compliance documentation.

* **PiiCandidate**: Represents a potential PII entity found during scanning with confidence metrics and context.

* **RelationshipInfo**: Describes foreign key relationships between database tables for context-aware PII detection.

* **ScanContext**: Contains all context and results from a database scan operation, serving as a comprehensive record.

* **DetectionConfig**: Configuration settings controlling PII detection behavior and thresholds.

* **ColumnInfo**: Metadata about database columns including data type, constraints, and statistics.

* **TableInfo**: Metadata about database tables including schema location and relationships.

* **SchemaInfo**: Representation of database schema structure and organization.

* **SampleData**: Collection of data samples extracted from database columns for analysis.

* **SamplingConfig**: Configuration for data sampling strategies and parameters.

* **Role**: User role definitions for authorization and access control.

* **User**: User account information for authentication and authorization.

* **ScanTemplate**: Reusable scan configuration templates for consistent scanning.

* **QuasiIdentifierColumnMapping**: Maps columns that together could identify individuals.

* **CorrelatedQuasiIdentifierGroup**: Groups of columns with correlation as potential quasi-identifiers.

* **DetectionRule**: Rules for PII detection based on patterns, keywords, or algorithms.

### Services

* **ScanPersistenceService**: Interface for persisting and retrieving scan data. Manages the lifecycle of scan records and associated detection results.

* **ScanExecutionService**: Handles the execution of database scans. Orchestrates the scanning process from connection to reporting.

* **PiiDetector**: Interface for different strategies to detect PII in data samples. The main contract for PII detection operations.

* **PiiDetectionStrategy**: Contract for implementing specific PII detection algorithms (pattern-based, ML-based, etc.).

* **MetadataExtractor**: Extracts database schema and structure information. Maps database structures to domain models.

* **ScanJobManagementService**: Manages the lifecycle of scan jobs including submission, scheduling, and status tracking.

* **DatabaseConnector**: Provides database connection management capabilities with connection pooling and security.

* **ScanReportService**: Generates reports from scan results with formatting and customization options.

* **ReportExportService**: Exports reports in various formats (PDF, CSV, JSON, etc.) for different consumer needs.

* **UserService**: Handles user management operations including authentication, authorization, and profile management.

* **NotificationService**: Manages notifications about scan progress, completion, and important findings.

### Repositories

* **ScanMetadataRepository**: Interface for scan metadata persistence operations.

* **DetectionResultRepository**: Interface for detection results persistence and retrieval.

* **PiiCandidateRepository**: Interface for PII candidate persistence and tracking.

* **ColumnInfoRepository**: Interface for column metadata persistence and lookup.

* **SchemaInfoRepository**: Interface for schema metadata persistence and querying.

* **TableInfoRepository**: Interface for table metadata persistence and querying.

* **RelationshipInfoRepository**: Interface for relationship information persistence.

* **ConnectionRepository**: Interface for database connection configuration persistence.

* **UserRepository**: Interface for user data persistence and authentication.

* **RoleRepository**: Interface for user role persistence and authorization.

* **ComplianceReportRepository**: Interface for storing and retrieving compliance reports.

* **ScanTemplateRepository**: Interface for managing reusable scan templates.

* **CorrelatedQuasiIdentifierGroupRepository**: Interface for managing groups of correlated quasi-identifiers.

### Exception Handling

* **PrivSenseException**: Base exception class for all application exceptions.

* **ResourceNotFoundException**: Thrown when a requested resource doesn't exist.

* **ValidationException**: Contains detailed field-level validation errors for input validation.

* **AccessDeniedException**: Thrown when a user lacks required permissions for an operation.

* **ConfigurationException**: Indicates issues with application configuration parameters.

* **DatabaseConnectionException**: Indicates problems establishing database connections.

* **DataSamplingException**: Indicates issues during data sampling operations.

* **MetadataExtractionException**: Indicates problems extracting database metadata.

* **PiiDetectionException**: Indicates issues during PII detection processing.

* **ReportGenerationException**: Indicates problems generating compliance reports.

### Utilities

* **ExceptionUtils**: Provides helper methods for consistent exception handling and error reporting.

## Technical Details

### Entity Relationships

The core module establishes several important entity relationships:

* **ScanMetadata** ↔ **DetectionResult**: One-to-many relationship representing PII findings within a scan.
* **ComplianceReport** ↔ **DetectionResult**: One-to-many relationship capturing all findings in a report.
* **ScanMetadata** → **DatabaseConnectionInfo**: Many-to-one relationship linking scans to database connections.
* **TableInfo** ↔ **ColumnInfo**: One-to-many relationship mapping tables to their columns.
* **SchemaInfo** ↔ **TableInfo**: One-to-many relationship mapping schemas to their tables.
* **User** ↔ **Role**: Many-to-many relationship for user authorization.
* **ColumnInfo** → **PiiCandidate**: One-to-many relationship mapping columns to potential PII.
* **RelationshipInfo** → **TableInfo**: Many-to-many relationship describing table relationships.

### PII Detection Strategy Pattern

The module implements the Strategy Pattern for PII detection:

```
PiiDetector (Interface)
  ↓
PiiDetectionStrategy (Interface)
  ↓
ConcreteDetectionStrategies (Implementations)
```

This allows for plugging in different detection algorithms while maintaining a consistent interface.

### Scan Status Lifecycle

ScanMetadata tracks scan status through a well-defined lifecycle:

```
PENDING → EXTRACTING_METADATA → SAMPLING → DETECTING_PII → GENERATING_REPORT → COMPLETED
                                                                            ↘
                                                                              FAILED
                                                                            ↗
                                                        CANCELLED ←
```

### Configuration Structure

The DetectionConfig class provides a centralized configuration approach with nested configuration classes:

* **Pattern Detection**: Controls regex pattern matching settings
* **NER Settings**: Named Entity Recognition service configuration
* **Thresholds**: Confidence thresholds for PII classification
* **Sampling Parameters**: Data sampling rates and methods
* **Reporting Options**: Report generation settings

## Usage Examples

### Creating a Scan

```java
ScanMetadata scan = scanPersistenceService.createScan(
    connectionId,
    databaseName,
    databaseProductName,
    databaseProductVersion
);
```

### Retrieving Scan Results

```java
List<DetectionResult> results = scanPersistenceService.getDetectionResultsByScanId(scanId);
```

### Submitting a Scan Job

```java
UUID jobId = scanJobManagementService.submitScanJob(scanRequest);
```

### Building a Database Connection

```java
String jdbcUrl = databaseConnectionInfo.buildJdbcUrl();
```

### Processing Detection Results

```java
List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(scanId);
ComplianceReport report = scanReportService.generateReport(scanId, piiResults);
scanPersistenceService.saveReport(scanId, report);
```

### Finding Recent Scans

```java
List<ScanMetadata> recentScans = scanPersistenceService.getRecentScans(10);
```

### Filtering Scans by Status

```java
List<ScanMetadata> failedScans = scanPersistenceService.getScansByStatus(ScanStatus.FAILED);
```

## Security Considerations

The core module includes several security-focused features:

* Password masking in toString() methods
* Exception types that avoid leaking sensitive information
* Clear separation of authentication and authorization concerns
* Secure handling of database credentials
* Support for encrypted configuration values
* Audit logging for security-relevant operations
* Input validation for all external data

## Integration Points

The core module is designed to integrate with:

* **privsense-api**: Exposes functionality through REST endpoints
* **privsense-pii-detector**: Implements PII detection strategies
* **privsense-db-connector**: Provides database connectivity
* **privsense-sampler**: Handles data sampling for efficient scanning
* **privsense-metadata-extractor**: Extracts database schema information
* **privsense-reporter**: Generates compliance reports
* **ner-service**: External Named Entity Recognition service for PII detection

## Dependencies

* **Jakarta Persistence API**: For entity mapping and persistence
* **Lombok**: For boilerplate reduction in entity and data classes
* **Hibernate**: For JPA implementation
* **Spring Data JPA**: For repository abstractions
* **Spring Validation**: For property validation
* **Jackson**: For JSON serialization/deserialization
* **HikariCP**: For database connection pooling
* **PostgreSQL JDBC**: Database connectivity

## Best Practices

* Use ExceptionUtils for consistent exception handling
* Always respect bidirectional relationships when setting entity associations
* Ensure proper sensitive data handling (passwords, connection details)
* Follow the Repository pattern for data access
* Validate inputs at service boundaries
* Use DTOs for transferring data across module boundaries
* Implement proper transaction management
* Use appropriate index strategies for database entities
* Cache frequently accessed reference data
* Implement appropriate logging for troubleshooting and auditing

## Future Enhancements

* Enhanced support for NoSQL databases
* Integration with cloud-native PII detection services
* Support for automated remediation of PII issues
* Machine learning enhancements for detection accuracy
* Expanded compliance report templates for different regulations
* Performance optimizations for large database scans
* Integration with data catalog systems
* Support for real-time PII detection in data streams

## Contributing

When contributing to the core module, follow these guidelines:

* Maintain backward compatibility
* Add comprehensive tests for new features
* Document public APIs with JavaDoc
* Follow the established exception hierarchy
* Respect the domain model integrity