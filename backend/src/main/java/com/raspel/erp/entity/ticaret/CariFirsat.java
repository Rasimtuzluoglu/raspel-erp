package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cari_firsat", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariFirsat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String ad;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(nullable = false, length = 30)
    private String durum; // YENI, TEMAS, TEKLIF, KAZANILDI, KAYBEDILDI

    @Column(length = 50)
    private String kaynak;

    @Column(precision = 19, scale = 2)
    private BigDecimal deger;

    @Column(name = "tahmini_kapanis")
    private LocalDate tahminiKapanis;

    @Column(length = 1000)
    private String aciklama;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "YENI";
    }
}
