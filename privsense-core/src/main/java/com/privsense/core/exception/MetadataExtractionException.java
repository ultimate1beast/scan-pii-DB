package com.privsense.core.exception;

/**
 * Exception thrown when there are issues extracting metadata from a database.
 */
public class MetadataExtractionException extends PrivSenseException {
    
    public MetadataExtractionException(String message) {
        super(message);
    }
    
    public MetadataExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MetadataExtractionException(Throwable cause) {
        super("Failed to extract database metadata: " + cause.getMessage(), cause);
    }
}