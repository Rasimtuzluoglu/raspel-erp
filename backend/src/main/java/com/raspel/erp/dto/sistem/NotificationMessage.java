package com.raspel.erp.dto.sistem;

import java.io.Serializable;

/**
 * RabbitMQ bildirim kuyruğunda taşınan mesaj.
 * WebSocket üzerinden dağıtılacak ya da kalıcı işlemeye (e-posta, arşiv) tabi tutulacak bildirim verisini taşır.
 */
public record NotificationMessage(Long sirketId, String tur, String baslik, String mesaj, String tarih)
        implements Serializable {
}
