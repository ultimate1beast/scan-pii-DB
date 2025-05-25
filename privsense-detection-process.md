# PrivSense PII Detection Process

This document provides a comprehensive explanation of how the PrivSense PII (Personally Identifiable Information) detection system works. We will walk through each step of the process and explain how the system identifies both direct PII and quasi-identifiers (QIs) using real data examples.

## Table of Contents

1. [Overview of the Detection Pipeline](#overview-of-the-detection-pipeline)
2. [Scan Initiation and Configuration](#scan-initiation-and-configuration)
3. [Database Connection and Metadata Extraction](#database-connection-and-metadata-extraction)
4. [Data Sampling Process](#data-sampling-process)
5. [PII Detection Strategies](#pii-detection-strategies)
   - [Heuristic Detection](#heuristic-detection)
   - [Regex Pattern Matching](#regex-pattern-matching)
   - [Named Entity Recognition (NER)](#named-entity-recognition-ner)
6. [Quasi-Identifier Detection](#quasi-identifier-detection)
   - [Column Filtering and Preparation](#column-filtering-and-preparation)
   - [Group Detection Approaches](#group-detection-approaches)
   - [Risk Assessment](#risk-assessment)
   - [Results and Use Cases](#results-and-use-cases)
7. [Report Generation and Analysis](#report-generation-and-analysis)
8. [Real-world Example with Sample Data](#real-world-example-with-sample-data)
   - [Detailed Quasi-Identifier Analysis](#detailed-quasi-identifier-analysis)
9. [Performance Considerations](#performance-considerations)
10. [Conclusion](#conclusion)

## Overview of the Detection Pipeline

PrivSense uses a multi-stage pipeline to detect and classify sensitive data in databases. The complete pipeline consists of:

1. **Scan initiation**: User configures and submits a scan request
2. **Database connection**: System establishes a connection to the target database
3. **Metadata extraction**: Schema, tables, and columns are identified
4. **Sampling**: Data is sampled from each column
5. **PII detection**: Multiple detection strategies are applied
6. **Quasi-identifier detection**: ML clustering to identify correlated identifiers
7. **Report generation**: Findings are compiled into a comprehensive report

Each stage includes configurable parameters to optimize for accuracy, performance, and specific use cases.

## Scan Initiation and Configuration

The detection process begins when a user submits a scan request to the API. This request includes configuration parameters that control how the scan will operate:

```json
{
  "connectionId": "550e8400-e29b-41d4-a716-446655440000",
  "targetTables": ["t_usr_data", "t_txn_data", "t_med_data", "t_emp_rec"],
  "samplingConfig": {
    "sampleSize": 80,
    "samplingMethod": "RANDOM",
    "entropyCalculationEnabled": true,
    "maxConcurrentQueries": 5
  },
  "detectionConfig": {
    "heuristicThreshold": 0.7,
    "regexThreshold": 0.8,
    "nerThreshold": 0.3,
    "reportingThreshold": 0.5,
    "stopPipelineOnHighConfidence": true,
    "entropyCalculationEnabled": true,
    "quasiIdentifier": {
      "enabled": true
    }
  }
}
```

These parameters control:
- Which tables to scan (targeted approach)
- How much data to sample (balancing accuracy vs performance)
- Detection thresholds for different strategies
- Whether to enable advanced features like quasi-identifier detection

## Database Connection and Metadata Extraction

Once the scan is initiated, PrivSense connects to the database using the provided connection ID and begins extracting metadata:

1. **Schema detection**: The system identifies available schemas
2. **Table enumeration**: All tables (or targeted tables) are enumerated
3. **Column metadata**: For each table, column names, data types, and key relationships are extracted
4. **Relationship mapping**: Primary key, foreign key, and other relationships are identified

This metadata provides context for the subsequent analysis and helps optimize the sampling and detection strategies based on data types and relationships.

For example, in our sample database:
- Tables like `t_usr_data`, `t_emp_rec`, `t_txn_data`, and `t_med_data` were identified
- Their columns and data types were extracted (e.g., `vctr_1` in `t_emp_rec` as VARCHAR)
- Primary keys (`rec_id`, `usr_id`, etc.) were identified
- Foreign key relationships were mapped (e.g., `usr_fk` in `t_med_data` referencing `usr_id` in `t_usr_data`)

## Data Sampling Process

To analyze data efficiently without processing entire tables, PrivSense employs intelligent sampling:

1. **Sampling strategy selection**: Based on configuration (RANDOM, SYSTEMATIC, or STRATIFIED)
2. **Query construction**: Database-specific optimal queries are built
3. **Concurrent execution**: Multiple sampling operations run in parallel (controlled by `maxConcurrentQueries`)
4. **Sample collection**: Results are collected into `SampleData` objects for each column

For our example scan, with `sampleSize: 80` and `samplingMethod: "RANDOM"`, the system constructs queries like:

```sql
-- For MySQL:
SELECT `vctr_1` FROM `t_emp_rec` ORDER BY RAND() LIMIT 80;

-- For PostgreSQL:
SELECT "data_1" FROM "t_usr_data" TABLESAMPLE SYSTEM (10) LIMIT 80;
```

These queries ensure we get representative data while minimizing database load. Sample data for column `t_emp_rec.vctr_1` might include values like "John Robert Smith", "Ana Maria Garcia", etc.

## PII Detection Strategies

PrivSense employs multiple detection strategies in a configurable pipeline. Each strategy has strengths for different data types:

### Heuristic Detection

The first line of defense uses metadata-based heuristics:

1. **Column name analysis**: Examines column names for PII-related keywords (e.g., "email", "ssn", "phone")
2. **Column comment analysis**: Looks for PII indicators in any database comments associated with the column
3. **Confidence scoring**: Assigns different confidence scores based on match type (exact name match, partial name match, or comment match)

Example: When examining a column named "cr_card_num", the heuristic detector would recognize the keywords "card" and potentially "cr" (credit) and flag it as a possible credit card field with appropriate confidence.

The actual pattern analysis of data values is performed by the Regex Strategy, not the Heuristic Strategy.

### Regex Pattern Matching

More precise than heuristics, regex patterns target specific PII formats:

1. **Pattern matching**: Applies predefined regex patterns from configuration
2. **Confidence scoring**: Assigns scores based on pattern match quality
3. **Categorization**: Maps matched patterns to PII types

PrivSense includes dozens of built-in regex patterns for common PII types. For example:

```
Email: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}
SSN: \\b(?!000|666|9\\d\\d)\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b
Phone: \\b\\(\\d{3}\\)\\s?\\d{3}-\\d{4}\\b|\\b\\d{3}-\\d{3}-\\d{4}\\b
```

In our sample data, this strategy identified:
- Email addresses in `t_usr_data.data_1` like "jsmith84@example.com" (confidence: 0.9)
- SSNs in `t_usr_data.uni_code` like "078-05-1120" (confidence: 0.867)
- Phone numbers in `t_usr_data.data_3` like "617-555-0192" (confidence: 0.8)

### Named Entity Recognition (NER)

The most sophisticated detection strategy leverages machine learning:

1. **ML model invocation**: Sends sample data to the integrated NER service
2. **Entity extraction**: The model identifies entities like PERSON, ORGANIZATION, LOCATION
3. **Confidence scoring**: Returns confidence values for each detected entity

PrivSense uses a specialized NER model trained on PII data that's stored in the `ner-service` folder. The service is accessed via API:

```
POST http://localhost:5000/detect-pii
{
  "text": "John Robert Smith works at Acme Corp.",
  "threshold": 0.3
}
```

Response:
```
{
  "entities": [
    {"text": "John Robert Smith", "label": "PERSON", "score": 0.901},
    {"text": "Acme Corp", "label": "ORGANIZATION", "score": 0.724}
  ]
}
```

In our sample, the NER model recognized:
- Person names in `t_emp_rec.vctr_1` like "John Robert Smith" (confidence: 0.901)
- Organization references in transaction data

## Quasi-Identifier Detection

Beyond direct PII, PrivSense detects quasi-identifiers - columns that aren't sensitive alone but can be combined to identify individuals. The process follows these steps:

### Column Filtering and Preparation

The `QuasiIdentifierAnalyzer` first prepares the data for analysis:

1. **Eligible column selection**: 
   - Filters out direct PII columns (already detected as containing sensitive information)
   - Excludes primary/foreign key columns (which are designed to be identifiers)
   - Applies configurable constraints like `minDistinctValueCount` and `maxDistinctValueRatio`
   - Filters columns based on their entropy values to ensure they contain meaningful information

2. **Correlation analysis**:
   - Calculates pairwise correlations between all eligible columns using `ColumnCorrelationCalculator`
   - Uses Pearson correlation for numeric columns and Cramér's V for categorical columns
   - Constructs a correlation matrix for further analysis

### Group Detection Approaches

The `ClusteringBasedQuasiIdentifierDetector` implements two different approaches, selected based on configuration:

1. **Graph-based approach** (default):
   - Creates a graph where columns are vertices
   - Adds edges between columns with correlation above the `correlationThreshold`
   - Uses breadth-first search to find connected components (column groups)
   - Filters groups by size constraints (`minGroupSize` and `maxGroupSize`)

2. **ML-based DBSCAN clustering** (when `useMachineLearning` is enabled):
   - Converts correlation values to distances (1 - |correlation|)
   - Applies DBSCAN clustering to identify column groups with similar patterns
   - Uses adaptive distance thresholds to find meaningful clusters
   - May retry clustering with relaxed parameters if no clusters are found initially

### Risk Assessment

For each identified group, risk is calculated using a weighted formula:

1. **K-anonymity factor**:
   - Based on the estimated number of distinct combinations
   - Normalized against a configurable `kAnonymityThreshold`
   - Formula: `kAnonymityFactor = min(kAnonymityThreshold / (kAnonymity + 1), 1)`

2. **Entropy contribution**:
   - Calculates the average normalized entropy of columns in the group
   - Higher entropy indicates more identifying information
   - Formula: `normalizedEntropy = avgEntropy / maxPossibleEntropy`

3. **Final risk calculation**:
   ```
   risk = 0.6 * kAnonymityFactor + 0.4 * normalizedEntropy
   ```

The system flags all columns in high-risk groups as quasi-identifiers, annotating them with:
- The calculated risk score
- The clustering method used (either "GRAPH_CORRELATION" or "ML_CLUSTERING")
- A list of correlated columns in the same group

### Results and Use Cases

This approach effectively identifies combinations of seemingly innocuous columns that together pose privacy risks, such as:

1. **Demographic combinations** like [ZIP code + Age + Gender] that can uniquely identify individuals
2. **Behavioral attributes** that correlate strongly enough to form identification patterns
3. **Time-sequence data** that may expose usage patterns unique to individuals

By identifying these quasi-identifiers, PrivSense provides a more comprehensive privacy assessment than systems that only detect direct PII.

## Report Generation and Analysis

After detection completes, PrivSense generates a comprehensive report:

1. **Summary statistics**: Total tables/columns scanned, PII found
2. **Detection results**: For each column, detailed information about detected PII
3. **Risk assessment**: Overall risk profile of the database
4. **Quasi-identifier groups**: Detailed information about QI correlations

The report can be exported in multiple formats (JSON, CSV, PDF, text) for integration with other systems.

## Real-world Example with Sample Data

Let's walk through the entire process with our sample data:

1. **Scan initiation**: 
   - User submits scan request targeting tables: `t_usr_data`, `t_txn_data`, `t_med_data`, `t_emp_rec`
   - Configuration includes 80 samples per column and enables quasi-identifier detection

2. **Data sampling**:
   - From `t_emp_rec`, samples include employee records with names, emails, etc.
   - From `t_usr_data`, samples include user data with emails, phone numbers, and SSNs

3. **PII detection**:
   - **Heuristic detection** identifies potential credit card numbers in `t_emp_rec.cr_num`
   - **Regex detection** finds:
     - Emails in `t_emp_rec.vctr_2` like "jrsmith@personal.net"
     - SSNs in `t_usr_data.uni_code` like "078-05-1120"
     - Phone numbers in `t_usr_data.data_3` like "617-555-0192"
   - **NER detection** identifies:
     - Person names in `t_emp_rec.vctr_1` like "John Robert Smith"
     - Organization names in `t_txn_data.loc_dt`

### Detailed Quasi-Identifier Analysis

Now let's walk through a detailed step-by-step analysis of how the quasi-identifier detection worked on our sample data:

#### Step 1: Eligible Column Selection

First, the system filters out unsuitable columns:

1. **Excluded direct PII columns** (already identified in previous steps):
   - `t_emp_rec.vctr_1` (person names)
   - `t_emp_rec.vctr_2` (email addresses)
   - `t_usr_data.data_1` (email addresses)
   - `t_usr_data.data_3` (phone numbers)
   - `t_usr_data.uni_code` (SSNs)
   - `t_txn_data.cc_val` (credit card numbers)

2. **Excluded primary/foreign key columns**:
   - `t_emp_rec.rec_id`
   - `t_usr_data.usr_id`
   - `t_txn_data.txn_id`
   - `t_act_log.log_id`
   - `t_med_data.data_id`

3. **Entropy and distribution filtering**:
   - System calculated entropy for each column using Shannon entropy formula
   - Applied minimum distinct value count threshold (>= 5)
   - Applied maximum distinct value ratio threshold (<= 0.8)
   - Columns with entropy below 0.3 were excluded

4. **Eligible columns that passed filtering**:
   - `t_emp_rec.emp_code` (VARCHAR)
   - `t_emp_rec.dept_cd` (VARCHAR)
   - `t_emp_rec.lvl_val` (INT)
   - `t_usr_data.eth_val` (VARCHAR)
   - `t_usr_data.sal_val` (DECIMAL)
   - `t_usr_data.dt_val` (DATE)
   - `t_txn_data.amt_val` (DECIMAL)
   - And several others that passed initial filtering criteria

#### Step 2: Correlation Analysis

The system calculated a correlation matrix between all eligible columns:

1. **Different correlation methods based on data type**:
   - For numeric column pairs: Pearson correlation coefficient
   - For categorical column pairs: Cramér's V (based on chi-square statistic)
   - For mixed pairs: Cramér's V

2. **Excerpt from the resulting correlation matrix**:
```
Correlation Matrix (excerpt):
                emp_code  dept_cd  lvl_val  eth_val  sal_val  dt_val  ...
emp_code           1.000    0.879    0.836    0.814    0.803   0.123  ...
dept_cd            0.879    1.000    0.893    0.824    0.845   0.146  ...
lvl_val           0.836    0.893    1.000    0.874    0.921   0.182  ...
eth_val           0.814    0.824    0.874    1.000    0.867   0.154  ...
sal_val           0.803    0.845    0.921    0.867    1.000   0.203  ...
dt_val            0.123    0.146    0.182    0.154    0.203   1.000  ...
...                 ...      ...      ...      ...      ...     ...  ...
```

#### Step 3: Column Group Detection

The system identified column groups using one of two approaches (based on configuration):

1. **Graph-based approach** (default):
   - Created a graph where columns are vertices
   - Added edges between columns with correlation above 0.7
   - Used breadth-first search to find connected components
   - Identified Group 1: `emp_code`, `dept_cd`, `lvl_val`, `eth_val`, `sal_val` with average correlation 0.856
   - Other potential groups had correlations below threshold and were excluded

2. **DBSCAN clustering** (when ML option was enabled):
   - Converted correlation values to distances (1 - |correlation|)
   - Applied DBSCAN with eps=0.3 and minPoints=3
   - Identified the same group as the graph approach
   - Added "ML_CLUSTERING" tag to the results

#### Step 4: Risk Assessment

For the identified Group 1, the system calculated risk metrics:

1. **Estimated distinct combinations**:
   - Analyzed sample distribution of each column
   - Calculated approximate distinct combinations: 68
   - k-anonymity value: 1.17 (80 records / 68 unique combinations)
   - k-anonymity factor: 0.85

2. **Entropy assessment**:
   - Calculated normalized entropy for each column
   - Average normalized entropy: 0.76
   - This indicates high information content in the column group

3. **Final risk score calculation**:
   ```
   risk = (0.6 * 0.85) + (0.4 * 0.76)
        = 0.51 + 0.30
        = 0.81
   ```

The final risk score of 0.81 exceeded the high-risk threshold of 0.7, so these columns were flagged as quasi-identifiers in the report.

#### Step 5: Result Annotation

For each column in the group, the system:

1. **Updated detection results**:
   - Set `isQuasiIdentifier` flag to true
   - Set `quasiIdentifierRiskScore` to 0.81
   - Added the clustering method ("GRAPH_CORRELATION" or "ML_CLUSTERING")
   - Added list of correlated columns to each result

2. **Generated evidence information**:
   ```
   emp_code  dept_cd  lvl_val  eth_val  sal_val   count
   EMP001    IT       3        CAUC     75000     1
   EMP002    MKT      4        ASIAN    82000     1
   EMP003    HR       2        HISP     63000     1
   ...
   ```

This demonstrates how PrivSense successfully identified a combination of seemingly innocent columns that together create a significant re-identification risk.

## Performance Considerations

PrivSense includes several optimizations for performance:

1. **Sampling**: Only analyzes a subset of data
2. **Concurrent processing**: Parallel execution of sampling and detection
3. **Early termination**: `stopPipelineOnHighConfidence` skips unnecessary detection steps
4. **Targeted scanning**: Only processes specified tables

These optimizations allow scanning large databases efficiently while maintaining high accuracy.

## Conclusion

PrivSense's multi-layered detection approach provides comprehensive identification of sensitive data:

1. **Multiple detection strategies** catch different types of PII
2. **Advanced machine learning techniques** enhance detection capabilities:
   - Named Entity Recognition for unstructured text
   - Clustering and dimensionality reduction for quasi-identifier detection
   - Risk scoring algorithms for prioritization
3. **Quasi-identifier detection** finds hidden re-identification risks that traditional tools miss
4. **Configurable thresholds and algorithms** allow tuning for specific use cases and data environments

This comprehensive approach ensures organizations can identify and protect all sensitive data in their databases, both obvious PII and subtle quasi-identifiers that might otherwise be overlooked, providing a robust foundation for privacy compliance and data governance initiatives.