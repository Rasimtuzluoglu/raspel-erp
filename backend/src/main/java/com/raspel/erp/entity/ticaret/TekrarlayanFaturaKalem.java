package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tekrarlayan_fatura_kalem", schema = "fatura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TekrarlayanFaturaKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tekrarlayan_fatura_id", nullable = false)
    private TekrarlayanFatura tekrarlayanFatura;

    @Column(nullable = false, length = 300)
    private String aciklama;

    @Column(nullable = false)
    private Integer adet;

    @Column(name = "birim_fiyat", nullable = false, precision = 19, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    private BigDecimal kdvOrani;

    @Column(name = "iskonto_orani", precision = 5, scale = 2)
    private BigDecimal iskontoOrani;

    @Column(name = "stok_id")
    private Long stokId;
}
