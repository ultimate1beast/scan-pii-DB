package com.privsense.core.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * This is used to indicate that a requested entity or resource does not exist,
 * which typically results in a 404 HTTP status response.
 */
public class ResourceNotFoundException extends PrivSenseException {
    
    private static final long serialVersionUID = 1L;
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = "Unknown";
        this.resourceId = "Unknown";
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("Resource of type '%s' with identifier '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId, Throwable cause) {
        super(String.format("Resource of type '%s' with identifier '%s' not found", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * Gets the type of resource that was not found (e.g., "User", "Report", etc.)
     * 
     * @return The resource type
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Gets the ID or identifier of the resource that was not found
     * 
     * @return The resource identifier
     */
    public String getResourceId() {
        return resourceId;
    }
}