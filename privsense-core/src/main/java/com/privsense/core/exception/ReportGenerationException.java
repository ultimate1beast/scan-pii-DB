package com.privsense.core.exception;

/**
 * Exception thrown when there are issues during the report generation process.
 */
public class ReportGenerationException extends PrivSenseException {
    
    public ReportGenerationException(String message) {
        super(message);
    }
    
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ReportGenerationException(Throwable cause) {
        super("Report generation error: " + cause.getMessage(), cause);
    }
}