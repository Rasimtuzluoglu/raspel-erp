package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalepTahminiDTO {
    private Long stokId;
    private String stokKodu;
    private String ad;
    private String birim;
    private BigDecimal mevcutMiktar;
    private BigDecimal gunlukOrtalamaTuketim;
    private Integer tahminiTukenmeGunu;
    private Integer tedarikSuresiGun;
    private BigDecimal onerilenSiparisMiktari;
    private String tedarikciAd;
    private String durum; // "KRITIK", "DIKKAT", "GUVENLI"
    private String proaktifOneri;
}
