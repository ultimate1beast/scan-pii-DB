package com.privsense.api.exception;

/**
 * Exception thrown when there is an error retrieving or processing dashboard data.
 */
public class DashboardException extends RuntimeException {

    /**
     * Constructs a new dashboard exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DashboardException(String message) {
        super(message);
    }

    /**
     * Constructs a new dashboard exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }
}