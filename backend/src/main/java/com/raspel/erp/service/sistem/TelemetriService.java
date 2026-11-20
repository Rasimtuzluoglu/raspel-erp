package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.FrontendHataDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Frontend hata telemetrisini hız sınırından geçirip yapılandırılmış biçimde
 * loglar ve Prometheus sayacına işler. Veritabanı kullanılmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetriService {

    private static final int AZAMI_STACK_SATIRI = 20;
    private static final int AZAMI_STACK_KARAKTER = 4000;

    private final TelemetriHizSiniri hizSiniri;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    /** Hız sınırı aşıldıysa false döner (controller 429 üretir). */
    public boolean kabulEt(Long kullaniciId) {
        boolean izin = hizSiniri.izinVar(kullaniciId);
        if (!izin) {
            sayac("reddedilen");
        }
        return izin;
    }

    public void kaydet(FrontendHataDTO dto, Long kullaniciId, Long sirketId) {
        if (dto == null) return;
        String tur = dto.getTur() == null || dto.getTur().isBlank() ? "bilinmeyen" : dto.getTur();
        sayac(tur);

        log.warn(
                "Frontend hatasi [{}] tur={} kullanici={} sirket={} rota={} surum={} mesaj={} stack={}",
                tekSatir(dto.getHataId(), 64),
                tekSatir(tur, 20),
                kullaniciId,
                sirketId,
                tekSatir(dto.getRota(), 300),
                tekSatir(dto.getSurum(), 50),
                tekSatir(dto.getMesaj(), 1000),
                stackKirp(dto.getStack()));
    }

    private void sayac(String tur) {
        try {
            MeterRegistry registry = meterRegistry.getIfAvailable();
            if (registry == null) return;
            Counter.builder("frontend_errors_total")
                    .description("Istekci tarafindan bildirilen hata sayisi")
                    .tag("tur", tur)
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            // Metrik altyapısı yoksa telemetri logu yine de yazılmalı.
            log.debug("Telemetri sayaci yazilamadi: {}", e.getMessage());
        }
    }

    /** Log enjeksiyonunu ve tek satır taşmasını engeller. */
    private static String tekSatir(String deger, int azami) {
        if (deger == null) return "-";
        String temiz = deger.replace('\r', ' ').replace('\n', ' ').trim();
        return temiz.length() <= azami ? temiz : temiz.substring(0, azami) + "…";
    }

    private static String stackKirp(String stack) {
        if (stack == null || stack.isBlank()) return "-";
        String temiz = stack.replace("\r", "");
        String[] satirlar = temiz.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < satirlar.length && i < AZAMI_STACK_SATIRI; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(satirlar[i].trim());
        }
        String sonuc = sb.toString();
        if (sonuc.length() > AZAMI_STACK_KARAKTER) {
            sonuc = sonuc.substring(0, AZAMI_STACK_KARAKTER) + "…";
        }
        return sonuc;
    }
}
