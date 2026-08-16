package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Sunucu hatası kaydı. Hata tespiti ve müdahale için saklanır.
 */
@Entity
@Table(name = "hata_log", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HataLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(length = 200)
    private String tur;

    @Column(columnDefinition = "TEXT")
    private String mesaj;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
