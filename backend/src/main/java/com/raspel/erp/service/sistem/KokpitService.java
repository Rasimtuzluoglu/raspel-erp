package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardGrafikDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardKarAnaliziDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.sistem.KokpitDTO;
import com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO;
import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.CariHesapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Anasayfa finans kokpiti servisi.
 *
 * <p>Veriyi mevcut {@link DashboardService} ve {@link RaporService}
 * hesaplarından derler; iş mantığını kopyalamaz. Tek yanıtta özet KPI'lar,
 * kritik durum şeridi ve dört finans bölümü döner. Ağır bölümler kendi
 * servislerinde cache'lendiği için burada ek bir hesaplama yapılmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KokpitService {

    private static final String VARSAYILAN_DONEM = "BU_AY";
    private static final Set<String> GECERLI_DONEMLER = Set.of("BU_AY", "GECEN_AY", "BU_CEYREK", "BU_YIL");
    private static final Set<String> KAR_ROLLERI = Set.of("ADMIN", "MUHASEBE");
    private static final Set<String> ONAY_TURLERI =
            Set.of("ONAY_IZIN", "ONAY_MASRAF", "ONAY_SATINALMA", "ONAY_SIPARIS");
    private static final List<String> ODEME_KAPALI_DURUMLAR = List.of("ODENDI", "IPTAL");
    private static final int CARI_LIMIT = 8;
    private static final int KAR_LIMIT = 5;
    private static final int VADESI_GECEN_LIMIT = 8;
    private static final int PROJEKSIYON_GUN = 30;

    private final DashboardService dashboardService;
    private final RaporService raporService;
    private final CariHesapService cariHesapService;
    private final CariHesapRepository cariHesapRepository;
    private final FaturaRepository faturaRepository;

    /**
     * Kokpit yanıtını üretir.
     *
     * @param sirketId tenant; zorunlu
     * @param rol      kullanıcının aktif rolü (kâr/marj görünürlüğü için)
     * @param donem    BU_AY | GECEN_AY | BU_CEYREK | BU_YIL; boş/geçersizde BU_AY
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "kokpit", key = "#sirketId + ':' + #rol + ':' + #donem")
    public KokpitDTO kokpitGetir(Long sirketId, String rol, String donem) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }
        String cozulenDonem = donemCoz(donem);
        String rolDegeri = rol != null ? rol.trim().toUpperCase(Locale.ROOT) : "USER";

        DashboardOzetDTO ozet = dashboardService.ozetGetir(sirketId, null, null, rolDegeri, cozulenDonem);
        DashboardGrafikDTO grafikler = dashboardService.grafiklerGetir(sirketId);
        List<DashboardGorevDTO> gorevler = guvenliListe(
                () -> dashboardService.gorevlerGetir(sirketId, rolDegeri));

        DashboardGorevDTO vadesiGecenGorev = gorevBul(gorevler, "VADESI_GECEN");
        BigDecimal vadesiGecenTutar = vadesiGecenGorev != null ? sifirla(vadesiGecenGorev.getTutar()) : BigDecimal.ZERO;
        long vadesiGecenAdet = vadesiGecenGorev != null && vadesiGecenGorev.getAdet() != null
                ? vadesiGecenGorev.getAdet() : 0L;

        BigDecimal alacak = sifirla(cariHesapService.toplamNegatifBakiyeGetir(sirketId)).abs();
        BigDecimal verecek = sifirla(cariHesapService.toplamPozitifBakiyeGetir(sirketId));
        BigDecimal tahsilatOrani = tahsilatOrani(sirketId);
        List<DashboardIcgoruDTO> icgoruler = guvenliListe(
                () -> dashboardService.icgorulerGetir(sirketId));

        return KokpitDTO.builder()
                .donem(donemDTO(ozet, cozulenDonem))
                .ozet(kpiListesi(ozet, grafikler, alacak, verecek, vadesiGecenTutar, vadesiGecenAdet))
                .kritikDurum(kritikDurumListesi(gorevler, ozet, vadesiGecenTutar, vadesiGecenAdet))
                .nakit(nakitBolumu(sirketId, ozet, grafikler))
                .alacakVerecek(alacakVerecekBolumu(sirketId, alacak, verecek, tahsilatOrani))
                .karMarj(KAR_ROLLERI.contains(rolDegeri)
                        ? karMarjBolumu(sirketId, rolDegeri, cozulenDonem, grafikler) : null)
                .faturaSiparis(faturaSiparisBolumu(sirketId, grafikler, vadesiGecenTutar,
                        vadesiGecenAdet, tahsilatOrani))
                .icgoruler(icgoruler)
                .build();
    }

    // ------------------------------------------------------------------
    // Özet KPI'lar
    // ------------------------------------------------------------------

    private List<KokpitDTO.KpiDTO> kpiListesi(DashboardOzetDTO ozet, DashboardGrafikDTO grafikler,
                                              BigDecimal alacak, BigDecimal verecek,
                                              BigDecimal vadesiGecenTutar, long vadesiGecenAdet) {
        DashboardOzetDTO.KpiDTO kpi = ozet != null ? ozet.getKpi() : null;
        DashboardOzetDTO.LikiditeDTO likidite = kpi != null ? kpi.getLikidite() : null;
        DashboardOzetDTO.CiroDTO ciro = kpi != null ? kpi.getCiro() : null;
        DashboardOzetDTO.KarDTO kar = kpi != null ? kpi.getKar() : null;

        BigDecimal nakit = likidite != null ? sifirla(likidite.getDeger()) : BigDecimal.ZERO;
        BigDecimal alacakDegeri = sifirla(alacak);
        BigDecimal verecekDegeri = sifirla(verecek);
        BigDecimal netCalismaSermayesi = nakit.add(alacakDegeri).subtract(verecekDegeri);

        BigDecimal ciroDegeri = ciro != null ? sifirla(ciro.getDeger()) : BigDecimal.ZERO;
        BigDecimal karDegeri = kar != null ? sifirla(kar.getDeger()) : BigDecimal.ZERO;
        BigDecimal marj = kar != null && kar.getMarj() != null
                ? kar.getMarj() : yuzde(karDegeri, ciroDegeri);
        BigDecimal oncekiMarj = kar != null && ciro != null
                ? yuzde(sifirla(kar.getOnceki()), sifirla(ciro.getOnceki())) : null;

        List<DashboardDTO.AylikGelirGiderDTO> aylik = grafikler != null && grafikler.getAylikGelirGider() != null
                ? grafikler.getAylikGelirGider() : Collections.emptyList();
        List<BigDecimal> ciroSparkline = aylik.stream().map(a -> sifirla(a.getGelir())).toList();
        List<BigDecimal> karSparkline = aylik.stream()
                .map(a -> sifirla(a.getGelir()).subtract(sifirla(a.getGider()))).toList();
        List<BigDecimal> marjSparkline = aylik.stream()
                .map(a -> {
                    BigDecimal aylikKar = sifirla(a.getGelir()).subtract(sifirla(a.getGider()));
                    BigDecimal deger = yuzde(aylikKar, sifirla(a.getGelir()));
                    return deger != null ? deger : BigDecimal.ZERO;
                }).toList();

        List<KokpitDTO.KpiDTO> liste = new ArrayList<>(8);
        liste.add(kpi("netNakit", "Net Nakit", nakit, null,
                likidite != null ? likidite.getTrendYuzde() : null,
                likidite != null ? likidite.getSparkline() : Collections.emptyList(),
                nakit.compareTo(BigDecimal.ZERO) < 0, "/kasa", null));
        liste.add(kpi("alacak", "Alacak", alacakDegeri, null, null,
                Collections.emptyList(), false, "/cari-hesaplar", Map.of("bakiyeYonu", "alacak")));
        liste.add(kpi("verecek", "Verecek", verecekDegeri, null, null,
                Collections.emptyList(), false, "/cari-hesaplar", Map.of("bakiyeYonu", "borc")));
        liste.add(kpi("netCalismaSermayesi", "Net Çalışma Sermayesi", netCalismaSermayesi, null, null,
                Collections.emptyList(), netCalismaSermayesi.compareTo(BigDecimal.ZERO) < 0,
                "/cari-hesaplar", null));
        liste.add(kpi("ciro", "Dönem Cirosu", ciroDegeri,
                ciro != null ? ciro.getOnceki() : null,
                ciro != null ? ciro.getDegisimYuzde() : null,
                ciroSparkline,
                ciro != null && ciro.getHedef() != null && ciro.getHedef().compareTo(BigDecimal.ZERO) > 0
                        && ciro.getYuzde() != null && ciro.getYuzde().compareTo(BigDecimal.valueOf(50)) < 0,
                "/faturalar", null));
        liste.add(kpi("brutKar", "Dönem Kârı", karDegeri,
                kar != null ? kar.getOnceki() : null,
                kar != null ? kar.getDegisimYuzde() : null,
                karSparkline, karDegeri.compareTo(BigDecimal.ZERO) < 0, "/raporlar", null));
        liste.add(kpi("marj", "Kâr Marjı", marj,
                oncekiMarj,
                degisimYuzde(marj, oncekiMarj),
                marjSparkline,
                marj != null && marj.compareTo(BigDecimal.ZERO) < 0, "/raporlar", null));
        liste.add(kpi("vadesiGecen", "Vadesi Geçen", vadesiGecenTutar, null, null,
                Collections.emptyList(), vadesiGecenAdet > 0 || vadesiGecenTutar.compareTo(BigDecimal.ZERO) > 0,
                "/tahsilat", null));
        return liste;
    }

    private KokpitDTO.KpiDTO kpi(String anahtar, String etiket, BigDecimal deger, BigDecimal onceki,
                                 BigDecimal degisimYuzde, List<BigDecimal> sparkline, boolean kritik,
                                 String route, Map<String, String> query) {
        return KokpitDTO.KpiDTO.builder()
                .anahtar(anahtar)
                .etiket(etiket)
                .deger(sifirla(deger))
                .onceki(onceki)
                .degisimYuzde(degisimYuzde)
                .sparkline(sparkline != null ? sparkline : Collections.emptyList())
                .kritik(kritik)
                .hedef(hedef(route, query))
                .build();
    }

    // ------------------------------------------------------------------
    // Kritik durum şeridi
    // ------------------------------------------------------------------

    private List<KokpitDTO.KritikDurumDTO> kritikDurumListesi(List<DashboardGorevDTO> gorevler,
                                                              DashboardOzetDTO ozet,
                                                              BigDecimal vadesiGecenTutar,
                                                              long vadesiGecenAdet) {
        List<KokpitDTO.KritikDurumDTO> liste = new ArrayList<>(4);

        liste.add(kritik("vadesiGecen", "Vadesi Geçen Fatura",
                seviye(vadesiGecenAdet, 5), vadesiGecenTutar, vadesiGecenAdet, "/tahsilat", null));

        long kritikStok = gorevSayisi(ozet != null ? ozet.getGorevSayilari() : null, "kritikStok");
        liste.add(kritik("kritikStok", "Kritik Stok", seviye(kritikStok, 10),
                null, kritikStok, "/stoklar", Map.of("kritik", "1")));

        long onayBekleyen = gorevler.stream()
                .filter(g -> g.getTur() != null && ONAY_TURLERI.contains(g.getTur()))
                .mapToLong(g -> g.getAdet() != null ? g.getAdet() : 0L)
                .sum();
        if (onayBekleyen > 0) {
            liste.add(kritik("bekleyenOnay", "Bekleyen Onay", "WARN",
                    null, onayBekleyen, "/onaylar", null));
        }

        BigDecimal trend = ozet != null && ozet.getKpi() != null && ozet.getKpi().getLikidite() != null
                ? ozet.getKpi().getLikidite().getTrendYuzde() : null;
        liste.add(kritik("likiditeTrendi", "Likidite Trendi",
                trend != null && trend.compareTo(BigDecimal.ZERO) < 0 ? "WARN" : "BILGI",
                trend, null, "/kasa", null));
        return liste;
    }

    private long gorevSayisi(DashboardOzetDTO.GorevSayilariDTO sayilar, String alan) {
        if (sayilar == null) return 0L;
        return switch (alan) {
            case "kritikStok" -> sayilar.getKritikStok() != null ? sayilar.getKritikStok() : 0L;
            case "vadesiGecen" -> sayilar.getVadesiGecen() != null ? sayilar.getVadesiGecen() : 0L;
            case "onayBekleyen" -> sayilar.getOnayBekleyen() != null ? sayilar.getOnayBekleyen() : 0L;
            default -> 0L;
        };
    }

    private KokpitDTO.KritikDurumDTO kritik(String anahtar, String etiket, String seviye,
                                            BigDecimal tutar, Long adet, String route,
                                            Map<String, String> query) {
        return KokpitDTO.KritikDurumDTO.builder()
                .anahtar(anahtar)
                .etiket(etiket)
                .seviye(seviye)
                .tutar(tutar)
                .adet(adet)
                .hedef(hedef(route, query))
                .build();
    }

    private String seviye(long adet, long kritikEsik) {
        if (adet <= 0) return "BILGI";
        return adet >= kritikEsik ? "KRITIK" : "WARN";
    }

    private DashboardGorevDTO gorevBul(List<DashboardGorevDTO> gorevler, String tur) {
        return gorevler.stream()
                .filter(g -> tur.equals(g.getTur()))
                .findFirst()
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // Nakit & likidite
    // ------------------------------------------------------------------

    private KokpitDTO.NakitDTO nakitBolumu(Long sirketId, DashboardOzetDTO ozet, DashboardGrafikDTO grafikler) {
        DashboardOzetDTO.KpiDTO kpi = ozet != null ? ozet.getKpi() : null;
        DashboardOzetDTO.LikiditeDTO likidite = kpi != null ? kpi.getLikidite() : null;
        DashboardGrafikDTO.KasaBankaDTO kasaBanka = grafikler != null ? grafikler.getKasaBanka() : null;

        BigDecimal kasa = kasaBanka != null ? sifirla(kasaBanka.getKasa())
                : likidite != null ? sifirla(likidite.getKasa()) : BigDecimal.ZERO;
        BigDecimal banka = kasaBanka != null ? sifirla(kasaBanka.getBanka())
                : likidite != null ? sifirla(likidite.getBanka()) : BigDecimal.ZERO;

        List<KokpitDTO.GunlukAkisDTO> gunluk = grafikler != null && grafikler.getGunlukNakitAkisi() != null
                ? grafikler.getGunlukNakitAkisi().stream()
                        .map(g -> KokpitDTO.GunlukAkisDTO.builder()
                                .tarih(tarihCoz(g.getGun()))
                                .giris(sifirla(g.getGelir()))
                                .cikis(sifirla(g.getGider()))
                                .net(sifirla(g.getGelir()).subtract(sifirla(g.getGider())))
                                .build())
                        .toList()
                : Collections.emptyList();

        return KokpitDTO.NakitDTO.builder()
                .kasa(kasa)
                .banka(banka)
                .toplam(kasa.add(banka))
                .trendYuzde(likidite != null ? likidite.getTrendYuzde() : null)
                .sparkline(likidite != null && likidite.getSparkline() != null
                        ? likidite.getSparkline() : Collections.emptyList())
                .gunlukAkis(gunluk)
                .projeksiyon(projeksiyon(sirketId))
                .hedef(hedef("/kasa", null))
                .build();
    }

    private KokpitDTO.ProjeksiyonDTO projeksiyon(Long sirketId) {
        NakitAkisiProjeksiyonDTO veri;
        try {
            veri = raporService.nakitAkisiProjeksiyonu(PROJEKSIYON_GUN, sirketId);
        } catch (RuntimeException e) {
            log.warn("Nakit projeksiyonu alınamadı: {}", e.getMessage());
            return KokpitDTO.ProjeksiyonDTO.builder()
                    .baslangicBakiyesi(BigDecimal.ZERO)
                    .toplamBeklenenGiris(BigDecimal.ZERO)
                    .toplamBeklenenCikis(BigDecimal.ZERO)
                    .tahminiBitisBakiyesi(BigDecimal.ZERO)
                    .gunSayisi(PROJEKSIYON_GUN)
                    .gunluk(Collections.emptyList())
                    .build();
        }
        if (veri == null) {
            return KokpitDTO.ProjeksiyonDTO.builder()
                    .baslangicBakiyesi(BigDecimal.ZERO)
                    .toplamBeklenenGiris(BigDecimal.ZERO)
                    .toplamBeklenenCikis(BigDecimal.ZERO)
                    .tahminiBitisBakiyesi(BigDecimal.ZERO)
                    .gunSayisi(PROJEKSIYON_GUN)
                    .gunluk(Collections.emptyList())
                    .build();
        }
        List<KokpitDTO.ProjeksiyonGunDTO> gunluk = veri.getGunlukAkis() != null
                ? veri.getGunlukAkis().stream()
                        .map(g -> KokpitDTO.ProjeksiyonGunDTO.builder()
                                .tarih(g.getTarih())
                                .giris(sifirla(g.getBeklenenGiris()))
                                .cikis(sifirla(g.getBeklenenCikis()))
                                .net(sifirla(g.getNetAkis()))
                                .kumulatif(sifirla(g.getKumulatifBakiye()))
                                .build())
                        .toList()
                : Collections.emptyList();
        return KokpitDTO.ProjeksiyonDTO.builder()
                .baslangicBakiyesi(sifirla(veri.getBaslangicBakiyesi()))
                .toplamBeklenenGiris(sifirla(veri.getToplamBeklenenGiris()))
                .toplamBeklenenCikis(sifirla(veri.getToplamBeklenenCikis()))
                .tahminiBitisBakiyesi(sifirla(veri.getTahminiBitisBakiyesi()))
                .gunSayisi(veri.getProjeksiyonGunu() != null ? veri.getProjeksiyonGunu() : PROJEKSIYON_GUN)
                .gunluk(gunluk)
                .build();
    }

    // ------------------------------------------------------------------
    // Alacak / verecek
    // ------------------------------------------------------------------

    private KokpitDTO.AlacakVerecekDTO alacakVerecekBolumu(Long sirketId, BigDecimal alacak,
                                                          BigDecimal verecek, BigDecimal tahsilatOrani) {
        List<KokpitDTO.CariSatirDTO> borclular = cariSatirlari(sirketId, true);
        List<KokpitDTO.CariSatirDTO> tedarikciler = cariSatirlari(sirketId, false);

        return KokpitDTO.AlacakVerecekDTO.builder()
                .alacakToplam(sifirla(alacak))
                .verecekToplam(sifirla(verecek))
                .net(sifirla(alacak).subtract(sifirla(verecek)))
                .yaslanma(yaslanmaKovalari(sirketId))
                .borclular(borclular)
                .tedarikciler(tedarikciler)
                .tahsilatOrani(tahsilatOrani)
                .hedef(hedef("/cari-hesaplar", null))
                .build();
    }

    /**
     * Genel tahsilat oranı: tahsil edilen / (tahsil edilen + kalan) × 100.
     * Veri yoksa 0 döner; sıfıra bölme yapılmaz.
     */
    private BigDecimal tahsilatOrani(Long sirketId) {
        try {
            BigDecimal odenen = sifirla(faturaRepository.toplamOdenenKesilmisSatis(sirketId));
            BigDecimal kalan = sifirla(faturaRepository.toplamKalanKesilmisSatis(sirketId));
            BigDecimal toplam = odenen.add(kalan);
            if (toplam.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return odenen.multiply(BigDecimal.valueOf(100)).divide(toplam, 1, RoundingMode.HALF_UP);
        } catch (RuntimeException e) {
            log.warn("Tahsilat oranı hesaplanamadı: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<KokpitDTO.CariSatirDTO> cariSatirlari(Long sirketId, boolean mutlak) {
        List<CariHesap> cariler;
        try {
            cariler = mutlak
                    ? cariHesapRepository.findTop8BySirketIdAndBakiyeLessThanOrderByBakiyeAsc(
                            sirketId, BigDecimal.ZERO)
                    : cariHesapRepository.findTop8BySirketIdAndBakiyeGreaterThanOrderByBakiyeDesc(
                            sirketId, BigDecimal.ZERO);
        } catch (RuntimeException e) {
            log.warn("Cari bakiye listesi alınamadı: {}", e.getMessage());
            return Collections.emptyList();
        }
        if (cariler == null) return Collections.emptyList();
        return cariler.stream()
                .map(c -> KokpitDTO.CariSatirDTO.builder()
                        .cariId(c.getId())
                        .ad(c.getAd())
                        .tutar(mutlak ? sifirla(c.getBakiye()).abs() : sifirla(c.getBakiye()))
                        .build())
                .toList();
    }

    private List<KokpitDTO.KovaDTO> yaslanmaKovalari(Long sirketId) {
        RaporDTO.YaslandirmaRaporDTO rapor;
        try {
            rapor = raporService.yaslandirmaRaporu(sirketId, LocalDate.now());
        } catch (RuntimeException e) {
            log.warn("Yaşlandırma raporu alınamadı: {}", e.getMessage());
            rapor = null;
        }
        Map<String, BigDecimal> kovalar = rapor != null && rapor.getOzet() != null && rapor.getOzet().getKovalar() != null
                ? rapor.getOzet().getKovalar() : Collections.emptyMap();
        Map<String, String> etiketler = new LinkedHashMap<>();
        etiketler.put(RaporService.KOVA_VADEDI_GELMEMIS, "Vadesi Gelmemiş");
        etiketler.put("GUN_0_30", "0-30 Gün");
        etiketler.put("GUN_31_60", "31-60 Gün");
        etiketler.put("GUN_61_90", "61-90 Gün");
        etiketler.put("GUN_90_PLUS", "90+ Gün");
        List<KokpitDTO.KovaDTO> liste = new ArrayList<>(etiketler.size());
        for (Map.Entry<String, String> giris : etiketler.entrySet()) {
            liste.add(KokpitDTO.KovaDTO.builder()
                    .anahtar(giris.getKey())
                    .etiket(giris.getValue())
                    .tutar(sifirla(kovalar.get(giris.getKey())))
                    .adet(null)
                    .build());
        }
        return liste;
    }

    // ------------------------------------------------------------------
    // Kâr / marj
    // ------------------------------------------------------------------

    private KokpitDTO.KarMarjDTO karMarjBolumu(Long sirketId, String rol, String donem,
                                                DashboardGrafikDTO grafikler) {
        DashboardKarAnaliziDTO kar = dashboardService.karAnaliziGetir(sirketId, rol, donem);
        DashboardKarAnaliziDTO.OzetDTO ozet = kar != null ? kar.getOzet() : null;

        List<KokpitDTO.KarKalemDTO> urunler = kar != null && kar.getUrunler() != null
                ? kar.getUrunler().stream().limit(KAR_LIMIT)
                        .map(u -> karKalem(u.getStokId(), u.getAd(), u.getCiro(), u.getMaliyet(), u.getKar(), u.getMarj()))
                        .toList()
                : Collections.emptyList();
        List<KokpitDTO.KarKalemDTO> cariler = kar != null && kar.getCariler() != null
                ? kar.getCariler().stream().limit(KAR_LIMIT)
                        .map(c -> karKalem(c.getCariId(), c.getAd(), c.getCiro(), c.getMaliyet(), c.getKar(), c.getMarj()))
                        .toList()
                : Collections.emptyList();

        List<DashboardDTO.AylikGelirGiderDTO> aylik = grafikler != null && grafikler.getAylikGelirGider() != null
                ? grafikler.getAylikGelirGider() : Collections.emptyList();
        List<KokpitDTO.KarTrendDTO> trend = aylik.stream()
                .map(a -> {
                    BigDecimal ciro = sifirla(a.getGelir());
                    BigDecimal maliyet = sifirla(a.getGider());
                    BigDecimal karDegeri = ciro.subtract(maliyet);
                    return KokpitDTO.KarTrendDTO.builder()
                            .ay(a.getAy())
                            .ciro(ciro)
                            .maliyet(maliyet)
                            .kar(karDegeri)
                            .marj(yuzde(karDegeri, ciro))
                            .build();
                })
                .toList();

        return KokpitDTO.KarMarjDTO.builder()
                .ciro(sifirla(ozet != null ? ozet.getCiro() : null))
                .maliyet(sifirla(ozet != null ? ozet.getMaliyet() : null))
                .kar(sifirla(ozet != null ? ozet.getKar() : null))
                .marj(ozet != null ? ozet.getMarj() : null)
                .trend(trend)
                .urunler(urunler)
                .cariler(cariler)
                .maliyetsizKalem(ozet != null && ozet.getMaliyetsizKalem() != null
                        ? ozet.getMaliyetsizKalem() : 0L)
                .hedef(hedef("/raporlar", null))
                .build();
    }

    private KokpitDTO.KarKalemDTO karKalem(Long id, String ad, BigDecimal ciro, BigDecimal maliyet,
                                           BigDecimal kar, BigDecimal marj) {
        return KokpitDTO.KarKalemDTO.builder()
                .id(id)
                .ad(ad)
                .ciro(sifirla(ciro))
                .maliyet(sifirla(maliyet))
                .kar(sifirla(kar))
                .marj(marj)
                .build();
    }

    // ------------------------------------------------------------------
    // Fatura / sipariş pipeline
    // ------------------------------------------------------------------

    private KokpitDTO.FaturaSiparisDTO faturaSiparisBolumu(Long sirketId, DashboardGrafikDTO grafikler,
                                                           BigDecimal vadesiGecenTutar,
                                                           long vadesiGecenAdet,
                                                           BigDecimal tahsilatOrani) {
        List<KokpitDTO.KovaDTO> siparisDurumlari = grafikler != null && grafikler.getSiparisDurumlari() != null
                ? grafikler.getSiparisDurumlari().stream()
                        .map(s -> KokpitDTO.KovaDTO.builder()
                                .anahtar(s.getDurum())
                                .etiket(s.getDurum())
                                .adet(s.getAdet())
                                .tutar(null)
                                .build())
                        .toList()
                : Collections.emptyList();

        return KokpitDTO.FaturaSiparisDTO.builder()
                .durumlar(faturaDurumlari(sirketId, vadesiGecenTutar, vadesiGecenAdet))
                .vadesiGecenTutar(sifirla(vadesiGecenTutar))
                .vadesiGecenAdet(vadesiGecenAdet)
                .vadesiGecenFaturalar(vadesiGecenFaturalar(sirketId))
                .tahsilatOrani(tahsilatOrani)
                .siparisDurumlari(siparisDurumlari)
                .hedef(hedef("/tahsilat", null))
                .build();
    }

    /**
     * Fatura pipeline kovaları. Durum dağılımı SQL agregasyonundan gelir;
     * tüm fatura listesi belleğe alınmaz. Vadesi geçen ve tahsil edilen
     * kovalar özet toplamlardan eklenir.
     */
    private List<KokpitDTO.KovaDTO> faturaDurumlari(Long sirketId, BigDecimal vadesiGecenTutar,
                                                    long vadesiGecenAdet) {
        Map<String, KokpitDTO.KovaDTO> kovalar = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> satirlar = faturaRepository.kokpitFaturaDurumDagilimi(sirketId);
            if (satirlar != null) {
                for (Map<String, Object> satir : satirlar) {
                    String durum = satir.get("durum") != null ? String.valueOf(satir.get("durum")) : "DIGER";
                    long adet = sayi(satir.get("adet"));
                    BigDecimal tutar = para(satir.get("tutar"));
                    kovalar.put(durum, kova(durum, faturaDurumEtiketi(durum), tutar, adet));
                }
            }
        } catch (RuntimeException e) {
            log.warn("Fatura durum dağılımı alınamadı: {}", e.getMessage());
        }

        List<KokpitDTO.KovaDTO> liste = new ArrayList<>();
        for (String anahtar : List.of("TASLAK", "TEKLIF", "KESILDI", "IPTAL")) {
            KokpitDTO.KovaDTO mevcut = kovalar.get(anahtar);
            liste.add(mevcut != null ? mevcut
                    : kova(anahtar, faturaDurumEtiketi(anahtar), BigDecimal.ZERO, 0L));
        }
        if (vadesiGecenAdet > 0 || sifirla(vadesiGecenTutar).compareTo(BigDecimal.ZERO) > 0) {
            liste.add(kova("VADESI_GECEN", "Vadesi Geçen", vadesiGecenTutar, vadesiGecenAdet));
        }
        long tahsilEdilenAdet = 0L;
        BigDecimal tahsilEdilenTutar = BigDecimal.ZERO;
        try {
            tahsilEdilenAdet = faturaRepository.countOdenenKesilmisSatis(sirketId);
            tahsilEdilenTutar = sifirla(faturaRepository.toplamTahsilEdilenKesilmisSatis(sirketId));
        } catch (RuntimeException e) {
            log.warn("Tahsil edilen fatura özeti alınamadı: {}", e.getMessage());
        }
        if (tahsilEdilenAdet > 0) {
            liste.add(kova("TAHSIL_EDILDI", "Tahsil Edildi", tahsilEdilenTutar, tahsilEdilenAdet));
        }
        return liste;
    }

    private KokpitDTO.KovaDTO kova(String anahtar, String etiket, BigDecimal tutar, Long adet) {
        return KokpitDTO.KovaDTO.builder()
                .anahtar(anahtar)
                .etiket(etiket)
                .tutar(sifirla(tutar))
                .adet(adet)
                .build();
    }

    private String faturaDurumEtiketi(String durum) {
        return switch (durum) {
            case "TASLAK" -> "Taslak";
            case "TEKLIF" -> "Teklif";
            case "KESILDI" -> "Kesildi";
            case "IPTAL" -> "İptal";
            case "VADESI_GECEN" -> "Vadesi Geçen";
            case "TAHSIL_EDILDI" -> "Tahsil Edildi";
            default -> durum;
        };
    }

    private List<KokpitDTO.VadesiGecenFaturaDTO> vadesiGecenFaturalar(Long sirketId) {
        try {
            List<Fatura> faturalar = faturaRepository.findVadesiGecen(sirketId, Fatura.FaturaDurum.KESILDI,
                    ODEME_KAPALI_DURUMLAR, LocalDate.now(), PageRequest.of(0, VADESI_GECEN_LIMIT));
            if (faturalar == null) return Collections.emptyList();
            return faturalar
                    .stream()
                    .map(f -> KokpitDTO.VadesiGecenFaturaDTO.builder()
                            .faturaId(f.getId())
                            .cariId(f.getCariHesap() != null ? f.getCariHesap().getId() : null)
                            .faturaNumarasi(f.getFaturaNumarasi())
                            .cariAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                            .vadeTarihi(f.getVadeTarihi())
                            .kalanTutar(sifirla(f.getKalanTutar()))
                            .gecikmeGun(f.getVadeTarihi() != null
                                    ? ChronoUnit.DAYS.between(f.getVadeTarihi(), LocalDate.now()) : 0L)
                            .build())
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Vadesi geçen faturalar alınamadı: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------
    // Yardımcılar
    // ------------------------------------------------------------------

    private KokpitDTO.DonemDTO donemDTO(DashboardOzetDTO ozet, String kod) {
        DashboardOzetDTO.DonemDTO donem = ozet != null ? ozet.getDonem() : null;
        if (donem == null) {
            return KokpitDTO.DonemDTO.builder().kod(kod).etiket(donemEtiketi(kod)).build();
        }
        return KokpitDTO.DonemDTO.builder()
                .kod(donem.getKod() != null ? donem.getKod() : kod)
                .etiket(donem.getEtiket() != null ? donem.getEtiket() : donemEtiketi(kod))
                .baslangic(donem.getBaslangic())
                .bitis(donem.getBitis())
                .build();
    }

    private String donemEtiketi(String kod) {
        return switch (kod) {
            case "GECEN_AY" -> "Geçen Ay";
            case "BU_CEYREK" -> "Bu Çeyrek";
            case "BU_YIL" -> "Bu Yıl";
            default -> "Bu Ay";
        };
    }

    private String donemCoz(String donem) {
        if (donem == null || donem.isBlank()) {
            return VARSAYILAN_DONEM;
        }
        String kod = donem.trim().toUpperCase(Locale.ROOT);
        return GECERLI_DONEMLER.contains(kod) ? kod : VARSAYILAN_DONEM;
    }

    private KokpitDTO.HedefDTO hedef(String route, Map<String, String> query) {
        return KokpitDTO.HedefDTO.builder()
                .route(route)
                .query(query != null ? query : Collections.emptyMap())
                .build();
    }

    private BigDecimal yuzde(BigDecimal pay, BigDecimal payda) {
        if (pay == null || payda == null || payda.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return pay.multiply(BigDecimal.valueOf(100)).divide(payda, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal degisimYuzde(BigDecimal deger, BigDecimal onceki) {
        if (deger == null || onceki == null || onceki.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return deger.subtract(onceki).multiply(BigDecimal.valueOf(100))
                .divide(onceki.abs(), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal sifirla(BigDecimal deger) {
        return deger != null ? deger : BigDecimal.ZERO;
    }

    /** JPQL toplam alanı sürüme göre Long/Integer/Double dönebilir. */
    private BigDecimal para(Object deger) {
        if (deger == null) return BigDecimal.ZERO;
        if (deger instanceof BigDecimal bd) return bd;
        if (deger instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(deger));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private long sayi(Object deger) {
        if (deger == null) return 0L;
        if (deger instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(deger));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private LocalDate tarihCoz(String deger) {
        if (deger == null || deger.isBlank()) return null;
        try {
            return LocalDate.parse(deger);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private <T> List<T> guvenliListe(java.util.function.Supplier<List<T>> tedarikci) {
        try {
            List<T> liste = tedarikci.get();
            return liste != null ? liste : Collections.emptyList();
        } catch (RuntimeException e) {
            log.warn("Kokpit listesi alınamadı: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
