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
}