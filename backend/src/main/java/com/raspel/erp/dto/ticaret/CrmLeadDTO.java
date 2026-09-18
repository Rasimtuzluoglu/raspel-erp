package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmLeadDTO {
    private Long id;
    private Long sirketId;

    @NotBlank(message = "Lead adı zorunludur")
    private String ad;

    private String firma;
    private String email;
    private String telefon;
    private String kaynak;
    private String durum;
    private Integer skor;
    private BigDecimal tahminiDeger;
    private Long cariHesapId;
    private String cariHesapAd;
    private Long firsatId;
    private Long kampanyaId;
    private String kampanyaAd;
    private String aciklama;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
}
