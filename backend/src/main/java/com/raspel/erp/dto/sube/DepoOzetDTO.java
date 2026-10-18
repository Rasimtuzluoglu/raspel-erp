package com.raspel.erp.dto.sube;

import lombok.*;

/**
 * Depolar ekranı KPI özeti. Sayaçlar şirket kapsamındadır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoOzetDTO {
    private long toplamDepo;
    private long aktifDepo;
    private long pasifDepo;
    /** Depolardaki toplam stok kalemi (depo-stok satırı) sayısı. */
    private long stokKalemiSayisi;
    /** Miktarı sıfır veya negatif olan stok kalemi sayısı. */
    private long sifirStokluKalem;
    /** Onay bekleyen depo transfer talebi sayısı. */
    private long bekleyenTalep;
}
