package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.dto.sistem.SohbetOdaDTO;
import com.raspel.erp.dto.sistem.SohbetOdaUyeDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.sistem.SohbetOda;
import com.raspel.erp.entity.sistem.SohbetOdaUye;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.sistem.SohbetOdaRepository;
import com.raspel.erp.repository.sistem.SohbetOdaUyeRepository;
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
public class SohbetOdaService {

    private final SohbetOdaRepository odaRepository;
    private final SohbetOdaUyeRepository uyeRepository;
    private final SohbetMesajRepository mesajRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<SohbetOdaDTO> odalar(Long sirketId, Long kullaniciId) {
        List<SohbetOda> odalar = odaRepository.findBySirketIdOrderByAd(sirketId);
        List<Long> uyelikler = uyeRepository.findByKullaniciId(kullaniciId).stream()
                .map(SohbetOdaUye::getOdaId)
                .collect(Collectors.toList());
        return odalar.stream().map(o -> toDTO(o, kullaniciId, uyelikler.contains(o.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public SohbetOdaDTO olustur(SohbetOdaDTO dto, Long sirketId, Long kullaniciId) {
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("Oda adı boş olamaz");
        }
        SohbetOda oda = SohbetOda.builder()
                .sirketId(sirketId)
                .ad(dto.getAd().trim())
                .aciklama(dto.getAciklama())
                .olusturanKullaniciId(kullaniciId)
                .build();
        oda = odaRepository.save(oda);
        uyeRepository.save(SohbetOdaUye.builder().odaId(oda.getId()).kullaniciId(kullaniciId).build());
        return toDTO(oda, kullaniciId, true);
    }

    @Transactional
    public SohbetOdaDTO guncelle(Long odaId, SohbetOdaDTO dto, Long sirketId, Long kullaniciId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, kullaniciId);
        if (dto.getAd() != null && !dto.getAd().isBlank()) {
            oda.setAd(dto.getAd().trim());
        }
        if (dto.getAciklama() != null) {
            oda.setAciklama(dto.getAciklama());
        }
        oda = odaRepository.save(oda);
        return toDTO(oda, kullaniciId, true);
    }

    @Transactional
    public void sil(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, kullaniciId);
        uyeRepository.deleteByOdaId(odaId);
        odaRepository.delete(oda);
    }

    @Transactional
    public SohbetOdaDTO uyeEkle(Long odaId, Long hedefKullaniciId, Long sirketId, Long istekSahibiId) {
        SohbetOda oda = uyeOdasiBul(odaId, sirketId);
        boolean uyeMi = uyeRepository.existsByOdaIdAndKullaniciId(odaId, istekSahibiId);
        if (!uyeMi) {
            throw new BusinessException("Odaya üye olmadan üye ekleyemezsiniz");
        }
        if (hedefKullaniciId == null) {
            throw new BusinessException("Eklenecek kullanıcı belirtilmedi");
        }
        if (!uyeRepository.existsByOdaIdAndKullaniciId(odaId, hedefKullaniciId)) {
            uyeRepository.save(SohbetOdaUye.builder().odaId(odaId).kullaniciId(hedefKullaniciId).build());
        }
        return toDTO(oda, istekSahibiId, true);
    }

    @Transactional
    public SohbetOdaDTO uyeCikar(Long odaId, Long hedefKullaniciId, Long sirketId, Long istekSahibiId) {
        SohbetOda oda = yetkiliOdaBul(odaId, sirketId, istekSahibiId);
        uyeRepository.deleteByOdaIdAndKullaniciId(odaId, hedefKullaniciId);
        return toDTO(oda, istekSahibiId, uyeRepository.existsByOdaIdAndKullaniciId(odaId, istekSahibiId));
    }

    @Transactional
    public SohbetOdaDTO katil(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        if (!uyeRepository.existsByOdaIdAndKullaniciId(odaId, kullaniciId)) {
            uyeRepository.save(SohbetOdaUye.builder().odaId(odaId).kullaniciId(kullaniciId).build());
        }
        return toDTO(oda, kullaniciId, true);
    }

    @Transactional
    public void ayril(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        uyeRepository.deleteByOdaIdAndKullaniciId(odaId, kullaniciId);
        if (oda.getOlusturanKullaniciId() != null && oda.getOlusturanKullaniciId().equals(kullaniciId)) {
            uyeRepository.deleteByOdaId(odaId);
            odaRepository.delete(oda);
        }
    }

    @Transactional(readOnly = true)
    public List<SohbetMesajDTO> mesajlar(Long odaId, Long sirketId, Long kullaniciId) {
        uyeKontrol(odaId, sirketId, kullaniciId);
        List<SohbetMesaj> mesajlar = mesajRepository.findTop100ByOdaIdOrderByOlusturmaTarihiDesc(odaId);
        java.util.Collections.reverse(mesajlar);
        return mesajlar.stream().map(this::mesajDTO).collect(Collectors.toList());
    }

    @Transactional
    public SohbetMesajDTO mesajGonder(Long odaId, SohbetMesajDTO dto, Long sirketId, Long kullaniciId, String kullaniciAd) {
        uyeKontrol(odaId, sirketId, kullaniciId);
        if (dto.getMesaj() == null || dto.getMesaj().isBlank()) {
            throw new BusinessException("Mesaj boş olamaz");
        }
        SohbetMesaj mesaj = SohbetMesaj.builder()
                .sirketId(sirketId)
                .odaId(odaId)
                .kullaniciId(kullaniciId)
                .kullaniciAd(kullaniciAd)
                .mesaj(dto.getMesaj().trim())
                .build();
        mesaj = mesajRepository.save(mesaj);
        SohbetMesajDTO dtoKayit = mesajDTO(mesaj);
        try {
            messagingTemplate.convertAndSend("/topic/sohbet/oda/" + sirketId + "/" + odaId, dtoKayit);
        } catch (Exception e) {
            log.warn("Oda mesajı yayınlanamadı: {}", e.getMessage());
        }
        return dtoKayit;
    }

    private SohbetOda odaBul(Long odaId, Long sirketId) {
        SohbetOda oda = odaRepository.findById(odaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oda bulunamadı"));
        if (sirketId != null && !sirketId.equals(oda.getSirketId())) {
            throw new BusinessException("Bu odaya erişim yetkiniz yok");
        }
        return oda;
    }

    private SohbetOda uyeOdasiBul(Long odaId, Long sirketId) {
        return odaBul(odaId, sirketId);
    }

    private SohbetOda yetkiliOdaBul(Long odaId, Long sirketId, Long kullaniciId) {
        SohbetOda oda = odaBul(odaId, sirketId);
        boolean yonetici = kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole()))
                .orElse(false);
        boolean kurucu = oda.getOlusturanKullaniciId() != null && oda.getOlusturanKullaniciId().equals(kullaniciId);
        if (!yonetici && !kurucu) {
            throw new BusinessException("Bu odayı yönetme yetkiniz yok");
        }
        return oda;
    }

