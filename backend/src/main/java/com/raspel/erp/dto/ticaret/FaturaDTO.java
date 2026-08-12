package com.raspel.erp.dto.ticaret;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.raspel.erp.entity.ticaret.Fatura;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaDTO {

    private Long id;

    private String faturaNumarasi;

    @NotNull(message = "Fatura tarihi girilmelidir")
    private LocalDate tarih;

    @NotBlank(message = "Fatura türü seçilmelidir")
    private String tur;

    private String durum;

    private Long cariHesapId;
    private String cariHesapAd;

    private String aciklama;

    private BigDecimal araToplam;
    private BigDecimal kdv;
    private BigDecimal genelToplam;
    private BigDecimal genelIskontoTutari;
    private String odemeDurumu;
    private BigDecimal odenenTutar;
    private BigDecimal kalanTutar;

    @NotEmpty(message = "En az bir kalem eklenmelidir")
    @Valid
    private List<FaturaKalemDTO> kalemler;

    private LocalDateTime olusturmaTarihi;
    private Long olusturanKullaniciId;
    private String olusturanKullaniciAdi;
    private String teslimEden;
}