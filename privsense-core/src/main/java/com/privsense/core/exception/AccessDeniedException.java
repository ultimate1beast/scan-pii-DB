package com.privsense.core.exception;

/**
 * Exception thrown when a user tries to access a resource without appropriate permissions.
 * This is used to indicate security violations that should result in an HTTP 403 Forbidden response.
 */
public class AccessDeniedException extends PrivSenseException {
    
    private static final long serialVersionUID = 1L;
    
    private final String requiredPermission;
    private final String resourceType;
    
    public AccessDeniedException(String message) {
        super(message);
        this.requiredPermission = null;
        this.resourceType = null;
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.requiredPermission = null;
        this.resourceType = null;
    }
    
    public AccessDeniedException(String resourceType, String requiredPermission) {
        super(String.format("Access denied to resource '%s': required permission '%s' not granted",
                resourceType, requiredPermission));
        this.resourceType = resourceType;
        this.requiredPermission = requiredPermission;
    }
    
    /**
     * Gets the required permission that was missing
     * 
     * @return The permission name or null if not specified
     */
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    /**
     * Gets the type of resource access was denied to
     * 
     * @return The resource type or null if not specified
     */
    public String getResourceType() {
        return resourceType;
    }
}