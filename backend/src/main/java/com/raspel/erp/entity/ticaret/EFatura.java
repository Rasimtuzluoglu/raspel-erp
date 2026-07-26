package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "e_fatura", schema = "fatura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EFatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(name = "ettn", nullable = false, unique = true, length = 36)
    private String ettn; // UUID formatında E-Fatura Benzersiz Kimlik

    @Column(name = "fatura_no", nullable = false, length = 50)
    private String faturaNo;

    @Column(name = "senaryo", length = 30)
    private String senaryo; // TICARIFATURA, TEMELFATURA, EARSIVEFATURA

    @Column(name = "tip", length = 30)
    private String tip; // SATIS, IADE, TEVKIFAT, ISTISNA

    @Column(name = "gib_durum_kodu")
    private Integer gibDurumKodu; // 1000: Hazırlandı, 1200: GİB'e Gönderildi, 1300: Onaylandı, 1350: Reddedildi

    @Column(name = "gib_durum_aciklama", length = 500)
    private String gibDurumAciklama;

    @Column(name = "alici_vkn_tckn", length = 20)
    private String aliciVknTckn;

    @Column(name = "alici_unvan", length = 250)
    private String aliciUnvan;

    @Column(name = "odenecek_tutar", precision = 19, scale = 2)
    private BigDecimal odenecekTutar;

    @Column(name = "ubl_xml", columnDefinition = "TEXT")
    private String ublXml;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
