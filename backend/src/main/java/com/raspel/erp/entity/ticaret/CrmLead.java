package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CRM potansiyel müşteri (lead). Nitelikli hale geldiğinde cari hesaba/fırsata
 * dönüştürülebilir.
 */
@Entity
@Table(name = "crm_lead", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 250)
    private String ad;

    @Column(length = 250)
    private String firma;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefon;

    @Column(length = 50)
    private String kaynak;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String durum = "YENI";

    @Column(nullable = false)
    @Builder.Default
    private Integer skor = 0;

    @Column(name = "tahmini_deger", precision = 19, scale = 2)
    private BigDecimal tahminiDeger;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(name = "firsat_id")
    private Long firsatId;

    @Column(name = "kampanya_id")
    private Long kampanyaId;

    @Column(length = 1000)
    private String aciklama;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "YENI";
        if (skor == null) skor = 0;
    }
}
