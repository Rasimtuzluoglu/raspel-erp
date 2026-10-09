package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sifre kasasi kaydi formu (istek govdesi).
 *
 * <p>Sifre metni bu DTO ile gelir ve servis katmaninda AES-256-GCM ile
 * sifrelenir. Alan adi bilerek {@code sifre} olarak secildi: AuditAspect'in
 * HASSAS_ALANLAR listesinde "sifre" bulundugu icin denetim izine yazilirken
 * otomatik olarak "***" ile maskelenir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SifreKasaDTO {

    private Long id;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir")
    private String baslik;

    @Size(max = 200, message = "Kullanıcı adı en fazla 200 karakter olabilir")
    private String kullaniciAdi;

    /**
     * Duz metin sifre. Kaydetmede zorunlu; guncellemede bos birakilirsa
     * mevcut sifre KORUNUR (degistirilmedigi varsayilir).
     */
    @Size(max = 500, message = "Şifre en fazla 500 karakter olabilir")
    private String sifre;

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String url;

    /** SISTEM | BANKA | EPOSTA | SOSYAL | DIGER */
    private String kategori;

    private String notlar;

    /** Gecerlilik suresi (gun). Bos veya 0 -> sifresiz. */
    private Integer gecerlilikGun;

    /** KISISEL | GLOBAL. Global yazma yetkisi serviste ADMIN ile sinirlanir. */
    private String kapsam;

    /** SAHIS | TUMU. Yalniz GLOBAL kapsamda TUMU secilebilir. */
    private String sifreGorunurlugu;

    /** Sure durumu: SURESIZ | GECERLI | SURE_YAKLASTI | SURESI_BITTI. */
    private String durum;

    /** Sure dolumuna kalan gun (sadece SURE_YAKLASTI/SURESI_BITTI icin anlamli). */
    private Integer kalanGun;

    /** Kayit arsivlendi mi? */
    private Boolean arsiv;

    private LocalDateTime olusturmaTarihi;
    private LocalDateTime guncellemeTarihi;
    private LocalDateTime sonGoruntuleme;

    /** Arsivden cikarildi mi? (geri alma icin sunucuya gonderilir) */
    private Boolean geriAl;
}