package com.raspel.erp.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Veritabanı transaction'ı içinde yapılmaması gereken yan etkileri (e-posta, WebSocket/
 * RabbitMQ bildirimi gibi dış çağrılar) commit SONRASINA erteler. Böylece:
 * <ul>
 *   <li>Transaction, yavaş dış servis (SMTP, broker) için açık tutulmaz (kilit süresi kısalır),</li>
 *   <li>Geri alınan (rollback) bir işlem için yanlış bildirim gönderilmez.</li>
 * </ul>
 * Aktif bir transaction yoksa (ör. birim testi) görev anında çalıştırılır. Durum tutmadığı
 * için statik yardımcı olarak kullanılır.
 */
@Slf4j
public final class AfterCommitExecutor {

    private AfterCommitExecutor() {
    }

    public static void calistir(Runnable gorev) {
        if (gorev == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    guvenliCalistir(gorev);
                }
            });
        } else {
            guvenliCalistir(gorev);
        }
    }

    private static void guvenliCalistir(Runnable gorev) {
        try {
            gorev.run();
        } catch (Exception e) {
            // Bildirim/e-posta hatası iş akışını bozmamalı; yalnızca logla.
            log.warn("Commit sonrası gorev calistirilamadi: {}", e.getMessage());
        }
    }
}
