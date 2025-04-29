package com.privsense.api.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that adds a correlation ID to every request.
 * This ID is used to track requests across components and appears in logs
 * and error responses for easier debugging and troubleshooting.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Check if there's already a correlation ID in the request header
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            
            // If not present, generate a new one
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = generateCorrelationId();
            }
            
            // Add the correlation ID to the MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add the correlation ID as a response header
            response.addHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Continue with the request
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear the MDC to prevent memory leaks in thread pools
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
    
    /**
     * Generate a new correlation ID
     * 
     * @return A UUID string to use as the correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}