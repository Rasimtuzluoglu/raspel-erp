package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anasayfa hızlı özeti ({@code GET /api/dashboard/ozet}).
 *
 * <p>İlk boyada karar verilebilir KPI'lar ve görev sayaçlarını taşır; ağır
 * listeler bu uçta yer almaz (grafikler/akış ayrı uçlardan gelir).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOzetDTO {

    private HeroDTO hero;
    private BugunDTO bugun;
    private KpiDTO kpi;
    private StokDTO stok;
    private GorevSayilariDTO gorevSayilari;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeroDTO {
        private String kullaniciAd;
        private String sirketAd;
        /** Canlı akış rozeti (şu an her zaman true; ileride SSE durumuna bağlanabilir). */
        private Boolean canli;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BugunDTO {
        private BigDecimal tahsilat;
        private BigDecimal odeme;
        private Long siparis;
        private Long teslimat;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpiDTO {
        private LikiditeDTO likidite;
        private NetCariDTO netCari;
        private CiroDTO ciro;
        private KarDTO kar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LikiditeDTO {
        private BigDecimal deger;
        private BigDecimal kasa;
        private BigDecimal banka;
        /** Son 7 günlük net nakit akışının likiditeye oranı (yüzde). */
        private BigDecimal trendYuzde;
        /** Son 7 günün günlük net nakit akışı (eskiden yeniye). */
        private List<BigDecimal> sparkline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NetCariDTO {
        private BigDecimal deger;
        private BigDecimal alacak;
        private BigDecimal borc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CiroDTO {
        private BigDecimal deger;
        private BigDecimal hedef;
        private BigDecimal yuzde;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KarDTO {
        private BigDecimal deger;
        private BigDecimal hedef;
        private BigDecimal marj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StokDTO {
        private Long cesit;
        private BigDecimal deger;
        private Long kritikAdet;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GorevSayilariDTO {
        private Long onayBekleyen;
        private Long vadesiGecen;
        private Long kritikStok;
        private Long bekleyenTeslimat;
        private Long vadesiYaklasan;
    }
}
