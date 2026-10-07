package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    /** Faz 3.2: oda üyelik rolü (OWNER/ADMIN/MEMBER). */
    @Column(length = 20)
    @Builder.Default
    private String rol = "MEMBER";

    @Column(name = "son_okuma")
    private Instant sonOkuma;

    @Column(name = "eklenme_tarihi", nullable = false)
    private Instant eklenmeTarihi;

    @PrePersist
    protected void onCreate() {
        eklenmeTarihi = Instant.now();
    }
}
