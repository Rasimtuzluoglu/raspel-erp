package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vade takvimi gün hücresi. Aralıktaki her gün için bir satır üretilir
 * (kayıt olmayan günler {@code adet=0} ile döner).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetTakvimDTO {
    private LocalDate tarih;
    private int adet;
    private BigDecimal tutar;
    private List<CekSenetDTO> kayitlar;
}
