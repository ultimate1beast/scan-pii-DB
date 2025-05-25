package com.privsense.api.exception;

/**
 * Exception thrown for general service-level failures.
 * Used when a service operation fails due to business logic
 * or underlying technical issues.
 */
public class ServiceException extends RuntimeException {

    /**
     * Constructs a new service exception with the specified detail message.
     *
     * @param message The detail message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new service exception with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}