package com.privsense.core.exception;

/**
 * Exception thrown when there are issues establishing or maintaining database connections.
 */
public class DatabaseConnectionException extends PrivSenseException {
    
    public DatabaseConnectionException(String message) {
        super(message);
    }
    
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseConnectionException(Throwable cause) {
        super("Database connection error: " + cause.getMessage(), cause);
    }
}