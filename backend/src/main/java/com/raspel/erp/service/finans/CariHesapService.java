package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.sistem.Sirket;

/**
 * Cari Hesap Service
 * Cari hesap işlemlerinin business logic'ini yönetir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CariHesapService {
    
    private final CariHesapRepository cariHesapRepository;
    private final HareketRepository hareketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;

    /**
     * Tüm cari hesapları getir
     */
    public Page<CariHesapDTO> tumCariHesaplariGetir(Long sirketId, Pageable pageable) {
        log.debug("Tüm cari hesaplar getiriliyor, sirketId: {}", sirketId);
        return cariHesapRepository.findBySirketId(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    /**
     * İsme göre cari hesapları ara
     */
    public List<CariHesapDTO> cariHesapAra(String query, Long sirketId) {
        log.debug("Cari hesaplar aranıyor: {}, sirketId: {}", query, sirketId);
        return cariHesapRepository.findBySirketIdAndAdContainingIgnoreCase(sirketId, query)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }
    
    /**
     * ID'ye göre cari hesap getir
     */
    @Cacheable(value = "cariHesaplar", key = "#id")
    public CariHesapDTO cariHesapGetir(Long id) {
        log.debug("ID: {} için cari hesap getiriliyor", id);
        CariHesap cariHesap = cariHesapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", id));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        return entityDTOyeCevir(cariHesap);
    }
    
    /**
     * Yeni cari hesap oluştur
     */
    @CacheEvict(value = "cariHesaplar", allEntries = true)
    public CariHesapDTO cariHesapOlustur(CariHesapDTO dto, Long sirketId) {
        log.info("Yeni cari hesap oluşturuluyor: {}, sirketId: {}", dto.getAd(), sirketId);
        
        CariHesap cariHesap = CariHesap.builder()
                .ad(dto.getAd())
                .vergiNumarasi(dto.getVergiNumarasi())
                .telefon(dto.getTelefon())
                .email(dto.getEmail())
                .adres(dto.getAdres())
                .tur(dto.getTur())
                .il(dto.getIl())
                .ilce(dto.getIlce())
                .vergiDairesi(dto.getVergiDairesi())
                .yetkiliKisi(dto.getYetkiliKisi())
                .yetkiliTelefon(dto.getYetkiliTelefon())
                .iban(dto.getIban())
                .notlar(dto.getNotlar())
                .krediLimiti(dto.getKrediLimiti())
                .odemeVadesi(dto.getOdemeVadesi())
                .bakiye(BigDecimal.ZERO)
                .sirketId(sirketId)
                .build();
        
        CariHesap kaydedilenCariHesap = cariHesapRepository.save(cariHesap);
        log.info("Cari hesap başarıyla oluşturuldu - ID: {}", kaydedilenCariHesap.getId());
        
        return entityDTOyeCevir(kaydedilenCariHesap);
    }
    
    /**
     * Cari hesap güncelle
     */
    @CacheEvict(value = "cariHesaplar", allEntries = true)
    public CariHesapDTO cariHesapGuncelle(Long id, CariHesapDTO dto) {
        log.info("Cari hesap güncelleniyor - ID: {}", id);
        
        CariHesap cariHesap = cariHesapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", id));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        
        if (dto.getAd() != null) cariHesap.setAd(dto.getAd());
        if (dto.getVergiNumarasi() != null) cariHesap.setVergiNumarasi(dto.getVergiNumarasi());
        if (dto.getTelefon() != null) cariHesap.setTelefon(dto.getTelefon());
        if (dto.getEmail() != null) cariHesap.setEmail(dto.getEmail());
        if (dto.getAdres() != null) cariHesap.setAdres(dto.getAdres());
        if (dto.getTur() != null) cariHesap.setTur(dto.getTur());
        if (dto.getIl() != null) cariHesap.setIl(dto.getIl());
        if (dto.getIlce() != null) cariHesap.setIlce(dto.getIlce());
        if (dto.getVergiDairesi() != null) cariHesap.setVergiDairesi(dto.getVergiDairesi());
        if (dto.getYetkiliKisi() != null) cariHesap.setYetkiliKisi(dto.getYetkiliKisi());
        if (dto.getYetkiliTelefon() != null) cariHesap.setYetkiliTelefon(dto.getYetkiliTelefon());
        if (dto.getIban() != null) cariHesap.setIban(dto.getIban());
        if (dto.getNotlar() != null) cariHesap.setNotlar(dto.getNotlar());
        if (dto.getAktif() != null) cariHesap.setAktif(dto.getAktif());
        if (dto.getKrediLimiti() != null) cariHesap.setKrediLimiti(dto.getKrediLimiti());
        if (dto.getOdemeVadesi() != null) cariHesap.setOdemeVadesi(dto.getOdemeVadesi());
        
        CariHesap guncellenenCariHesap = cariHesapRepository.save(cariHesap);
        log.info("Cari hesap başarıyla güncellendi - ID: {}", id);
        
        return entityDTOyeCevir(guncellenenCariHesap);
    }
    
    /**
     * Cari hesap sil
     */
    @CacheEvict(value = "cariHesaplar", allEntries = true)
    public void cariHesapSil(Long id) {
        log.info("Cari hesap siliniliyor - ID: {}", id);

        CariHesap cariHesap = cariHesapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", id));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");

        long hareketSayisi = hareketRepository.countByCariHesapId(id);
        if (hareketSayisi > 0) {
            throw new BusinessException("Bu cari hesaba ait " + hareketSayisi + " adet hareket bulunmaktadır. Önce hareketleri siliniz.");
        }

        cariHesapRepository.deleteById(id);
        log.info("Cari hesap başarıyla silindi - ID: {}", id);
    }
    
    /**
     * Bakiyeyi güncelle (Hareket eklendiğinde çağrılır)
     */
    public void bakiyeGuncelle(Long cariHesapId, BigDecimal tutar) {
        log.debug("Bakiye güncelleniyor - ID: {}, Tutar: {}", cariHesapId, tutar);
        
        CariHesap cariHesap = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        
        BigDecimal yeniBakiye = cariHesap.getBakiye().add(tutar);
        cariHesap.setBakiye(yeniBakiye);
        cariHesapRepository.save(cariHesap);
        cacheYardimci.temizle("cariHesaplar", "dashboard");
    }
    


    /**
     * Toplam cari sayısını getir (tenant filtreli)
     */
    public Long toplamCariSayisiGetir(Long sirketId) {
        if (sirketId == null) {
            log.warn("sirketId is null, falling back to global count. Check user/sirket association.");
            return cariHesapRepository.count();
        }
        return cariHesapRepository.countBySirketId(sirketId);
    }

    /**
     * Toplam bakiyeyi getir (tenant filtreli)
     */
    public BigDecimal toplamBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            log.warn("sirketId is null, falling back to global bakiye. Check user/sirket association.");
            return cariHesapRepository.toplamBakiyeHesapla();
        }
        return cariHesapRepository.toplamBakiyeHesaplaBySirketId(sirketId);
    }

    public BigDecimal toplamPozitifBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            log.warn("sirketId is null, falling back to global pozitif bakiye.");
            return cariHesapRepository.toplamPozitifBakiye();
        }
        return cariHesapRepository.toplamPozitifBakiyeBySirketId(sirketId);
    }

    public BigDecimal toplamNegatifBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            log.warn("sirketId is null, falling back to global negatif bakiye.");
            return cariHesapRepository.toplamNegatifBakiye();
        }
        return cariHesapRepository.toplamNegatifBakiyeBySirketId(sirketId);
    }

    /**
     * Entity'yi DTO'ya çevir
     */
    private CariHesapDTO entityDTOyeCevir(CariHesap cariHesap) {
        return CariHesapDTO.builder()
                .id(cariHesap.getId())
                .ad(cariHesap.getAd())
                .vergiNumarasi(cariHesap.getVergiNumarasi())
                .telefon(cariHesap.getTelefon())
                .email(cariHesap.getEmail())
                .adres(cariHesap.getAdres())
                .tur(cariHesap.getTur())
                .il(cariHesap.getIl())
                .ilce(cariHesap.getIlce())
                .vergiDairesi(cariHesap.getVergiDairesi())
                .yetkiliKisi(cariHesap.getYetkiliKisi())
                .yetkiliTelefon(cariHesap.getYetkiliTelefon())
                .iban(cariHesap.getIban())
                .notlar(cariHesap.getNotlar())
                .aktif(cariHesap.getAktif())
                .krediLimiti(cariHesap.getKrediLimiti())
                .odemeVadesi(cariHesap.getOdemeVadesi())
                .bakiye(cariHesap.getBakiye())
                .olusturmaTarihi(cariHesap.getOlusturmaTarihi())
                .guncellemeTarihi(cariHesap.getGuncellemeTarihi())
                .build();
    }
}