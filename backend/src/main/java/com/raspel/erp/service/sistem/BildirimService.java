package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.NotificationMessage;
import com.raspel.erp.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BildirimService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;

    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj) {
        var bildirim = Map.of(
                "tur", tur,
                "baslik", baslik,
                "mesaj", mesaj,
                "tarih", LocalDateTime.now().toString()
        );
        String destination = "/topic/bildirimler/" + sirketId;
        messagingTemplate.convertAndSend(destination, bildirim);
        log.info("Bildirim gönderildi -> {} : {}", destination, baslik);

        kuyrugaGonder(sirketId, tur, baslik, mesaj);
    }

    /**
     * Bildirimi RabbitMQ kuyruğuna asenkron işlenmek üzere gönderir.
     * Kuyruk mevcut değilse WebSocket akışı bozulmadan sessizce atlanır.
     */
    private void kuyrugaGonder(Long sirketId, String tur, String baslik, String mesaj) {
        try {
            var message = new NotificationMessage(sirketId, tur, baslik, mesaj, LocalDateTime.now().toString());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BILDIRIM_EXCHANGE,
                    RabbitMQConfig.BILDIRIM_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            log.warn("Bildirim kuyruğa alınamadı: {}", e.getMessage());
        }
    }
}
