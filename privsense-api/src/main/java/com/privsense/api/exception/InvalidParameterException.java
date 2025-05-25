package com.privsense.api.exception;

/**
 * Exception thrown when an API request contains invalid parameters.
 */
public class InvalidParameterException extends RuntimeException {

    /**
     * Constructs a new invalid parameter exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidParameterException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid parameter exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}