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
public class CariFirsatDTO {
    private Long id;

    @NotBlank(message = "Fırsat adı girilmelidir")
    private String ad;

    private Long cariHesapId;
    private String cariHesapAd;

    private String durum;
    private String kaynak;

    private BigDecimal deger;

    private LocalDate tahminiKapanis;
    private String aciklama;

    private Long kullaniciId;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
