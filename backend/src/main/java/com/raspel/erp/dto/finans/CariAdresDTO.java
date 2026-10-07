package com.raspel.erp.dto.finans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/** Faz 2.5: Cariye ait adres/iletişim kaydı. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariAdresDTO {

    private Long id;
    private Long cariHesapId;
    private Long sirketId;

    @Size(max = 100, message = "Başlık en fazla 100 karakter olabilir")
    private String baslik;

    @NotBlank(message = "Adres boş olamaz")
    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String adres;

    @Size(max = 50)
    private String il;
    @Size(max = 50)
    private String ilce;
    @Size(max = 100)
    private String yetkiliKisi;
    @Size(max = 20)
    private String telefon;

    private Boolean varsayilan;
    private LocalDateTime olusturmaTarihi;
}
