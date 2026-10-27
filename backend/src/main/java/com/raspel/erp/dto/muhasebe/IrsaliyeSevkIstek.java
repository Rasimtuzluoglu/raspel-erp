package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.math.BigDecimal;

/** Sevk geçişi gövdesi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeSevkIstek {
    private String plaka;
    private Long soforId;
    private String teslimatAdresi;
    private String tasiyiciFirma;
    private Integer koliAdedi;
    private BigDecimal desi;
    private BigDecimal brutKg;
}
