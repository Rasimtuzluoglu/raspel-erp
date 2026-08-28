package com.raspel.erp.dto.muhasebe;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BilancoDTO {
    private List<KalemDTO> aktifler;
    private List<KalemDTO> pasifler;
    private BigDecimal aktifToplam;
    private BigDecimal pasifToplam;

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
