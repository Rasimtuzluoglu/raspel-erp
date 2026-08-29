package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TahsilatDTO {

    /** Ödenmemiş tüm satış faturalarının toplam kalan tutarı */
    private BigDecimal toplamAlacak;

    /** Vadesi geçmiş faturaların toplam kalan tutarı */
    private BigDecimal vadesiGecmisToplam;

    /** Vadesi önümüzdeki 30 gün içinde gelecek faturaların toplam kalan tutarı */
    private BigDecimal vadesiYaklasanToplam;

    /** Açık (ödenmemiş) fatura sayısı */
    private int acikFaturaSayisi;

    /** Vadesi geçmiş alacağı olan cari sayısı */
    private int gecikmisCariSayisi;

    /** Cari bazında alacak özetleri */
    private List<CariOzet> cariler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CariOzet {
        private Long cariId;
        private String cariAd;
        private String telefon;
        private String email;
        private BigDecimal toplamAlacak;
        private BigDecimal gecikmisAlacak;
        private int faturaSayisi;
        private int maxGecikmeGunu;
        private String aralik;
        private List<FaturaOzet> faturalar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaturaOzet {
        private Long faturaId;
        private String faturaNumarasi;
        private LocalDate vadeTarihi;
        private BigDecimal kalanTutar;
        private int gecikmeGunu;
    }
}
