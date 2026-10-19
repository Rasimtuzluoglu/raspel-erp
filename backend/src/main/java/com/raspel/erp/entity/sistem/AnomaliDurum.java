package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Bir anomali bulgusunun kalıcı inceleme durumu (INCELENDI / YOK_SAYILDI). */
@Entity
@Table(name = "anomali_durum", schema = "sistem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sirket_id", "anahtar"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomaliDurum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** Deterministik anomali anahtarı (tur + ilgili kayıt id'sinden UUID). */
    @Column(nullable = false, length = 64)
    private String anahtar;

    /** INCELENDI | YOK_SAYILDI */
    @Column(nullable = false, length = 20)
    private String durum;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
