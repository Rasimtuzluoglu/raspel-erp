package com.raspel.erp.dto.ticaret;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariUrunFiyatDTO {
    private List<Kayit> gecmis;
    private BigDecimal sonFiyat;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Kayit {
        private BigDecimal birimFiyat;
        private LocalDate tarih;
        private String faturaNumarasi;
        private Integer adet;
    }
}
