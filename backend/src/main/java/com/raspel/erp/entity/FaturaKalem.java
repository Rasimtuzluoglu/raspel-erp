package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fatura_kalem", schema = "fatura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;

    @Column(nullable = false, length = 300)
    private String aciklama;

    @Column(nullable = false)
    private Integer adet;

    @Column(name = "birim_fiyat", nullable = false, precision = 19, scale = 2)
    private BigDecimal birimFiyat;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal kdvOrani;

    @Column(name = "iskonto_orani", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal iskontoOrani = BigDecimal.ZERO;

    @Column(name = "tutar", nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
