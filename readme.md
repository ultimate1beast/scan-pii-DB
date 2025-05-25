# PrivSense-CGI

## Overview

PrivSense-CGI is a comprehensive data privacy system designed to detect, manage, and report on Personally Identifiable Information (PII) in database systems. The platform employs advanced techniques including pattern matching, heuristic analysis, and machine learning to identify sensitive information across multiple database types and formats.

The system follows a modular architecture with specialized components working together to provide a complete solution for privacy compliance, data governance, and risk management.

## Key Features

- **Multi-Database Support**: Compatible with MySQL, PostgreSQL, Oracle, SQL Server, and extensible to other database systems
- **Advanced PII Detection**: Multi-strategy approach combining regex patterns, heuristic analysis, and NER machine learning
- **Quasi-Identifier Detection**: Identifies combinations of non-PII fields that could potentially re-identify individuals
- **Risk Scoring**: Assesses privacy risks based on PII sensitivity and detection confidence
- **Comprehensive Reporting**: Generates detailed compliance reports in multiple formats (JSON, CSV, PDF, HTML, Excel)
- **Real-Time Updates**: WebSocket integration for live scan status and progress updates
- **User Management**: Role-based access control for organizational security
- **RESTful API**: Complete API for integration with other systems and services

## Architecture

PrivSense-CGI follows a modular architecture consisting of multiple specialized components:

```
                           ┌───────────────────┐
                           │                   │
                           │   PrivSense API   │
                           │                   │
                           └─────────┬─────────┘
                                     │
                                     ▼
┌─────────────┐  ┌─────────┐  ┌────────────┐  ┌───────────┐  ┌────────────┐
│ PII Detector│◄─┤ Sampler │◄─┤   Core     │─►│ Reporter  │  │   NER      │
└──────┬──────┘  └─────────┘  │  Services  │  └───────────┘  │  Service   │
       │                      └──────┬─────┘                 └─────┬──────┘
       │                             │                             │
       └─────────────────────────────┼─────────────────────────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │ Metadata        │
                            │ Extractor       │
                            └────────┬────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │ DB Connector    │
                            └─────────────────┘
```

## Full Architecture and Module Interconnection

### Architectural Overview

PrivSense-CGI follows a layered, modular architecture designed for flexibility, extensibility, and maintainability. The system is built on a Java Spring Boot foundation with a Python-based NER microservice, combining the strengths of both ecosystems.

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                 CLIENT LAYER                                  │
│                                                                               │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────┐  ┌─────────┐  │
│  │   Web Browser   │  │ Mobile App   │  │ Integration Client │  │  CLI    │  │
│  └────────┬────────┘  └───────┬──────┘  └─────────┬──────────┘  └────┬────┘  │
│           │                   │                    │                  │       │
└───────────┼───────────────────┼────────────────────┼──────────────────┼───────┘
            │                   │                    │                  │
            ▼                   ▼                    ▼                  ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                               API GATEWAY LAYER                               │
│                                                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐ │
│  │                        privsense-api (Spring Boot)                       │ │
│  │                                                                          │ │
│  │  ┌──────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────────┐   │ │
│  │  │  REST API    │  │ WebSockets │  │   Security │  │ Validation     │   │ │
│  │  │  Controllers │  │  Endpoints │  │   Filters  │  │ & Error Handling│   │ │
│  │  └──────────────┘  └────────────┘  └────────────┘  └────────────────┘   │ │
│  │                                                                          │ │
│  └──────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
└─────────────────────────────────────┬─────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               SERVICE LAYER                                     │
│                                                                                 │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐  ┌───────────────────┐ │
│  │ Scan         │  │ Connection     │  │ User         │  │ Report            │ │
│  │ Orchestration│  │ Management     │  │ Management   │  │ Generation        │ │
│  └───────┬──────┘  └────────┬───────┘  └──────┬───────┘  └─────────┬─────────┘ │
│          │                  │                  │                    │          │
└──────────┼──────────────────┼──────────────────┼────────────────────┼──────────┘
           │                  │                  │                    │
           ▼                  ▼                  ▼                    ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              BUSINESS LOGIC LAYER                                │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                           privsense-core                                   │  │
│  │                                                                            │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────┐  ┌─────────────────┐  │  │
│  │  │ Domain     │  │ Service    │  │ Repository     │  │ Utility         │  │  │
│  │  │ Models     │  │ Interfaces │  │ Interfaces     │  │ Classes         │  │  │
│  │  └────────────┘  └────────────┘  └────────────────┘  └─────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ privsense-      │  │ privsense-     │  │ privsense-   │  │ privsense-   │    │
│  │ pii-detector    │  │ sampler        │  │ reporter     │  │ metadata-    │    │
│  │                 │  │                │  │              │  │ extractor     │    │
│  └────────┬────────┘  └───────┬────────┘  └──────┬───────┘  └───────┬──────┘    │
│           │                   │                  │                   │          │
└───────────┼───────────────────┼──────────────────┼───────────────────┼──────────┘
            │                   │                  │                   │
            ▼                   ▼                  ▼                   ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                             INFRASTRUCTURE LAYER                               │
