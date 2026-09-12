package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * PWA Web Push aboneligi. Bir tarayici/cihaz icin push endpoint'i ve
 * sifreleme anahtarlarini tutar.
 */
@Entity
@Table(name = "push_abonelik", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushAbonelik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(nullable = false, length = 512)
    private String p256dh;

    @Column(nullable = false, length = 512)
    private String auth;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "olusturma_tarihi", nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) {
            olusturmaTarihi = LocalDateTime.now();
        }
    }
}
