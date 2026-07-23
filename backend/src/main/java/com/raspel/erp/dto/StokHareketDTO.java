package com.raspel.erp.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StokHareketDTO {
    private Long id;
    private Long stokId;
    private String stokAd;
    private String stokKodu;
    @NotBlank(message = "Hareket türü seçilmelidir")
    private String tur;
    @NotNull(message = "Miktar girilmelidir")
    private BigDecimal miktar;
    @NotNull(message = "Tarih girilmelidir")
    private LocalDate hareketTarihi;
    private String aciklama;
    private Long cariHesapId;
    private String cariHesapAd;
    private LocalDateTime olusturmaTarihi;
}
