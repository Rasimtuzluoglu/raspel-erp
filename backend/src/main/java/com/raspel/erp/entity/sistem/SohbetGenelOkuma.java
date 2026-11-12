package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Genel sohbette kullanıcının son okuduğu mesaj işareti.
 *
 * Faz 9: Okuma durumu tarayıcı localStorage'ında tutuluyordu; cihaz değişince
 * kayboluyor veya tüm geçmiş okunmamış görünüyordu. Artık sunucuda, kullanıcı
 * bazında ve cihazdan bağımsızdır.
 */
@Entity
@Table(name = "sohbet_genel_okuma", schema = "sistem",
        uniqueConstraints = @UniqueConstraint(name = "uq_sohbet_genel_okuma",
                columnNames = {"sirket_id", "kullanici_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetGenelOkuma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "son_okunan_id", nullable = false)
    @Builder.Default
    private Long sonOkunanId = 0L;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private Instant guncellemeTarihi;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = Instant.now();
    }
}
