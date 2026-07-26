package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doviz_kuru", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DovizKuru {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doviz_kodu", nullable = false, length = 10)
    private String dovizKodu; // USD, EUR, GBP

    @Column(name = "doviz_adi", length = 50)
    private String dovizAdi; // ABD Doları, Euro, İngiliz Sterlini

    @Column(name = "tarih", nullable = false)
    private LocalDate tarih;

    @Column(name = "alis_kuru", nullable = false, precision = 19, scale = 4)
    private BigDecimal alisKuru;

    @Column(name = "satis_kuru", nullable = false, precision = 19, scale = 4)
    private BigDecimal satisKuru;

    @Column(name = "efektif_alis", precision = 19, scale = 4)
    private BigDecimal efektifAlis;

    @Column(name = "efektif_satis", precision = 19, scale = 4)
    private BigDecimal efektifSatis;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
