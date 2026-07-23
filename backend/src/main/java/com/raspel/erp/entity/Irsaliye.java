package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "irsaliye", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Irsaliye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "irsaliye_no", nullable = false, unique = true, length = 50)
    private String irsaliyeNo;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(name = "cari_hesap_id", nullable = false)
    private Long cariHesapId;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(nullable = false, length = 10)
    private String tur;

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
        if (tur == null) tur = "SATIS";
    }
}
