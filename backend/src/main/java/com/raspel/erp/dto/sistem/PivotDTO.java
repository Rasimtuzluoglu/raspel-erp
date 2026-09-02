package com.raspel.erp.dto.sistem;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dinamik pivot tablo sonucu.
 * satirlar/sutunlar: boyut etiketleri; hucreler: [satir][sutun] -> değer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotDTO {
    private List<String> satirlar;
    private List<String> sutunlar;
    private Map<String, Map<String, BigDecimal>> hucreler;
    private Map<String, BigDecimal> satirToplamlari;
    private Map<String, BigDecimal> sutunToplamlari;
    private BigDecimal genelToplam;
}
