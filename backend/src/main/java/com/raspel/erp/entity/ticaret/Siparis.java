package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "siparis", schema = "siparis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Siparis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "siparis_no", nullable = false, unique = true, length = 50)
    private String siparisNo;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "cari_hesap_id", nullable = true)
    private Long cariHesapId;

    @Column(length = 20)
    private String tur;

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
        if (durum == null) durum = "TEKLIF";
        if (tur == null) tur = "SATIS";
    }
}