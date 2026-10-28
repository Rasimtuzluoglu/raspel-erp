package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Kategori registry DTO'su.
 *
 * <p>{@code tur} alanı geriye uyumluluk için korunur: kasa kapsamlarında
 * {@code GELIR}/{@code GIDER}, diğer kapsamlarda {@code null} döner. Gönderimde
 * {@code kapsam} boşsa {@code tur} alanından kapsam türetilir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class KategoriDTO {

    private Long id;

    /** KASA_GELIR, KASA_GIDER, STOK, MASRAF, BUTCE */
    private String kapsam;

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 100, message = "Kategori adı en fazla 100 karakter olabilir")
    private String ad;

    private Long ustKategoriId;
    private String ustKategoriAd;
    private String renk;
    private String ikon;
    private Integer sira;
    private Boolean aktif;
    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    private String aciklama;

    private Long altKategoriSayisi;
    private Long kullanimSayisi;

    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;

    /** @deprecated Kapsam tabanlı API'ye geçildi; geriye uyumluluk için tutulur. */
    @Deprecated
    private String tur;
}
