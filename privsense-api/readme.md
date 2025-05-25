# PrivSense API Module Documentation

## Overview

The PrivSense API module serves as the RESTful interface for the PrivSense-CGI application, a comprehensive solution for detecting and managing Personally Identifiable Information (PII) in database systems. This Spring Boot application provides endpoints for database connection management, scanning databases for PII, generating compliance reports, and exporting findings in various formats.

## Architecture

### Core Components

The API module follows a layered architecture with the following components:

1. **Controllers**: REST endpoints for client interactions
2. **Services**: Business logic implementation
3. **Repositories**: Data access layer for database operations
4. **Mappers**: Object transformation between domain models and DTOs
5. **Security**: JWT-based authentication and authorization
6. **Filters & Interceptors**: Request processing and monitoring
7. **Configuration**: Application setup and bean definitions
8. **Exception Handling**: Global exception handling and custom exceptions
9. **Validation**: Input validation mechanisms
10. **Models**: Entity models and DTOs

### Integration with Other Modules

The API integrates with several internal modules:

- `privsense-core`: Core domain models and interfaces
- `privsense-db-connector`: Database connection management
- `privsense-metadata-extractor`: Database schema and metadata extraction
- `privsense-sampler`: Database sampling functionality
- `privsense-pii-detector`: PII detection algorithms
- `privsense-reporter`: Report generation capabilities

## Key Features

### Database Connection Management

- Create, view, and delete database connections
- Support for multiple database types (PostgreSQL, MySQL, SQL Server, Oracle)
- Secure credential management
- Metadata extraction from connected databases

### PII Detection and Scanning

- Asynchronous scan execution with real-time status updates
- Configurable sampling strategies for large data sets
- Multi-stage PII detection pipeline:
  - Metadata extraction
  - Data sampling
  - PII detection (using regex patterns, heuristics, NER)
  - Quasi-identifier detection through correlation analysis
  - Report generation

### Report Generation and Export

- Detailed compliance reports with PII findings
- Export reports in multiple formats (JSON, CSV, PDF, Excel, HTML, Text)
- Custom report formatting options
- Statistical information about detected PII types

### WebSocket Notifications

- Real-time scan status updates
- Progress tracking for long-running operations
- Client notification for completed scans
- Topic-based subscription system for targeted updates

### Security

- JWT token-based authentication and authorization
- Customizable token expiration and validation
- Role-based access control
- Secure password handling with BCrypt encoding
- CORS configuration for frontend integration
- Protection against common web vulnerabilities

## API Endpoints

The API is accessible under the base path `/privsense/api/v1/`:

### Authentication

- `POST /auth/login`: Authenticate user and receive JWT token
- `POST /auth/refresh`: Refresh an existing JWT token
- `GET /auth/validate`: Validate token status

### Connection Management

- `POST /connections`: Create a new database connection
- `GET /connections`: List all database connections
- `GET /connections/{connectionId}`: Get connection details
- `GET /connections/{connectionId}/metadata`: Get database schema metadata
- `DELETE /connections/{connectionId}`: Close and delete a connection

### Scan Management

- `POST /scans`: Submit a new scan job
- `GET /scans`: List all scan jobs with pagination and filtering
- `GET /scans/{jobId}`: Get status of a specific scan job
- `DELETE /scans/{jobId}`: Cancel an in-progress scan
- `GET /scans/{jobId}/report`: Get the scan report
- `GET /scans/{jobId}/statistics`: Get scan statistics
- `GET /scans/{jobId}/tables`: Get list of scanned tables
- `GET /scans/{jobId}/tables/{tableName}/columns`: Get columns from a scanned table

### Report Export

- `GET /scans/{jobId}/export/{format}`: Export the report in specified format
- `POST /scans/{jobId}/export/{format}`: Export with custom options
- Supported export formats include:
  - `json`: JSON format for programmatic consumption
  - `csv`: CSV format for spreadsheet applications
  - `pdf`: PDF format for formal reporting
  - `text`: Plain text format for simple viewing
  - `html`: HTML format for web display
  - `excel`: Excel format for data analysis

### Sampling

- `POST /sampling`: Sample a specific column from a database
- `POST /sampling/batch`: Batch sampling for multiple columns
- `GET /sampling/configuration`: Retrieve current sampling configuration

### User Management

- `GET /users`: Get all users (admin only)
- `GET /users/{id}`: Get user by ID (admin or self)
- `POST /users`: Create a new user (admin only)
- `PUT /users/{id}`: Update user (admin or self)
- `DELETE /users/{id}`: Delete user (admin only)

## Configuration

### Application Configuration

The application uses `application.yml` for configuration:

- Server settings: Port (8080), context path (`/privsense`)
- Database connection: PostgreSQL for internal storage
- JPA/Hibernate settings
- PrivSense specific settings:
  - Async task execution
  - WebSocket configuration
  - Sampling parameters
  - PII detection thresholds and regex patterns
  - NER service integration
  - Database connection pooling
  - JWT authentication
  - Regular expression patterns for PII detection

