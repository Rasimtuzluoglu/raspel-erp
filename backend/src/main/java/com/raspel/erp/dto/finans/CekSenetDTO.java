package com.raspel.erp.dto.finans;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.raspel.erp.entity.sube.Sube;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetDTO {
    private Long id;
    @NotBlank(message = "Çek/senet türü seçilmelidir")
    private String tur;
    private Long cariHesapId;
    private String cariHesapAdi;
    private String bankaAdi;
    private String sube;
    private String cekNo;
    private String hesapNo;
    @NotNull(message = "Vade tarihi girilmelidir")
    private LocalDate vadeTarihi;
    private LocalDate kesinmeTarihi;
    @NotNull(message = "Tutar girilmelidir")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    private BigDecimal tutar;
    private String durum;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}