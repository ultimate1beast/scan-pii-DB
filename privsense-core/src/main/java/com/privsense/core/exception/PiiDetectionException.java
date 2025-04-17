package com.privsense.core.exception;

/**
 * Exception thrown when there are issues during the PII detection process.
 */
public class PiiDetectionException extends PrivSenseException {
    
    public PiiDetectionException(String message) {
        super(message);
    }
    
    public PiiDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PiiDetectionException(Throwable cause) {
        super("PII detection error: " + cause.getMessage(), cause);
    }
}