package com.privsense.core.exception;

/**
 * Base exception class for all exceptions in the PrivSense application.
 */
public class PrivSenseException extends RuntimeException {
    
    public PrivSenseException(String message) {
        super(message);
    }
    
    public PrivSenseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PrivSenseException(Throwable cause) {
        super(cause);
    }
}