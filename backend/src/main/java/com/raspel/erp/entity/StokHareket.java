package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_hareket", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stok_id", nullable = false)
    private Stok stok;

    @Column(nullable = false, length = 20)
    private String tur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "hareket_tarihi", nullable = false)
    private LocalDate hareketTarihi;

    @Column(length = 500)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_hesap_id", nullable = true)
    private CariHesap cariHesap;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() { olusturmaTarihi = LocalDateTime.now(); }
}
