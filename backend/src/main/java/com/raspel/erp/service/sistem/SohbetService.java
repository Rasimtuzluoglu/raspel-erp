package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SohbetService {

    private final SohbetMesajRepository sohbetMesajRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<SohbetMesajDTO> sonMesajlar(Long sirketId) {
        List<SohbetMesaj> mesajlar = sohbetMesajRepository.findTop50BySirketIdOrderByOlusturmaTarihiDesc(sirketId);
        java.util.Collections.reverse(mesajlar);
        return mesajlar.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public SohbetMesajDTO mesajGonder(SohbetMesajDTO dto, Long sirketId, Long kullaniciId, String kullaniciAd) {
        if (dto.getMesaj() == null || dto.getMesaj().isBlank()) {
            throw new com.raspel.erp.exception.BusinessException("Mesaj boş olamaz");
        }
        SohbetMesaj mesaj = SohbetMesaj.builder()
                .sirketId(sirketId)
                .kullaniciId(kullaniciId)
                .kullaniciAd(kullaniciAd)
                .mesaj(dto.getMesaj().trim())
                .build();
        mesaj = sohbetMesajRepository.save(mesaj);
        SohbetMesajDTO dtoKayit = toDTO(mesaj);

        try {
            if (sirketId != null) {
                messagingTemplate.convertAndSend("/topic/sohbet/" + sirketId, dtoKayit);
            }
        } catch (Exception e) {
            log.warn("Sohbet mesajı yayınlanamadı: {}", e.getMessage());
        }
        return dtoKayit;
    }

    private SohbetMesajDTO toDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .mesaj(m.getMesaj())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
