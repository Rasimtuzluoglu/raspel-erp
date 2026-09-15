package com.raspel.erp.entity.envanter;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Üretim emri durum değişim geçmişi (başlat/tamamla/iptal).
 */
@Entity
@Table(name = "uretim_emri_log", schema = "stok")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmriLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uretim_emri_id", nullable = false)
    private Long uretimEmriId;

    @Column(name = "onceki_durum", length = 20)
    private String oncekiDurum;

    @Column(name = "yeni_durum", length = 20)
    private String yeniDurum;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
