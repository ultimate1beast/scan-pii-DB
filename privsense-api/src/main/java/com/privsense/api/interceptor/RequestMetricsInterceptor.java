package com.privsense.api.interceptor;

import com.privsense.api.service.RequestMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that records metrics for API requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestMetricsInterceptor implements HandlerInterceptor {

    private final RequestMetricsService requestMetricsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only track API requests (not static resources)
        if (request.getRequestURI().contains("/api/")) {
            String endpoint = getSimplifiedEndpoint(request.getRequestURI());
            requestMetricsService.recordRequest(endpoint);
            log.trace("Recorded API request to endpoint: {}", endpoint);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Record errors (status code >= 400)
        int status = response.getStatus();
        if (status >= 400 && request.getRequestURI().contains("/api/")) {
            requestMetricsService.recordError(status);
            log.trace("Recorded API error with status code: {}", status);
        }
    }
    
    /**
     * Simplifies an endpoint path to a more general form for better grouping.
     * For example, "/api/v1/scans/123" becomes "/api/v1/scans".
     *
     * @param uri The original request URI
     * @return A simplified endpoint path
     */
    private String getSimplifiedEndpoint(String uri) {
        // Extract the base endpoint without IDs
        // Example: /privsense/api/v1/scans/123 -> /api/v1/scans
        String[] parts = uri.split("/");
        StringBuilder endpoint = new StringBuilder();
        
        boolean isApiPart = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            
            if (isApiPart) {
                endpoint.append("/").append(part);
                // Only include up to 2 parts after "api" (version and resource name)
                if (endpoint.toString().split("/").length > 3) {
                    break;
                }
            } else if (part.equals("api")) {
                endpoint.append("/").append(part);
                isApiPart = true;
            }
        }
        
        return endpoint.toString();
    }
}