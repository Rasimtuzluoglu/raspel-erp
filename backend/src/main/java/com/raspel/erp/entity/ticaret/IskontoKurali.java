package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kademeli fiyat/iskonto kuralı. Kapsam alanları (stok/cari/kategori) ve miktar
 * aralığına göre eşleşen kurallardan en yüksek öncelikli olan uygulanır.
 */
@Entity
@Table(name = "iskonto_kurali", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IskontoKurali {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 150)
    private String ad;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(length = 100)
    private String kategori;

    @Column(name = "min_adet", precision = 19, scale = 4)
    private BigDecimal minAdet;

    @Column(name = "max_adet", precision = 19, scale = 4)
    private BigDecimal maxAdet;

    @Column(name = "iskonto_orani", nullable = false, precision = 9, scale = 2)
    private BigDecimal iskontoOrani;

    @Column(nullable = false)
    @Builder.Default
    private Integer oncelik = 100;

    @Column(name = "gecerli_baslangic")
    private LocalDate gecerliBaslangic;

    @Column(name = "gecerli_bitis")
    private LocalDate gecerliBitis;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
        if (oncelik == null) oncelik = 100;
        if (iskontoOrani == null) iskontoOrani = BigDecimal.ZERO;
    }
}
