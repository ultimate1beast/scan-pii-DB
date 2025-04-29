package com.privsense.core.util;

import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.exception.DataSamplingException;
import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.exception.ResourceNotFoundException;
import com.privsense.core.exception.ValidationException;
import com.privsense.core.exception.ConfigurationException;
import com.privsense.core.exception.ReportGenerationException;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utilities for consistent exception handling across the application.
 * Provides helper methods to throw appropriate exceptions in various scenarios.
 */
public final class ExceptionUtils {
    
    private ExceptionUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Wraps an operation in a try-catch block and rethrows appropriate domain exceptions.
     * 
     * @param operation The function to execute
     * @param errorMessage The error message to use if the operation fails
     * @param logger Logger instance for recording errors
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T wrapExceptions(Supplier<T> operation, String errorMessage, Logger logger) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error(errorMessage, e);
            throw translateException(e, errorMessage);
        }
    }
    
    /**
     * Wraps an operation that doesn't return a result
     * 
     * @param operation The operation to execute
     * @param errorMessage The error message to use if the operation fails
     * @param logger Logger instance for recording errors
     */
    public static void wrapExceptions(Runnable operation, String errorMessage, Logger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.error(errorMessage, e);
            throw translateException(e, errorMessage);
        }
    }
    
    /**
     * Translates generic exceptions into domain-specific ones
     * 
     * @param e The exception to translate
     * @param defaultMessage Default message if the original exception doesn't have one
     * @return A domain-specific exception
     */
    public static RuntimeException translateException(Exception e, String defaultMessage) {
        // If it's already a domain exception, just return it
        if (e instanceof RuntimeException && !(e instanceof NullPointerException)) {
            return (RuntimeException) e;
        }
        
        // Translate based on exception type or message contents
        String message = e.getMessage() != null ? e.getMessage() : defaultMessage;
        
        if (e instanceof java.sql.SQLException) {
            return new DatabaseConnectionException(message, e);
        } else if (message.toLowerCase().contains("database") || 
                   message.toLowerCase().contains("connection") ||
                   message.toLowerCase().contains("sql")) {
            return new DatabaseConnectionException(message, e);
        } else if (message.toLowerCase().contains("metadata")) {
            return new MetadataExtractionException(message, e);
        } else if (message.toLowerCase().contains("sampl")) {
            return new DataSamplingException(message, e);
        } else if (message.toLowerCase().contains("pii") || 
                   message.toLowerCase().contains("detection")) {
            return new PiiDetectionException(message, e);
        } else if (message.toLowerCase().contains("report") || 
                   message.toLowerCase().contains("generat")) {
            return new ReportGenerationException(message, e);
        } else if (message.toLowerCase().contains("config")) {
            return new ConfigurationException(message, e);
        }
        
        // Default to a generic runtime exception
        return new RuntimeException(message, e);
    }
    
    /**
     * Creates a ValidationException with the specified field errors
     * 
     * @param message Overall validation error message
     * @return A new ValidationException
     */
    public static ValidationException validationError(String message) {
        return new ValidationException(message);
    }
    
    /**
     * Creates a ValidationException with the specified field errors
     * 
     * @param message Overall validation error message
     * @param fieldName Name of the field that failed validation
     * @param fieldError Validation error message for the field
     * @return A new ValidationException
     */
    public static ValidationException validationError(String message, String fieldName, String fieldError) {
        ValidationException ex = new ValidationException(message);
        ex.addFieldError(fieldName, fieldError);
        return ex;
    }
    
    /**
     * Creates a ValidationException with multiple field errors
     * 
     * @param message Overall validation error message
     * @param fieldErrors Map of field names to error messages
     * @return A new ValidationException
     */
    public static ValidationException validationError(String message, Map<String, String> fieldErrors) {
        return new ValidationException(message, fieldErrors);
    }
    
    /**
     * Creates a ResourceNotFoundException for the specified resource
     * 
     * @param resourceType Type of resource (e.g., "User", "Report")
     * @param resourceId Identifier of the resource
     * @return A new ResourceNotFoundException
     */
    public static ResourceNotFoundException resourceNotFound(String resourceType, String resourceId) {
        return new ResourceNotFoundException(resourceType, resourceId);
    }
    
    /**
     * Throws a ResourceNotFoundException if the specified object is null
     * 
     * @param obj Object to check
     * @param resourceType Type of resource
     * @param resourceId Identifier of the resource
     * @param <T> Type of the object
     * @return The object if not null
     */
    public static <T> T throwIfNotFound(T obj, String resourceType, String resourceId) {
        if (obj == null) {
            throw resourceNotFound(resourceType, resourceId);
        }
        return obj;
    }
}