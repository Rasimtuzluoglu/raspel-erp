package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiTokenDTO {
    private Long id;
    private Long kullaniciId;
    private String ad;
    private String token; // yalnızca oluşturma anında döndürülür
    private LocalDateTime sonKullanim;
    private LocalDateTime olusturmaTarihi;
}
