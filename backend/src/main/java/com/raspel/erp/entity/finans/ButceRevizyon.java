package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bütçe revizyon/onay snapshot'ı. */
@Entity
@Table(name = "butce_revizyon", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceRevizyon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "butce_id", nullable = false)
    private Long butceId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private Integer revizyon;

    @Column(length = 200)
    private String ad;

    @Column(length = 100)
    private String kategori;

    private Integer yil;
    private Integer ay;

    @Column(precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(length = 10)
    private String tur;

    @Column(length = 20)
    private String durum;

    /** OLSUTUR | GUNCELLE | ONAY | RED */
    @Column(length = 20)
    private String islem;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "degistiren_kullanici_id")
    private Long degistirenKullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
