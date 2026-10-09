package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Sifre kasasi kaydi.
 *
 * <p>Sifre metni DUZ METIN SAKLANMAZ: {@link #sifreCipher} alaninda
 * AES-256-GCM ile sifrelenmis, Base64 kodlanmis [IV + ciphertext] durur.
 * Cozme islemi yalnizca servis katmaninda, erisim kurali dogrulandiktan
 * sonra yapilir.
 *
 * <p>Kapsam/gorunurluk sozlesmesi:
 * <ul>
 *   <li>KISISEL: yalniz kaydi tutan kullanici yazar ve gorur.</li>
 *   <li>GLOBAL + SAHIS: sifreyi yazan kullanici ve ADMIN acar.</li>
 *   <li>GLOBAL + TUMU: herkes acar (ortak/ofis agi erisimi).</li>
 * </ul>
 */
@Entity
@Table(name = "sifre_kasa", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreKasa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** KISISEL kayitlarda dolu, GLOBAL kayitlarda null. */
    @Column(name = "kullanici_id")
    private Long kullaniciId;

    /** KISISEL | GLOBAL */
    @Column(nullable = false, length = 10)
    private String kapsam;

    /** SAHIS | TUMU. Yalniz GLOBAL kapsamda TUMU secilebilir. */
    @Column(name = "sifre_gorunurlugu", nullable = false, length = 10)
    private String sifreGorunurlugu;

    @Column(nullable = false, length = 200)
    private String baslik;

    /** Site/e-posta kullanici adi. */
    @Column(name = "kullanici_adi", length = 200)
    private String kullaniciAdi;

    /** AES-256-GCM sifreli sifre (Base64). Duz metin ASLA saklanmaz. */
    @Column(name = "sifre_cipher", nullable = false, columnDefinition = "TEXT")
    private String sifreCipher;

    @Column(length = 500)
    private String url;

    /** SISTEM | BANKA | EPOSTA | SOSYAL | DIGER */
    @Column(length = 40)
    private String kategori;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    /** Gecerlilik suresi (gun). Null veya 0 -> sifresiz. */
    @Column(name = "gecerlilik_gun")
    private Integer gecerlilikGun;

    /** Sifre metni en son yazildigi an; yalniz sifre alani degisince sifirlanir. */
    @Column(name = "sifre_degisim_tarihi")
    private LocalDateTime sifreDegisimTarihi;

    /** Sifre en son kime gosterildi. */
    @Column(name = "son_goruntuleme")
    private LocalDateTime sonGoruntuleme;

    /** Soft delete. Arsivlenen kayit aktif=false olur ve geri alinabilir. */
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
        if (sifreGorunurlugu == null) sifreGorunurlugu = "SAHIS";
        // Suresi belirtilmis kayitta referans noktasi olustur.
        if (sifreDegisimTarihi == null) sifreDegisimTarihi = olusturmaTarihi;
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }

    /** Global kayit mi? (TUMU gorunurlugu yalniz burada anlamlidir.) */
    public boolean globalMi() {
        return "GLOBAL".equals(kapsam);
    }
}