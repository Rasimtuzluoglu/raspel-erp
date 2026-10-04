package com.raspel.erp.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code @Scheduled} job'larının metriklerini toplar.
 *
 * <p><b>Neden gerekli:</b> 16 zamanlanmış görev vardı ve hiçbiri ölçülmüyordu.
 * Bir cron sessizce başarısız olduğunda (virüs, disk dolu, ağ hatası) tek
 * belirti log dosyasında kalıyordu; Prometheus/Grafana'da görünmüyordu. Üstelik
 * bu görevler yedekleme, kur tazeleme, gecikme uyarısı gibi <b>iş açısından
 * kritik</b> işlerdi.
 *
 * <p><b>Üretilen metrikler</b> (config/prometheus/alert.rules.yml kullanır):
 * <ul>
 *   <li>{@code raspel.job.calisma} (counter, etiket: ad, sonuc=basarili|hata)</li>
 *   <li>{@code raspel.job.sure} (timer, etiket: ad)</li>
 *   <li>{@code raspel.job.son.calisma} (gauge, etiket: ad; epoch saniye)</li>
 * </ul>
 *
 * <p>Etiket değerleri sabit metindir (metot adı); kardinalite patlamaz.
 */
@Aspect
@Component
@Slf4j
public class SchedulerMetricsAspect {

    private static final String METRIK_CALISMA = "raspel.job.calisma";
    private static final String METRIK_SURE = "raspel.job.sure";
    private static final String METRIK_SON = "raspel.job.son.calisma";

    /** Metot adi -> son basarili calisma epoch saniyesi. */
    private final ConcurrentHashMap<String, AtomicLong> sonCalisma = new ConcurrentHashMap<>();

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public SchedulerMetricsAspect(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /** Yalnızca zamanlanmış metotlar: {@code @Scheduled} ile işaretli her metot. */
    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledMethod() {
    }

    @Around("scheduledMethod()")
    public Object olcum(ProceedingJoinPoint pjp) throws Throwable {
        String ad = pjp.getSignature().getDeclaringType().getSimpleName() + "."
                + pjp.getSignature().getName();
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        long baslangic = System.nanoTime();
        String sonuc = "basarili";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            sonuc = "hata";
            // Job hatasini loglayip tekrar yukari ilet: sessizce yutulmaz.
            log.error("Zamanlanmis gorev basarisiz: {} - {}", ad, t.getMessage(), t);
            throw t;
        } finally {
            long sureMs = (System.nanoTime() - baslangic) / 1_000_000L;
            if (registry != null) {
                registry.counter(METRIK_CALISMA, "ad", ad, "sonuc", sonuc).increment();
                Timer.builder(METRIK_SURE).tag("ad", ad)
                        .description("Zamanlanmis gorev suresi")
                        .register(registry)
                        .record(sureMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                AtomicLong ref = sonCalisma.computeIfAbsent(ad, k -> new AtomicLong());
                if ("basarili".equals(sonuc)) {
                    ref.set(System.currentTimeMillis() / 1000L);
                }
                registry.gauge(METRIK_SON, java.util.List.of(io.micrometer.core.instrument.Tag.of("ad", ad)), ref,
                        r -> Math.max(r.get(), System.currentTimeMillis() / 1000L));
            }
            if (sureMs > 60_000) {
                log.warn("Zamanlanmis gorev uzun surtu: {} ({} ms)", ad, sureMs);
            }
        }
    }
}