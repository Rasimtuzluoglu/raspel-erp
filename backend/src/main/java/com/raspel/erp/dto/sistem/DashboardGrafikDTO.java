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
    /** Sipariş durum dağılımı (adet azalan). Durumu boş kayıtlar "DIGER" diliminde döner. */
    private List<SiparisDurumDTO> siparisDurumlari;
    /** Bu ayın GIDER bütçesi ile aynı ayın gerçekleşen masrafı (kategori bazında, en fazla 6). */
    private List<ButceGerceklesenDTO> butceGerceklesen;
    /** Stok değeri özeti ve kategori dağılımı (en fazla 6). */
    private StokDegeriDTO stokDegeri;

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

    /** Sipariş durumu ve bu durumdaki sipariş adedi. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SiparisDurumDTO {
        private String durum;
        private Long adet;
    }

    /** Kategori bazında planlanan gider bütçesi ve gerçekleşen masraf. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ButceGerceklesenDTO {
        private String kategori;
        private BigDecimal butce;
        private BigDecimal gerceklesen;
    }

    /** Stok değeri özeti: toplam değer, kritik ürün adedi ve kategori dağılımı. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StokDegeriDTO {
        private BigDecimal toplamDeger;
        private Long kritikAdet;
        private List<StokKategoriDTO> kategoriler;
    }

    /** Kategori bazında stok değeri (miktar × fiyat) ve ürün adedi. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StokKategoriDTO {
        private String kategori;
        private BigDecimal deger;
        private Long adet;
    }
}
