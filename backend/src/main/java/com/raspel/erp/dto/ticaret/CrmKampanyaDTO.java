package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmKampanyaDTO {
    private Long id;
    private Long sirketId;

    @NotBlank(message = "Kampanya adı zorunludur")
    private String ad;

    private String tur;
    private String durum;
    private LocalDate baslangic;
    private LocalDate bitis;
    private BigDecimal butce;
    private BigDecimal harcama;
    private Integer hedefKisi;
    private String aciklama;
    private Long leadSayisi;
    private LocalDateTime olusturmaTarihi;
}
