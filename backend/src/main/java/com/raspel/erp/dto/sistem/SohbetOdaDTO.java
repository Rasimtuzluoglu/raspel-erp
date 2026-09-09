package com.raspel.erp.dto.sistem;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetOdaDTO {
    private Long id;
    private Long sirketId;
    private String ad;
    private String aciklama;
    private Long olusturanKullaniciId;
    private boolean uyeMi;
    private long uyeSayisi;
    private long okunmamisSayisi;
    private List<SohbetOdaUyeDTO> uyeler;
}
