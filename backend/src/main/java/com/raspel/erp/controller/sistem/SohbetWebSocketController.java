package com.raspel.erp.controller.sistem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SohbetWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sohbet/oda/yaziyor")
    public void yaziyor(YaziyorMesaj msg) {
        if (msg == null || msg.sirketId() == null || msg.odaId() == null) return;
        messagingTemplate.convertAndSend(
                "/topic/sohbet/oda/" + msg.sirketId() + "/" + msg.odaId() + "/yaziyor", msg);
    }

    public record YaziyorMesaj(Long sirketId, Long odaId, String kullaniciAd) {}
}
