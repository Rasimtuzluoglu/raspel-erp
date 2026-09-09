package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POS terminali (kredi kartı tek çekim tahsilatı için). Bir bankaya bağlıdır ve
 * varsayılan bir komisyon oranı taşır.
 */
@Entity
@Table(name = "pos_terminali", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosTerminali {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, length = 150)
    private String ad;

    @Column(name = "banka_id")
    private Long bankaId;

    @Column(name = "komisyon_orani", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal komisyonOrani = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (komisyonOrani == null) komisyonOrani = BigDecimal.ZERO;
        if (aktif == null) aktif = true;
    }
}
