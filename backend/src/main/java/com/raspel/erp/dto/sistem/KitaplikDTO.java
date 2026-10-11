package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/** Kitaplik kaydi formu (istek/yanit govdesi). Sir icermez. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitaplikDTO {

    private Long id;

    /** YER_IMI | SNIPPET */
    private String tur;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir")
    private String baslik;

    @Size(max = 1000, message = "Adres en fazla 1000 karakter olabilir")
    private String url;

    private String icerik;

    /** SISTEM | ARAC | DOKUMAN | KOD | DIGER */
    private String kategori;

    private String notlar;

    /** KISISEL | GLOBAL */
    private String kapsam;

    /** SAHIS | TUMU */
    private String gorunurluk;

    /** Yanit: arsivde mi? */
    private Boolean arsiv;

    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
}