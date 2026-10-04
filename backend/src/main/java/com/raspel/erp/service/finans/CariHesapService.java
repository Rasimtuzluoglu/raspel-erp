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
import com.raspel.erp.repository.ticaret.TeklifRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisRepository;
import com.raspel.erp.repository.ticaret.CrmAktiviteRepository;
import com.raspel.erp.repository.ticaret.CariFirsatRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import com.raspel.erp.repository.finans.TaksitRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.service.sistem.AuditLogService;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

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
    private final TeklifRepository teklifRepository;
    private final SiparisRepository siparisRepository;
    private final SatinalmaSiparisRepository satinalmaSiparisRepository;
    private final CekSenetRepository cekSenetRepository;
    private final IrsaliyeRepository irsaliyeRepository;
    private final TaksitRepository taksitRepository;
    private final StokHareketRepository stokHareketRepository;
    private final NotRepository notRepository;
    private final CrmAktiviteRepository crmAktiviteRepository;
    private final CariFirsatRepository cariFirsatRepository;
    private final AuditLogService auditLogService;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final CariFiyatRepository cariFiyatRepository;
    private final StokRepository stokRepository;

    // ---------- CARİYE ÖZEL FİYAT ----------

    /**
     * Cariye özel fiyat listesi.
     *
     * <p>Tenant izolasyonu: yazma yolu ({@link #cariFiyatKaydet}) cariyi yükleyip
     * {@code tenantChecker.check} çağırıyordu, okuma yolu çağırmıyordu. Bu
     * boşluk sayesinde herhangi bir {@code USER} rolündeki kullanıcı başka bir
     * şirketin cari id'sini bilerek o cariye özel fiyatlarını ve bağlı stok
     * adlarını/kodlarını okuyabiliyordu (cross-tenant IDOR).
     */
