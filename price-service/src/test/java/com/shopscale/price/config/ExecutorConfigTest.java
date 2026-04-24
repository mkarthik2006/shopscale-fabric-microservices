package com.shopscale.price.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorConfigTest {

    @Test
    void virtualThreadExecutor_shouldExecuteSubmittedTask() throws Exception {
        ExecutorConfig config = new ExecutorConfig();
        ExecutorService executor = config.virtualThreadExecutor();
        try {
            Future<String> future = executor.submit(() -> "ok");
            assertThat(future.get()).isEqualTo("ok");
        } finally {
            executor.shutdown();
        }
    }
}
