package com.shopscale.price.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual Thread Executor Configuration (Java 21)
 * Used for async / high-concurrency tasks
 */
@Configuration
public class ExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean(destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutor() {
        log.info("Initializing Virtual Thread Executor (Java 21)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}