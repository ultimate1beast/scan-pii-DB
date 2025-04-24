# PrivSense Configuration Migration Guide

## Overview

This document provides guidance on migrating from the previous distributed configuration approach to the new unified configuration structure in the PrivSense application.

## Configuration Consolidation

### Problem

Previously, PrivSense had multiple configuration classes spread across different modules:

- `DetectionConfigProperties` in the API module
- `SamplingConfigProperties` in the API module
- `NerServiceConfigProperties` in the API module
- Various other configuration classes in different modules

This approach led to several issues:
- Redundancy in configuration definitions
- Inconsistent property names and structures
- Difficulty in maintaining a coherent configuration approach
- Risk of configuration drift between modules

### Solution

We've consolidated all configuration into a unified structure:

1. **Single Source of Truth**: Created `PrivSenseConfigProperties` in the core module as the central configuration class.
2. **Proper Validation**: Added JSR-380 validation annotations for better configuration validation.
3. **Backward Compatibility**: Maintained adapter classes (marked as deprecated) for backward compatibility.
4. **Clear Structure**: Organized configuration properties into logical domains.

## Migration Steps for Application Code

### Step 1: Update Dependencies

Ensure your module depends on `privsense-core` to access the unified configuration:

```xml
<dependency>
    <groupId>com.privsense</groupId>
    <artifactId>privsense-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 2: Update Injection Points

Replace injections of old configuration classes:

```java
// Old approach
@Autowired
private DetectionConfigProperties detectionConfig;
@Autowired
private SamplingConfigProperties samplingConfig;

// New approach
@Autowired
private PrivSenseConfigProperties config;
```

### Step 3: Update Property Access

Update how you access configuration properties:

```java
// Old approach
double threshold = detectionConfig.getThresholds().getHeuristic();
int sampleSize = samplingConfig.getDefaultSize();

// New approach
double threshold = config.getDetection().getHeuristicThreshold();
int sampleSize = config.getSampling().getDefaultSize();
```

## Migration Steps for Configuration Files

### application.yml/properties Structure

Update your configuration files to follow the new structure:

```yaml
privsense:
  sampling:
    default-size: 1000
    max-concurrent-db-queries: 5
    entropy-calculation-enabled: false
    default-method: RANDOM
  
  detection:
    heuristic-threshold: 0.7
    regex-threshold: 0.8
    ner-threshold: 0.6
    reporting-threshold: 0.5
    stop-pipeline-on-high-confidence: true
    entropy-enabled: false
  
  ner:
    service:
      url: http://localhost:5000/detect-pii
      timeout-seconds: 30
      circuit-breaker:
        enabled: true
        failure-threshold: 5
        reset-timeout-seconds: 30
        
  db:
    pool:
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      minimum-idle: 5
      maximum-pool-size: 10
    jdbc:
      driver-dir: ./drivers
```

## Deprecation Timeline

The old configuration classes will be maintained with deprecated status until version 2.0.0:

- `DetectionConfigProperties` - Deprecated, will be removed in v2.0.0
- `SamplingConfigProperties` - Deprecated, will be removed in v2.0.0
- `NerServiceConfigProperties` - Deprecated, will be removed in v2.0.0

## Support

If you encounter issues migrating to the new configuration structure, please contact the PrivSense development team.