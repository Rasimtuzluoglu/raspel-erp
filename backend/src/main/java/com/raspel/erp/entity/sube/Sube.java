package com.raspel.erp.entity.sube;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sube", schema = "sube")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String ad;

    @Column(length = 500)
    private String adres;

    @Column(length = 20)
    private String telefon;

    @Column(length = 100)
    private String yetkili;

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
