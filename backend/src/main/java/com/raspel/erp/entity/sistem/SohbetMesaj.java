package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Ekip içi sohbet mesajı.
 */
@Entity
@Table(name = "sohbet_mesaj", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetMesaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "kullanici_adi", length = 100)
    private String kullaniciAd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
