package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cari_fiyat", schema = "cari",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cari_hesap_id", "stok_id"}),
        indexes = @Index(name = "idx_cari_fiyat_cari", columnList = "cari_hesap_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariFiyat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cari_hesap_id", nullable = false)
    private Long cariHesapId;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

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
