package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** 12 aylık bütçe matrisi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceMatrisDTO {
    private Integer yil;
    private String tur;
    private List<Satir> satirlar;
    private BigDecimal[] aylikToplam;
    private BigDecimal genelToplam;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Satir {
        private String kategori;
        /** 12 elemanlı dizi (Ocak..Aralık). */
        private BigDecimal[] aylar;
        private BigDecimal toplam;
    }
}
