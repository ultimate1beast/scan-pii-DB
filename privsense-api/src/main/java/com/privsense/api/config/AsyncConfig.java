package com.privsense.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Provides a customized thread pool for handling long-running operations.
 */
@Configuration
public class AsyncConfig {

    @Value("${privsense.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${privsense.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${privsense.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${privsense.async.thread-name-prefix:privsense-async-}")
    private String threadNamePrefix;

    /**
     * Task executor for handling asynchronous operations like database scanning.
     * This executor is referenced in @Async("taskExecutor") annotations.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}