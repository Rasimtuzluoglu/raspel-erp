package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SohbetMesajDTO {
    private Long id;
    private Long sirketId;
    private Long kullaniciId;
    private String kullaniciAd;
    private Long odaId;
    @Size(max = 500, message = "Dosya bağlantısı çok uzun")
    private String dosyaUrl;
    @Size(max = 4000, message = "Mesaj en fazla 4000 karakter olabilir")
    private String mesaj;
    private Instant olusturmaTarihi;
}
