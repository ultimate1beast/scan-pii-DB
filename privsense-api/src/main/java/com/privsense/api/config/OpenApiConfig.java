package com.privsense.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI 3 documentation (Swagger).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI privSenseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PrivSense API")
                        .description("API for detecting PII in relational databases")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PrivSense Team")
                                .email("support@privsense.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.privsense.com/license")))
                .servers(List.of(
                        new Server().url("/privsense").description("Default server URL")
                ));
    }
}