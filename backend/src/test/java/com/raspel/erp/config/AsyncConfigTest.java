package com.raspel.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void sseHavuzuSinirlidir() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = config.sohbetSseExecutor();
        try {
            // Sinirsiz havuz (onceki hali: istek basina yeni thread) DoS'a acikti.
            assertEquals(16, executor.getMaxPoolSize());
            assertEquals(64, executor.getQueueCapacity());
            assertTrue(executor.getCorePoolSize() <= executor.getMaxPoolSize());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void sseHavuzuThreadleriDaemon() throws Exception {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = config.sohbetSseExecutor();
        try {
            Boolean daemonMi = executor.submit(() -> {
                Thread t = Thread.currentThread();
                return t.isDaemon() && t.getName().startsWith("raspel-sse-");
            }).get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(Boolean.TRUE, daemonMi);
        } finally {
            executor.shutdown();
        }
    }
}
