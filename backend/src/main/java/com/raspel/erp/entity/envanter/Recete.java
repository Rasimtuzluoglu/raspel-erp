package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recete", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, length = 150)
    private String ad;

    @Column(name = "urun_id")
    private Long urunId;

    @Column(length = 500)
    private String aciklama;

    @Column
    @Builder.Default
    private Boolean aktif = true;

    @Column
    @Builder.Default
    private Integer revizyon = 1;

    @Column(name = "fire_orani", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal fireOrani = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
