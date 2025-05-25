# PrivSense Reporter Module

## Overview
The PrivSense Reporter module is responsible for generating compliance reports based on PII detection results. It provides flexible report generation capabilities with support for multiple output formats including JSON, CSV, text, and PDF.

## Module Structure
```
privsense-reporter/
├── pom.xml                                         # Maven project configuration
├── src/main/java/com/privsense/reporter/
│   ├── ConsolidatedReportServiceImpl.java          # Main report generation implementation
│   ├── factory/
│   │   └── ReportGeneratorFactory.java             # Factory for creating report generators
│   └── service/
│       └── ReportingService.java                   # Facade service for report generation

```

## Features
- Generation of compliance reports from scan context data
- Export of reports in multiple formats (JSON, CSV, text, PDF)
- Consolidation of PII detection results into meaningful reports
- Support for customizable report formatting options
- Masking of sensitive information in reports

## Key Components

### ReportGeneratorFactory
A factory class that provides appropriate report generators based on the requested format. It follows the Factory pattern to create and return different types of report generators.

```java
// Key methods
public ConsolidatedReportService getReporter(String format)
public void registerReporter(String format, ConsolidatedReportService reporter)
```

### ConsolidatedReportServiceImpl
Implementation of the `ConsolidatedReportService` interface that combines functionality from multiple report generation services. Handles the actual generation of compliance reports.

```java
// Key methods
public ComplianceReport generateReport(ScanContext scanContext)
public String exportReportToJson(ComplianceReport report)
public String exportReportToCsv(ComplianceReport report)
public String exportReportToText(ComplianceReport report)
public byte[] exportReportAsBytes(ComplianceReport report, String format)
```

### ReportingService
A facade service that simplifies the process of generating compliance reports in various formats.

```java
// Key methods
public ComplianceReport generateReport(ScanContext context)
public String exportReportToJson(ComplianceReport report)
public String exportReport(ComplianceReport report, String format)
public void exportReport(ComplianceReport report, String format, OutputStream outputStream)
```

## Dependencies
- **privsense-core** - Core models and interfaces for PII detection
- **Spring Framework** - For dependency injection and component management
- **Jackson** - For JSON processing and data binding
- **Apache Commons CSV** - For CSV report generation
- **Apache PDFBox** - For PDF report generation
- **Apache Commons Lang** - For utility functions

## Report Types
1. **JSON**: Structured data format suitable for API responses and machine processing
2. **CSV**: Tabular format suitable for spreadsheet applications and data analysis
3. **Text**: Human-readable plain text format with formatted summaries
4. **PDF**: Document format suitable for formal reports and printing

## Usage Examples

### Generating a Report
```java
// Using ReportingService
ScanContext scanContext = ...; // Obtained from scan process
ReportingService reportingService = ...; // Injected or instantiated
ComplianceReport report = reportingService.generateReport(scanContext);
```

### Exporting a Report to JSON
```java
// Using ReportingService
String jsonReport = reportingService.exportReportToJson(report);
```

### Exporting a Report to Different Formats
```java
// Using ReportingService
String csvReport = reportingService.exportReport(report, "csv");
String textReport = reportingService.exportReport(report, "text");

// Write to file or stream
try (OutputStream outputStream = new FileOutputStream("report.pdf")) {
    reportingService.exportReport(report, "pdf", outputStream);
}
```

### Adding a Custom Report Format
```java
// Using ReportGeneratorFactory
CustomReportService customReporter = new CustomReportService();
reportGeneratorFactory.registerReporter("custom", customReporter);

// Then use it
String customReport = reportingService.exportReport(report, "custom");
```

## Report Content
Generated reports include the following information:
- Scan metadata (ID, timestamps, duration)
- Database information (host, name, product)
- Summary statistics (tables/columns scanned, PII columns found)
- Detailed PII findings with confidence scores
- Quasi-identifier information and risk scores
- Detection method information

## Integration Points
- Integrates with the core module for data models and services
- Can be used by the API module to expose reporting endpoints
- Produces output that can be saved to the file system or database

## Best Practices
- Use the ReportingService facade for most operations
- Register custom report generators with the factory for extensibility
- Configure format options before generating reports for customization
- Handle binary formats (like PDF) with appropriate output streams