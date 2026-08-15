package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.NotificationMessage;
import com.raspel.erp.service.sistem.BildirimKuyrukConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BildirimKuyrukConsumerTest {

    @Test
    void isle_acceptsMessage() {
        BildirimKuyrukConsumer consumer = new BildirimKuyrukConsumer();
        NotificationMessage msg = new NotificationMessage(1L, "STOK", "Kritik Stok", "Ürün azaldı", "2026-08-15T10:00:00");
        assertDoesNotThrow(() -> consumer.isle(msg));
        assertEquals("Kritik Stok", msg.baslik());
        assertEquals(1L, msg.sirketId());
    }
}
