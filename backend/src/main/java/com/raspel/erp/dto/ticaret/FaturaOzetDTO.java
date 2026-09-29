package com.raspel.erp.dto.ticaret;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fatura liste ekranları için özet toplamlar (KPI şeridi).
 * Gerçekleşmiş (KESİLDİ) satış/alış faturalarını temel alır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaOzetDTO {
    private Long adet;
    private BigDecimal ciro;
    private BigDecimal tahsilEdilen;
    private BigDecimal kalan;
}
