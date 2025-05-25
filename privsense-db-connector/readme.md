# PrivSense Database Connector

## Overview

The PrivSense Database Connector module provides a robust and flexible solution for managing database connections within the PrivSense system. It allows the application to connect to various database systems dynamically without requiring all JDBC drivers to be bundled at compile-time.

This module is responsible for:
- Dynamically loading JDBC drivers at runtime
- Managing database connection pools using HikariCP
- Providing a uniform interface for database connections
- Handling connection lifecycle (creation, validation, and cleanup)
- Supporting secure connections with SSL

## Architecture

### Core Components

#### 1. DatabaseConnector Interface
The core interface defining the contract for database connection operations. This interface is implemented in this module but defined in the core module.

#### 2. JdbcDriverLoader
`com.privsense.db.service.JdbcDriverLoader`

A service responsible for dynamically loading JDBC drivers at runtime from the classpath or from an external directory. This allows support for multiple database types without having all drivers bundled in the application.

Key features:
- Dynamic loading of JDBC drivers from external directories
- Driver class validation and registration
- Caching of loaded drivers for performance

#### 3. DatabaseConnectorImpl
`com.privsense.db.service.impl.DatabaseConnectorImpl`

The primary implementation of the DatabaseConnector interface, managing connections using HikariCP connection pools.

Key features:
- Connection pooling with HikariCP for performance
- Secure storage of connection information
- Connection validation and testing
- Dynamic driver loading using JdbcDriverLoader
- Connection cleanup during application shutdown
- SSL support for secure database connections

## Dependencies

- **HikariCP**: High-performance JDBC connection pool
- **Spring Framework**: For dependency injection and lifecycle management
- **JDBC Drivers**:
  - MySQL Connector/J (v8.2.0)
  - PostgreSQL JDBC Driver (v42.6.0)
- **Resilience4j**: For resilience patterns (circuit breakers, retries)
- **PrivSense Core**: Provides interfaces and models used by this module

## Usage

### Connecting to a Database

To connect to a database:

```java
// Create connection information
DatabaseConnectionInfo connectionInfo = DatabaseConnectionInfo.builder()
    .host("localhost")
    .port(3306)
    .databaseName("myDatabase")
    .username("user")
    .password("password")
    .jdbcDriverClass("com.mysql.cj.jdbc.Driver")
    .sslEnabled(true)
    .build();

// Connect using the connector
UUID connectionId = databaseConnector.connect(connectionInfo);

// Later, get a connection from the pool
try (Connection connection = databaseConnector.getConnection(connectionId)) {
    // Use the connection
}
```

### JDBC Driver Configuration

JDBC drivers can be loaded from:

1. **Classpath**: Drivers bundled with the application will be automatically detected
2. **External Directory**: Additional drivers can be placed in an external directory specified by the configuration property `privsense.db.jdbc.driver-dir`

### Connection Pooling

This module uses HikariCP for connection pooling with these configurable properties:

- `connectionTimeout`: Maximum time to wait for a connection (default: 30 seconds)
- `idleTimeout`: Maximum time a connection can sit idle (default: 10 minutes)
- `maxLifetime`: Maximum lifetime of a connection (default: 30 minutes)
- `minimumIdle`: Minimum number of idle connections (default: 5)
- `maximumPoolSize`: Maximum size of the connection pool (default: 10)

These properties are configured through the PrivSenseConfigProperties class.

### SSL Support

The module supports secure connections with SSL/TLS:

- Enable SSL by setting `sslEnabled` to true in the DatabaseConnectionInfo
- Optionally specify a truststore path and password for custom certificate validation

## Security Considerations

- Passwords are never stored in the persistent repository
- Connection URLs in logs have passwords redacted
- SSL support for encrypted connections
- Connection pools are properly closed during application shutdown

## Error Handling

The module uses a custom DatabaseConnectionException for error reporting. Common errors include:

- Failed driver loading
- Connection establishment failures
- Validation errors
- Connection pool issues

## Thread Safety

The implementation is thread-safe, using:
- Synchronization for driver loading
- ConcurrentHashMap for storing connection pools
- Thread-safe connection pools from HikariCP

## Extension Points

Support for new database systems can be added by:
1. Adding appropriate JDBC drivers
2. Placing driver JARs in the configured driver directory
3. Specifying the driver class name when connecting