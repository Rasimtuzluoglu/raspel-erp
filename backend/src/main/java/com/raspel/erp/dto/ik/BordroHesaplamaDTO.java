package com.raspel.erp.dto.ik;

import lombok.*;

import java.math.BigDecimal;

/**
 * Bordro hesaplama isteği/sonucu. İstekte brutMaas verilmezse personelin kayıtlı maaşı
 * kullanılır. kumulatifMatrah, yıl içinde önceki aylarda oluşan gelir vergisi matrahıdır
 * (dilimli vergi hesabı için); verilmezse yalnızca bu ay hesaplanır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BordroHesaplamaDTO {

    private Long personelId;
    private String personelAdi;
    private Integer yil;
    private Integer ay;
    private BigDecimal brutMaas;
    private BigDecimal kumulatifMatrah;

    // ---- Hesaplanan değerler ----
    private BigDecimal asgariUcret;
    private BigDecimal sgkIsciKesintisi;
    private BigDecimal gelirVergisiMatrahi;
    private BigDecimal gelirVergisi;
    private BigDecimal damgaVergisi;
    private BigDecimal toplamKesinti;
    private BigDecimal netMaas;
    private BigDecimal isverenMaliyeti;
    private String aciklama;
}
