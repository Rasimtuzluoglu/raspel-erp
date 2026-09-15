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

    @Value("${ai.encryption.key:}")
    private String aiEncryptionKey;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${MINIO_ROOT_USER:}")
    private String minioAccessKey;

    @Value("${MINIO_ROOT_PASSWORD:}")
    private String minioSecretKey;

    @Value("${app.websocket.relay-enabled:false}")
    private boolean relayEnabled;

    @Value("${spring.rabbitmq.password:}")
    private String rabbitmqPassword;

    @PostConstruct
    public void kontrol() {
        boolean prodAktif = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!prodAktif) return;

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "prod profilinde JWT_SECRET guclu bir degerle (en az 32 karakter) ayarlanmalidir.");
        }
        if (aiEncryptionKey == null || aiEncryptionKey.isBlank() || aiEncryptionKey.length() < 16) {
            throw new IllegalStateException(
                    "prod profilinde AI_ENCRYPTION_KEY zorunludur (en az 16 karakter). Varsayilan dev anahtari kullanilamaz.");
        }
        if ("minio".equalsIgnoreCase(storageType)) {
            if (minioAccessKey == null || minioAccessKey.isBlank() || minioSecretKey == null || minioSecretKey.isBlank()) {
                throw new IllegalStateException(
                        "prod profilinde MINIO_ROOT_USER ve MINIO_ROOT_PASSWORD zorunludur.");
            }
        }
        if (relayEnabled && (rabbitmqPassword == null || rabbitmqPassword.isBlank())) {
            throw new IllegalStateException(
                    "prod profilinde WebSocket relay aktifken RABBITMQ_PASSWORD zorunludur.");
        }
        log.info("Prod guvenlik kontrolu tamam: JWT_SECRET, AI_ENCRYPTION_KEY ve depolama kredileri guclu.");
    }
}
