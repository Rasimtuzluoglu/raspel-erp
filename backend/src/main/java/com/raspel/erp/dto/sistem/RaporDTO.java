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
        private BigDecimal donemBasBakiye;
        private BigDecimal donemSonBakiye;
        private List<HareketDTO> hareketler;
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

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class YaslandirmaDTO {
        private String cariAd;
        private BigDecimal bakiye;
        private int gun;
        private String aralik;
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