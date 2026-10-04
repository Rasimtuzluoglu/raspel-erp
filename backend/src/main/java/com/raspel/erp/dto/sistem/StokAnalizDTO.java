package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stok analiz raporları: değerleme (ağırlıklı ortalama + FIFO), sipariş önerisi ve
 * satış temsilcisi performansı.
 */
@Data
public class StokAnalizDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DegerlemeSatiri {
        private Long stokId;
        private String stokKodu;
        private String ad;
        private BigDecimal miktar;
        private BigDecimal ortalamaBirimMaliyet;
        private BigDecimal ortalamaDeger;
        private BigDecimal fifoBirimMaliyet;
        private BigDecimal fifoDeger;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Degerleme {
        private BigDecimal toplamOrtalamaDeger;
        private BigDecimal toplamFifoDeger;
        private int kalemSayisi;
        @Builder.Default
        private List<DegerlemeSatiri> satirlar = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OneriSatiri {
        private Long stokId;
        private String stokKodu;
        private String ad;
        private BigDecimal mevcut;
        private BigDecimal minMiktar;
        private BigDecimal hedefMiktar;
        private BigDecimal oneriMiktar;
        private BigDecimal birimMaliyet;
        private BigDecimal tahminiTutar;
        private Long tedarikciId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemsilciSatiri {
        private Long temsilciId;
        private String temsilciAd;
        private long faturaSayisi;
        private BigDecimal toplamSatis;
        private BigDecimal ortalamaFatura;
        /**
         * Cari kartında temsilci atanmamış satışlar. {@code temsilciAd} bu durumda
         * null gelir; frontend yerelleştirilmiş etiket gösterir (backend'den
         * Türkçe sabit metin döndürmek i18n kaçağıydı).
         */
        private boolean atanmamisMi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemsilciPerformans {
        private BigDecimal toplamSatis;
        private long toplamFatura;
        @Builder.Default
        private List<TemsilciSatiri> satirlar = new ArrayList<>();
    }
}
