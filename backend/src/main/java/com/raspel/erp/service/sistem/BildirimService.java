package com.raspel.erp.service.sistem;

import com.raspel.erp.config.RabbitMQConfig;
import com.raspel.erp.dto.sistem.BildirimDTO;
import com.raspel.erp.dto.sistem.NotificationMessage;
import com.raspel.erp.entity.sistem.Bildirim;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.BildirimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BildirimService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final BildirimRepository bildirimRepository;

    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj) {
        bildirimGonder(sirketId, tur, baslik, mesaj, null);
    }

    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj, String kullaniciAdi) {
        var bildirim = Map.of(
                "tur", tur,
                "baslik", baslik,
                "mesaj", mesaj,
                "kullaniciAdi", kullaniciAdi != null ? kullaniciAdi : "",
                "tarih", LocalDateTime.now().toString()
        );
        String destination = "/topic/bildirimler/" + sirketId;
        messagingTemplate.convertAndSend(destination, bildirim);
        log.info("Bildirim gönderildi -> {} : {}", destination, baslik);

        try {
            bildirimRepository.save(Bildirim.builder()
                    .sirketId(sirketId).tur(tur).kullaniciAdi(kullaniciAdi)
                    .baslik(baslik).mesaj(mesaj).okundu(false).build());
        } catch (Exception e) {
            log.warn("Bildirim kaydedilemedi: {}", e.getMessage());
        }

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

    @Transactional(readOnly = true)
    public List<BildirimDTO> liste(Long sirketId) {
        return bildirimRepository.findTop50BySirketIdOrderByOlusturmaTarihiDesc(sirketId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long okunmamisSayisi(Long sirketId) {
        return bildirimRepository.countBySirketIdAndOkunduFalse(sirketId);
    }

    @Transactional
    public void okunduIsaretle(Long id) {
        Bildirim b = bildirimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim", id));
        b.setOkundu(true);
        bildirimRepository.save(b);
    }

    @Transactional
    public void tumunuOkunduIsaretle(Long sirketId) {
        bildirimRepository.findTop50BySirketIdOrderByOlusturmaTarihiDesc(sirketId).forEach(b -> {
            if (b.getOkundu() == null || !b.getOkundu()) {
                b.setOkundu(true);
                bildirimRepository.save(b);
            }
        });
    }

    private BildirimDTO toDTO(Bildirim b) {
        return BildirimDTO.builder()
                .id(b.getId()).sirketId(b.getSirketId()).tur(b.getTur())
                .kullaniciAdi(b.getKullaniciAdi())
                .baslik(b.getBaslik()).mesaj(b.getMesaj()).okundu(b.getOkundu())
                .olusturmaTarihi(b.getOlusturmaTarihi()).build();
    }
}
