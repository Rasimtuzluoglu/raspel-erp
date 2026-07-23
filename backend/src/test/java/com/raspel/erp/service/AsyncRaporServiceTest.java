package com.raspel.erp.service;

import com.raspel.erp.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncRaporServiceTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private AsyncRaporService asyncRaporService;
    @Captor private ArgumentCaptor<Map<String, Object>> messageCaptor;

    @Test
    void raporOlustur_sendsToQueue() {
        asyncRaporService.raporOlustur("SIPARIS", 1L);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.RAPOR_ROUTING_KEY), messageCaptor.capture());
        var sent = messageCaptor.getValue();
        assertEquals("SIPARIS", sent.get("tur"));
        assertEquals(1L, sent.get("refId"));
        assertNotNull(sent.get("timestamp"));
    }

    @Test
    void handleRapor_processesMessage() {
        Map<String, Object> message = Map.of(
            "tur", "FATURA",
            "refId", Long.valueOf(5L),
            "timestamp", System.currentTimeMillis()
        );
        assertDoesNotThrow(() -> asyncRaporService.handleRapor(message));
    }
}
