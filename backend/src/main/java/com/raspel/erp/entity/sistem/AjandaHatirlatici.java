package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Kişisel ajanda hatırlatıcısı.
 */
@Entity
@Table(name = "ajanda_hatirlatici", schema = "sistem", indexes = {
    @Index(name = "idx_ajanda_hatirlatici_kullanici", columnList = "kullanici_id"),
    @Index(name = "idx_ajanda_hatirlatici_zaman", columnList = "hatirlatma_zamani")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaHatirlatici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "gorev_id")
    private Long gorevId;

    @Column(nullable = false, length = 200)
    private String baslik;

    @Column(name = "hatirlatma_zamani", nullable = false)
    private LocalDateTime hatirlatmaZamani;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bildirildi = false;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (bildirildi == null) bildirildi = false;
    }
}
