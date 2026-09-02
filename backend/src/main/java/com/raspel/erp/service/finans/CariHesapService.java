package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.CariFiyatRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.entity.finans.CariFiyat;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.dto.finans.CariFiyatDTO;
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
import java.util.Map;
import java.util.LinkedHashMap;
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
    private final FaturaRepository faturaRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final CariFiyatRepository cariFiyatRepository;
    private final StokRepository stokRepository;

    // ---------- CARİYE ÖZEL FİYAT ----------

    @Transactional(readOnly = true)
    public List<CariFiyatDTO> cariFiyatlari(Long cariHesapId) {
        List<CariFiyat> fiyatlar = cariFiyatRepository.findByCariHesapIdOrderByStokId(cariHesapId);
        Map<Long, Stok> stokMap = fiyatlar.stream().map(CariFiyat::getStokId).distinct()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList()).isEmpty() ? Map.of()
                : stokRepository.findAllById(fiyatlar.stream().map(CariFiyat::getStokId).distinct().collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(Stok::getId, s -> s));
        return fiyatlar.stream().map(f -> {
            Stok s = stokMap.get(f.getStokId());
            return CariFiyatDTO.builder()
                    .id(f.getId()).cariHesapId(f.getCariHesapId()).stokId(f.getStokId())
                    .stokAd(s != null ? s.getAd() : null).stokKodu(s != null ? s.getStokKodu() : null)
                    .fiyat(f.getFiyat()).sirketId(f.getSirketId()).olusturmaTarihi(f.getOlusturmaTarihi())
                    .build();
        }).collect(Collectors.toList());
    }

    public CariFiyatDTO cariFiyatKaydet(Long cariHesapId, CariFiyatDTO dto, Long sirketId) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("CariHesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        CariFiyat fiyat = cariFiyatRepository.findByCariHesapIdAndStokId(cariHesapId, dto.getStokId())
                .orElseGet(() -> CariFiyat.builder().cariHesapId(cariHesapId).stokId(dto.getStokId())
                        .sirketId(sirketId).build());
        fiyat.setFiyat(dto.getFiyat() != null ? dto.getFiyat() : BigDecimal.ZERO);
        CariFiyat saved = cariFiyatRepository.save(fiyat);
        Stok s = stokRepository.findById(saved.getStokId()).orElse(null);
        return CariFiyatDTO.builder().id(saved.getId()).cariHesapId(saved.getCariHesapId())
                .stokId(saved.getStokId()).stokAd(s != null ? s.getAd() : null)
                .stokKodu(s != null ? s.getStokKodu() : null).fiyat(saved.getFiyat())
                .sirketId(saved.getSirketId()).olusturmaTarihi(saved.getOlusturmaTarihi()).build();
    }

    public void cariFiyatSil(Long id) {
        CariFiyat fiyat = cariFiyatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CariFiyat", id));
        CariHesap cari = cariHesapRepository.findById(fiyat.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("CariHesap", fiyat.getCariHesapId()));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        cariFiyatRepository.deleteById(id);
    }

    /** Bir cariye özel fiyat varsa onu, yoksa null döner (satışta kullanılır). */
    @Transactional(readOnly = true)
    public BigDecimal cariOzelFiyat(Long cariHesapId, Long stokId) {
        return cariFiyatRepository.findByCariHesapIdAndStokId(cariHesapId, stokId)
                .map(CariFiyat::getFiyat).orElse(null);
    }

    /**
     * Tüm cari hesapları getir
     */
    public Page<CariHesapDTO> tumCariHesaplariGetir(Long sirketId, Pageable pageable) {
        log.debug("Tüm cari hesaplar getiriliyor, sirketId: {}", sirketId);
        return cariHesapRepository.findBySirketId(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    /**
     * Sunucu tarafında filtrelenmiş, aranmış ve sayfalanmış cari listesi.
     */
    public Page<CariHesapDTO> filtreli(Long sirketId, String q, String tur, String bakiyeYonu, Pageable pageable) {
        return cariHesapRepository.filtreli(sirketId, bosIseNull(q), bosIseNull(tur), bosIseNull(bakiyeYonu), pageable)
                .map(this::entityDTOyeCevir);
    }

    private String bosIseNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
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
    @Cacheable(value = "cariHesaplar", key = "T(com.raspel.erp.config.TenantChecker).tenantKey(#id)")
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
                .fotoUrl(dto.getFotoUrl())
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
        if (dto.getFotoUrl() != null) cariHesap.setFotoUrl(dto.getFotoUrl());
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

        long faturaSayisi = faturaRepository.countByCariHesapId(id);
        if (faturaSayisi > 0) {
            throw new BusinessException("Bu cari hesaba ait " + faturaSayisi + " adet fatura bulunmaktadır. Önce faturaları iptal/siliniz.");
        }

        cariHesapRepository.deleteById(id);
        log.info("Cari hesap başarıyla silindi - ID: {}", id);
    }
    
    /**
     * Bakiyeyi güncelle (Hareket eklendiğinde çağrılır)
     */
    public void bakiyeGuncelle(Long cariHesapId, BigDecimal tutar) {
        log.debug("Bakiye güncelleniyor - ID: {}, Tutar: {}", cariHesapId, tutar);
        
        CariHesap cariHesap = cariHesapRepository.findByIdForUpdate(cariHesapId)
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
            return 0L;
        }
        return cariHesapRepository.countBySirketId(sirketId);
    }

    /**
     * Toplam bakiyeyi getir (tenant filtreli)
     */
    public BigDecimal toplamBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            return BigDecimal.ZERO;
        }
        return cariHesapRepository.toplamBakiyeHesaplaBySirketId(sirketId);
    }

    public BigDecimal toplamPozitifBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            return BigDecimal.ZERO;
        }
        return cariHesapRepository.toplamPozitifBakiyeBySirketId(sirketId);
    }

    public BigDecimal toplamNegatifBakiyeGetir(Long sirketId) {
        if (sirketId == null) {
            return BigDecimal.ZERO;
        }
        return cariHesapRepository.toplamNegatifBakiyeBySirketId(sirketId);
    }

    /** Cari listesi için istatistik özeti (toplam kayıt, alacaklı, borçlu). */
    @Transactional(readOnly = true)
    public Map<String, Object> ozet(Long sirketId) {
        Map<String, Object> ozet = new LinkedHashMap<>();
        ozet.put("toplamKayit", cariHesapRepository.countBySirketId(sirketId));
        ozet.put("alacakli", cariHesapRepository.toplamPozitifBakiyeBySirketId(sirketId));
        ozet.put("borclu", cariHesapRepository.toplamNegatifBakiyeBySirketId(sirketId).abs());
        return ozet;
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
                .fotoUrl(cariHesap.getFotoUrl())
                .aktif(cariHesap.getAktif())
                .krediLimiti(cariHesap.getKrediLimiti())
                .odemeVadesi(cariHesap.getOdemeVadesi())
                .bakiye(cariHesap.getBakiye())
                .olusturmaTarihi(cariHesap.getOlusturmaTarihi())
                .guncellemeTarihi(cariHesap.getGuncellemeTarihi())
                .build();
    }
}