package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KritikStokDTO {
    private Long id;
    private String stokKodu;
    private String ad;
    private String birim;
    private BigDecimal miktar;
    private BigDecimal minMiktar;
    private BigDecimal onerilenSiparisMiktari;
    private String kategori;
    private String marka;
    private String tedarikciAd;
}