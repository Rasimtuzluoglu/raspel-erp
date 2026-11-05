package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Anasayfa kâr analizi ({@code GET /api/dashboard/kar-analizi}).
 *
 * <p>Seçili dönemde kesilmiş satış faturalarının ciro/maliyet/kâr/marj
 * özetini ve en kârlı ürün/cari kırılımlarını taşır. Maliyeti olmayan
 * kalemler 0 maliyetle hesaba girer; sayıları {@code maliyetsizKalem} ile
 * raporlanır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKarAnaliziDTO {

    private DonemDTO donem;
    private OzetDTO ozet;
    /** En kârlı ürünler (en fazla 10). */
    private List<UrunDTO> urunler;
    /** En kârlı cariler (en fazla 10). */
    private List<CariDTO> cariler;

    /**
     * Analiz dönemi. {@code kod}: BU_AY | GECEN_AY | BU_CEYREK | BU_YIL.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DonemDTO {
        private String kod;
        private LocalDate baslangic;
        private LocalDate bitis;
        private String etiket;
    }

    /** Dönem toplamları; {@code marj} cirosu olmayan dönemlerde null'dır. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OzetDTO {
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        /** Kâr / ciro × 100; ciro 0 ise null. */
        private BigDecimal marj;
        /** Maliyet anlık görüntüsü hiç olmayan kalem sayısı. */
        private Long maliyetsizKalem;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UrunDTO {
        private Long stokId;
        private String ad;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        /** Kâr / ciro × 100; ciro 0 ise null. */
        private BigDecimal marj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CariDTO {
        private Long cariId;
        private String ad;
        private BigDecimal ciro;
        private BigDecimal maliyet;
        private BigDecimal kar;
        /** Kâr / ciro × 100; ciro 0 ise null. */
        private BigDecimal marj;
    }
}
