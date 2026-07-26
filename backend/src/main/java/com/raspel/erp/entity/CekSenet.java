package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cek_senet", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(name = "cari_hesap_id", nullable = true)
    private Long cariHesapId;

    @Column(name = "banka_adi", length = 100)
    private String bankaAdi;

    @Column(name = "sube", length = 100)
    private String sube;

    @Column(name = "cek_no", length = 50)
    private String cekNo;

    @Column(name = "hesap_no", length = 50)
    private String hesapNo;

    @Column(nullable = false)
    private LocalDate vadeTarihi;

    @Column(name = "kesinme_tarihi")
    private LocalDate kesinmeTarihi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "PORTFOY";
    }
}
