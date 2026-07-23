package com.raspel.erp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
}
