package com.privsense.api.exception;

/**
 * Exception thrown during authentication process.
 * Used for login failures, token validation issues, etc.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}