@Transactional(readOnly = true)
    public List<CariFiyatDTO> cariFiyatlari(Long cariHesapId) {
        List<CariFiyat> fiyatlar;
        Long sirketId = tenantChecker.getCurrentSirketId();
        if (sirketId == null) {
            // Dahili çağrı (request bağlamı yok): tenant filtresi uygulanamaz.
            fiyatlar = cariFiyatRepository.findByCariHesapIdOrderByStokId(cariHesapId);
        } else {
            fiyatlar = cariFiyatRepository.findBySirketIdAndCariHesapIdOrderByStokId(sirketId, cariHesapId);
        }
        if (fiyatlar.isEmpty()) return List.of();
        Map<Long, Stok> stokMap = stokRepository
                .findAllById(fiyatlar.stream().map(CariFiyat::getStokId).distinct()
                        .filter(java.util.Objects::nonNull).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Stok::getId, s -> s, (a, b) -> a));
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

    /**
     * Tüm cari hesapları getir
     */
    public Page<CariHesapDTO> tumCariHesaplariGetir(Long sirketId, Pageable pageable) {
        log.debug("Tüm cari hesaplar getiriliyor, sirketId: {}", sirketId);
        return cariHesapRepository.findBySirketId(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    /**
     * Dışa aktarma için cari listesi.
     *
     * <p>{@code ids} verilirse YALNIZCA o kayıtlar aktarılır (toplu seçimden
     * gelen "CSV Aktar" aksiyonu). Verilmezse tüm şirket kayıtları döner.
     * Tenant izolasyonu zorunludur: başka şirketin id'si listelenebilseydi
     * sızıntı olurdu.
     */
    public List<CariHesapDTO> disaAktarimListesi(Long sirketId, List<Long> ids, int maxSatir) {
        if (ids != null && !ids.isEmpty()) {
            return cariHesapRepository.findBySirketIdAndIdIn(sirketId, ids).stream()
                    .sorted(Comparator.comparing(CariHesap::getId))
                    .map(this::entityDTOyeCevir)
                    .collect(Collectors.toList());
        }
        return cariHesapRepository.findBySirketId(sirketId,
                        org.springframework.data.domain.PageRequest.of(0, maxSatir))
                .map(this::entityDTOyeCevir)
                .getContent();
    }

    /**
     * Sunucu tarafında filtrelenmiş, aranmış ve sayfalanmış cari listesi.
     */
    public Page<CariHesapDTO> filtreli(Long sirketId, String q, String tur, String bakiyeYonu, Pageable pageable) {
        // Joker karakterler kaçışlanır; 1 karakterli arama reddedilir.
        String arama = com.raspel.erp.util.AramaTemizleyici.like(q);
        return cariHesapRepository.filtreli(sirketId, arama, bosIseNull(tur), bosIseNull(bakiyeYonu), pageable)
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
                .fotoThumbUrl(dto.getFotoThumbUrl())
                .krediLimiti(dto.getKrediLimiti())
                .odemeVadesi(dto.getOdemeVadesi())
                .bakiye(BigDecimal.ZERO)
                .sirketId(sirketId)
                .temsilciId(dto.getTemsilciId())
                .temsilciAd(dto.getTemsilciAd())
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
        if (dto.getFotoThumbUrl() != null) cariHesap.setFotoThumbUrl(dto.getFotoThumbUrl());
        if (dto.getAktif() != null) cariHesap.setAktif(dto.getAktif());
        if (dto.getKrediLimiti() != null) cariHesap.setKrediLimiti(dto.getKrediLimiti());
        if (dto.getOdemeVadesi() != null) cariHesap.setOdemeVadesi(dto.getOdemeVadesi());
        if (dto.getTemsilciId() != null) cariHesap.setTemsilciId(dto.getTemsilciId());
        if (dto.getTemsilciAd() != null) cariHesap.setTemsilciAd(dto.getTemsilciAd());
        
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

        // Islem gormus cari silinemez: finansal/operasyonel kayitlar engel teskil eder.
        silmeEngelleriniKontrol(id);

        // Islem niteligi tasimayan bagli kayitlar cari ile birlikte silinir.
        cariFiyatRepository.deleteByCariHesapId(id);
        notRepository.deleteByCariHesapId(id);
        crmAktiviteRepository.deleteByCariHesapId(id);
        cariFirsatRepository.deleteByCariHesapId(id);

        cariHesapRepository.deleteById(id);
        auditLogService.finansalSilmeLog("CariHesap", id,
                "Cari hesap silindi: " + cariHesap.getAd());
        log.info("Cari hesap başarıyla silindi - ID: {}", id);
    }

    /** Cariye bagli finansal/operasyonel kayit varsa silmeyi engeller. */
    private void silmeEngelleriniKontrol(Long id) {
        java.util.List<String> engeller = new java.util.ArrayList<>();
        long hareket = hareketRepository.countByCariHesapId(id);
        if (hareket > 0) engeller.add(hareket + " hareket");
        long fatura = faturaRepository.countByCariHesapId(id);
        if (fatura > 0) engeller.add(fatura + " fatura");
        long teklif = teklifRepository.countByCariHesapId(id);
        if (teklif > 0) engeller.add(teklif + " teklif");
        long siparis = siparisRepository.countByCariHesapId(id);
        if (siparis > 0) engeller.add(siparis + " sipariş");
        long satinalma = satinalmaSiparisRepository.countByCariHesapId(id);
        if (satinalma > 0) engeller.add(satinalma + " satınalma siparişi");
        long cekSenet = cekSenetRepository.countByCariHesapId(id);
        if (cekSenet > 0) engeller.add(cekSenet + " çek/senet");
        long irsaliye = irsaliyeRepository.countByCariHesapId(id);
        if (irsaliye > 0) engeller.add(irsaliye + " irsaliye");
        long taksit = taksitRepository.countByCariHesap_Id(id);
        if (taksit > 0) engeller.add(taksit + " taksit");
        long stokHareket = stokHareketRepository.countByCariHesap_Id(id);
        if (stokHareket > 0) engeller.add(stokHareket + " stok hareketi");
        if (!engeller.isEmpty()) {
            throw new BusinessException(
                    "Bu cariye ait işlem kayıtları bulunduğu için silinemez: " + String.join(", ", engeller) + ".");
        }
    }
    
    /**
     * Bakiyeyi güncelle (Hareket eklendiğinde çağrılır)
     */
    public void bakiyeGuncelle(Long cariHesapId, BigDecimal tutar) {
        log.debug("Bakiye güncelleniyor - ID: {}, Tutar: {}", cariHesapId, tutar);

        CariHesap cariHesap = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");

        // Atomik artirma: es zamanli islemlerde oku-degistir-yaz kaynakli
        // OptimisticLockingFailureException olusmaz.
        cariHesapRepository.bakiyeArttir(cariHesapId, tutar);
        cacheYardimci.commitSonrasiTemizle("cariHesaplar", "dashboard");
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
    /**
     * Cari listesi KPI özeti.
     *
     * <p>BAKİYE İŞARET KURALI (tüm modüllerde aynı): <b>negatif bakiye = cari
     * bize borçlu (alacak)</b>, pozitif bakiye = biz cariye borçluyuz.
     * Kaynak: {@code FaturaService.cariBakiyeGuncelle} — satış faturası tutarı
     * negatife çevirerek bakiyeye ekler.
     *
     * <p>ÖNCE {@code alacakli} pozitif, {@code borclu} negatif toplamı
     * döndürüyordu; yani etiketler ters yönü gösteriyordu: "Alacaklı" kartı
     * bizim cariye olan borçlarımızı listeliyordu.
     */
    public Map<String, Object> ozet(Long sirketId) {
        Map<String, Object> ozet = new LinkedHashMap<>();
        ozet.put("toplamKayit", cariHesapRepository.countBySirketId(sirketId));
        ozet.put("alacakli", bosMuMu(cariHesapRepository.toplamNegatifBakiyeBySirketId(sirketId)).abs());
        ozet.put("borclu", bosMuMu(cariHesapRepository.toplamPozitifBakiyeBySirketId(sirketId)));
        return ozet;
    }

    private BigDecimal bosMuMu(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
                .fotoThumbUrl(cariHesap.getFotoThumbUrl())
                .aktif(cariHesap.getAktif())
                .krediLimiti(cariHesap.getKrediLimiti())
                .odemeVadesi(cariHesap.getOdemeVadesi())
                .bakiye(cariHesap.getBakiye())
                .temsilciId(cariHesap.getTemsilciId())
                .temsilciAd(cariHesap.getTemsilciAd())
                .olusturmaTarihi(cariHesap.getOlusturmaTarihi())
                .guncellemeTarihi(cariHesap.getGuncellemeTarihi())
                .build();
    }
}