│                                                                                │
│  ┌─────────────────┐    ┌───────────────────┐    ┌────────────────────────┐    │
│  │ privsense-      │    │     ner-service   │    │ Database               │    │
│  │ db-connector    │    │     (Python)      │    │ (PostgreSQL)           │    │
│  └─────────────────┘    └───────────────────┘    └────────────────────────┘    │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Module Interactions and Dependencies

#### Core Dependencies Flow

The system follows a clear dependency structure where modules depend on lower-level modules:

```
privsense-api
 ├─► privsense-core
 ├─► privsense-db-connector
 │    └─► privsense-core
 ├─► privsense-metadata-extractor
 │    ├─► privsense-core
 │    └─► privsense-db-connector
 ├─► privsense-sampler
 │    ├─► privsense-core
 │    └─► privsense-db-connector
 ├─► privsense-pii-detector
 │    ├─► privsense-core
 │    └─► HTTP calls to ner-service
 └─► privsense-reporter
      └─► privsense-core
```

#### Data Flow Between Modules

1. **Database Connection Flow**
   ```
   privsense-api
        │ (Connection request from client)
        ▼
   privsense-db-connector
        │ (Connection establishment)
        ▼
   Target Database
   ```

2. **Metadata Extraction Flow**
   ```
   privsense-api
        │ (Metadata request)
        ▼
   privsense-metadata-extractor
        │ (Connection access)
        ▼
   privsense-db-connector
        │ (SQL queries)
        ▼
   Target Database
        │ (Schema metadata)
        ▼
   privsense-metadata-extractor
        │ (Processed schema)
        ▼
   privsense-api
   ```

3. **PII Detection Scan Flow**
   ```
   privsense-api
        │ (Scan request)
        ▼
   privsense-metadata-extractor
        │ (Schema extraction)
        ▼
   privsense-sampler
        │ (Data sampling)
        ▼
   privsense-pii-detector
        ├─► Heuristic Analysis
        ├─► Regex Analysis
        └─► ner-service
             │ (NER results)
             ▼
        privsense-pii-detector
             │ (Consolidated results)
             ▼
        privsense-reporter
             │ (Formatted report)
             ▼
        privsense-api
             │ (Response to client)
             ▼
        Client
   ```

### Communication Patterns

The PrivSense-CGI system employs multiple communication patterns:

1. **Synchronous Communication**
   - REST API calls between client and privsense-api
   - Method invocations between Java modules
   - HTTP calls from privsense-pii-detector to ner-service

2. **Asynchronous Communication**
   - WebSocket updates from privsense-api to clients
   - Async task execution within privsense-api

3. **Persistent Storage**
   - JPA/Hibernate for entity persistence in PostgreSQL database
   - Connection pooling for database access
   - Transaction management for data integrity

### Technology Stack Integration

The system integrates multiple technologies:

1. **Java Ecosystem**
   - Spring Boot framework for the API and core services
   - Maven for dependency management and build automation
   - JPA/Hibernate for database access
   - JWT for authentication
   - HikariCP for connection pooling
   - MapStruct for object mapping
   - Jackson for JSON processing

2. **Python Ecosystem**
   - FastAPI for the NER service
   - PyTorch for the GLiNER model
   - Uvicorn as the ASGI server
   - Docker for containerization

3. **Database Systems**
   - PostgreSQL for internal data storage
   - Support for scanning MySQL, PostgreSQL, Oracle, SQL Server databases

4. **Frontend Integration**
   - RESTful API for client integration
   - WebSockets for real-time updates
   - OpenAPI specification for API documentation

### Component Lifecycle Management

Each module handles specific aspects of component lifecycle management:

1. **privsense-db-connector**
   - Database connection lifecycle (creation, pooling, validation, and cleanup)
   - Dynamic JDBC driver loading
   - Connection pool management

2. **privsense-api**
   - Application startup and shutdown
   - HTTP request lifecycle
   - WebSocket session management
   - User session management

3. **privsense-pii-detector**
   - Detection strategy initialization and execution
   - Resource cleanup after detection

4. **ner-service**
   - Model loading and initialization
   - Request handling and response generation
   - Worker process management

### Resilience and Error Handling

The system implements multiple resilience patterns:

1. **Circuit Breakers**
   - NER service calls with fallback to simpler detection methods
   - Database operations with proper timeout and retry policies

2. **Graceful Degradation**
   - Detection pipeline continues even if one strategy fails
   - Multiple format options for report generation

