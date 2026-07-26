package com.raspel.erp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    private Long toplamCariSayisi;
    private BigDecimal toplamBakiye;
    private List<HareketDTO> sonHareketler;

    private Long aktifCalisan;
    private Long bugunIzinli;
    private Long buAyIseBaslayacak;

    private Long bugunkuSiparis;
    private Long bekleyenTeslimat;
    private BigDecimal iadeOrani;

    private BigDecimal stokDevirHizi;
    private List<EnCokSatanDTO> enCokSatanlar;

    private BigDecimal pozitifBakiye;
    private BigDecimal negatifBakiye;

    private BigDecimal bugunkuTahsilat;
    private BigDecimal bugunkuOdeme;
    private Long bekleyenIzinSayisi;
    private List<AylikGelirGiderDTO> aylikGelirGider;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnCokSatanDTO {
        private String stokAd;
        private String stokKodu;
        private BigDecimal satisMiktari;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AylikGelirGiderDTO {
        private String ay;
        private BigDecimal gelir;
        private BigDecimal gider;
    }
}
