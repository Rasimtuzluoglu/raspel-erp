package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "teklif_kalem", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeklifKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teklif_id", nullable = false)
    private Long teklifId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(nullable = false, length = 500)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal miktar = BigDecimal.ONE;

    @Column(length = 20)
    @Builder.Default
    private String birim = "Adet";

    @Column(name = "birim_fiyat", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal birimFiyat = BigDecimal.ZERO;

    @Column(name = "iskonto_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal iskontoOrani = BigDecimal.ZERO;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal kdvOrani = new BigDecimal("20");

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tutar = BigDecimal.ZERO;
}
