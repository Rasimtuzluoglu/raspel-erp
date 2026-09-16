package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fatura işlem geçmişi (birleşik): oluşturma, düzenleme, durum değişikliği,
 * silme ve yazdırma olaylarını tek zaman çizelgesinde tutar.
 */
@Entity
@Table(name = "fatura_gecmis", schema = "fatura", indexes = {
    @Index(name = "idx_fatura_gecmis_fatura", columnList = "fatura_id, tarih"),
    @Index(name = "idx_fatura_gecmis_sirket", columnList = "sirket_id, tarih"),
    @Index(name = "idx_fatura_gecmis_olay", columnList = "olay")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaGecmis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fatura_id", nullable = false)
    private Long faturaId;

    @Column(name = "sirket_id")
    private Long sirketId;

    /** OLUSTUR | GUNCELLE | DURUM | SIL | YAZDIR */
    @Column(nullable = false, length = 20)
    private String olay;

    @Column(length = 500)
    private String aciklama;

    @Column(name = "onceki_deger", columnDefinition = "TEXT")
    private String oncekiDeger;

    @Column(name = "yeni_deger", columnDefinition = "TEXT")
    private String yeniDeger;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "kullanici_adi", length = 100)
    private String kullaniciAdi;

    @Column(name = "ip_adresi", length = 50)
    private String ipAdresi;

    /** Yazdırma biçimi: A4 | A5 | LETTER | TERMAL80 | TERMAL58 | TERMAL */
    @Column(name = "yazdirma_format", length = 20)
    private String yazdirmaFormat;

    @Column(name = "yazici_adi", length = 150)
    private String yaziciAdi;

    @Column(name = "kopya_no")
    private Integer kopyaNo;

    @Column(nullable = false)
    private LocalDateTime tarih;

    @PrePersist
    protected void onCreate() {
        if (tarih == null) tarih = LocalDateTime.now();
    }
}
