package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personel", schema = "personel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ad;

    @Column(nullable = false, length = 50)
    private String soyad;

    @Column(name = "tc_kimlik", length = 11, unique = true)
    private String tcKimlik;

    @Column(name = "dogum_tarihi")
    private LocalDate dogumTarihi;

    @Column(name = "ise_giris_tarihi")
    private LocalDate iseGirisTarihi;

    @Column(name = "cikis_tarihi")
    private LocalDate cikisTarihi;

    @Column(length = 100)
    private String departman;

    @Column(length = 100)
    private String pozisyon;

    @Column(precision = 19, scale = 2)
    private BigDecimal maas;

    @Column(length = 20)
    private String telefon;

    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String adres;

    @Column(nullable = false)
    private Boolean aktif;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
    }
}
