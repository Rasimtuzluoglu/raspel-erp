package com.raspel.erp.dto.muhasebe;

import lombok.*;

/** İrsaliye listesi KPI özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeOzetDTO {
    private long taslak;
    private long hazirlanan;
    private long sevkEdilen;
    /** İçinde bulunulan ay (veya verilen aralık) içinde teslim edilenler. */
    private long teslimEdilenBuAy;
    /** Faturası olmayan ve iptal edilmemiş irsaliyeler. */
    private long faturasiz;
    private long iptal;
}
