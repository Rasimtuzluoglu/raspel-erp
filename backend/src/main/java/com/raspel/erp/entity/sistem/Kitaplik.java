package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Kitaplik kaydi: yer imi (URL) veya snippet (metin).
 *
 * <p>Kapsam/gorunurluk sozlesmesi (sifre kasasi ile ayni):
 * <ul>
 *   <li>KISISEL: yalniz kaydi tutan kullanici yazar ve gorur.</li>
 *   <li>GLOBAL + SAHIS: yalnizca ADMIN gorur (yonetici ozel notu).</li>
 *   <li>GLOBAL + TUMU: sirketteki herkes gorur (paylasimli kitaplik).</li>
 * </ul>
 * Yalniz GLOBAL kapsami ADMIN yazabilir.
 */
@Entity
@Table(name = "kitaplik", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kitaplik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** KISISEL kayitlarda dolu, GLOBAL kayitlarda null. */
    @Column(name = "kullanici_id")
    private Long kullaniciId;

    /** YER_IMI | SNIPPET */
    @Column(nullable = false, length = 10)
    private String tur;

    /** KISISEL | GLOBAL */
    @Column(nullable = false, length = 10)
    private String kapsam;

    /** SAHIS | TUMU. Yalniz GLOBAL kapsamda TUMU secilebilir. */
    @Column(nullable = false, length = 10)
    private String gorunurluk;

    @Column(nullable = false, length = 200)
    private String baslik;

    /** Yer imi adresi (tur=YER_IMI). */
    @Column(length = 1000)
    private String url;

    /** Snippet icerigi (tur=SNIPPET). */
    @Column(columnDefinition = "TEXT")
    private String icerik;

    /** SISTEM | ARAC | DOKUMAN | KOD | DIGER */
    @Column(length = 40)
    private String kategori;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Column(name = "arsiv_tarihi")
    private LocalDateTime arsivTarihi;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
        if (kapsam == null) kapsam = "KISISEL";
        if (gorunurluk == null) gorunurluk = "SAHIS";
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }

    public boolean globalMi() {
        return "GLOBAL".equals(kapsam);
    }
}