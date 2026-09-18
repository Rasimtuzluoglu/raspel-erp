package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IskontoKuraliDTO {
    private Long id;
    private Long sirketId;

    @NotBlank(message = "Kural adı zorunludur")
    private String ad;

    private Long stokId;
    private Long cariHesapId;
    private String kategori;

    @DecimalMin(value = "0", message = "Minimum adet negatif olamaz")
    private BigDecimal minAdet;

    @DecimalMin(value = "0", message = "Maksimum adet negatif olamaz")
    private BigDecimal maxAdet;

    @NotNull(message = "İskonto oranı zorunludur")
    @DecimalMin(value = "0", message = "İskonto oranı negatif olamaz")
    private BigDecimal iskontoOrani;

    private Integer oncelik;
    private LocalDate gecerliBaslangic;
    private LocalDate gecerliBitis;
    private Boolean aktif;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
}
