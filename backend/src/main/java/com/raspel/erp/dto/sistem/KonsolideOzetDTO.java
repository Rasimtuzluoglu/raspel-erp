package com.raspel.erp.dto.sistem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KonsolideOzetDTO {
    private Long anaSirketId;
    private String anaSirketAdi;
    private int altSirketSayisi;
    private BigDecimal toplamStokDegeri;
    private BigDecimal toplamAlacakBakiye;
    private BigDecimal toplamBorcBakiye;
    private BigDecimal toplamCiro;
    private List<SirketOzetDTO> sirketler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SirketOzetDTO {
        private Long sirketId;
        private String sirketAdi;
        private String tur;
        private Integer yil;
        private BigDecimal stokDegeri;
        private BigDecimal bakiye;
        private long stokSayisi;
        private long cariSayisi;
    }
}
