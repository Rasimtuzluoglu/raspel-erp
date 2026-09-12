package com.raspel.erp.dto.envanter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlisOzetDTO {
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private BigDecimal stokMiktar;
    private BigDecimal toplamAlisMiktar;
    private BigDecimal toplamAlisTutari;
    private BigDecimal ortalamaBirimFiyat;
    private BigDecimal sonAlisFiyati;
    private BigDecimal enDusukAlisFiyati;
    private BigDecimal enYuksekAlisFiyati;
    private LocalDate sonAlisTarihi;
    private int islemSayisi;
}