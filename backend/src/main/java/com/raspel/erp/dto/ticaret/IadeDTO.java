package com.raspel.erp.dto.ticaret;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IadeDTO {
    private Long id;
    private Long faturaId;
    private String tur;
    private LocalDate tarih;
    private BigDecimal tutar;
    private String aciklama;
    private String durum;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
    private List<IadeKalemDTO> kalemler;
}
