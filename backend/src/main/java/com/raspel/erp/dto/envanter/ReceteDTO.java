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
    @NotBlank(message = "Recete adi zorunludur")
    @Size(max = 200, message = "Recete adi en fazla 200 karakter olabilir")
    private String ad;
    @NotNull(message = "Uretilecek urun secilmelidir")
    private Long urunId;
    private String urunAd;
    private String aciklama;
    private Boolean aktif;
    private Integer revizyon;
    private BigDecimal fireOrani;
    private String notlar;
    private List<ReceteKalemDTO> kalemler;
}
