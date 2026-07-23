package com.raspel.erp.dto.ik;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaasBordroDTO {
    private Long id;
    private Long personelId;
    private String personelAdi;
    private Integer yil;
    private Integer ay;
    private BigDecimal brutMaas;
    private BigDecimal kesintiler;
    private BigDecimal netMaas;
    private LocalDate odemeTarihi;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
