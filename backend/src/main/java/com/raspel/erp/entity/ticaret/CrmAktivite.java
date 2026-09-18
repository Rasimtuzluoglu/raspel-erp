package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CRM aktivitesi: arama, toplantı, e-posta, not veya görev. Bir cari, fırsat
 * veya lead ile ilişkilendirilebilir.
 */
@Entity
@Table(name = "crm_aktivite", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmAktivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(nullable = false, length = 30)
    private String tur;

    @Column(nullable = false, length = 250)
    private String baslik;

    @Column(length = 2000)
    private String aciklama;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(name = "firsat_id")
    private Long firsatId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "planlanan_tarih")
    private LocalDateTime planlananTarih;

    @Column(nullable = false)
    @Builder.Default
    private Boolean tamamlandi = false;

    @Column(name = "tamamlanma_tarihi")
    private LocalDateTime tamamlanmaTarihi;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (tamamlandi == null) tamamlandi = false;
        if (tur == null) tur = "NOT";
    }
}
