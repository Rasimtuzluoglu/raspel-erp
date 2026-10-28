package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.sube.Sube;

@Entity
@Table(name = "cek_senet", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String tur;

    @Column(name = "cari_hesap_id", nullable = true)
    private Long cariHesapId;

    /** Tahsil edilecek fatura (opsiyonel). Verilirse tahsilat bu faturaya bağlanır. */
    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(name = "banka_adi", length = 100)
    private String bankaAdi;

    @Column(name = "sube", length = 100)
    private String sube;

    @Column(name = "cek_no", length = 50)
    private String cekNo;

    @Column(name = "hesap_no", length = 50)
    private String hesapNo;

    @Column(nullable = false)
    private LocalDate vadeTarihi;

    @Column(name = "kesinme_tarihi")
    private LocalDate kesinmeTarihi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(length = 500)
    private String aciklama;

    /** Tahsilde oluşan cari hareket; tahsil iptalinde ters kayıt için saklanır. */
    @Column(name = "tahsilat_hareket_id")
    private Long tahsilatHareketId;

    /** Tahsilde oluşan kasa hareketi. */
    @Column(name = "kasa_hareket_id")
    private Long kasaHareketId;

    /** Tahsilde oluşan banka hareketi. */
    @Column(name = "banka_hareketi_id")
    private Long bankaHareketiId;

    /** Ciro edilen cari (finansal hareket oluşmaz; belge takibi). */
    @Column(name = "ciro_cari_id")
    private Long ciroCariId;

    @Column(name = "ciro_tarihi")
    private LocalDateTime ciroTarihi;

    @Column(name = "ciro_notu", length = 500)
    private String ciroNotu;

    @Column(name = "teminat_tarihi")
    private LocalDateTime teminatTarihi;

    /** Banka/üçüncü taraf teminat açıklaması. */
    @Column(name = "teminat_aciklama", length = 500)
    private String teminatAciklama;

    @Column(name = "protesto_tarihi")
    private LocalDateTime protestoTarihi;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    /** İyimser kilitleme. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = "PORTFOY";
    }
}