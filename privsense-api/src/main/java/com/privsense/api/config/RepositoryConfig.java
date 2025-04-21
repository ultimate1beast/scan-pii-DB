package com.privsense.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.privsense.core.repository.ConnectionRepository;
import com.privsense.api.repository.JpaConnectionRepository;
import com.privsense.api.repository.jpa.ConnectionJpaRepository;

/**
 * Configuration for repository implementations.
 * Controls which implementation of ConnectionRepository is used.
 */
@Configuration
public class RepositoryConfig {

    /**
     * Provides the JPA implementation of ConnectionRepository as the primary bean.
     * This implementation will be used for database persistence.
     * 
     * @param jpaRepository the Spring Data JPA repository
     * @return the JPA-backed connection repository
     */
    @Bean
    @Primary
    public ConnectionRepository jpaConnectionRepository(ConnectionJpaRepository jpaRepository) {
        return new JpaConnectionRepository(jpaRepository);
    }
}