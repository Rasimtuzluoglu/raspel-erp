package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Gelişmiş kârlılık analizi: özet + aylık trend + grup kırılımı + negatif marjlı kalemler. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KarlilikAnalizDTO {

    private String grup;

    private Ozet ozet;
    private List<AylikKarlilik> aylikTrend;
    private List<Kirilim> kirilim;
    private List<Kirilim> negatifMarjli;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ozet {
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal brutKar;
        private BigDecimal brutKarMarji;
        private BigDecimal iadeTutari;
        private BigDecimal iadeMaliyeti;
        private int kalemSayisi;
        private int negatifMarjliAdet;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AylikKarlilik {
        /** yyyy-MM */
        private String ay;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal brutKar;
        private BigDecimal marj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Kirilim {
        private Long id;
        private String ad;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal brutKar;
        private BigDecimal marj;
        /** Ciroya göre yüzde pay. */
        private BigDecimal pay;
    }
}
