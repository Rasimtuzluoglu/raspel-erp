package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Çek/senet durum geçmişi. Her durum geçişi (ve tahsil/ciro/teminat/protesto
 * işlemi) burada denetim izi olarak saklanır.
 *
 * <p>Kayıt silinirse logları DB seviyesinde {@code ON DELETE CASCADE} ile
 * silinir (V171).
 */
@Entity
@Table(name = "cek_senet_durum_log", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetDurumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cek_senet_id", nullable = false)
    private Long cekSenetId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "eski_durum", length = 30)
    private String eskiDurum;

    @Column(name = "yeni_durum", length = 30)
    private String yeniDurum;

    /** OLUSTUR, GUNCELLE, TEMINAT, TEMINAT_IADE, CIRO, CIRO_IADE, IADE_AL, TAHSIL, TAHSIL_IPTAL, PROTESTO. */
    @Column(nullable = false, length = 30)
    private String islem;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
