package com.privsense.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that adds API version information to the response headers.
 * This helps clients track which API version they're using.
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    @Value("${privsense.api.version:v1.0}")
    private String apiVersion;

    @Value("${privsense.api.content-type:application/json;charset=UTF-8}")
    private String defaultContentType;

    /**
     * Add API version headers to the response
     * 
     * @param request Current HTTP request
     * @param response Current HTTP response
     * @param handler Chosen handler to execute
     * @return True to continue, false otherwise
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Add version header to all API responses
        response.setHeader("X-API-Version", apiVersion);
        
        // Set default content type if not already set
        if (response.getContentType() == null && request.getRequestURI().startsWith("/api/")) {
            response.setContentType(defaultContentType);
        }
        
        return true;
    }
}