package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Anasayfa finans kokpiti ({@code GET /api/dashboard/kokpit}).
 *
 * <p>Tek istekte yönetici kokpitinin ihtiyaç duyduğu tüm bölümleri taşır:
 * özet KPI'lar, kritik durum şeridi, nakit/likidite, alacak/verecek,
 * kâr/marj ve fatura/sipariş pipeline'ı.
 *
 * <p>Bölümler boş veride bile {@code null} dönmez; listeler boş, tutarlar
 * sıfırdır. {@code karMarj} yalnızca yetkili rollerde (ADMIN/MUHASEBE)
 * doldurulur; diğer rollerde {@code null} döner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KokpitDTO {

    private DonemDTO donem;
    private List<KpiDTO> ozet;
    private List<KritikDurumDTO> kritikDurum;
    private NakitDTO nakit;
    private AlacakVerecekDTO alacakVerecek;
    /** Yalnızca ADMIN/MUHASEBE rollerinde dolar; diğer rollerde null. */
    private KarMarjDTO karMarj;
    private FaturaSiparisDTO faturaSiparis;
    /** Kural tabanlı yönetici içgörüleri (en fazla 3). */
    private List<DashboardIcgoruDTO> icgoruler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DonemDTO {
        private String kod;
        private String etiket;
        private LocalDate baslangic;
        private LocalDate bitis;
    }

    /** KPI veya kart tıklamasının gideceği filtreli hedef. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HedefDTO {
        private String route;
        private Map<String, String> query;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpiDTO {
        private String anahtar;
        private String etiket;
        private BigDecimal deger;
        private BigDecimal onceki;
        private BigDecimal degisimYuzde;
        private List<BigDecimal> sparkline;
        private Boolean kritik;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KritikDurumDTO {
        private String anahtar;
        private String etiket;
        /** KRITIK | WARN | BILGI */
        private String seviye;
        private BigDecimal tutar;
        private Long adet;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NakitDTO {
        private BigDecimal kasa;
        private BigDecimal banka;
        private BigDecimal toplam;
        private BigDecimal trendYuzde;
        private List<BigDecimal> sparkline;
        private List<GunlukAkisDTO> gunlukAkis;
        private ProjeksiyonDTO projeksiyon;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GunlukAkisDTO {
        private LocalDate tarih;
        private BigDecimal giris;
        private BigDecimal cikis;
        private BigDecimal net;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjeksiyonDTO {
        private BigDecimal baslangicBakiyesi;
        private BigDecimal toplamBeklenenGiris;
        private BigDecimal toplamBeklenenCikis;
        private BigDecimal tahminiBitisBakiyesi;
        private Integer gunSayisi;
        private List<ProjeksiyonGunDTO> gunluk;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjeksiyonGunDTO {
        private LocalDate tarih;
        private BigDecimal giris;
        private BigDecimal cikis;
        private BigDecimal net;
        private BigDecimal kumulatif;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlacakVerecekDTO {
        private BigDecimal alacakToplam;
        private BigDecimal verecekToplam;
        private BigDecimal net;
        private List<KovaDTO> yaslanma;
        private List<CariSatirDTO> borclular;
        private List<CariSatirDTO> tedarikciler;
        /** Dönem tahsilatı / dönem satış cirosu × 100; hesaplanamazsa null. */
        private BigDecimal tahsilatOrani;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CariSatirDTO {
        private Long cariId;
        private String ad;
        private BigDecimal tutar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KarMarjDTO {
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        private BigDecimal marj;
        private List<KarTrendDTO> trend;
        private List<KarKalemDTO> urunler;
        private List<KarKalemDTO> cariler;
        private Long maliyetsizKalem;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KarTrendDTO {
        private String ay;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        private BigDecimal marj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KarKalemDTO {
        private Long id;
        private String ad;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        private BigDecimal marj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaturaSiparisDTO {
        private List<KovaDTO> durumlar;
        private BigDecimal vadesiGecenTutar;
        private Long vadesiGecenAdet;
        private List<VadesiGecenFaturaDTO> vadesiGecenFaturalar;
        private BigDecimal tahsilatOrani;
        private List<KovaDTO> siparisDurumlari;
        private HedefDTO hedef;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VadesiGecenFaturaDTO {
        private Long faturaId;
        private Long cariId;
        private String faturaNumarasi;
        private String cariAd;
        private LocalDate vadeTarihi;
        private BigDecimal kalanTutar;
        private Long gecikmeGun;
    }

    /** Yaşlanma ve pipeline bölümlerinde ortak kova gösterimi. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KovaDTO {
        private String anahtar;
        private String etiket;
        private BigDecimal tutar;
        private Long adet;
    }
}
