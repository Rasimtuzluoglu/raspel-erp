package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hareket Entity
 * Cari hesaplara yapılan tahsilat veya ödeme işlemlerini temsil eder.
 */
@Entity
@Table(name = "hareket", schema = "cari")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hareket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** İlgili Cari Hesap */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_hesap_id", nullable = false)
    private CariHesap cariHesap;
    
    /** Hareket türü: TAHSILAT veya ODEME */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private HareketTuru tur;
    
    /** Hareket tutarı (her zaman pozitif, işaret Hareket Türü ile belirlenir) */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;
    
    /** Hareket tarihi */
    @Column(nullable = false)
    private LocalDate hareketTarihi;
    
    /** Hareket açıklaması */
    @Column(length = 500)
    private String aciklama;

    @Column(name = "odeme_sekli", length = 20)
    private String odemeSekli;

    /** Ödeme yöntemi: NAKIT, KART, TAKSIT, HAVALE */
    @Column(name = "odeme_yontemi", length = 20)
    private String odemeYontemi;

    /** Taksit çekilen banka / finans kurumu adı */
    @Column(name = "taksit_kurum", length = 255)
    private String taksitKurum;

    /** Taksit olarak çekilen tutar */
    @Column(name = "taksit_tutar", precision = 19, scale = 2)
    private BigDecimal taksitTutar;

    /** POS terminali ID (kart tek çekim) */
    @Column(name = "pos_terminali_id")
    private Long posTerminaliId;

    /** POS terminali adı (görüntüleme için denormalize) */
    @Column(name = "pos_ad", length = 150)
    private String posAd;

    /** Kart komisyon tutarı */
    @Column(name = "komisyon_tutar", precision = 19, scale = 2)
    private BigDecimal komisyonTutar;

    /** Valör (bankaya geçiş) tarihi */
    @Column(name = "valor_tarihi")
    private LocalDate valorTarihi;

    /** Bağlı fatura (opsiyonel). Tahsilat/ödeme faturaya işlenince fatura ödeme durumu güncellenir. */
    @Column(name = "fatura_id")
    private Long faturaId;
    
    @Column(name = "sirket_id")
    private Long sirketId;

    /** Oluşturma tarihi */
    @Column(nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;
    
    @PrePersist
    protected void onCreate() {
        this.olusturmaTarihi = LocalDateTime.now();
    }
    
    /**
     * Hareket türünü temsil eden enum
     */
    public enum HareketTuru {
        TAHSILAT("Tahsilat"),
        ODEME("Odeme");

        private final String goruntulemeAdi;

        HareketTuru(String goruntulemeAdi) {
            this.goruntulemeAdi = goruntulemeAdi;
        }

        public String getGoruntulemeAdi() {
            return goruntulemeAdi;
        }
    }
}