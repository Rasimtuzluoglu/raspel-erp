package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sirket_hedef", schema = "sistem",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"sirket_id", "yil", "ay"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SirketHedef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private Integer yil;

    @Column(nullable = false)
    private Integer ay;

    @Column(name = "hedef_ciro", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hedefCiro = BigDecimal.ZERO;

    @Column(name = "hedef_kar", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hedefKar = BigDecimal.ZERO;

    @Column(name = "hedef_yeni_musteri")
    @Builder.Default
    private Integer hedefYeniMusteri = 0;

    @Column(name = "hedef_satis_adedi")
    @Builder.Default
    private Integer hedefSatisAdedi = 0;

    @Column(length = 500)
    private String notlar;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
