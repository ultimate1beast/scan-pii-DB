package com.privsense.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * DataSource configuration for PostgreSQL database
 */
@Configuration
@PropertySource("classpath:database.properties")
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * Configure the data source using standard DriverManagerDataSource
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        
        return dataSource;
    }
}