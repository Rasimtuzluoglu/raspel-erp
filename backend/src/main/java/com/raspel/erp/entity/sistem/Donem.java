package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donem", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 100)
    private String ad;

    @Column(name = "baslangic", nullable = false)
    private LocalDate baslangic;

    @Column(name = "bitis", nullable = false)
    private LocalDate bitis;

    @Column(nullable = false)
    private Boolean aktif;

    @Column(nullable = false)
    @Builder.Default
    private Boolean kilitli = false;

    @Column(name = "kilit_tarihi")
    private LocalDateTime kilitTarihi;

    @Column(name = "kilit_kullanici_id")
    private Long kilitKullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (aktif == null) aktif = true;
        if (kilitli == null) kilitli = false;
    }
}