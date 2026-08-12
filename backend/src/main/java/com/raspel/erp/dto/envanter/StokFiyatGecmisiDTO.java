package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokFiyatGecmisiDTO {
    private List<Kayit> gecmis;
    private BigDecimal guncelFiyat;
    private String trend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Kayit {
        private BigDecimal birimFiyat;
        private LocalDate tarih;
        private String faturaNumarasi;
    }
}
