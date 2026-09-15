package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteDTO {
    private Long id;
    private Long sirketId;
    private String ad;
    private Long urunId;
    private String urunAd;
    private String aciklama;
    private Boolean aktif;
    private Integer revizyon;
    private BigDecimal fireOrani;
    private String notlar;
    private List<ReceteKalemDTO> kalemler;
}
