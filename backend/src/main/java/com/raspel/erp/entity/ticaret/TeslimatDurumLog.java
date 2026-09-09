package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Teslimat durum geçişi kaydı (tarihçe).
 */
@Entity
@Table(name = "teslimat_durum_log", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeslimatDurumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teslimat_id", nullable = false)
    private Long teslimatId;

    @Column(name = "onceki_durum", length = 20)
    private String oncekiDurum;

    @Column(name = "yeni_durum", length = 20)
    private String yeniDurum;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
