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
    private final com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;

    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj) {
        bildirimGonder(sirketId, tur, baslik, mesaj, null);
    }

    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj, String kullaniciAdi) {
        Long kullaniciId = kullaniciAdi != null ? kullaniciIdCoz(kullaniciAdi) : null;
        bildirimGonder(sirketId, tur, baslik, mesaj, kullaniciAdi, kullaniciId);
    }

    /** Kişisel hedefli bildirim: yalnızca ilgili kullanıcı id'sine ulaşır (WebSocket filtreli). */
    public void bildirimGonder(Long sirketId, String tur, String baslik, String mesaj,
                               String kullaniciAdi, Long kullaniciId) {
        var bildirim = Map.of(
                "tur", tur,
                "baslik", baslik,
                "mesaj", mesaj,
                "kullaniciAdi", kullaniciAdi != null ? kullaniciAdi : "",
                "kullaniciId", kullaniciId != null ? kullaniciId : 0L,
                "tarih", LocalDateTime.now().toString()
        );
        String destination = "/topic/bildirimler/" + sirketId;
        messagingTemplate.convertAndSend(destination, bildirim);
        log.info("Bildirim gönderildi -> {} : {}", destination, baslik);

        try {
            bildirimRepository.save(Bildirim.builder()
                    .sirketId(sirketId).tur(tur).kullaniciAdi(kullaniciAdi).kullaniciId(kullaniciId)
                    .baslik(baslik).mesaj(mesaj).okundu(false).build());
        } catch (Exception e) {
            log.warn("Bildirim kaydedilemedi: {}", e.getMessage());
        }

        kuyrugaGonder(sirketId, tur, baslik, mesaj);
    }

    /** Kullanıcı adından id çözer; bulunamazsa null. */
    private Long kullaniciIdCoz(String kullaniciAdi) {
        try {
            return kullaniciRepository.findByUsername(kullaniciAdi).map(k -> k.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
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

    /** Kullanıcıya özel liste: kendi bildirimleri + genel bildirimler. */
    @Transactional(readOnly = true)
    public List<BildirimDTO> liste(Long sirketId, Long kullaniciId) {
        if (kullaniciId == null) {
            return liste(sirketId);
        }
        return bildirimRepository.kullaniciBildirimleri(sirketId, kullaniciId).stream()
                .limit(50).map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long okunmamisSayisi(Long sirketId) {
        return bildirimRepository.countBySirketIdAndOkunduFalse(sirketId);
    }

    @Transactional(readOnly = true)
    public long okunmamisSayisi(Long sirketId, Long kullaniciId) {
        if (kullaniciId == null) {
            return okunmamisSayisi(sirketId);
        }
        return bildirimRepository.kullaniciOkunmamisSayisi(sirketId, kullaniciId);
    }

    @Transactional
    public void okunduIsaretle(Long id, Long sirketId, Long kullaniciId) {
        Bildirim b = bildirimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim", id));
        // Tenant izolasyonu: yalnizca kendi sirketinin (ve hedefliyse kendi)
        // bildirimini okundu isaretleyebilir.
        boolean sirketUygun = sirketId != null && sirketId.equals(b.getSirketId());
        boolean kullaniciUygun = b.getKullaniciId() == null
                || b.getKullaniciId().equals(kullaniciId);
        if (!sirketUygun || !kullaniciUygun) {
            throw new ResourceNotFoundException("Bildirim", id);
        }
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
                .kullaniciAdi(b.getKullaniciAdi()).kullaniciId(b.getKullaniciId())
                .baslik(b.getBaslik()).mesaj(b.getMesaj()).okundu(b.getOkundu())
                .olusturmaTarihi(b.getOlusturmaTarihi()).build();
    }
}
