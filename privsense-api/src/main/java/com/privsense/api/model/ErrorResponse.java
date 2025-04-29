package com.privsense.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standardized error response model for all API responses that indicate an error.
 * This model follows REST API best practices for consistent error reporting.
 * Uses Lombok's Builder pattern for cleaner code and easier instantiation.
 */
@Getter
@Setter
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private String correlationId;
    private String traceId;
    
    @Builder.Default
    private List<String> details = new ArrayList<>();
    
    @Builder.Default
    private Map<String, String> fieldErrors = new HashMap<>();
    
    /**
     * Utility method to add a detail to the error response
     * 
     * @param detail The detail to add
     * @return The updated ErrorResponse instance for method chaining
     */
    public ErrorResponse addDetail(String detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
        return this;
    }
    
    /**
     * Utility method to add a field error to the error response
     * 
     * @param field The field name that failed validation
     * @param error The validation error message
     * @return The updated ErrorResponse instance for method chaining
     */
    public ErrorResponse addFieldError(String field, String error) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new HashMap<>();
        }
        this.fieldErrors.put(field, error);
        return this;
    }
    
    /**
     * Factory method to create a builder with pre-populated common fields
     * 
     * @return A new builder with timestamp and correlationId pre-populated
     */
    public static ErrorResponseBuilder standardBuilder() {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID().toString());
    }
}