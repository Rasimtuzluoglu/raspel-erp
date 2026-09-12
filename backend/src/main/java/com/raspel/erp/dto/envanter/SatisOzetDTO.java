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
public class SatisOzetDTO {
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    private BigDecimal toplamSatisMiktar;
    private BigDecimal toplamSatisTutari;
    private BigDecimal ortalamaBirimFiyat;
    private BigDecimal sonSatisFiyati;
    private BigDecimal enDusukSatisFiyati;
    private BigDecimal enYuksekSatisFiyati;
    private LocalDate sonSatisTarihi;
    private int islemSayisi;
}