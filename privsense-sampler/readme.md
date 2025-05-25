# PrivSense Sampler Module

## Overview

The PrivSense Sampler module is responsible for efficiently extracting representative data samples from database columns. It implements database-specific optimizations and parallel processing techniques to ensure high performance even with large databases. This module serves as a critical component in the PrivSense data privacy detection pipeline, providing the raw data that will later be analyzed for PII detection.

## Key Features

- **Parallel Sampling**: Multi-threaded implementation for sampling multiple columns concurrently
- **Database-Specific Optimizations**: Custom SQL query generation for different database engines
- **Concurrency Control**: Prevents database overload through configurable query limits
- **Entropy Calculation**: Optional calculation of Shannon entropy for sampled data
- **Sample Size Optimization**: Dynamic sample size determination based on table characteristics

## Architecture

The sampler module follows a strategy pattern with factory implementation to support multiple database systems while maintaining a clean, consistent API.

### Core Components

#### ConsolidatedSamplerImpl

The main implementation of the `ConsolidatedSampler` interface that handles all sampling operations. It supports:
- Single column sampling
- Multiple columns sampling in parallel
- Full schema sampling
- Sample size optimization
- Sample extraction with configuration

The implementation uses a thread pool and semaphores to control concurrent execution and prevent overloading the database.

#### Database-Specific Strategies

The module includes specialized sampling strategies for different database systems:
- `MySqlSamplingStrategy` - Optimized for MySQL databases
- `PostgreSqlSamplingStrategy` - Optimized for PostgreSQL databases
- `OracleSamplingStrategy` - Optimized for Oracle databases
- `SqlServerSamplingStrategy` - Optimized for Microsoft SQL Server

Each strategy implements the `DbSpecificSamplingStrategy` interface and provides database-specific SQL for efficient data sampling.

#### SamplingStrategyFactory

A factory class that provides the appropriate sampling strategy based on the database type. It uses Spring dependency injection to collect all available strategies and select the right one for each database connection.

#### EntropyCalculator

A utility class that calculates Shannon entropy for sampled data, providing insight into data randomness and variability.

## Configuration Options

The module supports configuration through the central `PrivSenseConfigProperties` class:

- `defaultSize`: Default number of rows to sample per column (default: 1000)
- `maxConcurrentDbQueries`: Maximum number of concurrent database queries (default: 5)
- `entropyCalculationEnabled`: Whether to calculate entropy for samples (default: false)
- `defaultMethod`: Default sampling method ("RANDOM", "FIRST_N", etc.) (default: "RANDOM")

## Usage Example

```java
// Autowire the sampler in your service
@Autowired
private ConsolidatedSampler sampler;

// Sample a specific column
SampleData data = sampler.sampleColumn(connection, columnInfo, 1000);

// Sample multiple columns in parallel
List<SampleData> samples = sampler.sampleColumns(connection, columns, 1000);

// Sample an entire schema
Map<ColumnInfo, SampleData> schemaData = sampler.sampleSchema(connection, schemaInfo, 1000);

// Determine optimal sample size for a column
int optimalSize = sampler.determineOptimalSampleSize(connection, columnInfo);

// Sample with specific configuration
SamplingConfig config = SamplingConfig.builder()
    .sampleSize(500)
    .samplingMethod("RANDOM")
    .entropyCalculationEnabled(true)
    .build();
Map<ColumnInfo, SampleData> configuredSamples = sampler.extractSamples(connection, columns, config);
```

## Integration with Other Modules

This module is primarily used by:
- `privsense-api` - For testing sampling operations through the REST API
- `privsense-pii-detector` - To provide data samples for PII detection analysis

## Performance Considerations

- The module uses a fixed thread pool with size based on available processors to balance performance and resource usage
- A semaphore limits concurrent database queries to prevent overwhelming the database
- The fetch size for result sets is capped to improve memory efficiency
- Clean shutdown handling ensures all resources are properly released

## Error Handling

All database-related errors are wrapped in a `DataSamplingException` with contextual information to facilitate troubleshooting. The implementation includes detailed logging of sampling operations and any failures encountered.