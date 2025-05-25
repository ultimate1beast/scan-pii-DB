package com.privsense.api.config;

import com.privsense.api.interceptor.RequestMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration.
 * Registers interceptors and other web-related configurations.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestMetricsInterceptor requestMetricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor)
                .addPathPatterns("/**");  // Apply to all paths
    }
}