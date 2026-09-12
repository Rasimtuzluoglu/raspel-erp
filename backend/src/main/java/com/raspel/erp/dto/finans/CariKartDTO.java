package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Musteri 360 karti: cari bilgisi + kredi durumu + ticari ozet + son hareketler.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariKartDTO {

    private Long cariId;
    private String cariAd;
    private String telefon;
    private String email;
    private String tur;
    private Boolean aktif;
    private Long temsilciId;
    private String temsilciAd;

    private KrediDurumu kredi;
    private Ozet ozet;
    private List<FaturaOzet> sonFaturalar;
    private List<SiparisOzet> sonSiparisler;
    private List<IadeOzet> sonIadeler;
    private List<FirsatOzet> firsatlar;
    private List<NotOzet> notlar;
    private List<FiyatOzet> ozelFiyatlar;
    private List<TaksitOzet> taksitler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KrediDurumu {
        private BigDecimal krediLimiti;
        private BigDecimal bakiye;
        private BigDecimal kullanilabilirKredi;
        private BigDecimal riskOrani;
        private boolean limitAsimi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ozet {
        private long siparisSayisi;
        private BigDecimal siparisToplam;
        private long faturaSayisi;
        private BigDecimal faturaToplam;
        private BigDecimal kalanTutar;
        private long iadeSayisi;
        private BigDecimal iadeToplam;
        private BigDecimal tahsilatToplam;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaturaOzet {
        private Long id;
        private String faturaNumarasi;
        private LocalDate tarih;
        private String tur;
        private String durum;
        private String odemeDurumu;
        private BigDecimal genelToplam;
        private BigDecimal kalanTutar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SiparisOzet {
        private Long id;
        private String siparisNo;
        private LocalDate tarih;
        private String durum;
        private BigDecimal genelToplam;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IadeOzet {
        private Long id;
        private String tur;
        private LocalDate tarih;
        private BigDecimal tutar;
        private String durum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirsatOzet {
        private Long id;
        private String ad;
        private String durum;
        private String kaynak;
        private BigDecimal deger;
        private LocalDate tahminiKapanis;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotOzet {
        private Long id;
        private String baslik;
        private String icerik;
        private String onemDerecesi;
        private LocalDateTime olusturmaTarihi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FiyatOzet {
        private Long id;
        private Long stokId;
        private String stokAd;
        private String stokKodu;
        private BigDecimal fiyat;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaksitOzet {
        private Long id;
        private String planNo;
        private Integer taksitNo;
        private Integer taksitSayisi;
        private String kurum;
        private LocalDate vadeTarihi;
        private BigDecimal tutar;
        private String odemeDurumu;
        private long gecikmeGunu;
    }
}
