package com.raspel.erp.dto.sube;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Tek depo icin operasyon ozeti (KPI + son hareketler). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoDetayOzetDTO {
    private Long depoId;
    private Long kalemSayisi;
    private Long sifirStokSayisi;
    private Long kritikStokSayisi;
    private BigDecimal stokDegeri;
    private Integer dolulukYuzde;
    private List<DepoHareketDTO> sonHareketler;
}
