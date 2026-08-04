package com.raspel.erp.entity.ik;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personel_izin", schema = "personel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelIzin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personel_id", nullable = false)
    private Long personelId;

    @Column(name = "izin_turu", nullable = false, length = 50)
    private String izinTuru;

    @Column(nullable = false)
    private LocalDate baslangic;

    @Column(nullable = false)
    private LocalDate bitis;

    @Column(name = "gun_sayisi", nullable = false)
    private Integer gunSayisi;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    @Column(length = 100)
    private String onaylayan;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "BEKLEMEDE";
    }
}