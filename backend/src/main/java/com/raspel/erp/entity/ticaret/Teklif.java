package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teklif", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teklif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teklif_no", nullable = false, length = 50)
    private String teklifNo;

    @Column(name = "revizyon_no", nullable = false)
    @Builder.Default
    private Integer revizyonNo = 0;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "gecerlilik_tarihi")
    private LocalDate gecerlilikTarihi;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(length = 20)
    @Builder.Default
    private String tur = "SATIS";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String durum = "TASLAK";

    @Column(name = "ara_toplam", precision = 19, scale = 2)
    private BigDecimal araToplam;

    @Column(precision = 19, scale = 2)
    private BigDecimal kdv;

    @Column(name = "iskonto_orani", precision = 5, scale = 2)
    private BigDecimal iskontoOrani;

    @Column(name = "iskonto_tutari", precision = 19, scale = 2)
    private BigDecimal iskontoTutari;

    @Column(name = "genel_toplam", precision = 19, scale = 2)
    private BigDecimal genelToplam;

    @Column(name = "para_birimi", length = 10)
    @Builder.Default
    private String paraBirimi = "TRY";

    @Column(name = "teslimat_sarti", length = 255)
    private String teslimatSarti;

    @Column(name = "odeme_sarti", length = 255)
    private String odemeSarti;

    @Column(name = "garanti_sarti", length = 255)
    private String garantiSarti;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        guncellemeTarihi = LocalDateTime.now();
        if (durum == null) durum = "TASLAK";
        if (tur == null) tur = "SATIS";
        if (revizyonNo == null) revizyonNo = 0;
        if (paraBirimi == null) paraBirimi = "TRY";
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTeklifNo() { return teklifNo; }
    public void setTeklifNo(String teklifNo) { this.teklifNo = teklifNo; }
    public Integer getRevizyonNo() { return revizyonNo; }
    public void setRevizyonNo(Integer revizyonNo) { this.revizyonNo = revizyonNo; }
    public LocalDate getTarih() { return tarih; }
    public void setTarih(LocalDate tarih) { this.tarih = tarih; }
    public LocalDate getGecerlilikTarihi() { return gecerlilikTarihi; }
    public void setGecerlilikTarihi(LocalDate gecerlilikTarihi) { this.gecerlilikTarihi = gecerlilikTarihi; }
    public Long getCariHesapId() { return cariHesapId; }
    public void setCariHesapId(Long cariHesapId) { this.cariHesapId = cariHesapId; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public String getDurum() { return durum; }
    public void setDurum(String durum) { this.durum = durum; }
    public BigDecimal getAraToplam() { return araToplam; }
    public void setAraToplam(BigDecimal araToplam) { this.araToplam = araToplam; }
    public BigDecimal getKdv() { return kdv; }
    public void setKdv(BigDecimal kdv) { this.kdv = kdv; }
    public BigDecimal getIskontoOrani() { return iskontoOrani; }
    public void setIskontoOrani(BigDecimal iskontoOrani) { this.iskontoOrani = iskontoOrani; }
    public BigDecimal getIskontoTutari() { return iskontoTutari; }
    public void setIskontoTutari(BigDecimal iskontoTutari) { this.iskontoTutari = iskontoTutari; }
    public BigDecimal getGenelToplam() { return genelToplam; }
    public void setGenelToplam(BigDecimal genelToplam) { this.genelToplam = genelToplam; }
    public String getParaBirimi() { return paraBirimi; }
    public void setParaBirimi(String paraBirimi) { this.paraBirimi = paraBirimi; }
    public String getTeslimatSarti() { return teslimatSarti; }
    public void setTeslimatSarti(String teslimatSarti) { this.teslimatSarti = teslimatSarti; }
    public String getOdemeSarti() { return odemeSarti; }
    public void setOdemeSarti(String odemeSarti) { this.odemeSarti = odemeSarti; }
    public String getGarantiSarti() { return garantiSarti; }
    public void setGarantiSarti(String garantiSarti) { this.garantiSarti = garantiSarti; }
    public String getNotlar() { return notlar; }
    public void setNotlar(String notlar) { this.notlar = notlar; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
    public LocalDateTime getGuncellemeTarihi() { return guncellemeTarihi; }
    public void setGuncellemeTarihi(LocalDateTime guncellemeTarihi) { this.guncellemeTarihi = guncellemeTarihi; }

    public static TeklifBuilder builder() {
        return new TeklifBuilder();
    }

    public static class TeklifBuilder {
        private Long id;
        private String teklifNo;
        private Integer revizyonNo = 0;
        private LocalDate tarih;
        private LocalDate gecerlilikTarihi;
        private Long cariHesapId;
        private String tur = "SATIS";
        private String durum = "TASLAK";
        private BigDecimal araToplam;
        private BigDecimal kdv;
        private BigDecimal iskontoOrani;
        private BigDecimal iskontoTutari;
        private BigDecimal genelToplam;
        private String paraBirimi = "TRY";
        private String teslimatSarti;
        private String odemeSarti;
        private String garantiSarti;
        private String notlar;
        private Long sirketId;
        private LocalDateTime olusturmaTarihi;
        private LocalDateTime guncellemeTarihi;

        TeklifBuilder() {}

        public TeklifBuilder id(Long id) { this.id = id; return this; }
        public TeklifBuilder teklifNo(String teklifNo) { this.teklifNo = teklifNo; return this; }
        public TeklifBuilder revizyonNo(Integer revizyonNo) { this.revizyonNo = revizyonNo; return this; }
        public TeklifBuilder tarih(LocalDate tarih) { this.tarih = tarih; return this; }
        public TeklifBuilder gecerlilikTarihi(LocalDate gecerlilikTarihi) { this.gecerlilikTarihi = gecerlilikTarihi; return this; }
        public TeklifBuilder cariHesapId(Long cariHesapId) { this.cariHesapId = cariHesapId; return this; }
        public TeklifBuilder tur(String tur) { this.tur = tur; return this; }
        public TeklifBuilder durum(String durum) { this.durum = durum; return this; }
        public TeklifBuilder araToplam(BigDecimal araToplam) { this.araToplam = araToplam; return this; }
        public TeklifBuilder kdv(BigDecimal kdv) { this.kdv = kdv; return this; }
        public TeklifBuilder iskontoOrani(BigDecimal iskontoOrani) { this.iskontoOrani = iskontoOrani; return this; }
        public TeklifBuilder iskontoTutari(BigDecimal iskontoTutari) { this.iskontoTutari = iskontoTutari; return this; }
        public TeklifBuilder genelToplam(BigDecimal genelToplam) { this.genelToplam = genelToplam; return this; }
        public TeklifBuilder paraBirimi(String paraBirimi) { this.paraBirimi = paraBirimi; return this; }
        public TeklifBuilder teslimatSarti(String teslimatSarti) { this.teslimatSarti = teslimatSarti; return this; }
        public TeklifBuilder odemeSarti(String odemeSarti) { this.odemeSarti = odemeSarti; return this; }
        public TeklifBuilder garantiSarti(String garantiSarti) { this.garantiSarti = garantiSarti; return this; }
        public TeklifBuilder notlar(String notlar) { this.notlar = notlar; return this; }
        public TeklifBuilder sirketId(Long sirketId) { this.sirketId = sirketId; return this; }
        public TeklifBuilder olusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; return this; }
        public TeklifBuilder guncellemeTarihi(LocalDateTime guncellemeTarihi) { this.guncellemeTarihi = guncellemeTarihi; return this; }

        public Teklif build() {
            return new Teklif(id, teklifNo, revizyonNo, tarih, gecerlilikTarihi, cariHesapId, tur, durum, araToplam, kdv, iskontoOrani, iskontoTutari, genelToplam, paraBirimi, teslimatSarti, odemeSarti, garantiSarti, notlar, sirketId, olusturmaTarihi, guncellemeTarihi);
        }
    }
}
