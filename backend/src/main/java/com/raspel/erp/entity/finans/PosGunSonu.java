package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_gun_sonu", schema = "muhasebe",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pos_id", "tarih"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosGunSonu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pos_id", nullable = false)
    private Long posId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal komisyon;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (tutar == null) tutar = BigDecimal.ZERO;
        if (komisyon == null) komisyon = BigDecimal.ZERO;
    }
}
