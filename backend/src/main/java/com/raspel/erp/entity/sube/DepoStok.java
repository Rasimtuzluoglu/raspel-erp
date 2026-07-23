package com.raspel.erp.entity.sube;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "depo_stok", schema = "sube", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"depo_id", "stok_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoStok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "depo_id", nullable = false)
    private Long depoId;

    @Column(name = "stok_id", nullable = false)
    private Long stokId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal miktar;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (miktar == null) miktar = BigDecimal.ZERO;
    }
}
