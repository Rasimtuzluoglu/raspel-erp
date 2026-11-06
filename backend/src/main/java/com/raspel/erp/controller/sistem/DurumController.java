package com.raspel.erp.controller.sistem;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Giris ekranindaki public sistem durumu.
 *
 * <p>Yalnizca genel durum, surum ve zaman dondurulur; bilesen detaylari veya
 * altyapi bilgisi sizdirilmaz. Actuator health'in ayrintili ciktisi ADMIN'e
 * aittir.
 */
@RestController
@RequestMapping("/api/durum")
@RequiredArgsConstructor
public class DurumController {

    private final HealthEndpoint healthEndpoint;

    @Value("${app.surum:1.36.0}")
    private String surum;

    @GetMapping
    public ResponseEntity<Map<String, Object>> durum() {
        String kod;
        try {
            HealthComponent saglik = healthEndpoint.health();
            kod = saglik != null && saglik.getStatus() != null
                    ? saglik.getStatus().getCode()
                    : "UNKNOWN";
        } catch (Exception e) {
            kod = "DOWN";
        }

        Map<String, Object> govde = new LinkedHashMap<>();
        govde.put("durum", kod);
        govde.put("surum", surum);
        govde.put("zaman", OffsetDateTime.now().toString());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS).cachePublic())
                .body(govde);
    }
}
