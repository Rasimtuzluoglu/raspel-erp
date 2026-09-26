package com.raspel.erp.dto.ik;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VardiyaDTO {
    private Long id;
    @NotNull(message = "Personel seçilmelidir")
    private Long personelId;
    private String personelAdi;
    @NotNull(message = "Tarih zorunludur")
    private LocalDate tarih;
    private LocalTime baslangic;
    private LocalTime bitis;
    private String tur;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;
}
