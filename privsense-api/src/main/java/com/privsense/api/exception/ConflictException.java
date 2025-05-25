package com.privsense.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a conflict occurs during a resource operation.
 * Will result in a 409 CONFLICT response.
 * Typically used for scenarios like duplicate usernames or emails.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    /**
     * Creates a new ConflictException with the specified message.
     *
     * @param message the detail message
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Creates a new ConflictException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}