package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "satinalma_siparis", schema = "satinalma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatinalmaSiparis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "siparis_no", nullable = false, unique = true, length = 50)
    private String siparisNo;

    @Column(name = "tarih", nullable = false)
    private LocalDate tarih;

    @Column(name = "cari_hesap_id", nullable = false)
    private Long cariHesapId;

    @Column(name = "talep_id")
    private Long talepId;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(name = "ara_toplam", precision = 19, scale = 2)
    private BigDecimal araToplam;

    @Column(precision = 19, scale = 2)
    private BigDecimal kdv;

    @Column(name = "genel_toplam", precision = 19, scale = 2)
    private BigDecimal genelToplam;

    @Column(length = 500)
    private String aciklama;

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