package com.privsense.core.exception;

/**
 * Exception thrown when there are issues with application configuration.
 * This includes missing required properties, invalid property values,
 * or configuration that cannot be processed correctly.
 */
public class ConfigurationException extends PrivSenseException {
    
    private static final long serialVersionUID = 1L;
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConfigurationException(Throwable cause) {
        super("Configuration error: " + cause.getMessage(), cause);
    }
}