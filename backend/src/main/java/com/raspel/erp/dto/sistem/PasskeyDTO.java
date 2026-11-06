package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyDTO {
    private Long id;
    private String etiket;
    private String transports;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime sonKullanmaTarihi;
}
