package com.raspel.erp.dto.envanter;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimEmriDTO {
    private Long id;
    private Long sirketId;
    private Long siparisId;
    private Long urunId;
    private String urunAd;
    private BigDecimal miktar;
    private String durum;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime tamamlanmaTarihi;
}
