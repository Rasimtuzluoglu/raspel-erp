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
    private final com.raspel.erp.repository.envanter.StokRepository stokRepository;
    private final com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;

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
                .parentId(dto.getParentId())
                .tur(dto.getTur() != null ? dto.getTur() : "DIGER")
                .yil(dto.getYil())
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
        if (dto.getParentId() != null) s.setParentId(dto.getParentId());
        if (dto.getTur() != null) s.setTur(dto.getTur());
        if (dto.getYil() != null) s.setYil(dto.getYil());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        return entityToDTO(sirketRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        if (!sirketRepository.existsById(id)) throw new ResourceNotFoundException("Şirket", id);
        sirketRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sistem.KonsolideOzetDTO konsolideOzet(Long anaSirketId) {
        Sirket ana = sirketRepository.findById(anaSirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", anaSirketId));

        List<Sirket> tumGrup = sirketRepository.findAll().stream()
                .filter(s -> s.getId().equals(anaSirketId) || (s.getParentId() != null && s.getParentId().equals(anaSirketId)))
                .collect(Collectors.toList());

        java.math.BigDecimal toplamStokDegeri = java.math.BigDecimal.ZERO;
        java.math.BigDecimal toplamAlacak = java.math.BigDecimal.ZERO;
        java.math.BigDecimal toplamBorc = java.math.BigDecimal.ZERO;

        List<com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO> sirketOzetleri = new java.util.ArrayList<>();

        for (Sirket s : tumGrup) {
            List<com.raspel.erp.entity.envanter.Stok> stoklar = stokRepository.findBySirketIdOrderByAd(s.getId(), Pageable.unpaged()).getContent();
            java.math.BigDecimal sirketStokDeger = stoklar.stream()
                    .map(stok -> {
                        java.math.BigDecimal miktar = stok.getMiktar() != null ? stok.getMiktar() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal fiyat = stok.getFiyat() != null ? stok.getFiyat() : java.math.BigDecimal.ZERO;
                        return miktar.multiply(fiyat);
                    })
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            List<com.raspel.erp.entity.finans.CariHesap> cariler = cariHesapRepository.findBySirketId(s.getId(), Pageable.unpaged()).getContent();
            java.math.BigDecimal sirketBakiye = cariler.stream()
                    .map(c -> c.getBakiye() != null ? c.getBakiye() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            toplamStokDegeri = toplamStokDegeri.add(sirketStokDeger);
            if (sirketBakiye.compareTo(java.math.BigDecimal.ZERO) >= 0) {
                toplamAlacak = toplamAlacak.add(sirketBakiye);
            } else {
                toplamBorc = toplamBorc.add(sirketBakiye.abs());
            }

            sirketOzetleri.add(com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO.builder()
                    .sirketId(s.getId())
                    .sirketAdi(s.getAd())
                    .tur(s.getTur())
                    .yil(s.getYil())
                    .stokDegeri(sirketStokDeger)
                    .bakiye(sirketBakiye)
                    .stokSayisi(stoklar.size())
                    .cariSayisi(cariler.size())
                    .build());
        }

        return com.raspel.erp.dto.sistem.KonsolideOzetDTO.builder()
                .anaSirketId(ana.getId())
                .anaSirketAdi(ana.getAd())
                .altSirketSayisi(tumGrup.size() - 1)
                .toplamStokDegeri(toplamStokDegeri)
                .toplamAlacakBakiye(toplamAlacak)
                .toplamBorcBakiye(toplamBorc)
                .toplamCiro(java.math.BigDecimal.ZERO)
                .sirketler(sirketOzetleri)
                .build();
    }

    private SirketDTO entityToDTO(Sirket s) {
        return SirketDTO.builder()
                .id(s.getId()).ad(s.getAd())
                .vergiNo(s.getVergiNo()).vergiDairesi(s.getVergiDairesi())
                .adres(s.getAdres()).telefon(s.getTelefon())
                .email(s.getEmail()).webSite(s.getWebSite())
                .logoUrl(s.getLogoUrl())
                .parentId(s.getParentId())
                .tur(s.getTur())
                .yil(s.getYil())
                .aktif(s.getAktif()).olusturmaTarihi(s.getOlusturmaTarihi())
                .sonAdGuncellemeTarihi(s.getSonAdGuncellemeTarihi())
                .build();
    }
}
