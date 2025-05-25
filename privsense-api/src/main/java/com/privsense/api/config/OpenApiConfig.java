package com.privsense.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Enhanced configuration for OpenAPI 3 documentation (Swagger).
 * Provides comprehensive API documentation to help frontend developers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi restApiGroup() {
        return GroupedOpenApi.builder()
                .group("REST API")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi webSocketApiGroup() {
        return GroupedOpenApi.builder()
                .group("WebSocket API")
                .pathsToMatch("/websocket/**")
                .build();
    }

    @Bean
    public OpenAPI privSenseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PrivSense API")
                        .description("API for detecting personally identifiable information (PII) in relational databases")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PrivSense Team")
                                .email("support@privsense.com")
                                .url("https://www.privsense.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.privsense.com/license")))
                .servers(List.of(
                        new Server().url("/privsense").description("Default server URL")
                ))
                .tags(Arrays.asList(
                        new Tag().name("Database Connections").description("APIs for managing database connections"),
                        new Tag().name("PII Scans").description("APIs for managing database PII scans"),
                        new Tag().name("Database Sampling").description("APIs for testing database column sampling"),
                        new Tag().name("Dashboard").description("APIs for dashboard data and metrics"),
                        new Tag().name("System").description("APIs for system health and configuration"),
                        new Tag().name("User Management").description("APIs for managing user accounts and roles"),
                        new Tag().name("Authentication").description("APIs for user authentication and token management")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token with Bearer prefix. Example: Bearer eyJhbGciOiJIUzI1NiIsIn...")
                        ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}