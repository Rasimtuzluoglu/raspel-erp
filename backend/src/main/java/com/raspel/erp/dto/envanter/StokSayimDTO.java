package com.raspel.erp.dto.envanter;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokSayimDTO {
    private Long id;
    private LocalDate tarih;
    private Long stokId;
    private String stokAdi;
    private BigDecimal beklenenMiktar;
    private BigDecimal sayilanMiktar;
    private BigDecimal fark;
    private String durum;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
