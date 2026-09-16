package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.*;
import com.raspel.erp.dto.ticaret.SatinalmaTalepDTO;
import com.raspel.erp.dto.ticaret.SatinalmaTalepKalemDTO;
import com.raspel.erp.entity.envanter.Recete;
import com.raspel.erp.entity.envanter.ReceteKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.entity.envanter.UretimEmriLog;
import com.raspel.erp.entity.ticaret.SiparisKalem;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.ReceteKalemRepository;
import com.raspel.erp.repository.envanter.ReceteRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.UretimEmriLogRepository;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import com.raspel.erp.repository.ticaret.SiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.service.ticaret.SatinalmaTalepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UretimService {

    private final ReceteRepository receteRepository;
    private final ReceteKalemRepository receteKalemRepository;
    private final UretimEmriRepository uretimEmriRepository;
    private final UretimEmriLogRepository uretimEmriLogRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final SiparisRepository siparisRepository;
    private final SiparisKalemRepository siparisKalemRepository;
    private final SatinalmaTalepService satinalmaTalepService;

    private static final BigDecimal YUZ = BigDecimal.valueOf(100);

    // ---------- Reçeteler ----------

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
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .revizyon(dto.getRevizyon() != null ? dto.getRevizyon() : 1)
                .fireOrani(dto.getFireOrani() != null ? dto.getFireOrani() : BigDecimal.ZERO)
                .notlar(dto.getNotlar())
                .build());
        kalemleriKaydet(r.getId(), dto.getKalemler());
        return receteDTO(r);
    }

    @Transactional
    public ReceteDTO receteGuncelle(Long id, ReceteDTO dto, Long sirketId) {
        Recete r = receteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reçete", id));
        sirketKontrol(r.getSirketId(), sirketId);
        if (dto.getAd() != null && !dto.getAd().isBlank()) r.setAd(dto.getAd().trim());
        if (dto.getUrunId() != null) r.setUrunId(dto.getUrunId());
        r.setAciklama(dto.getAciklama());
        if (dto.getAktif() != null) r.setAktif(dto.getAktif());
        if (dto.getFireOrani() != null) r.setFireOrani(dto.getFireOrani());
        r.setNotlar(dto.getNotlar());
        r.setRevizyon((r.getRevizyon() != null ? r.getRevizyon() : 1) + 1);
        receteRepository.save(r);
        if (dto.getKalemler() != null) {
            receteKalemRepository.deleteByReceteId(id);
            kalemleriKaydet(id, dto.getKalemler());
        }
        return receteDTO(r);
    }

    private void kalemleriKaydet(Long receteId, List<ReceteKalemDTO> kalemler) {
        if (kalemler == null) return;
        for (ReceteKalemDTO k : kalemler) {
            if (k.getHammaddeId() == null) continue;
            receteKalemRepository.save(ReceteKalem.builder()
                    .receteId(receteId)
                    .hammaddeId(k.getHammaddeId())
                    .miktar(k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO)
                    .birim(k.getBirim())
                    .fireOrani(k.getFireOrani() != null ? k.getFireOrani() : BigDecimal.ZERO)
                    .build());
        }
    }

    @Transactional
    public void receteSil(Long id, Long sirketId) {
        Recete r = receteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reçete", id));
        sirketKontrol(r.getSirketId(), sirketId);
        receteKalemRepository.deleteByReceteId(id);
        receteRepository.delete(r);
    }

    // ---------- Üretim emirleri ----------

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
                .oncelik(dto.getOncelik() != null ? dto.getOncelik() : "NORMAL")
                .planlananBaslangic(dto.getPlanlananBaslangic())
                .planlananBitis(dto.getPlanlananBitis())
                .sorumluPersonelId(dto.getSorumluPersonelId())
                .depoId(dto.getDepoId())
                .aciklama(dto.getAciklama())
                .notlar(dto.getNotlar())
                .fireMiktar(BigDecimal.ZERO)
                .hammaddeMaliyeti(BigDecimal.ZERO)
                .iscilikMaliyeti(BigDecimal.ZERO)
                .toplamMaliyet(BigDecimal.ZERO)
                .build());
        logYaz(e.getId(), null, UretimEmri.Durum.TASLAK.name(), null, "Emir oluşturuldu");
        return emriDTO(e);
    }

    @Transactional
    public UretimEmriDTO emirBaslat(Long id, Long sirketId, Long kullaniciId) {
        UretimEmri e = emirGetir(id, sirketId);
        if (!UretimEmri.Durum.TASLAK.name().equals(e.getDurum())) {
            throw new BusinessException("Yalnızca taslak emir başlatılabilir (durum: " + e.getDurum() + ")");
        }
        String onceki = e.getDurum();
        e.setDurum(UretimEmri.Durum.URETIMDE.name());
        if (e.getBaslamaTarihi() == null) e.setBaslamaTarihi(LocalDateTime.now());
        uretimEmriRepository.save(e);
        logYaz(e.getId(), onceki, e.getDurum(), kullaniciId, null);
        return emriDTO(e);
    }

    @Transactional
    public UretimEmriDTO emirIptal(Long id, Long sirketId, String aciklama, Long kullaniciId) {
        UretimEmri e = emirGetir(id, sirketId);
        if (UretimEmri.Durum.TAMAMLANDI.name().equals(e.getDurum())
                || UretimEmri.Durum.IPTAL.name().equals(e.getDurum())) {
            throw new BusinessException("Bu emir iptal edilemez (durum: " + e.getDurum() + ")");
        }
        String onceki = e.getDurum();
        e.setDurum(UretimEmri.Durum.IPTAL.name());
        uretimEmriRepository.save(e);
        logYaz(e.getId(), onceki, e.getDurum(), kullaniciId, aciklama);
        return emriDTO(e);
    }

    @Transactional
    public UretimEmriDTO emirTamamla(Long id, Long sirketId, UretimTamamlaIstek istek, Long kullaniciId) {
        UretimEmri e = emirGetir(id, sirketId);
        if (UretimEmri.Durum.TAMAMLANDI.name().equals(e.getDurum())
                || UretimEmri.Durum.IPTAL.name().equals(e.getDurum())) {
            throw new BusinessException("Bu üretim emri tamamlanamaz (durum: " + e.getDurum() + ")");
        }

        BigDecimal plan = e.getMiktar();
        BigDecimal uretilen = istek != null && istek.getUretilenMiktar() != null ? istek.getUretilenMiktar() : plan;
        if (uretilen == null || uretilen.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Üretilen miktar 0'dan büyük olmalıdır");
        }
        BigDecimal fire = istek != null && istek.getFireMiktar() != null ? istek.getFireMiktar() : BigDecimal.ZERO;
        if (fire.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Fire miktarı negatif olamaz");
        }

        Recete recete = receteRepository.findFirstBySirketIdAndUrunId(sirketId, e.getUrunId())
                .orElseThrow(() -> new BusinessException("Bu ürün için tanımlı reçete bulunamadı"));
        List<ReceteKalem> kalemler = receteKalemRepository.findByReceteId(recete.getId());

        BigDecimal receteFire = recete.getFireOrani() != null ? recete.getFireOrani() : BigDecimal.ZERO;
        BigDecimal toplamTuketim = uretilen.add(fire).multiply(BigDecimal.ONE.add(receteFire.divide(YUZ, 6, RoundingMode.HALF_UP)));

        BigDecimal hammaddeMaliyet = BigDecimal.ZERO;
        for (ReceteKalem k : kalemler) {
            BigDecimal kalemFire = k.getFireOrani() != null ? k.getFireOrani() : BigDecimal.ZERO;
            BigDecimal gereken = (k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO)
                    .multiply(toplamTuketim)
                    .multiply(BigDecimal.ONE.add(kalemFire.divide(YUZ, 6, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
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
                    .depoId(e.getDepoId())
                    .kaynakTip("URETIM").kaynakId(e.getId())
                    .build());
            BigDecimal fiyat = hammadde.getFiyat() != null ? hammadde.getFiyat() : BigDecimal.ZERO;
            hammaddeMaliyet = hammaddeMaliyet.add(gereken.multiply(fiyat));
        }

        Stok mamul = stokRepository.findByIdForUpdate(e.getUrunId())
                .orElseThrow(() -> new ResourceNotFoundException("Ürün", e.getUrunId()));
        mamul.setMiktar((mamul.getMiktar() != null ? mamul.getMiktar() : BigDecimal.ZERO).add(uretilen));
        stokRepository.save(mamul);
        stokHareketRepository.save(StokHareket.builder()
                .stok(mamul).tur("GIRIS").miktar(uretilen)
                .hareketTarihi(LocalDate.now()).aciklama("Üretim: " + recete.getAd())
                .depoId(e.getDepoId())
                .kaynakTip("URETIM").kaynakId(e.getId())
                .build());

        BigDecimal iscilik = istek != null && istek.getIscilikMaliyeti() != null ? istek.getIscilikMaliyeti() : BigDecimal.ZERO;
        BigDecimal toplamMaliyet = hammaddeMaliyet.add(iscilik).setScale(2, RoundingMode.HALF_UP);

        String oncekiDurum = e.getDurum();
        e.setUretilenMiktar(uretilen);
        e.setFireMiktar(fire);
        e.setHammaddeMaliyeti(hammaddeMaliyet.setScale(2, RoundingMode.HALF_UP));
        e.setIscilikMaliyeti(iscilik.setScale(2, RoundingMode.HALF_UP));
        e.setToplamMaliyet(toplamMaliyet);
        if (e.getBaslamaTarihi() == null) e.setBaslamaTarihi(LocalDateTime.now());
        e.setDurum(UretimEmri.Durum.TAMAMLANDI.name());
        e.setTamamlanmaTarihi(LocalDateTime.now());
        if (istek != null && istek.getAciklama() != null && !istek.getAciklama().isBlank()) {
            e.setAciklama(istek.getAciklama());
        }
        uretimEmriRepository.save(e);
        logYaz(e.getId(), oncekiDurum, e.getDurum(), kullaniciId,
                "Üretilen: " + uretilen + ", Fire: " + fire + ", Maliyet: " + toplamMaliyet);
        log.info("Üretim emri tamamlandı - ID: {}, Ürün: {}, Üretilen: {}, Fire: {}",
                id, mamul.getAd(), uretilen, fire);
        return emriDTO(e);
    }

    @Transactional(readOnly = true)
    public List<UretimEmriLogDTO> emirGecmisi(Long id, Long sirketId) {
        emirGetir(id, sirketId);
        return uretimEmriLogRepository.findByUretimEmriIdOrderByOlusturmaTarihiDesc(id).stream()
                .map(l -> UretimEmriLogDTO.builder()
                        .id(l.getId()).uretimEmriId(l.getUretimEmriId())
                        .oncekiDurum(l.getOncekiDurum()).yeniDurum(l.getYeniDurum())
                        .kullaniciId(l.getKullaniciId()).aciklama(l.getAciklama())
                        .olusturmaTarihi(l.getOlusturmaTarihi())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Belirli bir ürün için istenen miktarı üretmek üzere gerekli hammadde ihtiyacını
     * (fire dahil), mevcut stoğu ve eksik miktarı hesaplar.
     */
    @Transactional(readOnly = true)
    public UretimIhtiyacDTO ihtiyacAnalizi(Long urunId, BigDecimal miktar, Long sirketId) {
        if (urunId == null || miktar == null || miktar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Ürün ve miktar zorunludur");
        }
        Recete recete = receteRepository.findFirstBySirketIdAndUrunId(sirketId, urunId)
                .orElseThrow(() -> new BusinessException("Bu ürün için tanımlı reçete bulunamadı"));
        List<ReceteKalem> kalemler = receteKalemRepository.findByReceteId(recete.getId());
        BigDecimal receteFire = recete.getFireOrani() != null ? recete.getFireOrani() : BigDecimal.ZERO;
        BigDecimal toplamMiktar = miktar.multiply(BigDecimal.ONE.add(receteFire.divide(YUZ, 6, RoundingMode.HALF_UP)));

        boolean yeterli = true;
        BigDecimal toplamMaliyet = BigDecimal.ZERO;
        List<UretimIhtiyacKalemDTO> sonuc = new ArrayList<>();
        for (ReceteKalem k : kalemler) {
            Stok hammadde = stokRepository.findById(k.getHammaddeId()).orElse(null);
            if (hammadde == null) continue;
            BigDecimal kalemFire = k.getFireOrani() != null ? k.getFireOrani() : BigDecimal.ZERO;
            BigDecimal gerekli = (k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO)
                    .multiply(toplamMiktar)
                    .multiply(BigDecimal.ONE.add(kalemFire.divide(YUZ, 6, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal mevcut = hammadde.getMiktar() != null ? hammadde.getMiktar() : BigDecimal.ZERO;
            BigDecimal eksik = mevcut.compareTo(gerekli) < 0 ? gerekli.subtract(mevcut) : BigDecimal.ZERO;
            if (eksik.compareTo(BigDecimal.ZERO) > 0) yeterli = false;
            BigDecimal fiyat = hammadde.getFiyat() != null ? hammadde.getFiyat() : BigDecimal.ZERO;
            BigDecimal tutar = gerekli.multiply(fiyat);
            toplamMaliyet = toplamMaliyet.add(tutar);
            sonuc.add(UretimIhtiyacKalemDTO.builder()
                    .hammaddeId(hammadde.getId())
                    .hammaddeAd(hammadde.getAd())
                    .birim(k.getBirim() != null ? k.getBirim() : hammadde.getBirim())
                    .gerekli(gerekli).mevcut(mevcut).eksik(eksik)
                    .birimFiyat(fiyat).tutar(tutar.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return UretimIhtiyacDTO.builder()
                .urunId(urunId).urunAd(stokAd(urunId))
                .miktar(miktar).fireOrani(receteFire)
                .toplamMaliyet(toplamMaliyet.setScale(2, RoundingMode.HALF_UP))
                .yeterli(yeterli).kalemler(sonuc)
                .build();
    }

    /** Bir üretim emrinin eksik hammaddeleri için satınalma talebi oluşturur. */
    @Transactional
    public SatinalmaTalepDTO ihtiyactanSatinalmaTalebi(Long id, Long sirketId, String talepEden) {
        UretimEmri e = emirGetir(id, sirketId);
        BigDecimal hedef = e.getMiktar();
        UretimIhtiyacDTO ihtiyac = ihtiyacAnalizi(e.getUrunId(), hedef, sirketId);
        List<SatinalmaTalepKalemDTO> eksikler = ihtiyac.getKalemler().stream()
                .filter(k -> k.getEksik() != null && k.getEksik().compareTo(BigDecimal.ZERO) > 0)
                .map(k -> SatinalmaTalepKalemDTO.builder()
                        .stokId(k.getHammaddeId())
                        .stokAdi(k.getHammaddeAd())
                        .miktar(k.getEksik())
                        .birim(k.getBirim())
                        .tahminiBirimFiyat(k.getBirimFiyat())
                        .build())
                .collect(Collectors.toList());
        if (eksikler.isEmpty()) {
            throw new BusinessException("Bu emir için eksik hammadde yok");
        }
        SatinalmaTalepDTO dto = SatinalmaTalepDTO.builder()
                .talepNo("UME-" + e.getId() + "-" + System.currentTimeMillis() % 100000)
                .tarih(LocalDate.now())
                .talepEden(talepEden != null && !talepEden.isBlank() ? talepEden : "Üretim")
                .departman("Üretim")
                .sirketId(sirketId)
                .aciklama("Üretim emri #" + e.getId() + " (" + ihtiyac.getUrunAd() + ") eksik hammaddeleri")
                .kalemler(eksikler)
                .build();
        return satinalmaTalepService.olustur(dto);
    }

    /** Sipariş kalemlerinden, stok bağlı olanlar için üretim emri(leri) oluşturur. */
    @Transactional
    public List<UretimEmriDTO> siparistenEmirOlustur(Long siparisId, Long sirketId) {
        var siparis = siparisRepository.findById(siparisId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", siparisId));
        if (sirketId != null && siparis.getSirketId() != null && !sirketId.equals(siparis.getSirketId())) {
            throw new BusinessException("Bu siparişe erişim yetkiniz yok");
        }
        List<SiparisKalem> kalemler = siparisKalemRepository.findBySiparisId(siparisId);
        List<UretimEmriDTO> olusanlar = new ArrayList<>();
        for (SiparisKalem k : kalemler) {
            if (k.getStokId() == null || k.getMiktar() == null || k.getMiktar().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            UretimEmriDTO dto = UretimEmriDTO.builder()
                    .siparisId(siparisId)
                    .urunId(k.getStokId())
                    .miktar(k.getMiktar())
                    .oncelik("YUKSEK")
                    .aciklama("Sipariş #" + siparis.getSiparisNo() + " için otomatik üretim emri")
                    .build();
            olusanlar.add(uretimEmriOlustur(dto, sirketId));
        }
        if (olusanlar.isEmpty()) {
            throw new BusinessException("Siparişte üretime uygun (stok bağlı) kalem yok");
        }
        return olusanlar;
    }

    @Transactional(readOnly = true)
    public UretimOzetDTO ozet(Long sirketId) {
        List<UretimEmri> emirler = uretimEmriRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId);
        LocalDate bugun = LocalDate.now();
        YearMonth buAy = YearMonth.from(bugun);
        long taslak = 0, uretimde = 0, tamamlandi = 0, iptal = 0, geciken = 0, buAyTamamlanan = 0;
        BigDecimal toplamUretilen = BigDecimal.ZERO;
        BigDecimal toplamFire = BigDecimal.ZERO;
        long sureSayisi = 0;
        BigDecimal toplamSureSaat = BigDecimal.ZERO;
        for (UretimEmri e : emirler) {
            String d = e.getDurum();
            if (UretimEmri.Durum.TASLAK.name().equals(d)) taslak++;
            else if (UretimEmri.Durum.URETIMDE.name().equals(d)) uretimde++;
            else if (UretimEmri.Durum.TAMAMLANDI.name().equals(d)) tamamlandi++;
            else if (UretimEmri.Durum.IPTAL.name().equals(d)) iptal++;
            if (e.getPlanlananBitis() != null && e.getPlanlananBitis().isBefore(bugun)
                    && !UretimEmri.Durum.TAMAMLANDI.name().equals(d)
                    && !UretimEmri.Durum.IPTAL.name().equals(d)) {
                geciken++;
            }
            if (e.getTamamlanmaTarihi() != null && YearMonth.from(e.getTamamlanmaTarihi()).equals(buAy)) {
                buAyTamamlanan++;
            }
            if (e.getUretilenMiktar() != null) toplamUretilen = toplamUretilen.add(e.getUretilenMiktar());
            if (e.getFireMiktar() != null) toplamFire = toplamFire.add(e.getFireMiktar());
            if (e.getBaslamaTarihi() != null && e.getTamamlanmaTarihi() != null) {
                toplamSureSaat = toplamSureSaat.add(BigDecimal.valueOf(
                        Duration.between(e.getBaslamaTarihi(), e.getTamamlanmaTarihi()).toMinutes() / 60.0));
                sureSayisi++;
            }
        }
        BigDecimal toplam = toplamUretilen.add(toplamFire);
        BigDecimal fireOrani = toplam.compareTo(BigDecimal.ZERO) > 0
                ? toplamFire.multiply(YUZ).divide(toplam, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal ortSure = sureSayisi > 0
                ? toplamSureSaat.divide(BigDecimal.valueOf(sureSayisi), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return UretimOzetDTO.builder()
                .taslak(taslak).uretimde(uretimde).tamamlandi(tamamlandi).iptal(iptal)
                .geciken(geciken).buAyTamamlanan(buAyTamamlanan)
                .fireOrani(fireOrani).ortalamaSureSaat(ortSure)
                .build();
    }

    // ---------- Yardımcılar ----------

    private UretimEmri emirGetir(Long id, Long sirketId) {
        UretimEmri e = uretimEmriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Üretim Emri", id));
        sirketKontrol(e.getSirketId(), sirketId);
        return e;
    }

    private void sirketKontrol(Long kayitSirketId, Long sirketId) {
        if (sirketId != null && kayitSirketId != null && !sirketId.equals(kayitSirketId)) {
            throw new BusinessException("Bu kayda erişim yetkiniz yok");
        }
    }

    private void logYaz(Long emriId, String onceki, String yeni, Long kullaniciId, String aciklama) {
        uretimEmriLogRepository.save(UretimEmriLog.builder()
                .uretimEmriId(emriId)
                .oncekiDurum(onceki)
                .yeniDurum(yeni)
                .kullaniciId(kullaniciId)
                .aciklama(aciklama)
                .build());
    }

    private ReceteDTO receteDTO(Recete r) {
        List<ReceteKalemDTO> kalemler = receteKalemRepository.findByReceteId(r.getId()).stream()
                .map(k -> ReceteKalemDTO.builder()
                        .id(k.getId()).receteId(k.getReceteId())
                        .hammaddeId(k.getHammaddeId())
                        .hammaddeAd(stokAd(k.getHammaddeId()))
                        .miktar(k.getMiktar())
                        .birim(k.getBirim())
                        .fireOrani(k.getFireOrani())
                        .build())
                .collect(Collectors.toList());
        return ReceteDTO.builder()
                .id(r.getId()).sirketId(r.getSirketId()).ad(r.getAd())
                .urunId(r.getUrunId()).urunAd(stokAd(r.getUrunId()))
                .aciklama(r.getAciklama())
                .aktif(r.getAktif()).revizyon(r.getRevizyon())
                .fireOrani(r.getFireOrani()).notlar(r.getNotlar())
                .kalemler(kalemler)
                .build();
    }

    private UretimEmriDTO emriDTO(UretimEmri e) {
        boolean gecikti = e.getPlanlananBitis() != null
                && e.getPlanlananBitis().isBefore(LocalDate.now())
                && !UretimEmri.Durum.TAMAMLANDI.name().equals(e.getDurum())
                && !UretimEmri.Durum.IPTAL.name().equals(e.getDurum());
        return UretimEmriDTO.builder()
                .id(e.getId()).sirketId(e.getSirketId()).siparisId(e.getSiparisId())
                .urunId(e.getUrunId()).urunAd(stokAd(e.getUrunId())).miktar(e.getMiktar()).durum(e.getDurum())
                .aciklama(e.getAciklama()).olusturmaTarihi(e.getOlusturmaTarihi())
                .tamamlanmaTarihi(e.getTamamlanmaTarihi())
                .planlananBaslangic(e.getPlanlananBaslangic()).planlananBitis(e.getPlanlananBitis())
                .baslamaTarihi(e.getBaslamaTarihi()).oncelik(e.getOncelik())
                .uretilenMiktar(e.getUretilenMiktar()).fireMiktar(e.getFireMiktar())
                .sorumluPersonelId(e.getSorumluPersonelId()).depoId(e.getDepoId())
                .hammaddeMaliyeti(e.getHammaddeMaliyeti()).iscilikMaliyeti(e.getIscilikMaliyeti())
                .toplamMaliyet(e.getToplamMaliyet()).notlar(e.getNotlar())
                .gecikti(gecikti)
                .build();
    }

    private String stokAd(Long stokId) {
        if (stokId == null) return null;
        return stokRepository.findById(stokId).map(Stok::getAd).orElse(null);
    }
}
