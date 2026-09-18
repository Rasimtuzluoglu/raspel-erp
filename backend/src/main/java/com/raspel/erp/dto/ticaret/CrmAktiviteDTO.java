package com.raspel.erp.dto.ticaret;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmAktiviteDTO {
    private Long id;
    private Long sirketId;

    @NotBlank(message = "Aktivite türü zorunludur")
    private String tur;

    @NotBlank(message = "Başlık zorunludur")
    private String baslik;

    private String aciklama;
    private Long cariHesapId;
    private String cariHesapAd;
    private Long firsatId;
    private Long leadId;
    private LocalDateTime planlananTarih;
    private Boolean tamamlandi;
    private LocalDateTime tamamlanmaTarihi;
    private Long kullaniciId;
    private LocalDateTime olusturmaTarihi;
}
