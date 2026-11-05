package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anasayfanın gerçek zamanlı tazelenmesi için STOMP yayını yapar.
 *
 * <p>Yayın {@code /topic/dashboard/{sirketId}} kanalına gider; payload
 * {@code {tur, mesaj, zaman}} alanlarından oluşur. Frontend yalnızca bunları
 * gösterir/tazeler.
 *
 * <p>Aktif bir transaction varsa yayın <b>commit sonrasına</b> ertelenir:
 * geri alınan (rollback) bir işlem için istemciye yanlış "güncellendi" sinyali
 * gitmez ve broker çağrısı transaction açıkken kilitleri tutmaz. Transaction
 * yoksa (ör. birim testi) doğrudan gönderilir.
 *
 * <p>Yayın hatası iş akışını asla bozmaz; yalnızca debug seviyesinde loglanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardYayinService {

    static final String DASHBOARD_TOPIC_ONEKI = "/topic/dashboard/";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * @param sirketId yayının gideceği şirket; {@code null} ise yayın atlanır (tenant zorunlu)
     * @param tur      olay türü (FATURA, SIPARIS, STOK, ...)
     * @param mesaj    insan okunur kısa açıklama
     */
    public void yayinla(Long sirketId, String tur, String mesaj) {
        if (sirketId == null) {
            return;
        }
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        gonder(sirketId, tur, mesaj);
                    }
                });
            } else {
                gonder(sirketId, tur, mesaj);
            }
        } catch (Exception e) {
            // Yayın planlanamasa bile iş akışı devam etmeli.
            log.debug("Dashboard yayını planlanamadı (sirketId={}, tur={}): {}",
                    sirketId, tur, e.getMessage());
        }
    }

    /** Broker'a gönderir; hata durumunda yalnızca loglar. */
    void gonder(Long sirketId, String tur, String mesaj) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tur", tur != null ? tur : "");
            payload.put("mesaj", mesaj != null ? mesaj : "");
            payload.put("zaman", Instant.now().toString());
            messagingTemplate.convertAndSend(DASHBOARD_TOPIC_ONEKI + sirketId, payload);
        } catch (Exception e) {
            log.debug("Dashboard yayını gönderilemedi (sirketId={}, tur={}): {}",
                    sirketId, tur, e.getMessage());
        }
    }
}
