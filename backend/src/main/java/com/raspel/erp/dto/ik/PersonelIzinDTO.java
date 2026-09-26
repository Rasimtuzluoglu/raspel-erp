package com.raspel.erp.dto.ik;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelIzinDTO {
    private Long id;
    @NotNull(message = "Personel seçilmelidir")
    private Long personelId;
    private String personelAdi;
    @NotNull(message = "İzin türü zorunludur")
    private String izinTuru;
    @NotNull(message = "Başlangıç tarihi zorunludur")
    private LocalDate baslangic;
    @NotNull(message = "Bitiş tarihi zorunludur")
    private LocalDate bitis;
    private Integer gunSayisi;
    private String durum;
    private String aciklama;
    private String onaylayan;
    private LocalDateTime olusturmaTarihi;
}