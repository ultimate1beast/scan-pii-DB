package com.privsense.api.exception;

import jakarta.validation.ConstraintViolationException;

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
 * Global exception handler to standardize error responses across the API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException and returns a 404 NOT FOUND response.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }
    
    /**
     * Handles ConflictException and returns a 409 CONFLICT response.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(
            ConflictException ex, WebRequest request) {
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }
    
    /**
     * Handles BadRequestException and returns a 400 BAD REQUEST response.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }
    
    /**
     * Handles validation exceptions from @Valid annotations and returns a 400 BAD REQUEST response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return createValidationErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }
    
    /**
     * Handles constraint violation exceptions and returns a 400 BAD REQUEST response.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        
        return createValidationErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }
    
    /**
     * Creates a standardized error response with the given status, message, and request information.
     */
    private ResponseEntity<Object> createErrorResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(body, status);
    }
    
    /**
     * Creates a standardized validation error response with detailed field errors.
     */
    private ResponseEntity<Object> createValidationErrorResponse(
            HttpStatus status, String message, Map<String, String> errors, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        body.put("errors", errors);
        
        return new ResponseEntity<>(body, status);
    }
}