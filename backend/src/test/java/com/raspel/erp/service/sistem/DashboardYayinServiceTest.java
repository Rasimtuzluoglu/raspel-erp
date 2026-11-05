package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardYayinServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void temizle() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private DashboardYayinService servis() {
        return new DashboardYayinService(messagingTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void yayinla_dogruTopicVePayloadGonderir() {
        servis().yayinla(7L, "FATURA", "Yeni fatura: FTR-1");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/7"), payloadCaptor.capture());

        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("FATURA", payload.get("tur"));
        assertEquals("Yeni fatura: FTR-1", payload.get("mesaj"));
        assertNotNull(payload.get("zaman"));
        assertFalse(payload.get("zaman").toString().isBlank());
    }

    @Test
    void yayinla_sirketIdNullIseHicGondermez() {
        servis().yayinla(null, "FATURA", "Yeni fatura");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void yayinla_payloadNullAlanlariBosStringYapar() {
        servis().yayinla(3L, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/3"), payloadCaptor.capture());
        assertEquals("", payloadCaptor.getValue().get("tur"));
        assertEquals("", payloadCaptor.getValue().get("mesaj"));
    }

    @Test
    void yayinla_commitSonrasiGonderir() {
        TransactionSynchronizationManager.initSynchronization();

        servis().yayinla(5L, "SIPARIS", "Yeni sipariş");

        // Henüz commit yok: yayın yapılmamalı.
        verifyNoInteractions(messagingTemplate);

        List<TransactionSynchronization> senkronizasyonlar =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, senkronizasyonlar.size());
        senkronizasyonlar.forEach(TransactionSynchronization::afterCommit);

        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/5"), any(Object.class));
    }

    @Test
    void yayinla_brokerHatasiIsAkisiniBozmaz() {
        doThrow(new IllegalStateException("broker yok"))
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        assertDoesNotThrow(() -> servis().yayinla(1L, "STOK", "hareket"));
    }
}
