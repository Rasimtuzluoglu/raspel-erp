package com.raspel.erp.entity.ik;

import com.raspel.erp.entity.ik.Personel;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "maas_bordro", schema = "ik")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaasBordro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personel_id", nullable = false)
    private Personel personel;

    @Column(nullable = false)
    private Integer yil;

    @Column(nullable = false)
    private Integer ay;

    @Column(name = "brut_maas", nullable = false, precision = 19, scale = 2)
    private BigDecimal brutMaas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal kesintiler;

    @Column(name = "net_maas", nullable = false, precision = 19, scale = 2)
    private BigDecimal netMaas;

    @Column(name = "odeme_tarihi")
    private LocalDate odemeTarihi;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    /** Bordro durumu: TASLAK | ONAYLANDI. ONAYLANDI sonrası düzenleme/silme kilitlenir. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String durum = "TASLAK";

    @Column(name = "onay_tarihi")
    private LocalDateTime onayTarihi;

    @Column(name = "onaylayan", length = 100)
    private String onaylayan;

    /** Odeme durumu: ODENMEDI | ODENDI. Onaydan bagimsizdir; cift odemeyi engeller. */
    @Column(name = "odeme_durumu", nullable = false, length = 20)
    @Builder.Default
    private String odemeDurumu = "ODENMEDI";

    @Column(name = "odeme_kasa_id")
    private Long odemeKasaId;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (kesintiler == null) kesintiler = BigDecimal.ZERO;
        if (durum == null) durum = "TASLAK";
        if (odemeDurumu == null) odemeDurumu = "ODENMEDI";
    }
}
