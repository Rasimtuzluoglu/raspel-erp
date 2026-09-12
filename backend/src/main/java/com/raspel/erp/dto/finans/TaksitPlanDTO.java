package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaksitPlanDTO {

    @NotNull(message = "Cari hesap secilmelidir")
    private Long cariId;

    @NotNull(message = "Toplam tutar girilmelidir")
    @DecimalMin(value = "0.01", message = "Toplam tutar 0'dan buyuk olmalidir")
    private BigDecimal toplamTutar;

    @NotNull(message = "Taksit sayisi girilmelidir")
    @Min(value = 1, message = "Taksit sayisi en az 1 olmalidir")
    @Max(value = 60, message = "Taksit sayisi en fazla 60 olabilir")
    private Integer taksitSayisi;

    /** Ilk taksitin vade tarihi (bos ise bugun). */
    private LocalDate baslangicTarihi;

    /** Iki taksit arasindaki ay sayisi (bos ise 1). */
    @Min(value = 1, message = "Periyot en az 1 ay olmalidir")
    @Max(value = 12, message = "Periyot en fazla 12 ay olabilir")
    private Integer periyotAy;

    @Size(max = 255, message = "Kurum adi en fazla 255 karakter olabilir")
    private String kurum;

    private Long faturaId;

    @Size(max = 500, message = "Aciklama en fazla 500 karakter olabilir")
    private String aciklama;
}
