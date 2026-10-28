package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Merkezi kategori registry'si. Kasa gelir/gider, stok, masraf ve bütçe
 * kategorileri bu tek tabloda tutulur ve {@code kapsam} ile ayrılır.
 *
 * <p>Hiyerarşi en fazla 2 seviyedir (üst kategori + alt kategori); bu kural
 * DB'de değil servis katmanında uygulanır.
 */
@Entity
@Table(name = "kategori", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kategori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KategoriKapsam kapsam;

    @Column(nullable = false, length = 100)
    private String ad;

    /** Üst kategori (null = kök kategori). En fazla 2 seviye. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ust_kategori_id")
    private Kategori ustKategori;

    /** Sabit paletten hex renk (ör. #2563EB). */
    @Column(length = 20)
    private String renk;

    /** PrimeIcons sınıfı (ör. pi pi-box). */
    @Column(length = 60)
    private String ikon;

    @Column(nullable = false)
    @Builder.Default
    private Integer sira = 0;

    /** Arşiv = false. Pasif kategoriler yeni seçimlerde görünmez. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(length = 255)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (sira == null) sira = 0;
        if (aktif == null) aktif = true;
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
