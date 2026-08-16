package com.raspel.erp.dto.sistem;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HataLogDTO {
    private Long id;
    private Long sirketId;
    private String tur;
    private String mesaj;
    private String endpoint;
    private LocalDateTime olusturmaTarihi;
}
