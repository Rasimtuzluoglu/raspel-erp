package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "onay_ayari", schema = "sistem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sirket_id", "modul"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayAyari {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 50)
    private String modul; // MASRAF, SATINALMA, IZIN

    @Column(name = "esik_tutar", nullable = false, precision = 19, scale = 2)
    private BigDecimal esikTutar;

    @Column(name = "otomatik_onay", nullable = false)
    private Boolean otomatikOnay;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (esikTutar == null) esikTutar = BigDecimal.ZERO;
        if (otomatikOnay == null) otomatikOnay = false;
    }
}
