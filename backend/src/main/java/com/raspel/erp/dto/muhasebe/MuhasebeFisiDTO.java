package com.raspel.erp.dto.muhasebe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuhasebeFisiDTO {
    private Long id;
    private String fisNo;

    @NotNull(message = "Fiş tarihi girilmelidir")
    private LocalDate tarih;

    private String aciklama;
    private String durum;
    private Long sirketId;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;

    private BigDecimal toplamBorc;
    private BigDecimal toplamAlacak;

    @Valid
    private List<MuhasebeFisKalemDTO> kalemler;
}
