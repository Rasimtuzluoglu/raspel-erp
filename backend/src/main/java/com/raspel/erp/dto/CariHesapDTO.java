package com.raspel.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariHesapDTO {

    private Long id;

    @NotBlank(message = "Cari adı boş olamaz")
    @Size(max = 255, message = "Cari adı en fazla 255 karakter olabilir")
    private String ad;

    @Size(max = 50, message = "Vergi numarası en fazla 50 karakter olabilir")
    private String vergiNumarasi;

    @Size(max = 20, message = "Telefon en fazla 20 karakter olabilir")
    private String telefon;

    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir")
    private String email;

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String adres;

    private String tur;
    private String il;
    private String ilce;
    private String vergiDairesi;
    private String yetkiliKisi;
    private String yetkiliTelefon;
    private String iban;
    private String notlar;
    private Boolean aktif;

    private BigDecimal krediLimiti;
    private Integer odemeVadesi;
    private BigDecimal bakiye;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
}
