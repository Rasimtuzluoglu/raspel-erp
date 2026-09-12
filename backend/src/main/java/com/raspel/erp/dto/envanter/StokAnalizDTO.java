package com.raspel.erp.dto.envanter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StokAnalizDTO {
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private String birim;
    private AlisOzetDTO alisOzet;
    private SatisOzetDTO satisOzet;
    private KarlilikDTO karlilik;
}