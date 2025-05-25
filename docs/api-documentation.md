# PrivSense API Documentation

This document provides detailed information about the REST API endpoints available in the PrivSense application. Use this documentation to develop a frontend application that interacts with the PrivSense backend.

## Table of Contents
- [Authentication](#authentication)
- [Database Connections](#database-connections)
- [PII Scanning](#pii-scanning)
- [Data Sampling](#data-sampling)
- [User Management](#user-management)
- [System Information](#system-information)
- [Scan Templates](#scan-templates)
- [Configuration Management](#configuration-management)
- [Dashboard](#dashboard)
- [Monitoring](#monitoring)
- [WebSockets](#websockets)
- [Error Handling](#error-handling)

## Authentication

The PrivSense API uses JWT (JSON Web Token) authentication. Most endpoints require authentication except for login, registration, and some system health endpoints.

### Roles and Permissions

The API supports two main roles:
- **ADMIN**: Full access to all endpoints
- **API_USER**: Limited access to viewing data and running scans, but cannot create connections or manage users

### Authentication Endpoints

#### Login

```
POST /api/v1/auth/login
```

Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:** (200 OK)
```json
{
  "token": "JWT_TOKEN_STRING",
  "username": "string",
  "roles": ["ROLE_ADMIN"] 
}
```

#### Register (Admin only can register users in production)

```
POST /api/v1/auth/register
```

Registers a new user account.

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "fullName": "string"
}
```

**Response:** (201 Created)
```json
{
  "id": "UUID",
  "username": "string",
  "email": "string",
  "roles": ["ROLE_API_USER"],
  "status": "ACTIVE"
}
```

#### Get Current User

```
GET /api/v1/auth/me
```

Returns information about the currently authenticated user.

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["ROLE_ADMIN"],
  "status": "ACTIVE"
}
```

## Database Connections

### Connection Management Endpoints

#### Create Connection (Admin only)

```
POST /api/v1/connections
```

Establishes a new database connection.

**Request Body:**
```json
{
  "name": "Production Database",
  "host": "db.example.com",
  "port": 5432,
  "databaseName": "customer_db",
  "username": "db_user",
  "password": "password",
  "databaseType": "POSTGRESQL"
}
```

**Response:** (200 OK)
```json
{
  "connectionId": "UUID",
  "name": "Production Database",
  "host": "db.example.com",
  "port": 5432,
  "databaseName": "customer_db",
  "username": "db_user",
  "databaseType": "POSTGRESQL",
  "databaseProductName": "PostgreSQL",
  "databaseProductVersion": "13.4",
  "status": "CONNECTED"
}
```

#### List Connections

```
GET /api/v1/connections
```

Returns a list of all active database connections.

**Response:** (200 OK)
```json
[
  {
    "connectionId": "UUID",
    "name": "Production Database",
    "host": "db.example.com",
    "port": 5432,
    "databaseName": "customer_db",
    "username": "db_user",
    "databaseType": "POSTGRESQL",
    "databaseProductName": "PostgreSQL",
    "databaseProductVersion": "13.4",
    "status": "AVAILABLE"
  }
]
```

#### Get Connection Details

```
GET /api/v1/connections/{connectionId}
```

Returns details for a specific database connection.

**Response:** (200 OK)
```json
{
  "connectionId": "UUID",
  "name": "Production Database",
  "host": "db.example.com",
  "port": 5432,
  "databaseName": "customer_db",
  "username": "db_user",
  "databaseType": "POSTGRESQL",
  "databaseProductName": "PostgreSQL",
  "databaseProductVersion": "13.4",
  "status": "AVAILABLE"
}
```

#### Get Database Metadata

```
GET /api/v1/connections/{connectionId}/metadata
```

Returns the schema information for a database connection.

**Response:** (200 OK)
```json
{
  "catalogName": "string",
  "schemaName": "string",
  "totalTableCount": 25,
  "totalColumnCount": 150,
  "totalRelationshipCount": 30,
  "tables": [
    {
      "name": "users",
      "type": "TABLE",
      "comments": "User information",
      "columns": [
        {
          "name": "id",
          "type": "INTEGER",
          "comments": "Primary key",
          "size": 10,
          "decimalDigits": 0,
          "nullable": false,
          "primaryKey": true,
          "foreignKey": false
        }
      ],
      "importedRelationships": [],
      "exportedRelationships": []
    }
  ]
}
```

#### Close Connection (Admin only)

```
DELETE /api/v1/connections/{connectionId}
```

Closes a database connection and releases resources.

**Response:** (204 No Content)

## PII Scanning

### Scan Management Endpoints

#### Start New Scan (Admin only)

```
POST /api/v1/scans
```

Initiates a new database scan to detect PII.

**Request Body:**
```json
{
  "connectionId": "UUID",
  "scanType": "FULL",
  "includedSchemas": ["public"],
  "includedTables": ["users", "customers"],
  "excludedTables": ["logs", "metrics"],
  "maxSampleSize": 1000,
  "confidenceThreshold": 0.7,
  "templateId": "UUID"
}
```

**Response:** (201 Created)
```json
{
  "jobId": "UUID",
  "status": "PENDING",
  "progress": 0,
  "started": "2023-05-19T10:30:00Z",
  "completed": false,
  "connectionId": "UUID",
  "databaseName": "customer_db"
}
```

#### List All Scans

```
GET /api/v1/scans?page=0&size=20&status=completed&connectionId=UUID
```

Returns a paginated list of scan jobs.

**Query Parameters:**
- `page`: Page number (0-based)
- `size`: Items per page
- `status`: Filter by status (completed, pending, failed)
- `connectionId`: Filter by database connection ID

**Response:** (200 OK)
```json
{
  "content": [
    {
      "jobId": "UUID",
      "status": "COMPLETED",
      "progress": 100,
      "started": "2023-05-19T10:30:00Z",
      "completed": true,
      "completedAt": "2023-05-19T10:45:00Z",
      "connectionId": "UUID",
      "databaseName": "customer_db"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 50,
  "totalPages": 3,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

#### Get Scan Status

```
GET /api/v1/scans/{jobId}
```

Returns the current status of a scan job.

**Response:** (200 OK)
```json
{
  "jobId": "UUID",
  "status": "RUNNING",
  "progress": 45,
  "started": "2023-05-19T10:30:00Z",
  "completed": false,
  "connectionId": "UUID",
  "databaseName": "customer_db",
  "currentStage": "SCANNING_TABLES",
  "tablesCompleted": 10,
  "totalTables": 25
}
```

#### Get Scan Report

```
GET /api/v1/scans/{jobId}/report?format=json
```

Exports the scan report in various formats.

**Query Parameters:**
- `format`: Export format (json, csv, text, pdf)

**Response:** (200 OK)
For JSON format:
```json
{
  "scanId": "UUID",
  "databaseName": "customer_db",
  "scanDate": "2023-05-19T10:30:00Z",
  "piiDiscoveryCount": 150,
  "tableCount": 25,
  "tableFindings": {
    "users": {
      "tableName": "users",
      "piiCount": 45,
      "columns": [
        {
          "name": "email",
          "piiType": "EMAIL",
          "confidence": 0.95,
          "sampleMatches": 25
        }
      ]
    }
  }
}
```

#### Get Scan Results with Filtering

```
GET /api/v1/scans/{jobId}/results?piiType=EMAIL&confidenceMin=0.8&page=0&size=20
```

Returns PII detection results with filtering options.

**Query Parameters:**
- `piiType`: Filter by PII type (e.g., EMAIL, NAME, SSN)
- `confidenceMin`: Minimum confidence score threshold (0.0-1.0)
- `page`: Page number (0-based)
- `size`: Items per page

**Response:** (200 OK)
```json
[
  {
    "resultId": "UUID",
    "tableName": "users",
    "columnName": "email",
    "piiType": "EMAIL",
    "confidenceScore": 0.95,
    "detectionMethod": "REGEX",
    "sampleCount": 25,
    "examples": ["example@domain.com"]
  }
]
```

#### Get Scan Statistics

```
GET /api/v1/scans/{jobId}/stats
```

Returns statistics about the scan results.

**Response:** (200 OK)
```json
{
  "scanId": "UUID",
  "databaseName": "customer_db",
  "databaseProductName": "PostgreSQL",
  "databaseProductVersion": "13.4",
  "startTime": "2023-05-19T10:30:00Z",
  "endTime": "2023-05-19T10:45:00Z",
  "duration": "15m 0s",
  "status": "COMPLETED",
  "completed": true,
  "failed": false,
  "tableScanCount": 25,
  "columnScanCount": 150,
  "rowsSampled": 25000,
  "piiTypeDistribution": {
    "EMAIL": 45,
    "NAME": 35,
    "PHONE_NUMBER": 25,
    "ADDRESS": 20,
    "SSN": 5
  },
  "confidenceDistribution": {
    "90-100": 75,
    "80-89": 35,
    "70-79": 20,
    "60-69": 15,
    "50-59": 5
  },
  "topPiiTables": [
    {
      "tableName": "users",
      "piiCount": 45
    }
  ]
}
```

#### Get Scanned Tables

```
GET /api/v1/scans/{jobId}/tables
```

Returns a list of tables that were scanned with PII statistics.

**Response:** (200 OK)
```json
[
  {
    "tableName": "users",
    "schemaName": "public",
    "rowCount": 1000,
    "columnCount": 10,
    "piiColumnCount": 4,
    "totalPiiInstances": 45,
    "highestConfidencePii": "EMAIL",
    "highestConfidenceScore": 0.95
  }
]
```

#### Get Table Columns

```
GET /api/v1/scans/{jobId}/tables/{tableName}/columns
```

Returns columns from a specific table with PII detection results.

**Response:** (200 OK)
```json
[
  {
    "columnName": "email",
    "dataType": "VARCHAR",
    "piiDetected": true,
    "piiType": "EMAIL",
    "confidenceScore": 0.95,
    "detectionMethod": "REGEX",
    "sampleSize": 100,
    "matchCount": 95
  }
]
```

#### Cancel Scan (Admin only)

```
DELETE /api/v1/scans/{jobId}
```

Cancels an in-progress scan job.

**Response:** (204 No Content)

## Data Sampling

### Sampling Endpoints

#### Test Column Sampling

```
POST /api/v1/sampling
```

Samples data from a specific column to validate sampling functionality.

**Request Body:**
```json
{
  "connectionId": "UUID",
  "schemaName": "public",
  "tableName": "users",
  "columnName": "email",
  "sampleSize": 100,
  "samplingMethod": "RANDOM"
}
```

**Response:** (200 OK)
```json
{
  "samples": [
    "example1@domain.com",
    "example2@domain.com"
  ],
  "sampleCount": 100,
  "actualSampleSize": 100,
  "totalRows": 5000,
  "samplingRate": 0.02,
  "dataType": "VARCHAR",
  "executionTimeMs": 125
}
```

#### Test Parallel Sampling

```
POST /api/v1/sampling/batch
```

Samples data from multiple tables and columns in parallel.

**Request Body:**
```json
{
  "connectionId": "UUID",
  "samplingRequests": [
    {
      "schemaName": "public",
      "tableName": "users",
      "columnName": "email",
      "sampleSize": 100
    },
    {
      "schemaName": "public",
      "tableName": "customers",
      "columnName": "phone",
      "sampleSize": 100
    }
  ],
  "samplingMethod": "RANDOM"
}
```

**Response:** (200 OK)
```json
{
  "samples": {
    "public.users.email": [
      "example1@domain.com",
      "example2@domain.com"
    ],
    "public.customers.phone": [
      "555-123-4567",
      "555-987-6543"
    ]
  },
  "totalTablesRequested": 2,
  "totalTablesProcessed": 2,
  "totalSamplesRequested": 200,
  "totalSamplesReturned": 200,
  "executionTimeMs": 250
}
```

#### Get Sampling Configuration

```
GET /api/v1/sampling/configuration
```

Returns the available sampling configuration options.

**Response:** (200 OK)
```json
{
  "availableSamplingMethods": [
    "RANDOM",
    "FIRST_N",
    "SYSTEMATIC",
    "RESERVOIR"
  ],
  "defaultSampleSize": 100,
  "maxSampleSize": 1000,
  "defaultMethod": "RANDOM"
}
```

## User Management

### User Management Endpoints (Admin only)

#### Create User

```
POST /api/v1/users
```

Creates a new user account.

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["ADMIN", "API_USER"]
}
```

**Response:** (201 Created)
```json
{
  "id": "UUID",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["ADMIN", "API_USER"],
  "status": "ACTIVE",
  "created": "2023-05-19T10:30:00Z"
}
```

#### List Users

```
GET /api/v1/users
```

Returns a list of all users.

**Response:** (200 OK)
```json
[
  {
    "id": "UUID",
    "username": "string",
    "email": "string",
    "fullName": "string",
    "roles": ["ADMIN"],
    "status": "ACTIVE",
    "created": "2023-05-19T10:30:00Z",
    "lastLogin": "2023-05-19T15:45:00Z"
  }
]
```

#### Get User

```
GET /api/v1/users/{id}
```

Returns details for a specific user.

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["ADMIN"],
  "status": "ACTIVE",
  "created": "2023-05-19T10:30:00Z",
  "lastLogin": "2023-05-19T15:45:00Z"
}
```

#### Update User Status

```
PUT /api/v1/users/{id}/status
```

Updates the status of a user account.

**Request Body:**
```json
{
  "status": "INACTIVE",
  "reason": "User no longer active"
}
```

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "username": "string",
  "status": "INACTIVE"
}
```

#### Update User Roles

```
PUT /api/v1/users/{id}/roles
```

Updates the roles assigned to a user.

**Request Body:**
```json
{
  "roles": ["API_USER"]
}
```

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "username": "string",
  "roles": ["API_USER"]
}
```

#### Delete User

```
DELETE /api/v1/users/{id}
```

Deletes a user account.

**Response:** (204 No Content)

## System Information

### System Endpoints

#### Get System Health

```
GET /api/v1/system/health
```

Returns the health status of the system. This endpoint is publicly accessible.

**Response:** (200 OK)
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "13.4"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": "500GB",
        "free": "350GB",
        "threshold": "10GB"
      }
    }
  }
}
```

#### Get System Information (Admin only)

```
GET /api/v1/system/info
```

Returns detailed system information.

**Response:** (200 OK)
```json
{
  "application": {
    "name": "PrivSense",
    "version": "1.0.0",
    "buildDate": "2023-05-01T10:00:00Z"
  },
  "runtime": {
    "javaVersion": "21.0.1",
    "availableProcessors": 8,
    "freeMemory": "1.5GB",
    "totalMemory": "4GB"
  },
  "database": {
    "activeConnections": 3,
    "databaseType": "PostgreSQL",
    "databaseVersion": "13.4"
  },
  "system": {
    "operatingSystem": "Linux",
    "architecture": "x86_64",
    "systemLoad": 0.35
  }
}
```

## Scan Templates

### Scan Template Endpoints

#### Create Scan Template (Admin only)

```
POST /api/v1/scan-templates
```

Creates a new scan template configuration.

**Request Body:**
```json
{
  "name": "Standard PII Scan",
  "description": "Template for standard PII scanning of customer data",
  "includedSchemas": ["public"],
  "excludedSchemas": ["system", "information_schema"],
  "includedTables": ["users", "customers"],
  "excludedTables": ["logs", "metrics"],
  "scanType": "FULL",
  "maxSampleSize": 1000,
  "confidenceThreshold": 0.7,
  "detectionSettings": {
    "enableRegexDetection": true,
    "enableHeuristicDetection": true,
    "enableNerDetection": true,
    "regexThreshold": 0.8,
    "heuristicThreshold": 0.7,
    "nerThreshold": 0.6
  }
}
```

**Response:** (201 Created)
```json
{
  "id": "UUID",
  "name": "Standard PII Scan",
  "description": "Template for standard PII scanning of customer data",
  "created": "2023-05-19T10:30:00Z",
  "createdBy": "admin"
}
```

#### Update Scan Template (Admin only)

```
PUT /api/v1/scan-templates/{id}
```

Updates an existing scan template.

**Request Body:** (Same as Create)

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "name": "Standard PII Scan",
  "description": "Updated template for standard PII scanning",
  "updated": "2023-05-19T11:30:00Z",
  "updatedBy": "admin"
}
```

#### List Scan Templates

```
GET /api/v1/scan-templates
```

Returns a list of all scan templates.

**Response:** (200 OK)
```json
[
  {
    "id": "UUID",
    "name": "Standard PII Scan",
    "description": "Template for standard PII scanning of customer data",
    "created": "2023-05-19T10:30:00Z",
    "createdBy": "admin",
    "scanType": "FULL"
  }
]
```

#### Get Scan Template

```
GET /api/v1/scan-templates/{id}
```

Returns details for a specific scan template.

**Response:** (200 OK)
```json
{
  "id": "UUID",
  "name": "Standard PII Scan",
  "description": "Template for standard PII scanning of customer data",
  "includedSchemas": ["public"],
  "excludedSchemas": ["system", "information_schema"],
  "includedTables": ["users", "customers"],
  "excludedTables": ["logs", "metrics"],
  "scanType": "FULL",
  "maxSampleSize": 1000,
  "confidenceThreshold": 0.7,
  "detectionSettings": {
    "enableRegexDetection": true,
    "enableHeuristicDetection": true,
    "enableNerDetection": true,
    "regexThreshold": 0.8,
    "heuristicThreshold": 0.7,
    "nerThreshold": 0.6
  },
  "created": "2023-05-19T10:30:00Z",
  "createdBy": "admin"
}
```

#### Execute Scan from Template

```
POST /api/v1/scan-templates/{id}/execute
```

Starts a new scan using a predefined template configuration.

**Path Parameters:**
- `id`: Template ID (UUID)

**Query Parameters:**
- `connectionId`: Optional. Override the connection ID from the template

**Response:** (201 Created)
```json
{
  "jobId": "UUID",
  "status": "PENDING",
  "progress": 0,
  "started": "2023-05-19T10:30:00Z",
  "completed": false,
  "connectionId": "UUID",
  "databaseName": "customer_db"
}
```

#### Delete Scan Template (Admin only)

```
DELETE /api/v1/scan-templates/{id}
```

Deletes a scan template.

**Response:** (204 No Content)

## Configuration Management

### Application Configuration Endpoints

#### Get Application Configuration

```
GET /api/v1/config
```

Returns the current application configuration.

**Response:** (200 OK)
```json
{
  "detection": {
    "heuristicThreshold": 0.7,
    "regexThreshold": 0.8,
    "nerThreshold": 0.6,
    "reportingThreshold": 0.5,
    "stopPipelineOnHighConfidence": true,
    "entropyEnabled": true
  },
  "sampling": {
    "defaultSize": 100,
    "maxConcurrentDbQueries": 10,
    "entropyCalculationEnabled": true,
    "defaultMethod": "RANDOM"
  },
  "ner": {
    "enabled": true,
    "serviceUrl": "http://localhost:5000/detect-pii",
    "timeoutSeconds": 30,
    "maxSamples": 100,
    "retryAttempts": 2,
    "circuitBreaker": {
      "enabled": true,
      "failureThreshold": 5,
      "resetTimeoutSeconds": 30
    }
  },
  "reporting": {
    "pdfEnabled": true,
    "csvEnabled": true,
    "textEnabled": true,
    "reportOutputPath": "./reports"
  },
  "database": {
    "pool": {
      "connectionTimeout": 30000,
      "idleTimeout": 600000,
      "maxLifetime": 1800000,
      "minimumIdle": 5,
      "maximumPoolSize": 10
    },
    "driverDir": "./drivers"
  }
}
```

#### Update Application Configuration (Admin only)

```
PUT /api/v1/config
```

Updates the application configuration.

**Request Body:** (Same structure as the GET response)

**Response:** (200 OK)
```json
{
  "detection": {
    "heuristicThreshold": 0.7,
    "regexThreshold": 0.8,
    "nerThreshold": 0.6,
    "reportingThreshold": 0.5,
    "stopPipelineOnHighConfidence": true,
    "entropyEnabled": true
  },
  // ...other configuration sections...
}
```

### Detection Rules Endpoints

#### Get All Detection Rules

```
GET /api/v1/config/detection-rules
```

Returns all PII detection rules configured in the system.

**Response:** (200 OK)
```json
[
  {
    "id": "rule-123",
    "name": "Credit Card Detector",
    "pattern": "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}",
    "piiType": "CREDIT_CARD",
    "confidenceScore": 0.95,
    "description": "Detects credit card numbers",
    "enabled": true,
    "ruleType": "REGEX"
  }
]
```

#### Create Detection Rule (Admin only)

```
POST /api/v1/config/detection-rules
```

Creates a new PII detection rule.

**Request Body:**
```json
{
  "name": "New Email Pattern",
  "pattern": "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
  "piiType": "EMAIL",
  "confidenceScore": 0.9,
  "description": "Enhanced email detection pattern",
  "enabled": true,
  "ruleType": "REGEX"
}
```

**Response:** (201 Created)
```json
{
  "id": "rule-456",
  "name": "New Email Pattern",
  "pattern": "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
  "piiType": "EMAIL",
  "confidenceScore": 0.9,
  "description": "Enhanced email detection pattern",
  "enabled": true,
  "ruleType": "REGEX"
}
```

#### Get Detection Rule

```
GET /api/v1/config/detection-rules/{ruleId}
```

Returns a specific detection rule.

**Path Parameters:**
- `ruleId`: ID of the rule

**Response:** (200 OK)
```json
{
  "id": "rule-123",
  "name": "Credit Card Detector",
  "pattern": "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}",
  "piiType": "CREDIT_CARD",
  "confidenceScore": 0.95,
  "description": "Detects credit card numbers",
  "enabled": true,
  "ruleType": "REGEX"
}
```

#### Update Detection Rule (Admin only)

```
PUT /api/v1/config/detection-rules/{ruleId}
```

Updates an existing detection rule.

**Path Parameters:**
- `ruleId`: ID of the rule

**Request Body:** (Same as Create)

**Response:** (200 OK)
```json
{
  "id": "rule-123",
  "name": "Updated Credit Card Detector",
  "pattern": "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}",
  "piiType": "CREDIT_CARD",
  "confidenceScore": 0.97,
  "description": "Updated credit card detection pattern",
  "enabled": true,
  "ruleType": "REGEX"
}
```

#### Delete Detection Rule (Admin only)

```
DELETE /api/v1/config/detection-rules/{ruleId}
```

Deletes a detection rule.

**Path Parameters:**
- `ruleId`: ID of the rule

**Response:** (204 No Content)

## Dashboard

### Dashboard Endpoints

#### Get Dashboard Summary

```
GET /api/v1/dashboard/summary
```

Returns summary statistics including counts of connections, scans, and PII findings for dashboard display.

**Response:** (200 OK)
```json
{
  "totalScans": 256,
  "completedScans": 254,
  "failedScans": 2,
  "totalColumnsScanned": 3250,
  "totalPiiColumnsFound": 415,
  "piiPercentage": 12.8,
  "lastUpdated": "2023-05-19T10:30:00Z"
}
```

#### Get Recent Scans

```
GET /api/v1/dashboard/recent-scans?limit=5
```

Returns the most recent scan jobs for dashboard display.

**Query Parameters:**
- `limit`: Maximum number of scans to return (default: 5)

**Response:** (200 OK)
```json
[
  {
    "jobId": "UUID",
    "status": "COMPLETED",
    "progress": 100,
    "started": "2023-05-19T10:30:00Z",
    "completed": true,
    "completedAt": "2023-05-19T10:45:00Z",
    "connectionId": "UUID",
    "databaseName": "customer_db"
  }
]
```

#### Get Scan Trends

```
GET /api/v1/dashboard/trends?timeframe=30
```

Returns time series data for chart visualizations.

**Query Parameters:**
- `timeframe`: Number of days to include in the trends (default: 30)

**Response:** (200 OK)
```json
{
  "timeframe": "30",
  "startDate": "2023-04-19",
  "endDate": "2023-05-19",
  "scanCountByDate": {
    "2023-04-19": 5,
    "2023-04-20": 8,
    "2023-04-21": 3
    // ... more dates
  },
  "piiCountByDate": {
    "2023-04-19": 45,
    "2023-04-20": 62,
    "2023-04-21": 28
    // ... more dates
  },
  "timeSeriesData": [
    {
      "date": "2023-04-19",
      "scanCount": 5,
      "piiCount": 45
    }
    // ... more data points
  ]
}
```

#### Get Top PII Findings

```
GET /api/v1/dashboard/top-findings?limit=5
```

Returns the most common PII types found across all scans.

**Query Parameters:**
- `limit`: Maximum number of PII types to return (default: 5)

**Response:** (200 OK)
```json
[
  {
    "piiType": "EMAIL",
    "count": 235,
    "percentage": 25.8
  },
  {
    "piiType": "PHONE_NUMBER",
    "count": 187,
    "percentage": 20.5
  }
  // ... more PII types
]
```

## Monitoring

### Monitoring Endpoints

#### Get API Usage Statistics

```
GET /api/v1/monitoring/usage
```

Returns usage statistics for the API, including request counts and error rates.

**Response:** (200 OK)
```json
{
  "totalRequests": 12580,
  "totalErrors": 45,
  "errorRate": 0.36,
  "averageResponseTimeMs": 235,
  "requestsByEndpoint": {
    "/api/v1/scans": 3250,
    "/api/v1/connections": 875
    // ... more endpoints
  },
  "errorsByEndpoint": {
    "/api/v1/scans": 15,
    "/api/v1/connections": 8
    // ... more endpoints
  },
  "lastUpdated": "2023-05-19T10:30:00Z"
}
```

#### Reset API Usage Statistics (Admin only)

```
DELETE /api/v1/monitoring/usage
```

Clears all API usage statistics.

**Response:** (200 OK)
```json
{
  "message": "Usage statistics reset successfully",
  "timestamp": "2023-05-19T10:30:00Z"
}
```

#### Get API Performance Metrics

```
GET /api/v1/monitoring/metrics
```

Returns basic performance metrics for the API, including memory usage and thread counts.

**Response:** (200 OK)
```json
{
  "memory": {
    "freeMemoryMB": 1536,
    "totalMemoryMB": 4096,
    "maxMemoryMB": 8192,
    "heapMemoryUsageMB": 2560,
    "nonHeapMemoryUsageMB": 145
  },
  "threads": {
    "threadCount": 35,
    "peakThreadCount": 42,
    "daemonThreadCount": 28
  },
  "system": {
    "systemLoad": 0.45,
    "processCpuLoad": 0.38,
    "systemCpuLoad": 0.65
  },
  "jvm": {
    "uptime": "5d 12h 43m",
    "startTime": "2023-05-14T10:30:00Z"
  }
}
```

## WebSockets

The PrivSense API supports WebSocket connections for real-time updates.

### Available WebSocket Endpoints

#### Scan Progress Updates

```
ws://your-api-url/api/ws/scans/{scanId}?token={jwt_token}
```

Provides real-time updates on scan progress.

**Path Parameters:**
- `scanId`: ID of the scan job to monitor

**Query Parameters:**
- `token`: JWT authentication token

**Message Format:**
```json
{
  "jobId": "UUID",
  "status": "RUNNING",
  "progress": 45,
  "currentStage": "SCANNING_TABLES",
  "tablesCompleted": 10,
  "totalTables": 25,
  "lastUpdate": "2023-05-19T10:35:00Z"
}
```

#### System Metrics Stream

```
ws://your-api-url/api/ws/metrics?token={jwt_token}
```

Provides a continuous stream of system performance metrics.

**Query Parameters:**
- `token`: JWT authentication token

**Message Format:**
```json
{
  "timestamp": "2023-05-19T10:35:00Z",
  "memoryUsage": 65.4,
  "cpuLoad": 45.2,
  "activeThreads": 32,
  "activeConnections": 8
}
```

### Using WebSockets in React

Here's an example of how to use WebSockets for real-time scan progress in a React application:

```javascript
// ...existing code for WebSocket connections...
```

## Error Handling

### Error Response Format

All error responses follow this standard format:

```json
{
  "timestamp": "2023-05-19T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "path": "/api/v1/scans"
}
```

### Common HTTP Status Codes

- **200 OK**: The request was successful
- **201 Created**: The resource was successfully created
- **204 No Content**: The request was successful but there is no representation to return
- **400 Bad Request**: The request could not be understood or was missing required parameters
- **401 Unauthorized**: Authentication failed or user does not have permissions
- **403 Forbidden**: Access denied to the requested resource
- **404 Not Found**: The requested resource could not be found
- **409 Conflict**: The request could not be completed due to a conflict (e.g., scan not completed yet)
- **415 Unsupported Media Type**: The requested format is not supported
- **500 Internal Server Error**: An error occurred on the server

## Using the API with React.js

### Sample Authentication Code

```javascript
// Login function
const login = async (username, password) => {
  try {
    const response = await fetch('http://your-api-url/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    });
    
    if (!response.ok) {
      throw new Error('Login failed');
    }
    
    const data = await response.json();
    // Store token in localStorage or a secure state management solution
    localStorage.setItem('token', data.token);
    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

// Authenticated API request function
const fetchWithAuth = async (url, options = {}) => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    throw new Error('No authentication token found');
  }
  
  const defaultOptions = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
  
  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  };
  
  const response = await fetch(url, mergedOptions);
  
  if (response.status === 401) {
    // Handle token expiration
    localStorage.removeItem('token');
    window.location.href = '/login';
    throw new Error('Authentication token expired');
  }
  
  return response;
};

// Example usage
const fetchScans = async () => {
  try {
    const response = await fetchWithAuth('http://your-api-url/api/v1/scans');
    if (!response.ok) {
      throw new Error('Failed to fetch scans');
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching scans:', error);
    throw error;
  }
};
```

### Handling WebSocket Connections for Real-time Updates

The API supports WebSocket connections for real-time updates about scan progress:

```javascript
import { useEffect, useState } from 'react';

const ScanProgress = ({ scanId }) => {
  const [progress, setProgress] = useState(0);
  const [status, setStatus] = useState('PENDING');
  
  useEffect(() => {
    const token = localStorage.getItem('token');
    const socket = new WebSocket(`ws://your-api-url/api/ws/scans/${scanId}?token=${token}`);
    
    socket.onopen = () => {
      console.log('WebSocket connection established');
    };
    
    socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setProgress(data.progress);
      setStatus(data.status);
    };
    
    socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
    
    socket.onclose = () => {
      console.log('WebSocket connection closed');
    };
    
    return () => {
      socket.close();
    };
  }, [scanId]);
  
  return (
    <div>
      <h3>Scan Progress</h3>
      <p>Status: {status}</p>
      <progress value={progress} max="100" />
      <p>{progress}% complete</p>
    </div>
  );
};
```

This documentation provides all the necessary information to build a React.js frontend application that interacts with the PrivSense API.