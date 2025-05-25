package com.privsense.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the PrivSense application.
 * Configures Spring Boot and enables asynchronous processing.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.privsense.api", "com.privsense.core", "com.privsense.db", "com.privsense.metadata", "com.privsense.sampler", "com.privsense.pii", "com.privsense.reporter"})
@EntityScan(basePackages = {"com.privsense.core.model"})
public class PrivSenseApplication {

    public static void main(String[] args) {
        // Set default profile to dev if not specified
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile == null || activeProfile.isEmpty()) {
            System.setProperty("spring.profiles.active", "dev");
        }
        
        SpringApplication.run(PrivSenseApplication.class, args);
    }
}