package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SirketDTO;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SirketService {

    private final SirketRepository sirketRepository;

    public Page<SirketDTO> tumunuGetir(Pageable pageable) {
        return sirketRepository.findAll(pageable).map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'sirketlerAktif'")
    public List<SirketDTO> aktifOlanlariGetir() {
        return sirketRepository.findByAktifTrue().stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "lookup", key = "'sirket:' + #id")
    public SirketDTO getir(Long id) {
        return entityToDTO(sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id)));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO olustur(SirketDTO dto) {
        Sirket s = Sirket.builder()
                .ad(dto.getAd())
                .vergiNo(dto.getVergiNo())
                .vergiDairesi(dto.getVergiDairesi())
                .adres(dto.getAdres())
                .telefon(dto.getTelefon())
                .email(dto.getEmail())
                .webSite(dto.getWebSite())
                .logoUrl(dto.getLogoUrl())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        return entityToDTO(sirketRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO guncelle(Long id, SirketDTO dto) {
        Sirket s = sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id));
        boolean adDegisiyor = dto.getAd() != null && !dto.getAd().equals(s.getAd());
        if (adDegisiyor) {
            if (s.getSonAdGuncellemeTarihi() != null &&
                s.getSonAdGuncellemeTarihi().plusDays(30).isAfter(LocalDateTime.now())) {
                long kalanGun = 30 - java.time.temporal.ChronoUnit.DAYS.between(s.getSonAdGuncellemeTarihi(), LocalDateTime.now());
                throw new BusinessException("Şirket adı " + kalanGun + " gün içinde tekrar değiştirilemez. Son değişiklik: " +
                        s.getSonAdGuncellemeTarihi().toLocalDate());
            }
            s.setAd(dto.getAd());
            s.setSonAdGuncellemeTarihi(LocalDateTime.now());
        }
        if (dto.getVergiNo() != null) s.setVergiNo(dto.getVergiNo());
        if (dto.getVergiDairesi() != null) s.setVergiDairesi(dto.getVergiDairesi());
        if (dto.getAdres() != null) s.setAdres(dto.getAdres());
        if (dto.getTelefon() != null) s.setTelefon(dto.getTelefon());
        if (dto.getEmail() != null) s.setEmail(dto.getEmail());
        if (dto.getWebSite() != null) s.setWebSite(dto.getWebSite());
        if (dto.getLogoUrl() != null) s.setLogoUrl(dto.getLogoUrl());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        return entityToDTO(sirketRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        if (!sirketRepository.existsById(id)) throw new ResourceNotFoundException("Şirket", id);
        sirketRepository.deleteById(id);
    }

    private SirketDTO entityToDTO(Sirket s) {
        return SirketDTO.builder()
                .id(s.getId()).ad(s.getAd())
                .vergiNo(s.getVergiNo()).vergiDairesi(s.getVergiDairesi())
                .adres(s.getAdres()).telefon(s.getTelefon())
                .email(s.getEmail()).webSite(s.getWebSite())
                .logoUrl(s.getLogoUrl())
                .aktif(s.getAktif()).olusturmaTarihi(s.getOlusturmaTarihi())
                .sonAdGuncellemeTarihi(s.getSonAdGuncellemeTarihi())
                .build();
    }
}
