package com.raspel.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BildirimService {

    private final SimpMessagingTemplate messagingTemplate;

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
    }
}
