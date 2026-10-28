package com.raspel.erp.entity.sistem;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Birleşik onay ekranından verilen kararların denetim kaydı.
 *
 * <p>Yalnızca ilgili modül işlemi (izin/masraf/satınalma/sipariş durum
 * güncellemesi) başarılı olduktan sonra yazılır; modül hatasında karar
 * kaydı oluşmaz.
 */
@Entity
@Table(name = "onay_karar", schema = "sistem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayKarar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sirket_id", nullable = false)
    private Long sirketId;

    /** IZIN, MASRAF, SATINALMA, SIPARIS */
    @Column(nullable = false, length = 20)
    private String tur;

    @Column(name = "kaynak_id", nullable = false)
    private Long kaynakId;

    /** ONAY, RED */
    @Column(nullable = false, length = 10)
    private String karar;

    @Column(length = 500)
    private String gerekce;

    @Column(name = "onceki_durum", length = 40)
    private String oncekiDurum;

    @Column(name = "yeni_durum", length = 40)
    private String yeniDurum;

    @Column(name = "karar_veren_id")
    private Long kararVerenId;

    @Column(name = "karar_veren_ad", length = 150)
    private String kararVerenAd;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    protected void onCreate() {
        if (olusturmaTarihi == null) olusturmaTarihi = LocalDateTime.now();
    }
}
