package com.raspel.erp.service;

import com.raspel.erp.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncRaporService {

    private final RabbitTemplate rabbitTemplate;

    public void raporOlustur(String raporTuru, Long referansId) {
        Map<String, Object> message = Map.of(
            "tur", raporTuru,
            "refId", referansId,
            "timestamp", System.currentTimeMillis()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RAPOR_ROUTING_KEY, message);
        log.info("Rapor kuyruga gonderildi: {} #{}", raporTuru, referansId);
    }

    @RabbitListener(queues = RabbitMQConfig.RAPOR_QUEUE)
    public void handleRapor(Map<String, Object> message) {
        String tur = (String) message.get("tur");
        Long refId = Long.valueOf(message.get("refId").toString());
        log.info("Rapor isleniyor: {} #{}", tur, refId);
    }
}
