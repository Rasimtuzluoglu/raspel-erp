package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anasayfa grafik verileri ({@code GET /api/dashboard/grafikler}).
 *
 * <p>Alanlar, mevcut Dashboard hesaplarıyla (legacy {@code GET /api/dashboard})
 * aynı kaynaklardan üretilir; iç içe DTO'lar {@link DashboardDTO} ile
 * paylaşılır ki iki uç arasında şekil farkı oluşmasın.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardGrafikDTO {

    private List<DashboardDTO.AylikGelirGiderDTO> aylikGelirGider;
    private List<DashboardDTO.GunlukNakitAkisiDTO> gunlukNakitAkisi;
    private List<DashboardDTO.EnCokSatanDTO> enCokSatanlar;
    private List<DashboardDTO.KategoriSatisDTO> kategoriSatislari;
    private List<DashboardDTO.AlacakYasDTO> alacakYaslandirma;
    private List<DashboardDTO.CariOzetDTO> enCokBorcCariler;
    private List<DashboardDTO.CariOzetDTO> enCokAlacakCariler;
    private KasaBankaDTO kasaBanka;
    private HedefDTO hedef;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KasaBankaDTO {
        private BigDecimal kasa;
        private BigDecimal banka;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HedefDTO {
        private BigDecimal ciro;
        private BigDecimal gerceklesen;
        private BigDecimal kar;
        private BigDecimal hedefKar;
    }
}
