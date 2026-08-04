package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sirket", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sirket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(name = "vergi_no", length = 50)
    private String vergiNo;

    @Column(name = "vergi_dairesi", length = 100)
    private String vergiDairesi;

    @Column(length = 500)
    private String adres;

    @Column(length = 20)
    private String telefon;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String webSite;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private Boolean aktif;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "son_ad_guncelleme_tarihi")
    private LocalDateTime sonAdGuncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        sonAdGuncellemeTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
    }
}