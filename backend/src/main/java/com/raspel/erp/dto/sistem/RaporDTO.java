package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.dto.finans.HareketDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaporDTO {

    private CariEkstreDTO cariEkstre;
    private GelirGiderOzetDTO gelirGiderOzet;
    private KdvRaporDTO kdvRapor;
    private List<YaslandirmaDTO> yaslandirma;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CariEkstreDTO {
        private String cariAd;
        private String cariVergiNo;
        private String cariTelefon;
        private String cariEmail;
        private String cariAdres;
        private BigDecimal donemBasBakiye;
        private BigDecimal donemSonBakiye;
        private BigDecimal toplamBorc;
        private BigDecimal toplamAlacak;
        private BigDecimal netHareket;
        private List<CariEkstreSatiriDTO> hareketler;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CariEkstreSatiriDTO {
        private Long id;
        private java.time.LocalDate tarih;
        private String tur;
        private String aciklama;
        private String faturaNumarasi;
        private java.time.LocalDate vadeTarihi;
        private BigDecimal borc;
        private BigDecimal alacak;
        private BigDecimal yuruyenBakiye;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GelirGiderOzetDTO {
        private BigDecimal toplamGelir;
        private BigDecimal toplamGider;
        private BigDecimal netKarZarar;
        private List<Map<String, Object>> aylikDagilim;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KdvRaporDTO {
        private BigDecimal toplamKdvCikis;
        private BigDecimal toplamKdvGiris;
        private BigDecimal kdvFarki;
    }

    /**
     * Cari bazında vade yaşlandırma satırı.
     *
     * <p><b>Kural:</b> satırdaki tutarlar carinin NET bakiyesi değil, vadesi
     * geçmemiş her bir faturanın kalan tutarının kova toplamıdır. Önceden
     * {@code bakiye = |cari bakiyesi|} gösteriliyordu; 1.000 TL vadesi geçmiş
     * fatura + 500 TL peşin tahsilatı olan cari 90+ kovasında 500 TL ile
     * görünüyor, yani rapor tahsil edilebilecek tutarı olduğundan az gösteriyordu.
     *
     * <p>{@code kova} alanı i18n anahtarıdır; etiketler frontend'de çevrilir
     * (backend'de Türkçe sabit metin döndürmek EN kullanıcısına sızıyordu).
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class YaslandirmaDTO {
        private Long cariHesapId;
        private String cariAd;
        /** Kova toplamları: VADEDI_GELMEMIS, GUN_0_30, GUN_31_60, GUN_61_90, GUN_90_PLUS */
        private Map<String, BigDecimal> kovalar;
        /** Kova toplamlarının toplamı (cari toplam tahsil edilecek). */
        private BigDecimal toplam;
        /** En gecikmiş faturanın gecikme günü (0 = vadesi gelmemiş fatura yok). */
        private int enFazlaGecikmeGun;
        /** Ağırlıklı ortalama gecikme günü; yalnızca gecikmiş faturalar üzerinden. */
        private double ortalamaGecikmeGun;
        /** Gecikmiş tutar (VADEDI_GELMEMIS hariç kova toplamı). */
        private BigDecimal gecikmisTutar;
    }

    /** Yaşlandırma raporunun kova bazlı toplamı (tüm cariler). */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class YaslandirmaOzetDTO {
        private Map<String, BigDecimal> kovalar;
        private BigDecimal toplam;
        private BigDecimal gecikmisTutar;
        private int cariSayisi;
        /** Kova anahtarlarının görünüm sırası; frontend başlık sırasını buradan alır. */
        private List<String> kovaSirasi;
    }

    /** Yaşlandırma raporu: satırlar + kova toplamları. */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class YaslandirmaRaporDTO {
        private List<YaslandirmaDTO> satirlar;
        private YaslandirmaOzetDTO ozet;
        /** Referans tarih (gecikme günleri bu tarihe göre hesaplanır). */
        private java.time.LocalDate referansTarih;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KdvBeyannameSatiriDTO {
        private BigDecimal kdvOrani;
        private BigDecimal matrah;
        private BigDecimal kdv;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KdvBeyannameDTO {
        private String donem;
        private List<KdvBeyannameSatiriDTO> satislar;    // Hesaplanan KDV (1-2 no.lu tablo)
        private List<KdvBeyannameSatiriDTO> alislar;     // İndirilecek KDV (19-20 no.lu tablo)
        private BigDecimal toplamHesaplananKdv;
        private BigDecimal toplamIndirilecekKdv;
        private BigDecimal odenecekKdv;      // hesaplanan - indirilecek (pozitifse ödenecek)
        private BigDecimal devredenKdv;      // indirilecek > hesaplanan ise devreden
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BaBsSatiriDTO {
        private String faturaNo;
        private java.time.LocalDate tarih;
        private String cariAd;
        private String cariVkn;
        private BigDecimal matrah;
        private BigDecimal kdv;
        private BigDecimal tutar;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BaBsDTO {
        private String donem;
        private String tur; // BA (alış) veya BS (satış)
        private BigDecimal esik;
        private List<BaBsSatiriDTO> kayitlar;
        private BigDecimal toplamTutar;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CariKarlilikSatiriDTO {
        private Long cariId;
        private String cariAd;
        private BigDecimal toplamSatis;
        private BigDecimal toplamMaliyet;
        private BigDecimal kar;
        private BigDecimal karMarji;
        private long faturaSayisi;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CariKarlilikDTO {
        private BigDecimal toplamSatis;
        private BigDecimal toplamMaliyet;
        private BigDecimal toplamKar;
        private List<CariKarlilikSatiriDTO> satirlar;
    }
}