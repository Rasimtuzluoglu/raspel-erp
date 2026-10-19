package com.raspel.erp.dto.envanter;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSeriOzetDTO {
    private long toplam;
    private long stokta;
    private long tuketildi;
    /** SKT'si bugün ile (bugün + gun) arasında olan stoktaki seriler. */
    private long sktYaklasan;
    /** SKT'si geçmiş olan stoktaki seriler. */
    private long sktGecmis;
}
