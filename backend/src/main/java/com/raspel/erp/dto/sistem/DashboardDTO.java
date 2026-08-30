package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.raspel.erp.dto.finans.HareketDTO;

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
    private List<GunlukNakitAkisiDTO> gunlukNakitAkisi;
    private List<VadeBildirimiDTO> vadesiGecenFaturalar;
    private List<VadeBildirimiDTO> vadesiYaklasanFaturalar;

    private Long toplamStok;
    private Long kritikStokSayisi;
    private List<KritikStokDTO> kritikStoklar;
    private Long toplamFatura;
    private Long kesilenFatura;
    private BigDecimal toplamBankaBakiye;
    private BigDecimal toplamKasaBakiye;

    private BigDecimal hedefCiro;
    private BigDecimal gerceklesenCiro;
    private BigDecimal ciroIlerlemeYuzdesi;
    private BigDecimal hedefKar;
    private BigDecimal gerceklesenKar;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KritikStokDTO {
        private Long id;
        private String stokKodu;
        private String ad;
        private BigDecimal miktar;
        private String birim;
        private BigDecimal minMiktar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VadeBildirimiDTO {
        private Long faturaId;
        private String faturaNumarasi;
        private String cariHesapAd;
        private String cariTelefon;
        private LocalDate vadeTarihi;
        private BigDecimal kalanTutar;
    }

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GunlukNakitAkisiDTO {
        private String gun;
        private BigDecimal gelir;
        private BigDecimal gider;
    }
}