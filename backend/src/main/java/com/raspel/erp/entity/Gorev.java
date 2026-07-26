package com.raspel.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gorev", schema = "proje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gorev {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proje_id", nullable = false)
    private Long projeId;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(length = 1000)
    private String aciklama;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 100)
    private String atanan;

    @Column
    private LocalDate baslangic;

    @Column
    private LocalDate bitis;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "YAPILACAK";
    }
}
