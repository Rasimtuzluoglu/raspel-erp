package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cari Hesap Entity
 * Muhasebede yer alan müşteri veya tedarikçi hesaplarını temsil eder.
 */
@Entity
@Table(name = "cari_hesap", schema = "cari", indexes = {
    @Index(name = "idx_cari_hesap_sirket", columnList = "sirket_id"),
    @Index(name = "idx_cari_hesap_ad", columnList = "ad")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariHesap {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Cari hesap adı */
    @Column(nullable = false, length = 255)
    private String ad;
    
    /** Vergi numarası (T.C. Vergi Numarası veya Vergi Kimlik Numarası) */
    @Column(length = 50)
    private String vergiNumarasi;
    
    /** Telefon numarası */
    @Column(length = 20)
    private String telefon;

    /** E-posta adresi */
    @Column(length = 100)
    private String email;

    /** Fiziksel adres */
    @Column(length = 500)
    private String adres;

    @Column(name = "tur", length = 20)
    private String tur;

    @Column(length = 50)
    private String il;

    @Column(length = 50)
    private String ilce;

    @Column(name = "vergi_dairesi", length = 100)
    private String vergiDairesi;

    @Column(name = "yetkili_kisi", length = 100)
    private String yetkiliKisi;

    @Column(name = "yetkili_telefon", length = 20)
    private String yetkiliTelefon;

    @Column(length = 50)
    private String iban;

    @Column(length = 1000)
    private String notlar;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private Boolean aktif;

    /** Kredi limiti (TL) */
    @Column(name = "kredi_limiti", precision = 19, scale = 2)
    private BigDecimal krediLimiti;

    /** Ödeme vadesi (gün) */
    @Column(name = "odeme_vadesi")
    private Integer odemeVadesi;

    /** Cari hesabın bakiyesi (Alacak pozitif, Borç negatif) */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal bakiye;
    
    @Column(name = "sirket_id")
    private Long sirketId;

    /** Oluşturma tarihi */
    @Column(nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;
    
    /** Son güncelleme tarihi */
    @Column(nullable = false)
    private LocalDateTime guncellemeTarihi;
    
    @PrePersist
    protected void onCreate() {
        this.olusturmaTarihi = LocalDateTime.now();
        this.guncellemeTarihi = LocalDateTime.now();
        if (this.bakiye == null) {
            this.bakiye = BigDecimal.ZERO;
        }
        if (this.aktif == null) {
            this.aktif = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.guncellemeTarihi = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAd() { return ad; }
    public void setAd(String ad) { this.ad = ad; }
    public String getVergiNumarasi() { return vergiNumarasi; }
    public void setVergiNumarasi(String vergiNumarasi) { this.vergiNumarasi = vergiNumarasi; }
    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAdres() { return adres; }
    public void setAdres(String adres) { this.adres = adres; }
    public String getTur() { return tur; }
    public void setTur(String tur) { this.tur = tur; }
    public String getIl() { return il; }
    public void setIl(String il) { this.il = il; }
    public String getIlce() { return ilce; }
    public void setIlce(String ilce) { this.ilce = ilce; }
    public String getVergiDairesi() { return vergiDairesi; }
    public void setVergiDairesi(String vergiDairesi) { this.vergiDairesi = vergiDairesi; }
    public String getYetkiliKisi() { return yetkiliKisi; }
    public void setYetkiliKisi(String yetkiliKisi) { this.yetkiliKisi = yetkiliKisi; }
    public String getYetkiliTelefon() { return yetkiliTelefon; }
    public void setYetkiliTelefon(String yetkiliTelefon) { this.yetkiliTelefon = yetkiliTelefon; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getNotlar() { return notlar; }
    public void setNotlar(String notlar) { this.notlar = notlar; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Boolean getAktif() { return aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }
    public BigDecimal getKrediLimiti() { return krediLimiti; }
    public void setKrediLimiti(BigDecimal krediLimiti) { this.krediLimiti = krediLimiti; }
    public Integer getOdemeVadesi() { return odemeVadesi; }
    public void setOdemeVadesi(Integer odemeVadesi) { this.odemeVadesi = odemeVadesi; }
    public BigDecimal getBakiye() { return bakiye; }
    public void setBakiye(BigDecimal bakiye) { this.bakiye = bakiye; }
    public Long getSirketId() { return sirketId; }
    public void setSirketId(Long sirketId) { this.sirketId = sirketId; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public void setOlusturmaTarihi(LocalDateTime olusturmaTarihi) { this.olusturmaTarihi = olusturmaTarihi; }
    public LocalDateTime getGuncellemeTarihi() { return guncellemeTarihi; }
    public void setGuncellemeTarihi(LocalDateTime guncellemeTarihi) { this.guncellemeTarihi = guncellemeTarihi; }
}