package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KarZararDTO {
    private List<KalemDTO> gelirler;
    private List<KalemDTO> giderler;
    private BigDecimal gelirToplam;
    private BigDecimal giderToplam;
    private BigDecimal netKar;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KalemDTO {
        private String kod;
        private String ad;
        private BigDecimal tutar;
    }
}
