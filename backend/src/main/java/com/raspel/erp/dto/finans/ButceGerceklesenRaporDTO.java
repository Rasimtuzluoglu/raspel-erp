package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Tür ayrımlı bütçe–gerçekleşen raporu. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceGerceklesenRaporDTO {
    private String tur;
    private Integer yil;
    private Integer ay;
    private LocalDate baslangic;
    private LocalDate bitis;
    private BigDecimal butceToplam;
    private BigDecimal gerceklesenToplam;
    private BigDecimal sapma;
    private BigDecimal kullanimYuzde;
    private List<ButceGerceklesenSatirDTO> satirlar;
}
