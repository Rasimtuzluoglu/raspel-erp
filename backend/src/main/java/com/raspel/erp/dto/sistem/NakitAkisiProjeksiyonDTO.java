package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NakitAkisiProjeksiyonDTO {
    private BigDecimal baslangicBakiyesi;
    private BigDecimal toplamBeklenenGiris;
    private BigDecimal toplamBeklenenCikis;
    private BigDecimal tahminiBitisBakiyesi;
    private Integer projeksiyonGunu;
    private List<GunlukProjeksiyonDTO> gunlukAkis;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GunlukProjeksiyonDTO {
        private LocalDate tarih;
        private BigDecimal beklenenGiris;
        private BigDecimal beklenenCikis;
        private BigDecimal netAkis;
        private BigDecimal kumulatifBakiye;
        private String aciklama;
    }
}
