# PrivSense PII Detector Module

## Overview

The PrivSense PII Detector module is responsible for identifying personally identifiable information (PII) and quasi-identifiers in data samples. It employs a multi-strategy approach to detect different types of sensitive data with high accuracy and provides configurable thresholds for detection sensitivity.

## Architecture

### Core Components

The module consists of the following core components:

1. **PII Detection Strategies**
   - `HeuristicStrategy` - Analyzes column metadata (names, comments) for PII-related keywords
   - `RegexStrategy` - Uses regular expressions to identify common PII patterns
   - `NerClientStrategy` - Leverages an external Named Entity Recognition service for advanced PII detection

2. **Quasi-Identifier Detection**
   - `QuasiIdentifierAnalyzer` - Coordinates the analysis of quasi-identifiers
   - `ClusteringBasedQuasiIdentifierDetector` - Uses machine learning techniques to identify correlated columns
   - `ColumnCorrelationCalculator` - Calculates correlations between columns
   - `ColumnValueDistributionAnalyzer` - Analyzes the distribution characteristics of column values

3. **Detection Orchestration**
   - `PiiDetectorImpl` - Orchestrates the execution of multiple detection strategies in a pipeline

## PII Detection Strategies

### Heuristic Strategy

The Heuristic Strategy analyzes column metadata (names and comments) to identify PII by matching against a predefined set of keywords with confidence scores. This is typically the first strategy in the detection pipeline.

**Key features:**
- Configurable keywords and confidence scores via properties
- Exact and partial matching on column names
- Matching on column comments
- Language support for English and French keywords

### Regex Strategy

The Regex Strategy uses regular expression patterns to identify common PII formats in data samples.

**Key features:**
- Extensive library of predefined patterns for various PII types
- Confidence scoring based on match quality and pattern specificity
- Pattern matching with validation (e.g., checksum validation for credit cards)
- Internationalization support for different formats

### NER Client Strategy

The NER Client Strategy uses a non-blocking HTTP client to communicate with an external Named Entity Recognition service for advanced PII detection.

**Key features:**
- Reactive programming with Spring WebClient for non-blocking API calls
- Automatic circuit-breaking and retry mechanisms
- JSON-based communication with external NER service
- Confidence score calculation based on entity type, frequency, and match quality
- Mapping of NER entity types to standard PII types
- Text masking for privacy in logs
- Health check for service availability

## Quasi-Identifier Detection

The Quasi-Identifier detection subsystem identifies columns that may not be direct PII but can be combined to potentially identify individuals.

**Key components:**

1. **QuasiIdentifierAnalyzer**
   - Coordinates the entire QI detection process
   - Filters out PII columns and key columns
   - Manages persistence of detected QI groups
   - Updates detection results with QI information

2. **ClusteringBasedQuasiIdentifierDetector**
   - Uses machine learning clustering techniques to identify related columns
   - Calculates re-identification risk scores
   - Supports multiple clustering algorithms

3. **ColumnCorrelationCalculator**
   - Computes statistical correlations between columns
   - Supports various correlation measures (Pearson, Spearman, etc.)

4. **ColumnValueDistributionAnalyzer**
   - Analyzes distributions of column values
   - Calculates distinctness and entropy metrics

## Orchestration and Workflow

The `PiiDetectorImpl` class orchestrates the execution of all detection strategies in a configurable pipeline:

1. First executes the HeuristicStrategy
2. If no high-confidence PII is found, executes the RegexStrategy
3. If still no high-confidence PII is found, executes the NerClientStrategy
4. Resolves conflicts between detection strategies
5. Runs quasi-identifier detection if enabled

**Workflow features:**
- Early termination option when high-confidence PII is found
- Strategy-specific confidence thresholds
- Comprehensive result tracking and logging
- Optional scan metadata association

## Configuration

The module is highly configurable through Spring Boot's application properties and provides the following configuration options:

```properties
# General detection settings
privsense.detection.reporting-threshold=0.7
privsense.detection.stop-pipeline-on-high-confidence=true

# Strategy-specific thresholds
privsense.detection.heuristic-threshold=0.8
privsense.detection.regex-threshold=0.85
privsense.detection.ner-threshold=0.75

# NER service configuration
privsense.ner.service.url=http://localhost:8000
privsense.ner.service.timeout-seconds=10
privsense.ner.service.max-samples=50
privsense.ner.service.retry-attempts=3
privsense.ner.service.circuit-breaker.enabled=true

# Quasi-identifier detection configuration
privsense.quasi-identifier.enabled=true
privsense.quasi-identifier.correlation-threshold=0.7
privsense.quasi-identifier.clustering.algorithm=DBSCAN
```

## Dependencies

This module depends on:

1. **Internal dependencies:**
   - `privsense-core` - Core models, interfaces, and utilities

2. **External dependencies:**
   - Spring Framework (Context, WebFlux)
   - Jackson for JSON processing
   - Apache Commons Lang3 and Math3 for utility functions
   - Google Guava for collections and caching
   - libphonenumber for phone number validation
   - Stanford CoreNLP for advanced text processing (optional)

## Integration

The PII Detector integrates with:

1. **External NER service**
   - Communicates via REST API
   - JSON-based request/response
   - Health check endpoint for availability monitoring

2. **Database persistence**
   - Stores detection results and quasi-identifier groups
   - Transaction support for safe persistence

## Technical Details

### Performance Considerations

- Non-blocking I/O for NER service calls
- Early termination to reduce processing time
- Caching of NER results
- Reflection optimization for high-throughput scenarios

### Extensibility

- Strategy interface allows for easy addition of new detection approaches
- Configuration-based enabling/disabling of strategies
- Custom thresholds for each strategy
- Pluggable quasi-identifier detection algorithms

### Thread Safety

- Stateless design for detection strategies
- Thread-safe implementation for concurrent detection
- Proper resource handling and cleanup

## Usage

The primary entry point is the `PiiDetectorImpl` class, which is injected as a `PiiDetector` service:

```java
@Autowired
private PiiDetector piiDetector;

// Detect PII in a single column
DetectionResult result = piiDetector.detectPii(columnInfo, sampleData);

// Detect PII in multiple columns
Map<ColumnInfo, SampleData> columnDataMap = // ...
List<DetectionResult> results = piiDetector.detectPii(columnDataMap);

// Detect PII with scan metadata for persistence
ScanMetadata scanMetadata = // ...
List<DetectionResult> results = piiDetector.detectPii(columnDataMap, scanMetadata);
```