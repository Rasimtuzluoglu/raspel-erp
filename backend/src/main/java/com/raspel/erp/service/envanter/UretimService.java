package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.ReceteDTO;
import com.raspel.erp.dto.envanter.ReceteKalemDTO;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.entity.envanter.Recete;
import com.raspel.erp.entity.envanter.ReceteKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.ReceteKalemRepository;
import com.raspel.erp.repository.envanter.ReceteRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UretimService {

    private final ReceteRepository receteRepository;
    private final ReceteKalemRepository receteKalemRepository;
    private final UretimEmriRepository uretimEmriRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;

    @Transactional(readOnly = true)
    public List<ReceteDTO> receteler(Long sirketId) {
        return receteRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(this::receteDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReceteDTO receteOlustur(ReceteDTO dto, Long sirketId) {
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("Reçete adı boş olamaz");
        }
        if (dto.getUrunId() == null) {
            throw new BusinessException("Üretilecek ürün (mamul) seçilmelidir");
        }
        Recete r = receteRepository.save(Recete.builder()
                .sirketId(sirketId)
                .ad(dto.getAd().trim())
                .urunId(dto.getUrunId())
                .aciklama(dto.getAciklama())
                .build());
        if (dto.getKalemler() != null) {
            for (ReceteKalemDTO k : dto.getKalemler()) {
                if (k.getHammaddeId() == null) continue;
                receteKalemRepository.save(ReceteKalem.builder()
                        .receteId(r.getId())
                        .hammaddeId(k.getHammaddeId())
                        .miktar(k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO)
                        .build());
            }
        }
        return receteDTO(r);
    }

    @Transactional
    public void receteSil(Long id, Long sirketId) {
        Recete r = receteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reçete", id));
        receteKalemRepository.deleteByReceteId(id);
        receteRepository.delete(r);
    }

    @Transactional(readOnly = true)
    public List<UretimEmriDTO> uretimEmirleri(Long sirketId) {
        return uretimEmriRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId).stream()
                .map(this::emriDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UretimEmriDTO uretimEmriOlustur(UretimEmriDTO dto, Long sirketId) {
        if (dto.getUrunId() == null) {
            throw new BusinessException("Ürün seçilmelidir");
        }
        if (dto.getMiktar() == null || dto.getMiktar().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Üretim miktarı 0'dan büyük olmalıdır");
        }
        UretimEmri e = uretimEmriRepository.save(UretimEmri.builder()
                .sirketId(sirketId)
                .siparisId(dto.getSiparisId())
                .urunId(dto.getUrunId())
                .miktar(dto.getMiktar())
                .durum(UretimEmri.Durum.TASLAK.name())
                .aciklama(dto.getAciklama())
                .build());
        return emriDTO(e);
    }

    @Transactional
    public UretimEmriDTO uretimEmriTamamla(Long id, Long sirketId) {
        UretimEmri e = uretimEmriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Üretim Emri", id));
        if (UretimEmri.Durum.TAMAMLANDI.name().equals(e.getDurum())
                || UretimEmri.Durum.IPTAL.name().equals(e.getDurum())) {
            throw new BusinessException("Bu üretim emri tamamlanamaz (durum: " + e.getDurum() + ")");
        }

        Recete recete = receteRepository.findFirstBySirketIdAndUrunId(sirketId, e.getUrunId())
                .orElseThrow(() -> new BusinessException("Bu ürün için tanımlı reçete bulunamadı"));
        List<ReceteKalem> kalemler = receteKalemRepository.findByReceteId(recete.getId());

        // Hammaddeleri tüket
        for (ReceteKalem k : kalemler) {
            BigDecimal gereken = (k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO).multiply(e.getMiktar());
            Stok hammadde = stokRepository.findByIdForUpdate(k.getHammaddeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hammadde", k.getHammaddeId()));
            BigDecimal mevcut = hammadde.getMiktar() != null ? hammadde.getMiktar() : BigDecimal.ZERO;
            if (mevcut.compareTo(gereken) < 0) {
                throw new BusinessException("Yetersiz hammadde stoğu: " + hammadde.getAd()
                        + " (mevcut: " + mevcut + ", gerekli: " + gereken + ")");
            }
            hammadde.setMiktar(mevcut.subtract(gereken));
            stokRepository.save(hammadde);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(hammadde).tur("CIKIS").miktar(gereken)
                    .hareketTarihi(LocalDate.now()).aciklama("Üretim: " + recete.getAd())
                    .build());
        }

        // Mamul üret
        Stok mamul = stokRepository.findByIdForUpdate(e.getUrunId())
                .orElseThrow(() -> new ResourceNotFoundException("Ürün", e.getUrunId()));
        mamul.setMiktar((mamul.getMiktar() != null ? mamul.getMiktar() : BigDecimal.ZERO).add(e.getMiktar()));
        stokRepository.save(mamul);
        stokHareketRepository.save(StokHareket.builder()
                .stok(mamul).tur("GIRIS").miktar(e.getMiktar())
                .hareketTarihi(LocalDate.now()).aciklama("Üretim: " + recete.getAd())
                .build());

        e.setDurum(UretimEmri.Durum.TAMAMLANDI.name());
        e.setTamamlanmaTarihi(LocalDateTime.now());
        uretimEmriRepository.save(e);
        log.info("Üretim emri tamamlandı - ID: {}, Ürün: {}, Miktar: {}", id, mamul.getAd(), e.getMiktar());
        return emriDTO(e);
    }

    private ReceteDTO receteDTO(Recete r) {
        List<ReceteKalemDTO> kalemler = receteKalemRepository.findByReceteId(r.getId()).stream()
                .map(k -> ReceteKalemDTO.builder()
                        .id(k.getId()).receteId(k.getReceteId())
                        .hammaddeId(k.getHammaddeId())
                        .hammaddeAd(stokAd(k.getHammaddeId()))
                        .miktar(k.getMiktar())
                        .build())
                .collect(Collectors.toList());
        return ReceteDTO.builder()
                .id(r.getId()).sirketId(r.getSirketId()).ad(r.getAd())
                .urunId(r.getUrunId()).urunAd(stokAd(r.getUrunId()))
                .aciklama(r.getAciklama()).kalemler(kalemler)
                .build();
    }

    private UretimEmriDTO emriDTO(UretimEmri e) {
        return UretimEmriDTO.builder()
                .id(e.getId()).sirketId(e.getSirketId()).urunId(e.getUrunId())
                .urunAd(stokAd(e.getUrunId())).miktar(e.getMiktar()).durum(e.getDurum())
                .aciklama(e.getAciklama()).olusturmaTarihi(e.getOlusturmaTarihi())
                .tamamlanmaTarihi(e.getTamamlanmaTarihi())
                .build();
    }

    private String stokAd(Long stokId) {
        if (stokId == null) return null;
        return stokRepository.findById(stokId).map(Stok::getAd).orElse(null);
    }
}