3. **Centralized Error Handling**
   - GlobalExceptionHandler in the API
   - Standardized error response format
   - Correlation IDs for request tracing
   - Comprehensive logging across all modules

### Security Integration

Security is implemented across all layers:

1. **API Layer**
   - JWT authentication and authorization
   - Input validation and sanitization
   - Rate limiting and brute force protection
   - CORS configuration

2. **Service Layer**
   - Method-level security using Spring Security annotations
   - Role-based access control

3. **Data Layer**
   - Database connection encryption (SSL/TLS)
   - Password encryption
   - Data masking for sensitive information

4. **Infrastructure Layer**
   - Network isolation for the NER service
   - Transport layer security

## Module Details

### PrivSense Core

The foundation of the system containing:
- Domain models representing core business concepts (PII, scan operations, database structures)
- Service interfaces defining component behaviors
- Repository interfaces for data persistence
- Exception hierarchy for robust error handling
- Utility classes for common operations

Key features:
- Clean architecture with separation of concerns
- Domain-driven design principles
- Comprehensive entity relationships
- Well-defined scan lifecycle management

### PrivSense API

RESTful interface exposing the platform's capabilities:
- REST endpoints for database connections, scanning, and reporting
- JWT-based authentication and role-based access control
- Real-time updates via WebSocket
- Comprehensive exception handling and validation

Key endpoints:
- Connection management: `/connections/**`
- Scan operations: `/scans/**`
- Report export: `/scans/{jobId}/export/{format}`
- Authentication: `/auth/**`
- User management: `/users/**`

### PrivSense DB Connector

Provides database connectivity with:
- Dynamic JDBC driver loading at runtime
- Connection pooling with HikariCP
- Secure credential management
- Support for multiple database systems
- SSL/TLS encryption support

### PrivSense Metadata Extractor

Extracts and analyzes database schemas:
- Table and view metadata
- Column information including data types and constraints
- Primary keys and foreign key relationships
- Table and column comments
- Database-specific optimizations for different DBMS

### PrivSense Sampler

Efficiently samples data from database columns:
- Parallel sampling with multi-threading
- Database-specific optimizations
- Concurrency control to prevent database overload
- Entropy calculation for sampled data
- Sample size optimization based on table characteristics

### PrivSense PII Detector

Multi-strategy approach to PII detection:
- **Heuristic Strategy**: Analyzes column metadata (names, comments) for PII-related keywords
- **Regex Strategy**: Uses regular expressions to identify common PII patterns
- **NER Client Strategy**: Leverages Named Entity Recognition for advanced PII detection
- **Quasi-Identifier Detection**: Identifies columns that together could re-identify individuals

### PrivSense Reporter

Generates compliance reports with:
- Multiple output formats (JSON, CSV, text, PDF, HTML, Excel)
- Detailed findings and statistics
- Masking of sensitive information
- Customizable report templates

### NER Service

FastAPI microservice for Named Entity Recognition:
- Advanced NLP using the GLiNER model
- Detects over 50 types of PII entities
- Multi-language support
- High-performance with parallelization and caching
- Docker containerization for easy deployment

## Installation and Setup

### Prerequisites

- Java 21 or higher
- PostgreSQL database (for internal storage)
- Docker (optional, for the NER service)
- Maven

### Building the Project

1. Clone the repository
   ```bash
   git clone https://github.com/your-org/privsense-CGI.git
   cd privsense-CGI
   ```

2. Build with Maven
   ```bash
   mvn clean install
   ```

3. Set up the NER service
   ```bash
   cd ner-service
   python -m pip install -r requirements.txt
   python preload_model.py
   ```

### Running the Application

1. Configure the database connection in `application.yml`

2. Start the NER service
   ```bash
   cd ner-service
   python main.py --model-path models/E3-JSI_gliner-multi-pii-domains-v1
   ```
   
   Or using the provided batch file:
   ```bash
   cd ner-service
   start.bat
   ```

3. Start the PrivSense API
   ```bash
   cd privsense-api
   java -jar target/privsense-api-1.0.0-SNAPSHOT.jar
   ```

## Workflow

A typical workflow in the PrivSense system follows these steps:

1. **Connect to a Database**:
   - The API receives connection details
   - The DB Connector creates a connection pool
   - Connection is stored for future use

2. **Extract Metadata**:
   - The Metadata Extractor retrieves schema information
   - Tables, columns, and relationships are analyzed
   - Results are stored in the system

3. **Sample Data**:
   - The Sampler extracts representative data samples
   - Parallel processing for efficiency
   - Sample size optimization based on data characteristics

4. **Detect PII**:
   - Heuristic analysis of column names and comments
   - Regular expression matching for patterns
   - NER processing for advanced detection
   - Quasi-identifier analysis for correlated columns

