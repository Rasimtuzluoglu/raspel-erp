package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.dto.finans.CekSenetDurumLogDTO;
import com.raspel.erp.dto.finans.CekSenetOzetDTO;
import com.raspel.erp.dto.finans.CekSenetTakvimDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.CekSenet;
import com.raspel.erp.entity.finans.CekSenetDurumLog;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.CekSenetDurumLogRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Çek/senet yaşam döngüsü servisi.
 *
 * <p>Durumlar: {@code PORTFOY, TEMINATTA, CIRO_EDILDI, TAHSIL_EDILDI, PROTESTO, IADE}.
 * Legacy {@code ODENDI} kayıtları okunurken {@code TAHSIL_EDILDI} kabul edilir.
 * Tüm geçişlerde tenant, vade dönemi kilidi ve durum logu zorunludur.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CekSenetService {

    public static final String DURUM_PORTFOY = "PORTFOY";
    public static final String DURUM_TEMINATTA = "TEMINATTA";
    public static final String DURUM_CIRO_EDILDI = "CIRO_EDILDI";
    public static final String DURUM_TAHSIL_EDILDI = "TAHSIL_EDILDI";
    public static final String DURUM_PROTESTO = "PROTESTO";
    public static final String DURUM_IADE = "IADE";

    /** Legacy (revizyon öncesi) ödenmiş durumu. */
    public static final String LEGACY_ODENDI = "ODENDI";

    public static final String ISLEM_OLUSTUR = "OLUSTUR";
    public static final String ISLEM_GUNCELLE = "GUNCELLE";
    public static final String ISLEM_TAHSIL = "TAHSIL";
    public static final String ISLEM_TAHSIL_IPTAL = "TAHSIL_IPTAL";
    public static final String ISLEM_CIRO = "CIRO";
    public static final String ISLEM_CIRO_IADE = "CIRO_IADE";
    public static final String ISLEM_IADE_AL = "IADE_AL";
    public static final String ISLEM_TEMINAT = "TEMINAT";
    public static final String ISLEM_TEMINAT_IADE = "TEMINAT_IADE";
    public static final String ISLEM_PROTESTO = "PROTESTO";

    /** Kasa/banka hareketlerinde kaynak belge tipi. */
    public static final String KAYNAK_TIP_CEK_SENET = "CEK_SENET";

    /** Vade takvimi en fazla 62 günlük aralık döndürür. */
    public static final int TAKVIM_MAKS_GUN = 62;

    private static final int MIN_GEREKCE_UZUNLUK = 3;
    private static final int MAX_ACIKLAMA_UZUNLUK = 500;

    /** Bilinen durumlar ve izinli hedefleri (legacy serbest durumlar kısıtlanmaz). */
    private static final Map<String, Set<String>> GECISLER = Map.of(
            DURUM_PORTFOY, Set.of(DURUM_TEMINATTA, DURUM_CIRO_EDILDI, DURUM_TAHSIL_EDILDI, DURUM_PROTESTO),
            DURUM_TEMINATTA, Set.of(DURUM_PORTFOY, DURUM_TAHSIL_EDILDI, DURUM_PROTESTO),
            DURUM_CIRO_EDILDI, Set.of(DURUM_IADE),
            DURUM_IADE, Set.of(DURUM_PORTFOY),
            DURUM_TAHSIL_EDILDI, Set.of(),
            DURUM_PROTESTO, Set.of());

    private final CekSenetRepository cekSenetRepository;
    private final CekSenetDurumLogRepository cekSenetDurumLogRepository;
    private final CariHesapRepository cariHesapRepository;
    private final FaturaRepository faturaRepository;
    private final TenantChecker tenantChecker;
    private final HareketService hareketService;
    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final BankaRepository bankaRepository;
    private final BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;

    // ------------------------------------------------------------------
    // Okuma
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CekSenetDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<CekSenet> sayfa = cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(sirketId, pageable);
        List<CekSenetDTO> dtolar = sayfa.getContent().stream()
                .map(cs -> entityToDTO(cs, cariHaritasi(sirketId)))
                .collect(Collectors.toList());
        zenginlestir(sayfa.getContent(), dtolar);
        return new PageImpl<>(dtolar, pageable, sayfa.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<CekSenetDTO> filtreli(Long sirketId, String q, String tur, String durum, Long cariId,
                                      LocalDate baslangic, LocalDate bitis, String vadeAralik,
                                      BigDecimal minTutar, Boolean protesto, Pageable pageable) {
        String arama = com.raspel.erp.util.AramaTemizleyici.like(q);
        String turFiltre = bosMu(tur) ? null : tur.trim().toUpperCase(Locale.ROOT);
        String durumFiltre = bosMu(durum) ? null : durumNormalize(durum);
        LocalDate[] aralik = vadeAraligiCoz(vadeAralik, baslangic, bitis);

        Page<CekSenet> sayfa = cekSenetRepository.filtreliGetir(sirketId, arama, turFiltre, durumFiltre,
                cariId, aralik[0], aralik[1], minTutar, protesto, pageable);
        List<CekSenetDTO> dtolar = sayfa.getContent().stream()
                .map(cs -> entityToDTO(cs, cariHaritasi(sirketId)))
                .collect(Collectors.toList());
        zenginlestir(sayfa.getContent(), dtolar);
        return new PageImpl<>(dtolar, pageable, sayfa.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CekSenetDTO getir(Long id) {
        return detay(bul(id));
    }

    /** Detay DTO'su: zenginleştirilmiş alanlar + durum geçmişi. */
    private CekSenetDTO detay(CekSenet cs) {
        CekSenetDTO dto = entityToDTO(cs);
        zenginlestir(List.of(cs), List.of(dto));
        dto.setGecmis(gecmis(cs.getId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public CekSenetOzetDTO ozet(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        LocalDate bugun = LocalDate.now();
        LocalDate ayBas = bugun.withDayOfMonth(1);
        LocalDate ayBit = bugun.withDayOfMonth(bugun.lengthOfMonth());
        BigDecimal toplam = cekSenetRepository.toplamTutar(sirketId, baslangic, bitis);
        return CekSenetOzetDTO.builder()
                .portfoy(cekSenetRepository.countDurum(sirketId, DURUM_PORTFOY, baslangic, bitis))
                .teminatta(cekSenetRepository.countDurum(sirketId, DURUM_TEMINATTA, baslangic, bitis))
                .ciroEdilen(cekSenetRepository.countDurum(sirketId, DURUM_CIRO_EDILDI, baslangic, bitis))
                .tahsilEdilen(cekSenetRepository.countDurum(sirketId, DURUM_TAHSIL_EDILDI, baslangic, bitis))
                .protesto(cekSenetRepository.countDurum(sirketId, DURUM_PROTESTO, baslangic, bitis))
                .vadesiGecen(cekSenetRepository.countVadesiGecen(sirketId, bugun))
                .buAyVadesiGelen(cekSenetRepository.countVadesiGelen(sirketId, ayBas, ayBit))
                .toplamTutar(toplam != null ? toplam : BigDecimal.ZERO)
                .build();
    }

    /**
     * Gün bazlı vade takvimi. Aralık en fazla {@value #TAKVIM_MAKS_GUN} gün olabilir;
     * kaydı olmayan günler de {@code adet=0} ile döner (takvim grid'i boş gün ister).
     */
    @Transactional(readOnly = true)
    public List<CekSenetTakvimDTO> takvim(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        if (baslangic == null || bitis == null) {
            throw new BusinessException("Takvim için başlangıç ve bitiş tarihi zorunludur");
        }
        if (bitis.isBefore(baslangic)) {
            throw new BusinessException("Bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        long gunSayisi = ChronoUnit.DAYS.between(baslangic, bitis) + 1;
        if (gunSayisi > TAKVIM_MAKS_GUN) {
            throw new BusinessException("Takvim aralığı en fazla " + TAKVIM_MAKS_GUN + " gün olabilir");
        }

        List<CekSenet> kayitlar = cekSenetRepository
                .findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(sirketId, baslangic, bitis);

        Map<LocalDate, List<CekSenet>> gunluk = new LinkedHashMap<>();
        for (CekSenet cs : kayitlar) {
            gunluk.computeIfAbsent(cs.getVadeTarihi(), k -> new ArrayList<>()).add(cs);
        }

        Map<Long, String> cariHaritasi = cariHaritasi(sirketId);
        List<CekSenetTakvimDTO> sonuc = new ArrayList<>();
        LocalDate gun = baslangic;
        while (!gun.isAfter(bitis)) {
            List<CekSenet> gunkayitlari = gunluk.getOrDefault(gun, List.of());
            List<CekSenetDTO> dtolar = gunkayitlari.stream()
                    .map(cs -> entityToDTO(cs, cariHaritasi))
                    .collect(Collectors.toList());
            zenginlestir(gunkayitlari, dtolar);
            BigDecimal tutar = dtolar.stream()
                    .map(CekSenetDTO::getTutar)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sonuc.add(CekSenetTakvimDTO.builder()
                    .tarih(gun)
                    .adet(dtolar.size())
                    .tutar(tutar)
                    .kayitlar(dtolar)
                    .build());
            gun = gun.plusDays(1);
        }
        return sonuc;
    }

    // ------------------------------------------------------------------
    // Yazma
    // ------------------------------------------------------------------

    public CekSenetDTO olustur(CekSenetDTO dto) {
        tenantChecker.checkSirketId(dto.getSirketId(), "Çek/Senet");
        CekSenet cs = CekSenet.builder()
                .tur(dto.getTur()).cariHesapId(dto.getCariHesapId())
                .faturaId(dto.getFaturaId())
                .bankaAdi(dto.getBankaAdi()).sube(dto.getSube())
                .cekNo(dto.getCekNo()).hesapNo(dto.getHesapNo())
                .vadeTarihi(dto.getVadeTarihi()).tutar(dto.getTutar())
                .durum(DURUM_PORTFOY).aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId()).build();
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, null, DURUM_PORTFOY, ISLEM_OLUSTUR, null);
        return entityToDTO(kaydedilen);
    }

    public CekSenetDTO guncelle(Long id, CekSenetDTO dto) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "çek/senet düzenleme");
        String mevcut = durumNormalize(cs.getDurum());
        if (DURUM_TAHSIL_EDILDI.equals(mevcut)) {
            throw new BusinessException("Tahsil edilmiş çek/senet kaydı doğrudan düzenlenemez");
        }
        cs.setTur(dto.getTur());
        cs.setCariHesapId(dto.getCariHesapId());
        cs.setBankaAdi(dto.getBankaAdi());
        cs.setSube(dto.getSube());
        cs.setCekNo(dto.getCekNo());
        cs.setHesapNo(dto.getHesapNo());
        cs.setVadeTarihi(dto.getVadeTarihi());
        cs.setKesinmeTarihi(dto.getKesinmeTarihi());
        cs.setTutar(dto.getTutar());
        cs.setAciklama(dto.getAciklama());
        if (dto.getFaturaId() != null) cs.setFaturaId(dto.getFaturaId());
        String hedefDurum = durumNormalize(dto.getDurum());
        if (hedefDurum != null && !hedefDurum.equals(mevcut)) {
            // Tahsil, finansal hareket üretmesi gerektiği için düzenleme ucundan yapılamaz.
            if (DURUM_TAHSIL_EDILDI.equals(hedefDurum)) {
                throw new BusinessException("Tahsil edildi durumu yalnızca tahsil işlemi ile yapılabilir");
            }
            gecisDogrula(mevcut, hedefDurum);
            cs.setDurum(hedefDurum);
        }
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, durumNormalize(kaydedilen.getDurum()), ISLEM_GUNCELLE, null);
        return entityToDTO(kaydedilen);
    }

    /** Geriye uyum: durum güncelleme (kasa/banka/fatura olmadan). */
    public CekSenetDTO durumGuncelle(Long id, String durum) {
        return durumGuncelle(id, durum, null, null);
    }

    public CekSenetDTO durumGuncelle(Long id, String durum, Long kasaId, Long bankaId) {
        return durumGuncelle(id, durum, kasaId, bankaId, null);
    }

    /**
     * Legacy durum güncelleme ucu. {@code TAHSIL_EDILDI} hedefi tahsilat akışını
     * (cari hareket + kasa/banka + muhasebe fişi) tetikler. Diğer bilinen
     * durumlar geçiş matrisine göre doğrulanır; tanınmayan legacy durum
     * metinleri (ör. KARSILIKSIZ) eski davranışla yazılabilir.
     */
    public CekSenetDTO durumGuncelle(Long id, String durum, Long kasaId, Long bankaId, Long faturaId) {
        CekSenet cs = bul(id);
        if (bosMu(durum)) {
            throw new BusinessException("Durum boş olamaz");
        }
        String hedef = durumNormalize(durum);
        String mevcut = durumNormalize(cs.getDurum());
        kilitKontrol(cs, "çek/senet durum güncelleme");

        if (DURUM_TAHSIL_EDILDI.equals(hedef) && !DURUM_TAHSIL_EDILDI.equals(mevcut)) {
            return tahsilEt(cs, mevcut, kasaId, bankaId, faturaId);
        }
        if (DURUM_TAHSIL_EDILDI.equals(mevcut) && !DURUM_TAHSIL_EDILDI.equals(hedef)) {
            throw new BusinessException("Tahsil edilmiş çek/senet yalnızca tahsil iptali ile geri alınabilir");
        }
        if (GECISLER.containsKey(hedef)) {
            gecisDogrula(mevcut, hedef);
        }
        cs.setDurum(hedef);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, hedef, ISLEM_GUNCELLE, null);
        return entityToDTO(kaydedilen);
    }

    /**
     * Tahsil: cari hareketi (opsiyonel fatura bağıyla) ve seçildiyse tek bir
     * kasa/banka girişini oluşturur; oluşan hareket id'leri çek/senet kaydına
     * yazılır (tahsil iptalinde ters kayıt için).
     */
    private CekSenetDTO tahsilEt(CekSenet cs, String mevcut, Long kasaId, Long bankaId, Long faturaId) {
        if (kasaId != null && bankaId != null) {
            throw new BusinessException("Aynı tahsilat hem kasaya hem bankaya işlenemez; tek hesap seçin");
        }
        if (!DURUM_PORTFOY.equals(mevcut) && !DURUM_TEMINATTA.equals(mevcut)) {
            throw new BusinessException("Yalnızca portföy veya teminattaki çek/senet tahsil edilebilir (mevcut: "
                    + mevcut + ")");
        }
        // İstekte fatura verilmezse kayda bağlı fatura kullanılır (mevcut davranış).
        Long etkinFaturaId = faturaId != null ? faturaId : cs.getFaturaId();
        if (etkinFaturaId != null) {
            faturaDogrula(cs, etkinFaturaId);
        }
        cs.setDurum(DURUM_TAHSIL_EDILDI);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        tahsilatiIsle(kaydedilen, kasaId, bankaId, etkinFaturaId);
        otomatikMuhasebeService.cekSenetTahsilIsle(kaydedilen, kasaId, bankaId);
        kaydetLog(kaydedilen, mevcut, DURUM_TAHSIL_EDILDI, ISLEM_TAHSIL,
                tahsilAciklamasi(kasaId, bankaId, etkinFaturaId));
        return entityToDTO(kaydedilen);
    }

    /** Faturanın aynı şirkete ve çek/senedin carisine ait olduğunu doğrular. */
    private void faturaDogrula(CekSenet cs, Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + faturaId));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        if (cs.getCariHesapId() == null || fatura.getCariHesap() == null
                || !cs.getCariHesapId().equals(fatura.getCariHesap().getId())) {
            throw new BusinessException("Fatura bu cari hesaba ait değil");
        }
    }

    private String tahsilAciklamasi(Long kasaId, Long bankaId, Long faturaId) {
        StringBuilder sb = new StringBuilder("Tahsilat");
        if (kasaId != null) sb.append(" - Kasa #").append(kasaId);
        if (bankaId != null) sb.append(" - Banka #").append(bankaId);
        if (faturaId != null) sb.append(" - Fatura #").append(faturaId);
        return sb.toString();
    }

    private void tahsilatiIsle(CekSenet cs, Long kasaId, Long bankaId, Long faturaId) {
        if (cs.getTutar() == null || cs.getTutar().signum() <= 0) return;
        if (kasaId != null && bankaId != null) {
            throw new BusinessException("Aynı tahsilat hem kasaya hem bankaya işlenemez; tek hesap seçin");
        }
        boolean bagOlustu = false;
        if (cs.getCariHesapId() != null) {
            HareketDTO hareket = hareketService.hareketOlustur(HareketDTO.builder()
                    .cariHesapId(cs.getCariHesapId())
                    .faturaId(faturaId)
                    .tur("TAHSILAT")
                    .tutar(cs.getTutar())
                    .hareketTarihi(LocalDate.now())
                    .odemeSekli("SENET".equals(cs.getTur()) ? "SENET" : "CEK")
                    .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                    .build(), cs.getSirketId());
            if (hareket != null && hareket.getId() != null) {
                cs.setTahsilatHareketId(hareket.getId());
                bagOlustu = true;
            }
        }
        if (kasaId != null) {
            KasaHareket kh = kasaGirisi(kasaId, cs);
            if (kh != null && kh.getId() != null) {
                cs.setKasaHareketId(kh.getId());
                bagOlustu = true;
            }
        } else if (bankaId != null) {
            BankaHareketi bh = bankaGirisi(bankaId, cs);
            if (bh != null && bh.getId() != null) {
                cs.setBankaHareketiId(bh.getId());
                bagOlustu = true;
            }
        }
        if (bagOlustu) {
            // Bağ id'leri kalıcı olmalı; aksi halde tahsil iptali ters kaydı bulamaz.
            cekSenetRepository.save(cs);
        }
    }

    private KasaHareket kasaGirisi(Long kasaId, CekSenet cs) {
        Kasa kasa = kasaRepository.findByIdForUpdate(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setBakiye(kasa.getBakiye() != null ? kasa.getBakiye().add(cs.getTutar()) : cs.getTutar());
        kasaRepository.save(kasa);
        return kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(cs.getTutar())
                .hareketTarihi(LocalDate.now())
                .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                .kaynakTip(KAYNAK_TIP_CEK_SENET)
                .kaynakId(cs.getId())
                .build());
    }

    private BankaHareketi bankaGirisi(Long bankaId, CekSenet cs) {
        Banka banka = bankaRepository.findByIdForUpdate(bankaId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", bankaId));
        tenantChecker.check(banka.getSirketId(), "Banka");
        banka.setBakiye(banka.getBakiye() != null ? banka.getBakiye().add(cs.getTutar()) : cs.getTutar());
        bankaRepository.save(banka);
        return bankaHareketiRepository.save(BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(LocalDate.now())
                .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                .borc(BigDecimal.ZERO)
                .alacak(cs.getTutar())
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(cs.getSirketId())
                .kaynakTip(KAYNAK_TIP_CEK_SENET)
                .kaynakId(cs.getId())
                .build());
    }

    /**
     * Tahsil iptali: cari hareketi soft-iptal eder, kasa/banka hareketlerini
     * ters kaydeder ve çek/senedi portföye döndürür. Gerekçe zorunludur;
     * ikinci iptal engellenir.
     */
    public CekSenetDTO tahsilIptal(Long id, String gerekce) {
        CekSenet cs = bul(id);
        String mevcut = durumNormalize(cs.getDurum());
        if (!DURUM_TAHSIL_EDILDI.equals(mevcut)) {
            throw new BusinessException("Yalnızca tahsil edilmiş çek/senet tahsili iptal edilebilir (mevcut: "
                    + mevcut + ")");
        }
        if (gerekce == null || gerekce.trim().length() < MIN_GEREKCE_UZUNLUK) {
            throw new BusinessException("Tahsil iptali için en az " + MIN_GEREKCE_UZUNLUK
                    + " karakterlik gerekçe girilmelidir");
        }
        kilitKontrol(cs, "tahsil iptali");

        boolean bagYok = cs.getTahsilatHareketId() == null
                && cs.getKasaHareketId() == null
                && cs.getBankaHareketiId() == null;
        if (bagYok && cs.getCariHesapId() != null) {
            // Eski sürümde tahsil edilmiş kayıtlarda hareket bağı yoktur; yalnızca durumu
            // PORTFOY'e çevirmek cari/kasa bakiyesini bozar. Bu kayıtlar için iptal reddedilir.
            throw new BusinessException("Tahsilat hareket bağlantısı bulunamadı; bu kayıt eski sürümde "
                    + "tahsil edilmiş olabilir. Tahsil iptali için cari/kasa hareketlerini kontrol edin.");
        }

        if (cs.getTahsilatHareketId() != null) {
            try {
                hareketService.hareketIptal(cs.getTahsilatHareketId());
            } catch (ResourceNotFoundException e) {
                // Hareket başka bir ekrandan iptal/silinmişse cari etkisi zaten geri alınmıştır.
                log.warn("Tahsilat hareketi bulunamadı (muhtemelen iptal edilmiş), çek/senet iptaline devam: {}",
                        cs.getTahsilatHareketId());
            }
        }
        kasaBankaTersKaydet(cs);

        cs.setDurum(DURUM_PORTFOY);
        cs.setTahsilatHareketId(null);
        cs.setKasaHareketId(null);
        cs.setBankaHareketiId(null);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_PORTFOY, ISLEM_TAHSIL_IPTAL, gerekce.trim());
        return detay(kaydedilen);
    }

    /**
     * Tahsilde oluşturulan kasa/banka hareketlerini siler ve bakiyeleri düzeltir.
     * Kaynak bağı ({@code CEK_SENET} / çek-senet id) yedek olarak kullanılır.
     */
    private void kasaBankaTersKaydet(CekSenet cs) {
        if (cs.getKasaHareketId() != null) {
            kasaHareketRepository.findById(cs.getKasaHareketId()).ifPresent(this::kasaHareketTers);
        }
        if (cs.getBankaHareketiId() != null) {
            bankaHareketiRepository.findById(cs.getBankaHareketiId()).ifPresent(this::bankaHareketTers);
        }
    }

    private void kasaHareketTers(KasaHareket kh) {
        Kasa kasa = kh.getKasa();
        if (kasa != null) {
            BigDecimal tutar = kh.getTutar() != null ? kh.getTutar() : BigDecimal.ZERO;
            kasa.setBakiye((kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO).subtract(tutar));
            kasaRepository.save(kasa);
        }
        kasaHareketRepository.delete(kh);
    }

    private void bankaHareketTers(BankaHareketi bh) {
        BigDecimal tutar = bh.getAlacak() != null ? bh.getAlacak() : BigDecimal.ZERO;
        if (bh.getBankaId() != null) {
            bankaRepository.findById(bh.getBankaId()).ifPresent(banka -> {
                banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).subtract(tutar));
                bankaRepository.save(banka);
            });
        }
        bankaHareketiRepository.delete(bh);
    }

    /** Ciro: portföydeki çek/senedi belge olarak bir cariye devreder (finansal hareket yok). */
    public CekSenetDTO ciroEt(Long id, Long cariId, String not) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "çek/senet ciro");
        String mevcut = durumNormalize(cs.getDurum());
        if (!DURUM_PORTFOY.equals(mevcut)) {
            throw new BusinessException("Yalnızca portföydeki çek/senet ciro edilebilir (mevcut: " + mevcut + ")");
        }
        if (cariId == null) {
            throw new BusinessException("Ciro edilecek cari seçilmelidir");
        }
        CariHesap cari = cariHesapRepository.findById(cariId)
                .orElseThrow(() -> new BusinessException("Ciro edilecek cari bulunamadı: " + cariId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        if (cs.getSirketId() != null && !Objects.equals(cari.getSirketId(), cs.getSirketId())) {
            throw new BusinessException("Ciro edilecek cari aynı şirkete ait olmalıdır");
        }

        cs.setDurum(DURUM_CIRO_EDILDI);
        cs.setCiroCariId(cariId);
        cs.setCiroTarihi(LocalDateTime.now());
        cs.setCiroNotu(kisalt(not));
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_CIRO_EDILDI, ISLEM_CIRO,
                cari.getAd() + (not != null && !not.isBlank() ? " - " + not.trim() : ""));
        return entityToDTO(kaydedilen);
    }

    public CekSenetDTO ciroIade(Long id) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "ciro iadesi");
        String mevcut = durumNormalize(cs.getDurum());
        gecisDogrula(mevcut, DURUM_IADE);
        cs.setDurum(DURUM_IADE);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_IADE, ISLEM_CIRO_IADE, null);
        return entityToDTO(kaydedilen);
    }

    public CekSenetDTO iadeAl(Long id) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "portföye geri alma");
        String mevcut = durumNormalize(cs.getDurum());
        gecisDogrula(mevcut, DURUM_PORTFOY);
        cs.setDurum(DURUM_PORTFOY);
        cs.setCiroCariId(null);
        cs.setCiroTarihi(null);
        cs.setCiroNotu(null);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_PORTFOY, ISLEM_IADE_AL, null);
        return entityToDTO(kaydedilen);
    }

    public CekSenetDTO teminatVer(Long id, String aciklama) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "çek/senet teminat");
        String mevcut = durumNormalize(cs.getDurum());
        gecisDogrula(mevcut, DURUM_TEMINATTA);
        cs.setDurum(DURUM_TEMINATTA);
        cs.setTeminatTarihi(LocalDateTime.now());
        cs.setTeminatAciklama(kisalt(aciklama));
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_TEMINATTA, ISLEM_TEMINAT, kisalt(aciklama));
        return entityToDTO(kaydedilen);
    }

    public CekSenetDTO teminatIade(Long id) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "teminat iadesi");
        String mevcut = durumNormalize(cs.getDurum());
        gecisDogrula(mevcut, DURUM_PORTFOY);
        cs.setDurum(DURUM_PORTFOY);
        cs.setTeminatTarihi(null);
        cs.setTeminatAciklama(null);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_PORTFOY, ISLEM_TEMINAT_IADE, null);
        return entityToDTO(kaydedilen);
    }

    /** Protesto terminal durumdur; bu kayıt için başka geçiş yapılamaz. */
    public CekSenetDTO protesto(Long id, String aciklama) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "çek/senet protesto");
        String mevcut = durumNormalize(cs.getDurum());
        gecisDogrula(mevcut, DURUM_PROTESTO);
        cs.setDurum(DURUM_PROTESTO);
        cs.setProtestoTarihi(LocalDateTime.now());
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        kaydetLog(kaydedilen, mevcut, DURUM_PROTESTO, ISLEM_PROTESTO, kisalt(aciklama));
        return entityToDTO(kaydedilen);
    }

    public void sil(Long id) {
        CekSenet cs = bul(id);
        kilitKontrol(cs, "çek/senet silme");
        String mevcut = durumNormalize(cs.getDurum());
        if (DURUM_TAHSIL_EDILDI.equals(mevcut)) {
            throw new BusinessException("Tahsil edilmiş çek/senet kaydı silinemez");
        }
        cekSenetRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Yardımcılar
    // ------------------------------------------------------------------

    /** Legacy {@code ODENDI} değerini {@code TAHSIL_EDILDI} olarak eşler. */
    public static String durumNormalize(String durum) {
        if (durum == null) return null;
        String d = durum.trim().toUpperCase(Locale.ROOT);
        if (d.isEmpty()) return null;
        return LEGACY_ODENDI.equals(d) ? DURUM_TAHSIL_EDILDI : d;
    }

    private void gecisDogrula(String mevcut, String hedef) {
        if (mevcut == null || hedef == null) {
            throw new BusinessException("Geçersiz durum geçişi");
        }
        if (mevcut.equals(hedef)) return;
        Set<String> izinli = GECISLER.get(mevcut);
        if (izinli == null) return; // legacy serbest durum: kısıtlanmaz
        if (!izinli.contains(hedef)) {
            throw new BusinessException("Geçersiz durum geçişi: " + mevcut + " -> " + hedef);
        }
    }

    private CekSenet bul(Long id) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        return cs;
    }

    private void kilitKontrol(CekSenet cs, String islem) {
        donemService.kilitKontrol(cs.getSirketId(),
                cs.getVadeTarihi() != null ? cs.getVadeTarihi() : LocalDate.now(), islem);
    }

    private void kaydetLog(CekSenet cs, String eskiDurum, String yeniDurum, String islem, String aciklama) {
        if (cs == null || cs.getId() == null) return;
        cekSenetDurumLogRepository.save(CekSenetDurumLog.builder()
                .cekSenetId(cs.getId())
                .sirketId(cs.getSirketId())
                .eskiDurum(eskiDurum)
                .yeniDurum(yeniDurum)
                .islem(islem)
                .kullaniciId(tenantChecker.getCurrentKullaniciId())
                .aciklama(kisalt(aciklama))
                .build());
    }

    private List<CekSenetDurumLogDTO> gecmis(Long cekSenetId) {
        return cekSenetDurumLogRepository.findByCekSenetIdOrderByOlusturmaTarihiAscIdAsc(cekSenetId).stream()
                .map(l -> CekSenetDurumLogDTO.builder()
                        .id(l.getId()).cekSenetId(l.getCekSenetId()).sirketId(l.getSirketId())
                        .eskiDurum(l.getEskiDurum()).yeniDurum(l.getYeniDurum())
                        .islem(l.getIslem()).kullaniciId(l.getKullaniciId())
                        .aciklama(l.getAciklama()).olusturmaTarihi(l.getOlusturmaTarihi())
                        .build())
                .collect(Collectors.toList());
    }

    private Map<Long, String> cariHaritasi(Long sirketId) {
        if (sirketId == null) return Map.of();
        return cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                .collect(Collectors.toMap(CariHesap::getId, c -> c.getAd() != null ? c.getAd() : "", (a, b) -> a));
    }

    private String cariAdi(Long cariId, Map<Long, String> cariHaritasi) {
        if (cariId == null) return null;
        if (cariHaritasi != null && cariHaritasi.containsKey(cariId)) {
            return cariHaritasi.get(cariId);
        }
        return cariHesapRepository.findById(cariId).map(CariHesap::getAd).orElse(null);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs) {
        return entityToDTO(cs, null);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs, Map<Long, String> cariHaritasi) {
        return CekSenetDTO.builder()
                .id(cs.getId()).tur(cs.getTur())
                .cariHesapId(cs.getCariHesapId())
                .faturaId(cs.getFaturaId())
                .cariHesapAdi(cariAdi(cs.getCariHesapId(), cariHaritasi))
                .bankaAdi(cs.getBankaAdi()).sube(cs.getSube())
                .cekNo(cs.getCekNo()).hesapNo(cs.getHesapNo())
                .vadeTarihi(cs.getVadeTarihi()).kesinmeTarihi(cs.getKesinmeTarihi())
                .tutar(cs.getTutar())
                .durum(durumNormalize(cs.getDurum()))
                .aciklama(cs.getAciklama()).sirketId(cs.getSirketId())
                .olusturmaTarihi(cs.getOlusturmaTarihi())
                .tahsilatHareketId(cs.getTahsilatHareketId())
                .kasaHareketId(cs.getKasaHareketId())
                .bankaHareketiId(cs.getBankaHareketiId())
                .ciroCariId(cs.getCiroCariId())
                .ciroCariAd(cariAdi(cs.getCiroCariId(), cariHaritasi))
                .ciroTarihi(cs.getCiroTarihi())
                .ciroNotu(cs.getCiroNotu())
                .teminatTarihi(cs.getTeminatTarihi())
                .teminatAciklama(cs.getTeminatAciklama())
                .protestoTarihi(cs.getProtestoTarihi())
                .build();
    }

    /**
     * Liste DTO'larını toplu zenginleştirir (fatura no, ciro cari adı, kasa/banka
     * adı). Satır başına sorgu yerine ilişki tipi başına tek sorgu yapılır.
     */
    private void zenginlestir(List<CekSenet> kayitlar, List<CekSenetDTO> dtolar) {
        if (kayitlar.isEmpty() || kayitlar.size() != dtolar.size()) return;

        Set<Long> faturaIdler = new HashSet<>();
        Set<Long> ciroCariIdler = new HashSet<>();
        Set<Long> kasaHareketIdler = new HashSet<>();
        Set<Long> bankaHareketIdler = new HashSet<>();
        for (CekSenet cs : kayitlar) {
            if (cs.getFaturaId() != null) faturaIdler.add(cs.getFaturaId());
            if (cs.getCiroCariId() != null) ciroCariIdler.add(cs.getCiroCariId());
            if (cs.getKasaHareketId() != null) kasaHareketIdler.add(cs.getKasaHareketId());
            if (cs.getBankaHareketiId() != null) bankaHareketIdler.add(cs.getBankaHareketiId());
        }

        Map<Long, String> faturaNolari = new HashMap<>();
        if (!faturaIdler.isEmpty()) {
            for (Fatura f : faturaRepository.findAllById(faturaIdler)) {
                faturaNolari.put(f.getId(), f.getFaturaNumarasi());
            }
        }

        Map<Long, String> ciroAdlari = new HashMap<>();
        if (!ciroCariIdler.isEmpty()) {
            for (CariHesap c : cariHesapRepository.findAllById(ciroCariIdler)) {
                ciroAdlari.put(c.getId(), c.getAd());
            }
        }

        Map<Long, String> kasaAdlari = kasaAdlari(kasaHareketIdler);
        Map<Long, String> bankaAdlari = bankaAdlari(bankaHareketIdler);

        for (int i = 0; i < kayitlar.size(); i++) {
            CekSenet cs = kayitlar.get(i);
            CekSenetDTO dto = dtolar.get(i);
            if (cs.getFaturaId() != null) dto.setFaturaNo(faturaNolari.get(cs.getFaturaId()));
            if (cs.getCiroCariId() != null && dto.getCiroCariAd() == null) {
                dto.setCiroCariAd(ciroAdlari.get(cs.getCiroCariId()));
            }
            if (cs.getKasaHareketId() != null) dto.setKasaAd(kasaAdlari.get(cs.getKasaHareketId()));
            if (cs.getBankaHareketiId() != null) dto.setBankaAd(bankaAdlari.get(cs.getBankaHareketiId()));
        }
    }

    private Map<Long, String> kasaAdlari(Set<Long> kasaHareketIdler) {
        Map<Long, String> sonuc = new HashMap<>();
        if (kasaHareketIdler.isEmpty()) return sonuc;
        Map<Long, Long> hareketKasa = new HashMap<>();
        for (KasaHareket kh : kasaHareketRepository.findAllById(kasaHareketIdler)) {
            if (kh.getKasa() != null) hareketKasa.put(kh.getId(), kh.getKasa().getId());
        }
        if (hareketKasa.isEmpty()) return sonuc;
        Map<Long, String> adlar = new HashMap<>();
        for (Kasa k : kasaRepository.findAllById(new HashSet<>(hareketKasa.values()))) {
            adlar.put(k.getId(), k.getAd());
        }
        hareketKasa.forEach((hareketId, kasaId) -> sonuc.put(hareketId, adlar.get(kasaId)));
        return sonuc;
    }

    private Map<Long, String> bankaAdlari(Set<Long> bankaHareketIdler) {
        Map<Long, String> sonuc = new HashMap<>();
        if (bankaHareketIdler.isEmpty()) return sonuc;
        Map<Long, Long> hareketBanka = new HashMap<>();
        for (BankaHareketi bh : bankaHareketiRepository.findAllById(bankaHareketIdler)) {
            if (bh.getBankaId() != null) hareketBanka.put(bh.getId(), bh.getBankaId());
        }
        if (hareketBanka.isEmpty()) return sonuc;
        Map<Long, String> adlar = new HashMap<>();
        for (Banka b : bankaRepository.findAllById(new HashSet<>(hareketBanka.values()))) {
            adlar.put(b.getId(), b.getAd());
        }
        hareketBanka.forEach((hareketId, bankaId) -> sonuc.put(hareketId, adlar.get(bankaId)));
        return sonuc;
    }

    /**
     * Vade aralığı etiketini somut tarih aralığına çevirir ve verilen
     * başlangıç/bitiş ile kesiştirir. Desteklenen etiketler:
     * {@code GECIKEN, BU_HAFTA, BU_AY, 0-30, 31-60, 61-90, 90+}.
     */
    private LocalDate[] vadeAraligiCoz(String vadeAralik, LocalDate bas, LocalDate bit) {
        if (bosMu(vadeAralik)) return new LocalDate[]{bas, bit};
        LocalDate bugun = LocalDate.now();
        LocalDate aralikBas;
        LocalDate aralikBit;
        switch (vadeAralik.trim().toUpperCase(Locale.ROOT)) {
            case "GECIKEN" -> {
                aralikBas = null;
                aralikBit = bugun.minusDays(1);
            }
            case "BU_HAFTA" -> {
                aralikBas = bugun;
                aralikBit = bugun.plusDays(7);
            }
            case "BU_AY" -> {
                aralikBas = bugun.withDayOfMonth(1);
                aralikBit = bugun.withDayOfMonth(bugun.lengthOfMonth());
            }
            case "0-30" -> {
                aralikBas = bugun;
                aralikBit = bugun.plusDays(30);
            }
            case "31-60" -> {
                aralikBas = bugun.plusDays(31);
                aralikBit = bugun.plusDays(60);
            }
            case "61-90" -> {
                aralikBas = bugun.plusDays(61);
                aralikBit = bugun.plusDays(90);
            }
            case "90+" -> {
                aralikBas = bugun.plusDays(91);
                aralikBit = null;
            }
            default -> throw new BusinessException("Geçersiz vade aralığı: " + vadeAralik);
        }
        LocalDate sonucBas = maxTarih(bas, aralikBas);
        LocalDate sonucBit = minTarih(bit, aralikBit);
        return new LocalDate[]{sonucBas, sonucBit};
    }

    private static LocalDate maxTarih(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minTarih(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static boolean bosMu(String deger) {
        return deger == null || deger.trim().isEmpty();
    }

    private static String kisalt(String deger) {
        if (deger == null) return null;
        String temiz = deger.trim();
        if (temiz.isEmpty()) return null;
        return temiz.length() > MAX_ACIKLAMA_UZUNLUK ? temiz.substring(0, MAX_ACIKLAMA_UZUNLUK) : temiz;
    }
}
