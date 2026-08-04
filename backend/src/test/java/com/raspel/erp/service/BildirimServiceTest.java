package com.raspel.erp.service;

import com.raspel.erp.service.sistem.BildirimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.entity.envanter.Stok;

@ExtendWith(MockitoExtension.class)
class BildirimServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private BildirimService bildirimService;

    @Test
    void bildirimGonder_publishesToTopic() {
        bildirimService.bildirimGonder(1L, "STOK", "Kritik Stok", "Ürün stokta azaldı");

        verify(messagingTemplate).convertAndSend(eq("/topic/bildirimler/1"), any(Map.class));
    }

    @Test
    void bildirimGonder_farkliSirketFarkliTopic() {
        bildirimService.bildirimGonder(2L, "SIPARIS", "Yeni Sipariş", "Sipariş alındı");

        verify(messagingTemplate).convertAndSend(eq("/topic/bildirimler/2"), any(Map.class));
    }

    @Test
    void bildirimGonder_mesajIcerikKontrol() {
        bildirimService.bildirimGonder(1L, "UYARI", "Test Başlığı", "Test Mesajı");

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/bildirimler/1"), captor.capture());

        Map<String, Object> gonderilen = captor.getValue();
        assertEquals("Test Başlığı", gonderilen.get("baslik"));
        assertEquals("UYARI", gonderilen.get("tur"));
        assertEquals("Test Mesajı", gonderilen.get("mesaj"));
    }
}