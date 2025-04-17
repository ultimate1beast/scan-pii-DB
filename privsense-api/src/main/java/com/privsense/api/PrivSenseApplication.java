package com.privsense.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the PrivSense application.
 * Configures Spring Boot and enables asynchronous processing.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.privsense.api", "com.privsense.db", "com.privsense.metadata", "com.privsense.sampler", "com.privsense.pii", "com.privsense.reporter"})
public class PrivSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrivSenseApplication.class, args);
    }
}