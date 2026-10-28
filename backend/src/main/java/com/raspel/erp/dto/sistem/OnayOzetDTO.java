package com.raspel.erp.dto.sistem;

import lombok.*;

/** Birleşik onay ekranı KPI özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayOzetDTO {
    private long toplam;
    private long izin;
    private long masraf;
    private long satinalma;
    private long siparis;
}
