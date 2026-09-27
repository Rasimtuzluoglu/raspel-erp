package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kârlılık analizi satır detayı (drill-down): seçilen kategori/ürün/cari için
 * alt kırılım ve belge (fatura kalemi) listesi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KarlilikDetayDTO {

    private String grup;
    private String deger;
    private String altGrup;

    private BigDecimal ciro;
    private BigDecimal maliyet;
    private BigDecimal brutKar;
    private BigDecimal marj;

    @Builder.Default
    private List<KarlilikAnalizDTO.Kirilim> altKirilim = new ArrayList<>();

    @Builder.Default
    private List<Belge> belgeler = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Belge {
        private Long faturaId;
        private String faturaNumarasi;
        private LocalDate tarih;
        private String cariAd;
        private String urunAd;
        private BigDecimal adet;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal brutKar;
        /** İade kaynaklı negatif satır mı? */
        private Boolean iade;
    }
}
