package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ekip içi grup sohbet odası.
 */
@Entity
@Table(name = "sohbet_oda", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetOda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, length = 100)
    private String ad;

    @Column(length = 300)
    private String aciklama;

    @Column(name = "olusturan_kullanici_id")
    private Long olusturanKullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
