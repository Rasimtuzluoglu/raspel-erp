package com.raspel.erp.entity.muhasebe;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** İrsaliye durum geçişi / işlem kaydı. */
@Entity
@Table(name = "irsaliye_durum_log", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeDurumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "irsaliye_id", nullable = false)
    private Long irsaliyeId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "eski_durum", length = 20)
    private String eskiDurum;

    @Column(name = "yeni_durum", nullable = false, length = 20)
    private String yeniDurum;

    /** OLUSTUR | HAZIRLA | SEVK | TESLIM | SEVKI_GERI_AL | IPTAL | FATURA */
    @Column(nullable = false, length = 20)
    private String islem;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
