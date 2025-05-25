package com.privsense.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request contains invalid parameters or data.
 * Will result in a 400 BAD REQUEST response.
 * Used for client-side validation failures.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Creates a new BadRequestException with the specified message.
     *
     * @param message the detail message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Creates a new BadRequestException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}