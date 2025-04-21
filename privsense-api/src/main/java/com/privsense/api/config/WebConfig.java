package com.privsense.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.List;

/**
 * Configuration for web-related components, including HTTP message converters.
 * This class ensures that various content types are properly supported in REST endpoints.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Register custom message converters to handle different content types
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new CsvMessageConverter());
    }

    /**
     * Custom message converter for CSV content
     */
    public static class CsvMessageConverter extends AbstractHttpMessageConverter<byte[]> {
        
        public CsvMessageConverter() {
            super(new MediaType("text", "csv"), new MediaType("application", "csv"));
        }
        
        @Override
        protected boolean supports(Class<?> clazz) {
            // Only support byte arrays for our CSV response
            return byte[].class.isAssignableFrom(clazz);
        }
        
        @Override
        protected byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
            // Not used for reading CSV data
            return new byte[0];
        }
        
        @Override
        protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
            // Simply write the byte array to the output
            outputMessage.getBody().write(bytes);
        }
    }
}