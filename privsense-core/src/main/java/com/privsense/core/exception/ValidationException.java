package com.privsense.core.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when input validation fails.
 * This exception can contain detailed information about which fields failed validation
 * and why, allowing for comprehensive error reporting to clients.
 */
public class ValidationException extends PrivSenseException {
    
    private static final long serialVersionUID = 1L;
    
    private final Map<String, String> fieldErrors = new HashMap<>();
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        if (fieldErrors != null) {
            this.fieldErrors.putAll(fieldErrors);
        }
    }
    
    /**
     * Adds a field validation error
     * 
     * @param field The field name that failed validation
     * @param message The validation error message
     * @return This exception instance for method chaining
     */
    public ValidationException addFieldError(String field, String message) {
        fieldErrors.put(field, message);
        return this;
    }
    
    /**
     * Gets all field validation errors
     * 
     * @return Map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return new HashMap<>(fieldErrors);
    }
    
    /**
     * Checks if this validation exception contains any field errors
     * 
     * @return true if there are field errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}