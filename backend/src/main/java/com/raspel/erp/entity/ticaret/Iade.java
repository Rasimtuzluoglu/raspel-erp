package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "iade", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Iade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(length = 500)
    private String aciklama;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "TASLAK";
    }
}
