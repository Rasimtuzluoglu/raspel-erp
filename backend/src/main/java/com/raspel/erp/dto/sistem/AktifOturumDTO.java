package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AktifOturumDTO {
    private String jti;
    private Long kullaniciId;
    private String kullaniciAdi;
    private Long sirketId;
    private String ip;
    private LocalDateTime girisZamani;
    private LocalDateTime sonKullanim;
}
