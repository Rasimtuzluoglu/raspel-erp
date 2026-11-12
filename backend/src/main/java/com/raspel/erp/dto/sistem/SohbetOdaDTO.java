package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetOdaDTO {
    private Long id;
    private Long sirketId;
    @Size(max = 100, message = "Oda adı en fazla 100 karakter olabilir")
    private String ad;
    @Size(max = 300, message = "Açıklama en fazla 300 karakter olabilir")
    private String aciklama;
    private Long olusturanKullaniciId;
    /** Ozel kanal: yalnizca uyeler gorur; katilma yonetici davetiyle olur.
     *  Guncellemede `null` = degistirme (eski istemciler alani gondermez). */
    private Boolean ozel;
    /** Oda olusturulurken birlikte eklenecek uyeler. */
    private List<Long> uyeIdleri;
    private boolean uyeMi;
    private long uyeSayisi;
    private long okunmamisSayisi;
    private List<SohbetOdaUyeDTO> uyeler;
}
