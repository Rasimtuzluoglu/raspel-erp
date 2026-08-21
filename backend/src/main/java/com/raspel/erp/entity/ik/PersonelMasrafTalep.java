package com.raspel.erp.entity.ik;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personel_masraf_talep", schema = "ik")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelMasrafTalep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personel_id")
    private Long personelId;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tur = "MASRAF"; // MASRAF, AVANS

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String kategori = "DIGER"; // YAKIT, YEMEK, KONAKLAMA, ULASIM, MALZEME, DIGER

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tutar = BigDecimal.ZERO;

    @Column(name = "para_birimi", length = 10)
    @Builder.Default
    private String paraBirimi = "TRY";

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, length = 500)
    private String aciklama;

    @Column(name = "belge_url", columnDefinition = "TEXT")
    private String belgeUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String durum = "BEKLEMEDE"; // BEKLEMEDE, ONAYLANDI, REDDEDILDI

    @Column(length = 100)
    private String onaylayan;

    @Column(name = "onay_notu", length = 500)
    private String onayNotu;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        guncellemeTarihi = LocalDateTime.now();
        if (durum == null) durum = "BEKLEMEDE";
        if (tur == null) tur = "MASRAF";
        if (kategori == null) kategori = "DIGER";
        if (paraBirimi == null) paraBirimi = "TRY";
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
