package com.raspel.erp.dto.sistem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Frontend hata telemetrisi ({@code POST /api/telemetri/hata}).
 *
 * <p>Gövde bilinçli olarak küçüktür: yalnız teşhis için gereken alanlar alınır
 * ve hepsi uzunluk sınırına tabidir (log şişmesini engeller).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontendHataDTO {

    /** İstemcide üretilen benzersiz kimlik (tekrar gönderimde ayırt etmek için). */
    @Size(max = 64)
    private String hataId;

    /** 'js' | 'promise' | 'vue' */
    @Size(max = 20)
    private String tur;

    @NotBlank
    @Size(max = 1000)
    private String mesaj;

    @Size(max = 6000)
    private String stack;

    @Size(max = 300)
    private String rota;

    @Size(max = 50)
    private String surum;

    @Size(max = 300)
    private String tarayici;
}
