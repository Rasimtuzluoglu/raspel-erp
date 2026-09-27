package com.raspel.erp.dto.ik;

import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaasBordroDTO {
    private Long id;

    @NotNull(message = "Personel seçilmelidir")
    private Long personelId;
    private String personelAdi;

    @NotNull(message = "Yıl zorunludur")
    @Min(value = 2000, message = "Geçersiz yıl")
    @Max(value = 2100, message = "Geçersiz yıl")
    private Integer yil;

    @NotNull(message = "Ay zorunludur")
    @Min(value = 1, message = "Ay 1-12 arasında olmalıdır")
    @Max(value = 12, message = "Ay 1-12 arasında olmalıdır")
    private Integer ay;

    @NotNull(message = "Brüt maaş zorunludur")
    @DecimalMin(value = "0", message = "Brüt maaş negatif olamaz")
    private BigDecimal brutMaas;

    @DecimalMin(value = "0", message = "Kesintiler negatif olamaz")
    private BigDecimal kesintiler;

    private BigDecimal netMaas;
    private LocalDate odemeTarihi;
    private Long sirketId;
    private String aciklama;
    private LocalDateTime olusturmaTarihi;
    private String durum;
    private LocalDateTime onayTarihi;
    private String onaylayan;
    private String odemeDurumu;
    private Long odemeKasaId;
}
