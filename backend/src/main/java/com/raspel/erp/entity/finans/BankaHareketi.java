package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "banka_hareketi", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankaHareketi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "banka_id")
    private Long bankaId;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(length = 500)
    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal borc;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal alacak;

    @Column(precision = 19, scale = 2)
    private BigDecimal bakiye;

    @Column(name = "eslesen_fatura_id")
    private Long eslesenFaturaId;

    @Column(nullable = false)
    private Boolean eslestirildi;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (eslestirildi == null) eslestirildi = false;
        if (borc == null) borc = BigDecimal.ZERO;
        if (alacak == null) alacak = BigDecimal.ZERO;
    }
}
