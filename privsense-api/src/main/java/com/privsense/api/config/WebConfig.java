package com.privsense.api.config;

import com.privsense.api.interceptor.ApiVersionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration to enable Cross-Origin Resource Sharing (CORS).
 * This is essential for allowing frontend applications (like React) to communicate with our API
 * when deployed on different domains or ports.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*") // In production, specify exact origins instead of wildcard
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition", "X-Correlation-ID", "X-API-Version")
                .maxAge(3600); // Cache preflight request for 1 hour
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add API version header to all responses
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**");
    }
}