package com.raspel.erp.dto.sistem;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SirketHedefDTO {
    private Long id;
    private Long sirketId;
    private Integer yil;
    private Integer ay;
    private BigDecimal hedefCiro;
    private BigDecimal hedefKar;
    private Integer hedefYeniMusteri;
    private Integer hedefSatisAdedi;
    private String notlar;
    private LocalDateTime guncellemeTarihi;
}
