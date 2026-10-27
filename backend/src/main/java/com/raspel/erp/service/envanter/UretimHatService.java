package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.UretimHatDTO;
import com.raspel.erp.entity.envanter.UretimHat;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import com.raspel.erp.repository.envanter.UretimHatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Üretim hattı / iş merkezi yönetimi ve doluluk özeti. */
@Service
@RequiredArgsConstructor
public class UretimHatService {

    private final UretimHatRepository hatRepository;
    private final UretimEmriRepository emirRepository;

    @Transactional(readOnly = true)
    public List<UretimHatDTO> listele(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        return hatRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                .map(h -> dto(h, bugun))
                .toList();
    }

    @Transactional
    public UretimHatDTO olustur(UretimHatDTO dto, Long sirketId) {
        String ad = dto.getAd() != null ? dto.getAd().trim() : "";
        if (ad.isBlank()) {
            throw new BusinessException("Hat adı zorunludur");
        }
        if (hatRepository.existsBySirketIdAndAdIgnoreCase(sirketId, ad)) {
            throw new BusinessException("Bu isimde bir hat zaten var");
        }
        UretimHat hat = hatRepository.save(UretimHat.builder()
                .sirketId(sirketId)
                .ad(ad)
                .aciklama(dto.getAciklama())
                .kapasite(kapasiteDegeri(dto.getKapasite()))
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build());
        return dto(hat, LocalDate.now());
    }

    @Transactional
    public UretimHatDTO guncelle(Long id, UretimHatDTO dto, Long sirketId) {
        UretimHat hat = hatRepository.findByIdAndSirketId(id, sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Üretim Hattı", id));
        if (dto.getAd() != null) {
            String ad = dto.getAd().trim();
            if (ad.isBlank()) {
                throw new BusinessException("Hat adı boş olamaz");
            }
            boolean ayniAdBaskaHat = hatRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                    .anyMatch(h -> !h.getId().equals(id) && h.getAd().equalsIgnoreCase(ad));
            if (ayniAdBaskaHat) {
                throw new BusinessException("Bu isimde bir hat zaten var");
            }
            hat.setAd(ad);
        }
        if (dto.getAciklama() != null) hat.setAciklama(dto.getAciklama());
        if (dto.getKapasite() != null) hat.setKapasite(kapasiteDegeri(dto.getKapasite()));
        if (dto.getAktif() != null) hat.setAktif(dto.getAktif());
        hatRepository.save(hat);
        return dto(hat, LocalDate.now());
    }

    @Transactional
    public void sil(Long id, Long sirketId) {
        UretimHat hat = hatRepository.findByIdAndSirketId(id, sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Üretim Hattı", id));
        if (emirRepository.existsBySirketIdAndHatId(sirketId, id)) {
            throw new BusinessException("Bu hatta bağlı üretim emirleri var; önce emirleri başka hatta taşıyın");
        }
        hatRepository.delete(hat);
    }

    private int kapasiteDegeri(Integer kapasite) {
        if (kapasite == null || kapasite <= 0) return 5;
        return Math.min(kapasite, 9999);
    }

    private UretimHatDTO dto(UretimHat h, LocalDate bugun) {
        long aktif = emirRepository.countAktifByHat(h.getSirketId(), h.getId());
        long geciken = emirRepository.countGecikenByHat(h.getSirketId(), h.getId(), bugun);
        int kapasite = kapasiteDegeri(h.getKapasite());
        int doluluk = (int) Math.min(100, Math.round(aktif * 100.0 / kapasite));
        return UretimHatDTO.builder()
                .id(h.getId())
                .ad(h.getAd())
                .aciklama(h.getAciklama())
                .kapasite(kapasite)
                .aktif(!Boolean.FALSE.equals(h.getAktif()))
                .aktifEmir(aktif)
                .gecikenEmir(geciken)
                .dolulukYuzde(doluluk)
                .build();
    }
}
