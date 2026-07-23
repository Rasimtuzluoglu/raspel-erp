package com.raspel.erp.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrsaliyeKalemDTO {
    private Long id;
    private Long irsaliyeId;
    private Long stokId;
    private String stokAdi;
    private String aciklama;
    private Double miktar;
    private String birim;
    private LocalDateTime olusturmaTarihi;
}
