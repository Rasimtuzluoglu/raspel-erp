package com.raspel.erp.entity.sube;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "depo", schema = "sube")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Depo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(length = 500)
    private String adres;

    @Column(length = 100)
    private String yetkili;

    @Column(name = "sube_id", nullable = false)
    private Long subeId;

    /** SKU (kalem) kapasitesi; null ise doluluk hesaplanmaz. */
    @Column(name = "kapasite_kalem")
    private Integer kapasiteKalem;

    /** Depo sorumlusu; null ise stok islem yetkisi ADMIN'dedir. */
    @Column(name = "sorumlu_kullanici_id")
    private Long sorumluKullaniciId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false)
    private Boolean aktif;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
    }
}
