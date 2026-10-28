package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

/**
 * Masraf yönetimi. Tutar KDV DAHİL kabul edilir; matrah/KDV ayrıştırılır.
 * Ödeme kasa/banka seçilirse masraf anında ilgili hesaptan çıkış işlenir ve
 * muhasebe fişi (770 + 191 / kasa-banka veya satıcılar) otomatik üretilir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MasrafService {

    private final MasrafRepository masrafRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;
    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final BankaRepository bankaRepository;
    private final BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    private final com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    private final com.raspel.erp.service.sistem.KategoriService kategoriService;

    @Transactional(readOnly = true)
    public Page<MasrafDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return masrafRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    /** Masraf KPI özeti: bu ay/bu yıl toplam, bu yıl KDV ve toplam kayıt sayısı. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.finans.MasrafOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.finans.MasrafOzetDTO.builder()
                    .buAyToplam(BigDecimal.ZERO).buYilToplam(BigDecimal.ZERO).buYilKdv(BigDecimal.ZERO)
                    .toplamKayit(0).build();
        }
        LocalDate bugun = LocalDate.now();
        LocalDate ayBaslangic = bugun.withDayOfMonth(1);
        LocalDate ayBitis = bugun.withDayOfMonth(bugun.lengthOfMonth());
        LocalDate yilBaslangic = bugun.withDayOfYear(1);
        LocalDate yilBitis = bugun.withDayOfYear(bugun.lengthOfYear());
        return com.raspel.erp.dto.finans.MasrafOzetDTO.builder()
                .buAyToplam(masrafRepository.sumTutarBySirketIdAndTarihBetween(sirketId, ayBaslangic, ayBitis))
                .buYilToplam(masrafRepository.sumTutarBySirketIdAndTarihBetween(sirketId, yilBaslangic, yilBitis))
                .buYilKdv(masrafRepository.sumKdvBySirketIdAndTarihBetween(sirketId, yilBaslangic, yilBitis))
                .toplamKayit(masrafRepository.countBySirketId(sirketId))
                .build();
    }

    @Transactional(readOnly = true)
    public MasrafDTO getir(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
        return entityToDTO(m);
    }

    /**
     * Masraf listesi: kategori, tarih aralığı ve serbest metin araması + sayfalama.
     */
    @Transactional(readOnly = true)
    public Page<MasrafDTO> filtreli(Long sirketId, String kategori, LocalDate baslangic,
                                    LocalDate bitis, String q, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        String kategoriFiltre = (kategori == null || kategori.isBlank() || "TUMU".equalsIgnoreCase(kategori))
                ? null : kategori.trim().toUpperCase(Locale.ROOT);
        String arama = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        return masrafRepository.filtrele(sirketId, kategoriFiltre, baslangic, bitis, arama, pageable)
                .map(this::entityToDTO);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public MasrafDTO olustur(MasrafDTO dto, Long sirketId) {
        if (dto.getTutar() == null || dto.getTutar().signum() <= 0) {
            throw new BusinessException("Masraf tutarı sıfırdan büyük olmalıdır");
        }
        if (dto.getKasaId() != null && dto.getBankaId() != null) {
            throw new BusinessException("Ödeme hem kasadan hem bankadan yapılamaz; tek hesap seçin");
        }
        masrafDogrula(dto.getKategori(), dto.getOdemeYontemi(), dto.getCariHesapId());
        LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : LocalDate.now();
        donemService.kilitKontrol(sirketId, tarih, "masraf oluşturma");

        BigDecimal kdvOrani = dto.getKdvOrani() != null ? dto.getKdvOrani() : BigDecimal.ZERO;
        var satir = com.raspel.erp.util.FaturaTutar.satir(dto.getTutar(), BigDecimal.ONE, BigDecimal.ZERO, kdvOrani);

        // Kategori registry'de get-or-create edilir (MASRAF, adlar büyük harf).
        com.raspel.erp.entity.sistem.Kategori kategori = kategoriService.resolve(
                sirketId, com.raspel.erp.entity.sistem.KategoriKapsam.MASRAF, dto.getKategori());

        Masraf masraf = Masraf.builder()
                .tarih(tarih)
                .tutar(dto.getTutar())
                .matrah(satir.net())
                .kdvOrani(kdvOrani)
                .kdvTutar(satir.kdv())
                .aciklama(dto.getAciklama())
                .kategori(kategori != null ? kategori.getAd() : dto.getKategori().trim().toUpperCase(Locale.ROOT))
                .kategoriId(kategori != null ? kategori.getId() : null)
                .cariHesapId(dto.getCariHesapId())
                .belgeNo(dto.getBelgeNo())
                .odemeYontemi(normalizeOdemeYontemi(dto.getOdemeYontemi()))
                .kasaId(dto.getKasaId())
                .bankaId(dto.getBankaId())
                .sirketId(sirketId)
                .build();
        Masraf kaydedilen = masrafRepository.save(masraf);

        odemeIsle(kaydedilen);
        otomatikMuhasebeService.masrafIsle(kaydedilen);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kaydedilen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public MasrafDTO guncelle(Long id, MasrafDTO dto) {
        Masraf masraf = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(masraf.getSirketId(), "Masraf");
        // Faz 0.9: tutar/ödeme değişikliklerinde kasa/banka bakiyesi tutarlı
        // kalsın diye önce ESKİ ödeme tersine çevrilir, sonra yeni ödeme işlenir.
        if (dto.getTutar() != null && dto.getTutar().signum() <= 0) {
            throw new BusinessException("Masraf tutarı sıfırdan büyük olmalıdır");
        }
        if (dto.getKasaId() != null && dto.getBankaId() != null) {
            throw new BusinessException("Ödeme hem kasadan hem bankadan yapılamaz; tek hesap seçin");
        }
        String yeniKategori = dto.getKategori() != null ? dto.getKategori() : masraf.getKategori();
        String yeniOdeme = dto.getOdemeYontemi() != null ? dto.getOdemeYontemi() : masraf.getOdemeYontemi();
        Long yeniCari = dto.getCariHesapId() != null ? dto.getCariHesapId() : masraf.getCariHesapId();
        masrafDogrula(yeniKategori, yeniOdeme, yeniCari);

        // Eski ödeme (varsa) geri alınır; kasa/banka eski tutar kadar iade edilir.
        odemeTersineCevir(masraf);

        if (dto.getTarih() != null) masraf.setTarih(dto.getTarih());
        if (dto.getTutar() != null) masraf.setTutar(dto.getTutar());
        if (dto.getAciklama() != null) masraf.setAciklama(dto.getAciklama());
        com.raspel.erp.entity.sistem.Kategori kategori = kategoriService.resolve(
                masraf.getSirketId(), com.raspel.erp.entity.sistem.KategoriKapsam.MASRAF, yeniKategori);
        masraf.setKategori(kategori != null ? kategori.getAd() : yeniKategori.trim().toUpperCase(Locale.ROOT));
        masraf.setKategoriId(kategori != null ? kategori.getId() : null);
        if (dto.getCariHesapId() != null) masraf.setCariHesapId(dto.getCariHesapId());
        if (dto.getBelgeNo() != null) masraf.setBelgeNo(dto.getBelgeNo());
        masraf.setOdemeYontemi(normalizeOdemeYontemi(yeniOdeme));
        // PUT (tam güncelleme): ödeme hesabı istemciden gelen değerle belirlenir
        // (null = ödeme hesabı yok / kaldırılır). Böylece kasa↔banka geçişi doğru işler.
        masraf.setKasaId(dto.getKasaId());
        masraf.setBankaId(dto.getBankaId());
        // KDV yeniden ayrıştırılır (tutar KDV dahil).
        BigDecimal kdvOrani = dto.getKdvOrani() != null ? dto.getKdvOrani() : masraf.getKdvOrani();
        if (kdvOrani == null) kdvOrani = BigDecimal.ZERO;
        var satir = com.raspel.erp.util.FaturaTutar.satir(masraf.getTutar(), BigDecimal.ONE, BigDecimal.ZERO, kdvOrani);
        masraf.setKdvOrani(kdvOrani);
        masraf.setMatrah(satir.net());
        masraf.setKdvTutar(satir.kdv());
        Masraf kaydedilen = masrafRepository.save(masraf);
        // Yeni ödeme işlenir (güncel tutar/hesap ile).
        odemeIsle(kaydedilen);
        // Tutar/KDV değişmiş olabilir: eski fiş iptal edilip yenisi üretilir.
        otomatikMuhasebeService.kaynakFisIptal(kaydedilen.getSirketId(),
                com.raspel.erp.service.muhasebe.OtomatikMuhasebeService.KAYNAK_MASRAF, kaydedilen.getId());
        otomatikMuhasebeService.masrafIsle(kaydedilen);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kaydedilen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
        auditLogService.finansalSilmeLog("Masraf", id,
                "Masraf silindi: " + m.getTutar() + " TL - " + (m.getAciklama() != null ? m.getAciklama() : "")
                        + (m.getTarih() != null ? " (" + m.getTarih() + ")" : ""));
        odemeTersineCevir(m);
        otomatikMuhasebeService.kaynakFisIptal(m.getSirketId(),
                com.raspel.erp.service.muhasebe.OtomatikMuhasebeService.KAYNAK_MASRAF, m.getId());
        masrafRepository.deleteById(id);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    /** Masraf ödemesi: seçilen kasa/banka hesabından çıkış işlenir. */
    private void odemeIsle(Masraf m) {
        BigDecimal tutar = m.getTutar();
        if (tutar == null || tutar.signum() <= 0) return;
        if (m.getKasaId() != null) {
            Kasa kasa = kasaRepository.findByIdForUpdate(m.getKasaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kasa", m.getKasaId()));
            tenantChecker.check(kasa.getSirketId(), "Kasa");
            BigDecimal bakiye = kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO;
            if (bakiye.compareTo(tutar) < 0) {
                throw new BusinessException("Kasada yetersiz bakiye. Mevcut: " + bakiye + " ₺");
            }
            kasa.setBakiye(bakiye.subtract(tutar));
            kasaRepository.save(kasa);
            kasaHareketRepository.save(KasaHareket.builder()
                    .kasa(kasa).tur("GIDER").tutar(tutar)
                    .hareketTarihi(m.getTarih() != null ? m.getTarih() : LocalDate.now())
                    .aciklama("Masraf" + (m.getBelgeNo() != null ? " #" + m.getBelgeNo() : "")
                            + (m.getAciklama() != null ? " - " + m.getAciklama() : ""))
                    .build());
        } else if (m.getBankaId() != null) {
            Banka banka = bankaRepository.findByIdForUpdate(m.getBankaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Banka", m.getBankaId()));
            tenantChecker.check(banka.getSirketId(), "Banka");
            BigDecimal bakiye = banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO;
            if (bakiye.compareTo(tutar) < 0) {
                throw new BusinessException("Bankada yetersiz bakiye. Mevcut: " + bakiye + " ₺");
            }
            banka.setBakiye(bakiye.subtract(tutar));
            bankaRepository.save(banka);
            bankaHareketiRepository.save(BankaHareketi.builder()
                    .bankaId(banka.getId())
                    .tarih(m.getTarih() != null ? m.getTarih() : LocalDate.now())
                    .aciklama("Masraf" + (m.getBelgeNo() != null ? " #" + m.getBelgeNo() : "")
                            + (m.getAciklama() != null ? " - " + m.getAciklama() : ""))
                    .borc(tutar)
                    .alacak(BigDecimal.ZERO)
                    .bakiye(banka.getBakiye())
                    .eslestirildi(false)
                    .sirketId(m.getSirketId())
                    .kaynakTip("MASRAF")
                    .build());
        }
    }

    /** Masraf silinince yapılan ödeme kasa/banka hesabına geri eklenir. */
    private void odemeTersineCevir(Masraf m) {
        BigDecimal tutar = m.getTutar();
        if (tutar == null || tutar.signum() <= 0) return;
        if (m.getKasaId() != null) {
            Kasa kasa = kasaRepository.findByIdForUpdate(m.getKasaId()).orElse(null);
            if (kasa == null) return;
            kasa.setBakiye((kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO).add(tutar));
            kasaRepository.save(kasa);
            kasaHareketRepository.save(KasaHareket.builder()
                    .kasa(kasa).tur("GELIR").tutar(tutar)
                    .hareketTarihi(LocalDate.now())
                    .aciklama("Masraf silindi (geri alındı) #" + m.getId())
                    .build());
        } else if (m.getBankaId() != null) {
            Banka banka = bankaRepository.findByIdForUpdate(m.getBankaId()).orElse(null);
            if (banka == null) return;
            banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).add(tutar));
            bankaRepository.save(banka);
            bankaHareketiRepository.save(BankaHareketi.builder()
                    .bankaId(banka.getId())
                    .tarih(LocalDate.now())
                    .aciklama("Masraf silindi (geri alındı) #" + m.getId())
                    .borc(BigDecimal.ZERO)
                    .alacak(tutar)
                    .bakiye(banka.getBakiye())
                    .eslestirildi(false)
                    .sirketId(m.getSirketId())
                    .kaynakTip("MASRAF_IPTAL")
                    .build());
        }
    }

    private static final Set<String> ODEME_YONTEMLERI = Set.of("NAKIT", "HAVALE", "KART");

    private void masrafDogrula(String kategori, String odemeYontemi, Long cariHesapId) {
        if (kategori == null || kategori.isBlank()) {
            throw new BusinessException("Masraf kategorisi zorunludur");
        }
        if (odemeYontemi != null && !odemeYontemi.isBlank()
                && !ODEME_YONTEMLERI.contains(odemeYontemi.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Geçersiz ödeme yöntemi");
        }
        if (cariHesapId != null) {
            var cari = cariHesapRepository.findById(cariHesapId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", cariHesapId));
            tenantChecker.check(cari.getSirketId(), "Cari hesap");
        }
    }

    private String normalizeOdemeYontemi(String odemeYontemi) {
        return (odemeYontemi == null || odemeYontemi.isBlank())
                ? null : odemeYontemi.trim().toUpperCase(Locale.ROOT);
    }

    private MasrafDTO entityToDTO(Masraf m) {
        return MasrafDTO.builder()
                .id(m.getId()).tarih(m.getTarih()).tutar(m.getTutar())
                .aciklama(m.getAciklama()).kategori(m.getKategori())
                .kategoriId(m.getKategoriId())
                .cariHesapId(m.getCariHesapId()).belgeNo(m.getBelgeNo())
                .kdvOrani(m.getKdvOrani()).kdvTutar(m.getKdvTutar()).matrah(m.getMatrah())
                .odemeYontemi(m.getOdemeYontemi()).kasaId(m.getKasaId()).bankaId(m.getBankaId())
                .sirketId(m.getSirketId()).olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
