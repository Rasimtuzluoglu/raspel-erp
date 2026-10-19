package com.raspel.erp.dto.envanter;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSayimOzetDTO {
    private long toplam;
    private long taslak;
    private long tamamlanan;
    private long iptal;
    /** Tamamlanmış ve uygulanan farkı sıfır olmayan sayım sayısı. */
    private long farkli;
    /** Bugün tarihli sayım sayısı. */
    private long bugun;
}
