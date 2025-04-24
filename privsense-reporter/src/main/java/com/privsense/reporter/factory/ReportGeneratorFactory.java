package com.privsense.reporter.factory;

import com.privsense.core.service.ComplianceReporter;
import com.privsense.reporter.ComplianceReporterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory that provides the appropriate report generator based on the requested format.
 * This follows the Factory pattern to create and return different types of report generators.
 */
@Component
public class ReportGeneratorFactory {

    private final Map<String, ComplianceReporter> reportGenerators = new HashMap<>();
    
    @Autowired
    public ReportGeneratorFactory(ComplianceReporterImpl jsonReporter) {
        // Register the available report generators
        reportGenerators.put("json", jsonReporter);
        reportGenerators.put("default", jsonReporter);
        
        // Additional report generators can be registered as they are implemented
        // e.g., reportGenerators.put("pdf", pdfReporter);
    }
    
    /**
     * Gets a reporter based on the requested format.
     * 
     * @param format The report format (json, pdf, html, etc.)
     * @return The appropriate reporter, or the default reporter if format not supported
     */
    public ComplianceReporter getReporter(String format) {
        return Optional.ofNullable(reportGenerators.get(format.toLowerCase()))
                .orElseGet(() -> reportGenerators.get("default"));
    }
    
    /**
     * Registers a new report generator for a specific format.
     * 
     * @param format The format the generator handles
     * @param reporter The reporter implementation
     */
    public void registerReporter(String format, ComplianceReporter reporter) {
        reportGenerators.put(format.toLowerCase(), reporter);
    }
}