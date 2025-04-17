package com.privsense.api.exception;

import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.exception.DataSamplingException;
import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.exception.PrivSenseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for standardizing error responses across the API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all custom PrivSense exceptions with appropriate status codes
     */
    @ExceptionHandler(PrivSenseException.class)
    public ResponseEntity<Object> handlePrivSenseException(PrivSenseException ex, WebRequest request) {
        HttpStatus status;
        
        // Map specific exceptions to appropriate HTTP status codes
        if (ex instanceof DatabaseConnectionException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof MetadataExtractionException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof DataSamplingException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof PiiDetectionException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return buildErrorResponse(ex.getMessage(), status, request);
    }
    
    /**
     * Handles validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Validation failed");
        body.put("path", request.getDescription(false).replace("uri=", ""));
        body.put("fieldErrors", fieldErrors);
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Catch-all for any other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtExceptions(Exception ex, WebRequest request) {
        return buildErrorResponse(
                "An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }
    
    /**
     * Helper method to build consistent error responses
     */
    private ResponseEntity<Object> buildErrorResponse(
            String message, HttpStatus status, WebRequest request) {
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(body, status);
    }
}