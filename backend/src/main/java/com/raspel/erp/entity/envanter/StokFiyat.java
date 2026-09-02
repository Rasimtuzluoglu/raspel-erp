package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stok_fiyat", schema = "stok",
        indexes = @Index(name = "idx_stok_fiyat_stok", columnList = "stok_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokFiyat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

    @Column(nullable = false, length = 100)
    private String ad;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fiyat;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (fiyat == null) fiyat = BigDecimal.ZERO;
    }
}
