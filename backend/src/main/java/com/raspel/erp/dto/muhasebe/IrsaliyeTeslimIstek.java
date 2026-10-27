package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.time.LocalDateTime;

/** Teslim geçişi gövdesi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeTeslimIstek {
    private String teslimAlan;
    /** Boşsa teslim anı (şimdi) kullanılır. */
    private LocalDateTime teslimTarihi;
}
