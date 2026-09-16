package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.time.LocalDateTime;

/** Liste badge'i için fatura başına yazdırma özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaYazdirmaOzetDTO {
    private Long faturaId;
    private long adet;
    private LocalDateTime sonTarih;
    private String sonFormat;
    private String sonYazici;
}
