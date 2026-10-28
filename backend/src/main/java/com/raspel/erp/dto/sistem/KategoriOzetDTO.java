package com.raspel.erp.dto.sistem;

import lombok.*;

/** Kategori ekranı KPI özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriOzetDTO {
    private long toplam;
    private long aktif;
    private long pasif;
    private long altKategori;
}
