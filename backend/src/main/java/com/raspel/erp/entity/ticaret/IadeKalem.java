package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "iade_kalem", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IadeKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iade_id", nullable = false)
    private Long iadeId;

    @Column(name = "stok_id")
    private Long stokId;

    @Column(length = 300)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(length = 50)
    private String birim;

    @Column(name = "birim_fiyat", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal birimFiyat = BigDecimal.ZERO;

    @Column(name = "kdv_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal kdvOrani = BigDecimal.valueOf(20);

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tutar = BigDecimal.ZERO;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
