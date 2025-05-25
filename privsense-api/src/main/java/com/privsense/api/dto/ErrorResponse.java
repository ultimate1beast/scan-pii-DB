package com.privsense.api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

/**
 * Standard error response DTO for API errors.
 * Provides a consistent error format across the API.
 */
@Data
public class ErrorResponse {
    
    /**
     * The HTTP status code of the error.
     */
    private int status;
    
    /**
     * A short error code identifier.
     */
    private String code;
    
    /**
     * The error message.
     */
    private String message;
    
    /**
     * Detailed error information for debugging.
     */
    private String details;
    
    /**
     * The path that caused the error.
     */
    private String path;
    
    /**
     * Timestamp when the error occurred.
     */
    private String timestamp;
    
    /**
     * Default constructor.
     */
    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now().toString();
    }
    
    /**
     * Constructor with status and message.
     * 
     * @param status The HTTP status code
     * @param message The error message
     */
    public ErrorResponse(int status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    /**
     * Constructor with all fields.
     * 
     * @param status The HTTP status code
     * @param code The error code
     * @param message The error message
     * @param details Detailed error information
     * @param path The request path
     */
    public ErrorResponse(int status, String code, String message, String details, String path) {
        this();
        this.status = status;
        this.code = code;
        this.message = message;
        this.details = details;
        this.path = path;
    }
}