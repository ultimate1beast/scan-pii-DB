package com.privsense.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration class for customizing Jackson's ObjectMapper.
 * Configures JSON serialization settings to handle circular references and complex object graphs.
 */
@Configuration
public class JacksonConfig {

    @Autowired
    private ReportMixinModule reportMixinModule;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .modules(new JavaTimeModule(), reportMixinModule)
            .build();
        
        // Set a filter provider that ignores unknown filters
        objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        
        return objectMapper;
    }
    
    // Add a custom resolver that will be used by Spring MVC to handle circular references
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.modulesToInstall(reportMixinModule);
        return builder;
    }
}