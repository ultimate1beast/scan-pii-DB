package com.privsense.api.exception;

import com.privsense.api.model.ErrorResponse;
import com.privsense.core.exception.AccessDeniedException;
import com.privsense.core.exception.ConfigurationException;
import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.exception.DataSamplingException;
import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.exception.ResourceNotFoundException;
import com.privsense.core.exception.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for standardizing error responses across the API.
 * Provides detailed, consistent error responses with correlation IDs for tracking.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles validation exceptions with detailed field errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        logError(ex, "Validation error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(ex.getFieldErrors())
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logError(ex, "Resource not found");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.NOT_FOUND, request)
                .errorCode("RESOURCE_NOT_FOUND")
                .build();
                
        errorResponse.addDetail("Resource type: " + ex.getResourceType());
        errorResponse.addDetail("Resource ID: " + ex.getResourceId());
                
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handles access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logError(ex, "Access denied");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.FORBIDDEN, request)
                .errorCode("ACCESS_DENIED")
                .build();
                
        if (ex.getResourceType() != null) {
            errorResponse.addDetail("Resource type: " + ex.getResourceType());
        }
        
        if (ex.getRequiredPermission() != null) {
            errorResponse.addDetail("Required permission: " + ex.getRequiredPermission());
        }
                
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handles database connection exceptions
     */
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseConnectionException(DatabaseConnectionException ex, WebRequest request) {
        logError(ex, "Database connection error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request)
                .errorCode("DATABASE_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handles configuration exceptions
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationException(ConfigurationException ex, WebRequest request) {
        logError(ex, "Configuration error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("CONFIGURATION_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles report generation exceptions
     */
    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ErrorResponse> handleReportGenerationException(ReportGenerationException ex, WebRequest request) {
        logError(ex, "Report generation error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("REPORT_GENERATION_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles data sampling exceptions
     */
    @ExceptionHandler(DataSamplingException.class)
    public ResponseEntity<ErrorResponse> handleDataSamplingException(DataSamplingException ex, WebRequest request) {
        logError(ex, "Data sampling error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("DATA_SAMPLING_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles metadata extraction exceptions
     */
    @ExceptionHandler(MetadataExtractionException.class)
    public ResponseEntity<ErrorResponse> handleMetadataExtractionException(MetadataExtractionException ex, WebRequest request) {
        logError(ex, "Metadata extraction error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("METADATA_EXTRACTION_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles PII detection exceptions
     */
    @ExceptionHandler(PiiDetectionException.class)
    public ResponseEntity<ErrorResponse> handlePiiDetectionException(PiiDetectionException ex, WebRequest request) {
        logError(ex, "PII detection error");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("PII_DETECTION_ERROR")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logError(ex, "Method argument validation error");
        
        // Collect field errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        // Build the error response
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(fieldErrors)
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
            
        logError(ex, "Missing request parameter");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
                .errorCode("MISSING_PARAMETER")
                .build();
                
        errorResponse.addDetail("Parameter name: " + ex.getParameterName());
        errorResponse.addDetail("Parameter type: " + ex.getParameterType());
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles errors when converting method arguments
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
            
        logError(ex, "Method argument type mismatch");
        
        String detail = String.format(
            "Parameter '%s' should be of type '%s'", 
            ex.getName(), 
            ex.getRequiredType().getSimpleName()
        );
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
                .errorCode("TYPE_MISMATCH")
                .build();
                
        errorResponse.addDetail(detail);
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles malformed JSON requests
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
            
        logError(ex, "Malformed JSON request");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
                .errorCode("MALFORMED_JSON")
                .message("Malformed JSON request")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles 404 errors
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, WebRequest request) {
            
        logError(ex, "No handler found for request");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.NOT_FOUND, request)
                .errorCode("ENDPOINT_NOT_FOUND")
                .message("No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL())
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Catch-all for any other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtExceptions(Exception ex, WebRequest request) {
        logError(ex, "Uncaught exception");
        
        ErrorResponse errorResponse = buildBasicErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please contact support with the correlation ID.")
                .build();
                
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Helper method to build a basic error response with common fields
     */
    private ErrorResponse.ErrorResponseBuilder buildBasicErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        String path = extractPath(request);
        String correlationId = generateCorrelationId();
        
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId);
    }
    
    /**
     * Extract the request path from WebRequest
     */
    private String extractPath(WebRequest request) {
        String path = request.getDescription(false);
        
        // Remove "uri=" prefix if present
        if (path != null && path.startsWith("uri=")) {
            path = path.substring(4);
        }
        
        HttpServletRequest servletRequest = 
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        if (servletRequest != null) {
            String queryString = servletRequest.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                path = path + "?" + queryString;
            }
        }
        
        return path;
    }
    
    /**
     * Generate a correlation ID for tracking errors across services
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Log the exception with correlation ID and additional context
     */
    private void logError(Exception ex, String context) {
        String correlationId = generateCorrelationId();
        log.error("Error handling {} [correlationId={}]: {}", context, correlationId, ex.getMessage(), ex);
    }
}