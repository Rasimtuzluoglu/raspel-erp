package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proje", schema = "proje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(length = 1000)
    private String aciklama;

    @Column(nullable = false)
    private LocalDate baslangic;

    private LocalDate bitis;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 100)
    private String sorumlu;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "DEVAM_EDIYOR";
    }
}
