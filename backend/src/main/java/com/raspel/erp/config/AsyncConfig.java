package com.raspel.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return executor("raspel-async-", 4, 12, 200);
    }

    /**
     * AI SSE akislari icin AYRI ve kucuk havuz.
     *
     * <p>Onceden her SSE istegi kendi `Executors.newSingleThreadExecutor()`'ini
     * aciyor ve `SseEmitter(0L)` ile sonsuza kadar acik kaliyordu; kimligi
     * dogrulanmis bir kullanici yuzlerce istek atarak thread/bellek tuketebilirdi.
     * Bu havuz sinirli oldugu icin doygunlukta istek kontrollu sekilde reddedilir
     * (RejectedExecutionException -> SSE "error" olayi).
     */
    @Bean(name = "sohbetSseExecutor")
    public ThreadPoolTaskExecutor sohbetSseExecutor() {
        return executor("raspel-sse-", 4, 16, 64);
    }

    private ThreadPoolTaskExecutor executor(String onek, int cekirdek, int maksimum, int kuyruk) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cekirdek);
        executor.setMaxPoolSize(maksimum);
        executor.setQueueCapacity(kuyruk);
        executor.setThreadNamePrefix(onek);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        java.util.concurrent.ThreadFactory fabrika = r -> {
            Thread t = new Thread(r, onek + "worker");
            t.setDaemon(true);
            return t;
        };
        executor.setThreadFactory(fabrika);
        executor.setRejectedExecutionHandler((r, e) ->
                log.error("Async gorev kuyrugu dolu, gorev reddedildi: {}", r.getClass().getSimpleName()));
        // Yakalanmayan async hatalar varsayilan logger'da kaybolmasin; acikca logla.
        executor.setTaskDecorator(runnable -> () -> {
            // MDC aktarimi: async gorev KARSILAYAN thread'de calisir ve ThreadLocal
            // MDC otomatik tasinmaz. Aktarilmazsa asenkron log satirlari (bildirim,
            // rapor, dis-yuk) traceId/sirketId bilgisi TUMUR ve izlenemez olur.
            java.util.Map<String, String> mdc = org.slf4j.MDC.getCopyOfContextMap();
            if (mdc != null) {
                org.slf4j.MDC.setContextMap(mdc);
            }
            try {
                runnable.run();
            } catch (Throwable t) {
                log.error("Async gorev hata ile sonlandi", t);
            } finally {
                org.slf4j.MDC.clear();
            }
        });
        executor.initialize();
        return executor;
    }
}
