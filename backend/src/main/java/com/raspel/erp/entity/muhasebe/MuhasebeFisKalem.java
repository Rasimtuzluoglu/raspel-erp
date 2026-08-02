package com.raspel.erp.entity.muhasebe;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "muhasebe_fis_kalem", schema = "muhasebe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuhasebeFisKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fis_id", nullable = false)
    private Long fisId;

    @Column(name = "hesap_kodu", nullable = false, length = 20)
    private String hesapKodu;

    @Column(name = "hesap_adi", length = 200)
    private String hesapAdi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal borc;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal alacak;

    @Column(length = 500)
    private String aciklama;
}
