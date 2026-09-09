package com.raspel.erp.entity.ticaret;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Teslimat kaydı. Bir satış (fatura) için atanan şoför ve teslimat adresi bilgisini tutar.
 */
@Entity
@Table(name = "teslimat", schema = "ticaret")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teslimat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id")
    private Long sirketId;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(name = "fatura_numarasi", length = 100)
    private String faturaNumarasi;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "teslimat_adresi", columnDefinition = "TEXT")
    private String teslimatAdresi;

    @Column(name = "beklenen_teslim_tarihi")
    private LocalDate beklenenTeslimTarihi;

    @Column(name = "teslimat_foto", columnDefinition = "TEXT")
    private String teslimatFoto;

    @Column(name = "gecikme_bildirildi", nullable = false)
    @Builder.Default
    private Boolean gecikmeBildirildi = false;

    @Column(name = "musteri_adi", length = 255)
    private String musteriAdi;

    @Column(nullable = false, length = 20)
    private String durum;

    @Column(columnDefinition = "TEXT")
    private String notlar;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "teslim_tarihi")
    private LocalDateTime teslimTarihi;

    @PrePersist
    protected void onCreate() {
        olusturmaTarihi = LocalDateTime.now();
        if (durum == null) durum = Durum.BEKLEMEDE.name();
    }

    public enum Durum {
        BEKLEMEDE, YOLDA, TESLIM_EDILDI, IPTAL
    }
}
