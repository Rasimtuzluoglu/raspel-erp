package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TekrarlayanFaturaDTO {
    private Long id;
    private Long sirketId;
    private Long cariHesapId;
    private String cariHesapAd;

    @NotBlank(message = "Fatura türü seçilmelidir")
    private String tur;

    private String aciklama;

    @NotBlank(message = "Periyot seçilmelidir")
    private String periyot;

    @NotNull(message = "Başlangıç tarihi girilmelidir")
    private LocalDate baslangicTarihi;

    private LocalDate bitisTarihi;
    private LocalDate sonrakiCalistirma;
    private Boolean aktif;

    @NotNull(message = "En az bir kalem eklenmelidir")
    private List<TekrarlayanFaturaKalemDTO> kalemler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TekrarlayanFaturaKalemDTO {
        private Long id;

        @NotBlank(message = "Kalem açıklaması girilmelidir")
        private String aciklama;

        @NotNull(message = "Adet girilmelidir")
        private Integer adet;

        @NotNull(message = "Birim fiyat girilmelidir")
        private BigDecimal birimFiyat;

        private BigDecimal kdvOrani;
        private BigDecimal iskontoOrani;
        private Long stokId;
    }
}