### Configuration Classes

The API includes several critical configuration classes:

1. **SecurityConfig**: Configures Spring Security with JWT authentication, CORS settings, and endpoint authorization
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       // Password encoding, JWT filter setup, CORS configuration, and endpoint restrictions
   }
   ```

2. **AsyncConfig**: Configures thread pools for asynchronous operations
   ```java
   @Configuration
   public class AsyncConfig {
       // Configurable thread pool for handling asynchronous processing like database scanning
   }
   ```

3. **JacksonConfig**: Customizes JSON serialization settings
   ```java
   @Configuration
   public class JacksonConfig {
       // JSON serialization configuration including date handling and null value exclusion
   }
   ```

4. **WebSocketConfig**: Sets up WebSocket server for real-time updates
   ```java
   @Configuration
   @EnableWebSocketMessageBroker
   public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
       // WebSocket endpoint and message broker configuration
   }
   ```

5. **OpenApiConfig**: Configures Swagger/OpenAPI documentation
   ```java
   @Configuration
   public class OpenApiConfig {
       // API documentation with authentication, server info, and endpoint grouping
   }
   ```

6. **PersistenceConfig**: Configures JPA repositories and entity scanning
   ```java
   @Configuration
   @EnableJpaRepositories(basePackages = {"com.privsense.api.repository", "com.privsense.api.repository.jpa", "com.privsense.core.repository"})
   @EntityScan(basePackages = {"com.privsense.api.model", "com.privsense.core.model", "com.privsense.db.model"})
   @EnableTransactionManagement
   public class PersistenceConfig {
       // JPA configuration via annotations
   }
   ```

7. **MapStructConfig**: Configures MapStruct for object mapping
   ```java
   @MapperConfig(
       componentModel = "spring",
       injectionStrategy = InjectionStrategy.CONSTRUCTOR,
       nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
       unmappedTargetPolicy = ReportingPolicy.IGNORE
   )
   public interface MapStructConfig {
       // Configuration interface for MapStruct mappers
   }
   ```

### Dependencies

Key dependencies include:

- Spring Boot Web, Security, Validation, WebSocket
- JWT for authentication
- MapStruct for object mapping
- Springdoc OpenAPI for API documentation

## Development

### Technologies

- Java 21
- Spring Boot
- PostgreSQL
- JPA/Hibernate
- JWT
- WebSocket
- MapStruct

### Build Process

The project uses Maven for dependency management and build automation:

```bash
mvn clean install
```

### Runtime Requirements

- JDK 21 or higher
- PostgreSQL database
- Optional: NER service for enhanced PII detection

## Key Services

### ScanOrchestrationService

Central service that coordinates the entire scan process, following the Facade pattern to simplify interactions with the scanning subsystem. Provides methods for submitting scans, checking status, retrieving and exporting reports in multiple formats.

Key responsibilities:
- Managing the scan workflow
- Coordinating between multiple service components
- Handling report generation and export
- Processing scan results for API responses
- Publishing WebSocket notifications for scan progress

### SamplingService

Handles database column sampling operations, supporting different sampling strategies based on database type and providing statistical information about sampled data.

Key features:
- Single column sampling
- Batch sampling across multiple tables
- Parallel execution for performance
- Data statistics calculation
- Entropy calculation for columns

### WebSocketMessagingDelegate

Provides real-time updates about scan status and progress to client applications through WebSocket connections. Only instantiated when WebSocket support is enabled.

Key capabilities:
- Topic-based message routing
- Job-specific update channels
- General broadcast channels
- Error handling and recovery

### ReportExportService

Handles exporting compliance reports in various formats to meet different client needs.

Supported formats:
- JSON for API consumption
- CSV for data analysis
- PDF for formal reporting
- Plain text for simple viewing
- HTML for web display
- Excel for detailed analysis

### EntityRelationshipManager

Manages entity relationships for database objects, ensuring referential integrity when persisting database metadata.

### JwtService

Handles JWT token operations for authentication and authorization:
- Token generation and signing using HMAC-SHA256
- Token validation and parsing
- Claims extraction and management
- Token expiration handling

### Security Components

The security architecture includes:

1. **JwtAuthenticationFilter**: Intercepts all requests to validate JWT tokens
   - Extracts JWT tokens from Authorization header
   - Validates tokens using JwtService
   - Sets authentication in SecurityContext
   - Handles expired or invalid tokens

2. **SecurityConfig**: Configures Spring Security with:
   - JWT-based authentication
   - Endpoint authorization rules
   - Password encoding
   - CORS configuration
   - CSRF protection

3. **AuthenticationService**: Handles user authentication:
   - Validates credentials
   - Issues JWT tokens
   - Refreshes tokens
   - Manages user sessions

### Exception Handling

The API provides comprehensive exception handling:

1. **GlobalExceptionHandler**: Centralized exception handling for the entire API
   - Translates exceptions to appropriate HTTP response codes
   - Formats error responses consistently
   - Logs exceptions with contextual information
   - Provides helpful error messages without exposing internal details

2. **Custom Exception Classes**:
   - **ResourceNotFoundException**: When requested resources don't exist
   - **BadRequestException**: For invalid request parameters
   - **ServiceException**: For business logic errors
   - **AuthenticationException**: For authentication failures
   - **ConflictException**: For resource conflicts
   - **InvalidParameterException**: For validation failures
   - **DashboardException**: For dashboard-specific errors

### Filters and Interceptors

1. **CorrelationIdFilter**: Adds a unique correlation ID to each request
   - Generates or propagates correlation IDs
   - Adds correlation ID to MDC for logging
   - Includes correlation ID in response headers
   - Enables request tracing across microservices

2. **ApiVersionInterceptor**: Handles API versioning
   - Validates API version in requests
   - Routes requests to appropriate version handlers
   - Ensures backward compatibility

3. **RequestMetricsInterceptor**: Collects metrics on API usage
   - Records request timing and throughput
   - Captures endpoint performance statistics
   - Provides data for monitoring and optimization
   - Persists metrics data periodically

### Validation Framework

The API includes a validation framework for ensuring data integrity:

1. **Bean Validation Annotations**: Used throughout DTOs
   - Standard constraints like @NotNull, @Size, @Pattern
   - Custom constraints for domain-specific validation

2. **Custom Validators**:
   - **Threshold**: Custom validation for threshold values

3. **Validation Handler**: Centralizes validation response formatting

### Models & DTOs

1. **Entity Models**:
   - **PersistentMetricsData**: For storing API usage metrics
   - **ErrorResponse**: For standardized error responses

2. **Core Data Transfer Objects (DTOs)**:
   - Request DTOs for accepting API input
   - Response DTOs for returning data
   - Mapping DTOs for internal conversions

### Mappers (EntityMapper, DtoMapper)

MapStruct-based mappers that handle conversion between domain objects and DTOs, with specialized handling for various entity types and nested objects.

## Special Features

### Quasi-Identifier Detection

Beyond simple PII detection, the system can identify quasi-identifiers - combinations of non-PII fields that could potentially identify individuals when combined.

### Risk Scoring

Detected PII is assigned risk scores based on confidence levels and PII type sensitivity, helping prioritize remediation efforts.

### Real-time Updates

WebSocket integration provides real-time status updates during scanning operations.

### Comprehensive Metadata Analysis

Extracts and analyzes database schema information, including table relationships and column properties.

### Multi-Format Report Export

The system can export reports in various formats (JSON, CSV, PDF, Text, HTML, Excel) to suit different needs and consumption patterns.

### Request Correlation and Tracing

Every request is assigned a unique correlation ID that follows it through all processing steps, enabling effective debugging and monitoring across system components.

### Request Metrics Collection

The system collects detailed metrics on API usage patterns and performance, which are persisted periodically for analysis and optimization.

## Database Schema

The system uses its internal PostgreSQL database to store:

- Database connection details
- Scan metadata and results
- Detection findings and PII candidates
- Database schema information (tables, columns, relationships)
- User accounts and roles
- Request metrics and performance data
- System configuration and templates

## API Response Structure

All API responses follow a consistent structure:

```json
{
  "data": { /* Response data */ },
  "meta": {
    "status": "SUCCESS",
    "timestamp": "2023-05-13T10:30:45Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3
    }
  }
}
```

- `data`: Contains the actual response data
- `meta`: Contains metadata about the response:
  - `status`: SUCCESS or FAILED
  - `timestamp`: When the response was generated
  - `correlationId`: Unique ID for request tracing
  - `pagination`: Pagination information where applicable

## Error Handling

The API implements a global exception handling mechanism that:

1. Captures all exceptions that occur during request processing
2. Translates exceptions into appropriate HTTP status codes
3. Provides meaningful error messages and contextual information
4. Logs errors with correlation IDs for troubleshooting
5. Returns standardized error response objects

All error responses follow this format:

```json
{
  "error": {
    "code": "DATABASE_CONNECTION_FAILED",
    "message": "Failed to connect to database",
    "details": "Connection refused: connect",
    "timestamp": "2023-05-13T10:30:45Z",
    "path": "/api/v1/connections",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

Common error codes include:
- `VALIDATION_ERROR`: Invalid input data
- `RESOURCE_NOT_FOUND`: Requested resource does not exist
- `DATABASE_ERROR`: Database operation failed
- `AUTHENTICATION_ERROR`: Authentication failed
- `AUTHORIZATION_ERROR`: Insufficient permissions
- `PROCESSING_ERROR`: Error during data processing
- `CONFIGURATION_ERROR`: Invalid system configuration