package com.privsense.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT authentication.
 * These values are loaded from application.yml under the privsense.jwt prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "privsense.jwt")
@Data
public class JwtConfig {
    
    /**
     * Secret key used for signing JWT tokens
     */
    private String secretKey;
    
    /**
     * Token expiration time in milliseconds
     */
    private long expirationMs;
    
    /**
     * HTTP header name used for the JWT token
     */
    private String header;
    
    /**
     * Token prefix in the Authorization header (e.g., "Bearer ")
     */
    private String tokenPrefix;
    
    /**
     * Get the full token value from the raw token by adding the prefix
     * 
     * @param token Raw JWT token without prefix
     * @return Complete token with prefix for use in Authorization header
     */
    public String getFullTokenValue(String token) {
        return getTokenPrefix() + token;
    }
    
    /**
     * Extract the raw token from the full header value
     * 
     * @param headerValue Complete Authorization header value
     * @return Raw JWT token without prefix
     */
    public String extractTokenFromHeader(String headerValue) {
        if (headerValue != null && headerValue.startsWith(getTokenPrefix())) {
            return headerValue.substring(getTokenPrefix().length());
        }
        return null;
    }
}