5. **Generate Reports**:
   - Compilation of findings into structured reports
   - Risk scoring based on PII sensitivity
   - Export in the desired format

6. **Remediation Guidance**:
   - Recommendations for handling detected PII
   - Risk mitigation strategies

## API Usage Examples

### Authentication

```bash
curl -X POST http://localhost:8080/privsense/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

### Creating a Database Connection

```bash
curl -X POST http://localhost:8080/privsense/api/v1/connections \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "name": "Production Database",
    "host": "db.example.com",
    "port": 3306,
    "databaseName": "customer_data",
    "username": "scanner",
    "password": "secure-password",
    "dbType": "MYSQL",
    "sslEnabled": true
  }'
```

### Starting a Scan

```bash
curl -X POST http://localhost:8080/privsense/api/v1/scans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "connectionId": "550e8400-e29b-41d4-a716-446655440000",
    "sampleSize": 1000,
    "includeTables": ["customers", "orders"],
    "excludeTables": ["logs", "metrics"]
  }'
```

### Exporting a Report

```bash
curl -X GET http://localhost:8080/privsense/api/v1/scans/550e8400-e29b-41d4-a716-446655440000/export/pdf \
  -H "Authorization: Bearer {token}" \
  --output report.pdf
```

## Security Considerations

PrivSense-CGI implements several security measures:

- **Authentication**: JWT-based authentication with configurable token expiration
- **Authorization**: Role-based access control for endpoints
- **Secure Connections**: Support for SSL/TLS encrypted database connections
- **Password Protection**: Passwords are never stored in plain text
- **Input Validation**: Comprehensive validation for all inputs
- **Sensitive Data Masking**: PII is masked in logs and reports
- **CORS Protection**: Configurable CORS settings
- **Audit Logging**: Security-relevant events are logged

## Configuration

The system can be configured through application.yml with the following sections:

- **Server Settings**: Port, context path, etc.
- **Database Connection**: For internal storage
- **JWT Configuration**: Secret key, token validity, etc.
- **Scanning Parameters**: Default sample size, concurrency limits
- **PII Detection**: Confidence thresholds, patterns, NER settings
- **Report Generation**: Default formats, templates
- **WebSocket Settings**: Message broker configuration
- **Async Task Execution**: Thread pool settings

## PII Types Detected

PrivSense can detect over 50 types of PII, including:

- Personal identifiers (names, SSNs, passport numbers)
- Contact information (phone numbers, email addresses, physical addresses)
- Financial information (credit cards, bank accounts)
- Healthcare data (health insurance IDs, medical conditions)
- Authentication data (usernames, passwords)
- Digital identifiers (IP addresses, device IDs)
- Employment information (employee IDs, job titles)
- Educational information (student IDs, grades)

## Quasi-Identifier Detection

Beyond direct PII detection, PrivSense identifies quasi-identifiers - combinations of seemingly innocuous fields that together can potentially identify individuals. This includes:

- Demographic combinations (age, gender, ZIP code)
- Behavior patterns
- Location histories
- Transaction records
- Device usage patterns

## Performance Considerations

PrivSense is designed for high performance with:

- Connection pooling for database efficiency
- Parallel processing for data sampling
- Multi-threaded PII detection
- Caching mechanisms for repeated operations
- Early termination optimizations
- Non-blocking I/O for asynchronous operations
- Configurable concurrency limits

## Extending the System

The modular architecture allows for extension in several ways:

1. **New Database Support**: Implement the appropriate strategy interfaces in the connector and metadata extractor modules
2. **Additional PII Types**: Add new regex patterns or train custom NER models
3. **Custom Report Formats**: Add new report generators to the reporter module
4. **Enhanced Detection Algorithms**: Implement new detection strategies in the PII detector
5. **Integration with Other Systems**: Use the API to connect with other applications

## Troubleshooting

Common issues and solutions:

- **Database Connection Failures**: Check credentials, network access, and SSL settings
- **Slow Performance**: Adjust sampling size, concurrency settings, and thread pool configuration
- **NER Service Unavailability**: Ensure the service is running and correctly configured
- **Memory Issues**: Configure appropriate heap sizes for Java applications
- **Authentication Problems**: Verify JWT configuration and token validity

## Contributing

Guidelines for contributing to the project:

1. Follow the existing architecture and design patterns
2. Maintain backward compatibility
3. Include comprehensive tests
4. Document public APIs with JavaDoc
5. Respect the domain model integrity

## License

[License information would go here]

## Project Roadmap

Future enhancements planned for PrivSense:

- Enhanced support for NoSQL databases
- Integration with cloud-native PII detection services
- Automated remediation suggestions
- Machine learning improvements for detection accuracy
- Real-time monitoring for data streams
- Enhanced visualization of findings
- Mobile application interface

## Contact

[Contact information would go here]