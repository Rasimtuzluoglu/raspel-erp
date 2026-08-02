package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "not", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Not {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String baslik;

    @Column(columnDefinition = "TEXT")
    private String icerik;

    @Column(name = "onem_derecesi", length = 20)
    private String onemDerecesi;

    @Column(length = 20)
    private String renk;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (onemDerecesi == null) onemDerecesi = "NORMAL";
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
