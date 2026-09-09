package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kişisel ajanda görevi (task).
 */
@Entity
@Table(name = "ajanda_gorev", schema = "sistem", indexes = {
    @Index(name = "idx_ajanda_gorev_kullanici", columnList = "kullanici_id"),
    @Index(name = "idx_ajanda_gorev_tarih", columnList = "bitis_tarihi")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjandaGorev {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(nullable = false, length = 200)
    private String baslik;

    @Column(length = 1000)
    private String aciklama;

    @Column(name = "bitis_tarihi")
    private LocalDate bitisTarihi;

    @Column(nullable = false, length = 20)
    private String oncelik; // DUSUK | ORTA | YUKSEK

    @Column(nullable = false, length = 20)
    private String durum; // BEKLIYOR | TAMAMLANDI | IPTAL

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (oncelik == null) oncelik = "ORTA";
        if (durum == null) durum = "BEKLIYOR";
    }
}
