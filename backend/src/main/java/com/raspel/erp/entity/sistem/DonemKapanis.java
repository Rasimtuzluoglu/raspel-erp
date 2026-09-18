package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Yıl sonu kapanış kaydı. Bir mali yıl kapatıldığında özet kalıcı olarak saklanır
 * ve ilgili dönem kilitlenir.
 */
@Entity
@Table(name = "donem_kapanis", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonemKapanis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "donem_id")
    private Long donemId;

    @Column(nullable = false)
    private Integer yil;

    @Column(name = "kapanis_tarihi", nullable = false)
    private LocalDateTime kapanisTarihi;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(columnDefinition = "TEXT")
    private String ozet;

    @PrePersist
    protected void onCreate() {
        if (kapanisTarihi == null) kapanisTarihi = LocalDateTime.now();
    }
}
