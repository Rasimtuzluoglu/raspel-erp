package com.raspel.erp.dto.finans;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceDTO {
    private Long id;
    private String ad;
    private Integer yil;
    private Integer ay;
    private BigDecimal tutar;
    private String tur;
    private String kategori;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