    private void uyeKontrol(Long odaId, Long sirketId, Long kullaniciId) {
        odaBul(odaId, sirketId);
        boolean uye = uyeRepository.existsByOdaIdAndKullaniciId(odaId, kullaniciId);
        boolean yonetici = kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole()))
                .orElse(false);
        if (!uye && !yonetici) {
            throw new BusinessException("Mesaj görmek için önce odaya katılın");
        }
    }

    private SohbetOdaDTO toDTO(SohbetOda oda, Long kullaniciId, boolean uyeMi) {
        List<SohbetOdaUyeDTO> uyeler = uyeRepository.findByOdaId(oda.getId()).stream()
                .map(u -> SohbetOdaUyeDTO.builder()
                        .kullaniciId(u.getKullaniciId())
                        .kullaniciAd(kullaniciAdi(u.getKullaniciId()))
                        .build())
                .collect(Collectors.toList());
        return SohbetOdaDTO.builder()
                .id(oda.getId())
                .sirketId(oda.getSirketId())
                .ad(oda.getAd())
                .aciklama(oda.getAciklama())
                .olusturanKullaniciId(oda.getOlusturanKullaniciId())
                .uyeMi(uyeMi)
                .uyeSayisi(uyeler.size())
                .uyeler(uyeler)
                .build();
    }

    private String kullaniciAdi(Long kullaniciId) {
        if (kullaniciId == null) return null;
        return kullaniciRepository.findById(kullaniciId).map(Kullanici::getDisplayName).orElse(null);
    }

    private SohbetMesajDTO mesajDTO(SohbetMesaj m) {
        return SohbetMesajDTO.builder()
                .id(m.getId())
                .sirketId(m.getSirketId())
                .odaId(m.getOdaId())
                .kullaniciId(m.getKullaniciId())
                .kullaniciAd(m.getKullaniciAd())
                .mesaj(m.getMesaj())
                .olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
