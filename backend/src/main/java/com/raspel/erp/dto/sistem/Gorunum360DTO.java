package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 360 derece özet görünümleri: stok kârlılığı, çalışan performansı ve
 * müşteri segmentasyonu. Her biri tek endpoint'te toplanır.
 */
public final class Gorunum360DTO {

    private Gorunum360DTO() {
    }

    // ---------- Stok Kâr 360 ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StokKar {
        private List<Satir> urunler;
        private BigDecimal toplamCiro;
        private BigDecimal toplamKar;
        private BigDecimal genelMarj;
        private int negatifMarjAdet;
        private int toplamUrun;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Satir {
        private Long stokId;
        private String ad;
        private String stokKodu;
        private String kategori;
        private BigDecimal satisAdet;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal brutKar;
        private BigDecimal marj;
        private BigDecimal stokMiktar;
        private BigDecimal birimMaliyet;
    }

    // ---------- Çalışan Performans 360 ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalisanPerformans {
        private List<Calisan> calisanlar;
        private long toplamTeslimat;
        private long tamamlananTeslimat;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Calisan {
        private Long id;
        private String ad;
        private String rol;
        private long toplamTeslimat;
        private long tamamlananTeslimat;
        private long bekleyenTeslimat;
        private BigDecimal teslimTutari;
    }

    // ---------- Müşteri Segmentasyonu ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MusteriSegment {
        private List<SegmentOzet> segmentOzeti;
        private List<MusteriSegmentSatir> musteriler;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SegmentOzet {
        private String segment;
        private long adet;
        private BigDecimal toplamCiro;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MusteriSegmentSatir {
        private Long cariId;
        private String ad;
        /** VIP, DUZENLI, YENI, RISKLI, PASIF, GELISMEDE */
        private String segment;
        /** Segmentin gerekçesi (kullanıcıya gösterilir). */
        private String gerekce;
        private BigDecimal ciro;
        private BigDecimal bakiye;
        private BigDecimal kalanTutar;
        private long faturaAdet;
        private Integer sonFaturaGunOnce;
    }
}
