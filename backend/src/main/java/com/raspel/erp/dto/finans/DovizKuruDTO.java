package com.raspel.erp.dto.finans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DovizKuruDTO {
    private Long id;
    private String dovizKodu;
    private String dovizAdi;
    private LocalDate tarih;
    private BigDecimal alisKuru;
    private BigDecimal satisKuru;
    private BigDecimal efektifAlis;
    private BigDecimal efektifSatis;
}
