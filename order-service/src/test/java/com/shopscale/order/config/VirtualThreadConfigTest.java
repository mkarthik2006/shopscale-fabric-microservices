package com.shopscale.order.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualThreadConfigTest {

    @Test
    void executorService_shouldExecuteTasks() throws Exception {
        VirtualThreadConfig config = new VirtualThreadConfig();
        ExecutorService executor = config.executorService();
        try {
            Future<String> future = executor.submit(() -> "ok");
            assertThat(future.get()).isEqualTo("ok");
        } finally {
            executor.shutdown();
        }
    }
}
