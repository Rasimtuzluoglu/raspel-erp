package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteDTO {
    private Long id;
    private Long sirketId;
    @NotBlank(message = "Reçete adı zorunludur")
    @Size(max = 200, message = "Reçete adı en fazla 200 karakter olabilir")
    private String ad;
    @NotNull(message = "Üretilecek ürün secilmelidir")
    private Long urunId;
    private String urunAd;
    private String aciklama;
    private Boolean aktif;
    private Integer revizyon;
    private BigDecimal fireOrani;
    private String notlar;
    private List<ReceteKalemDTO> kalemler;
}
