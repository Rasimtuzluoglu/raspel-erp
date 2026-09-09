package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sohbet odası üyeliği.
 */
@Entity
@Table(name = "sohbet_oda_uye", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetOdaUye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oda_id", nullable = false)
    private Long odaId;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "son_okuma")
    private LocalDateTime sonOkuma;

    @Column(name = "eklenme_tarihi", nullable = false)
    private LocalDateTime eklenmeTarihi;

    @PrePersist
    protected void onCreate() {
        eklenmeTarihi = LocalDateTime.now();
    }
}
