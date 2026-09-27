package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "masraf", schema = "finans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Masraf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate tarih;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(length = 500)
    private String aciklama;

    @Column(length = 100)
    private String kategori;

    @Column(name = "cari_hesap_id")
    private Long cariHesapId;

    @Column(name = "belge_no", length = 50)
    private String belgeNo;

    /** KDV oranı (%). Tutar KDV DAHİL kabul edilir. */
    @Column(name = "kdv_orani", precision = 5, scale = 2)
    private BigDecimal kdvOrani;

    /** KDV tutarı (tutardan ayrıştırılır). */
    @Column(name = "kdv_tutar", precision = 19, scale = 2)
    private BigDecimal kdvTutar;

    /** KDV hariç matrah. */
    @Column(precision = 19, scale = 2)
    private BigDecimal matrah;

    /** Ödeme yöntemi: NAKIT, HAVALE, KART vb. */
    @Column(name = "odeme_yontemi", length = 20)
    private String odemeYontemi;

    /** Ödeme yapılan kasa (varsa masraf anında çıkış işlenir). */
    @Column(name = "kasa_id")
    private Long kasaId;

    /** Ödeme yapılan banka (varsa masraf anında çıkış işlenir). */
    @Column(name = "banka_id")
    private Long bankaId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    /** İyimser kilitleme. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
    }
}
