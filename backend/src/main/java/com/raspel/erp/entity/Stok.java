package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stok_kodu", length = 50)
    private String stokKodu;

    @Column(nullable = false, length = 300)
    private String ad;

    @Column(length = 50)
    private String birim;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fiyat;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "min_miktar", precision = 19, scale = 2)
    private BigDecimal minMiktar;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    private BigDecimal kdvOrani;

    @Column(name = "stok_grubu", length = 100)
    private String stokGrubu;

    @Column(length = 100)
    private String barkod;

    @Column(name = "raf_no", length = 50)
    private String rafNo;

    @Column(length = 100)
    private String marka;

    @Column(name = "satis_fiyati", precision = 19, scale = 2)
    private BigDecimal satisFiyati;

    @Column(precision = 10, scale = 2)
    private BigDecimal agirlik;

    @Column(length = 100)
    private String kategori;

    @Column(length = 500)
    private String aciklama;

    @Version
    private Integer version;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (miktar == null) miktar = BigDecimal.ZERO;
        if (fiyat == null) fiyat = BigDecimal.ZERO;
    }
}
