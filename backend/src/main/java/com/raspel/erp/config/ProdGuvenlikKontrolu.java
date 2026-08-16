package com.raspel.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Prod profilinde güvenlik doğrulaması: JWT secret'ı güçlü bir değerle ayarlanmadıysa
 * uygulama başlamaz (fail-fast). Varsayılan/zayıf secret ile prod'a çıkışı engeller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProdGuvenlikKontrolu {

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void kontrol() {
        boolean prodAktif = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!prodAktif) return;

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "prod profilinde JWT_SECRET guclu bir degerle (en az 32 karakter) ayarlanmalidir.");
        }
        log.info("Prod guvenlik kontrolu tamam: JWT_SECRET guclu.");
    }
}
