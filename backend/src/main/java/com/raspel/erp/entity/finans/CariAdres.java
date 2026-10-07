package com.raspel.erp.entity.finans;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Faz 2.5: Cariye ait çoklu adres / iletişim kaydı. Bir carinin birden fazla
 * teslimat/fatura adresi olabilir.
 */
@Entity
@Table(name = "cari_adres", schema = "cari",
        indexes = @Index(name = "idx_cari_adres_cari", columnList = "cari_hesap_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariAdres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cari_hesap_id", nullable = false)
    private Long cariHesapId;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** Adres başlığı (ör. "Merkez", "Depo", "Fatura"). */
    @Column(length = 100)
    private String baslik;

    @Column(length = 500)
    private String adres;

    @Column(length = 50)
    private String il;

    @Column(length = 50)
    private String ilce;

    @Column(name = "yetkili_kisi", length = 100)
    private String yetkiliKisi;

    @Column(length = 20)
    private String telefon;

    /** Varsayılan adres. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean varsayilan = false;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
        if (varsayilan == null) varsayilan = false;
    }
}
