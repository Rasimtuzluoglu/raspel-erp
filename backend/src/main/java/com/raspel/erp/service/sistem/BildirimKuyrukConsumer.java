package com.raspel.erp.service.sistem;

import com.raspel.erp.config.RabbitMQConfig;
import com.raspel.erp.dto.sistem.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bildirim kuyruğu tüketicisi.
 * Kuyruğa düşen bildirimleri işler; kalıcı arşivleme, e-posta veya ek bildirim kanalları
 * bu noktadan kolayca genişletilebilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class BildirimKuyrukConsumer {

    @RabbitListener(queues = RabbitMQConfig.BILDIRIM_QUEUE)
    public void isle(NotificationMessage message) {
        try {
            log.info("Bildirim kuyruğundan işlendi -> sirketId={}, tur={}, baslik={}", message.sirketId(), message.tur(), message.baslik());
        } catch (Exception e) {
            // İşlenemeyen mesaj retry (3 deneme) sonrası DLQ'ya düşer; sonsuz döngü oluşmaz
            log.error("Bildirim işlenirken hata -> baslik={}", message != null ? message.baslik() : null, e);
            throw e;
        }
    }
}
