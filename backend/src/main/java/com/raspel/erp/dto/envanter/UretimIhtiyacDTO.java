package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimIhtiyacDTO {
    private Long urunId;
    private String urunAd;
    private BigDecimal miktar;
    private BigDecimal fireOrani;
    private BigDecimal toplamMaliyet;
    private boolean yeterli;
    private List<UretimIhtiyacKalemDTO> kalemler;
}
