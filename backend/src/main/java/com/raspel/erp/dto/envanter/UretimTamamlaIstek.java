package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimTamamlaIstek {
    /** Gerçekleşen üretim miktarı (boşsa planlanan miktar kullanılır). */
    private BigDecimal uretilenMiktar;
    /** Fire/bozuk miktar (varsayılan 0). */
    private BigDecimal fireMiktar;
    /** Opsiyonel işçilik maliyeti. */
    private BigDecimal iscilikMaliyeti;
    private String aciklama;
}
