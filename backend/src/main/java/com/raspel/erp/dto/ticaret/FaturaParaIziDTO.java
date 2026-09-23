package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bir faturanın para izini (kasa/banka hareketleri, iade ve irsaliye bağları) taşır.
 * Bir satışta sorun olduğunda hangi hesaba ne kadar, hangi belgeyle bağlandığını gösterir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaParaIziDTO {

    private Long faturaId;
    private String faturaNumarasi;
    private BigDecimal genelToplam;
    private BigDecimal odenenTutar;
    private BigDecimal kalanTutar;
    private String odemeDurumu;

    private Long kasaId;
    private String kasaAd;
    private Long bankaId;
    private String bankaAd;
    private Long irsaliyeId;

    private List<HareketIzi> kasaHareketleri;
    private List<HareketIzi> bankaHareketleri;
    private List<IadeIzi> iadeler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HareketIzi {
        private Long id;
        private String hesapAd;
        private String tur;
        private BigDecimal tutar;
        private LocalDate tarih;
        private String aciklama;
        private String kaynakTip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IadeIzi {
        private Long id;
        private String tur;
        private LocalDate tarih;
        private BigDecimal tutar;
        private String durum;
        private String aciklama;
    }
}
