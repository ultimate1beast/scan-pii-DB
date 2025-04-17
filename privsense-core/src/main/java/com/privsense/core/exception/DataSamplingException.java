package com.privsense.core.exception;

/**
 * Exception thrown when there are issues sampling data from database columns.
 */
public class DataSamplingException extends PrivSenseException {
    
    public DataSamplingException(String message) {
        super(message);
    }
    
    public DataSamplingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DataSamplingException(Throwable cause) {
        super("Failed to sample data: " + cause.getMessage(), cause);
    }
}