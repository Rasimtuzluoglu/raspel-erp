package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoneticiKokpitDTO {
    private Integer yil;
    private Integer ay;
    private Integer ayinGunu;
    private Integer aydakiToplamGun;
    private Integer kalanGun;

    // Ciro ve Hedefler
    private BigDecimal gerceklesenCiro;
    private BigDecimal hedefCiro;
    private Double ciroIlerlemeYuzdesi;
    private BigDecimal kalanCiro;
    private BigDecimal gunlukOrtalamaCiro;

    // Kâr ve Maliyet
    private BigDecimal toplamAlisMaliyeti;
    private BigDecimal toplamMasraflar;
    private BigDecimal gerceklesenKar;
    private BigDecimal hedefKar;
    private Double karIlerlemeYuzdesi;
    private Double netKarMarji;

    // Adet ve Müşteri
    private Integer toplamSatisAdedi;
    private Integer hedefSatisAdedi;
    private Integer yeniMusteriSayisi;
    private Integer hedefYeniMusteri;

    // Likidite ve Alacak/Borç
    private BigDecimal kasaBankaToplam;
    private BigDecimal toplamAlacak;
    private BigDecimal vadesiGecenAlacak;
    private BigDecimal toplamBorc;

    // Detay Listeleri
    private List<TopMusteriDTO> topMusteriler;
    private List<TopUrunDTO> topUrunler;
    private List<KritikAlacakDTO> kritikAlacaklar;
    private List<GunlukCiroDTO> gunlukCiroTrendi;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopMusteriDTO {
        private Long cariId;
        private String unvan;
        private BigDecimal toplamCiro;
        private Integer faturaSayisi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopUrunDTO {
        private Long stokId;
        private String stokKodu;
        private String stokAdi;
        private BigDecimal satisMiktari;
        private BigDecimal toplamCiro;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KritikAlacakDTO {
        private Long cariId;
        private String unvan;
        private BigDecimal bakiye;
        private String telefon;
        private String email;
        private LocalDate vadeTarihi;
        private Long gecikmeGunu;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GunlukCiroDTO {
        private Integer gun;
        private String tarih;
        private BigDecimal ciro;
    }
}
