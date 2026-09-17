package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Adres defteri kaydı (elektrikçi, tesisatçı vb. hizmet kişileri). */
@Entity
@Table(name = "adres_defteri", schema = "sistem", indexes = {
    @Index(name = "idx_adres_defteri_sirket", columnList = "sirket_id"),
    @Index(name = "idx_adres_defteri_tur", columnList = "sirket_id, tur")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdresDefteri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 200)
    private String ad;

    /** Meslek/tür: Elektrikçi, Tesisatçı, Marangoz vb. (serbest metin). */
    @Column(length = 50)
    private String tur;

    @Column(length = 30)
    private String telefon;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String adres;

    /** Virgülle ayrık etiketler. */
    @Column(length = 500)
    private String etiketler;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        guncellemeTarihi = LocalDateTime.now();
    }
}
