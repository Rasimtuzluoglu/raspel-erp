package com.raspel.erp.dto.sistem;

import lombok.*;

import java.util.List;

/**
 * Toplu karar sonucu: başarılı kayıt sayısı + atlanan kayıtlar.
 *
 * <p>Toplu işlem kayıt bazında bağımsızdır; bir kayıt hata alsa da diğerleri
 * işlenir. Atlananlar kullanıcıya nedeniyle birlikte gösterilir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnayTopluSonucDTO {

    private int basarili;
    private List<Atlanan> atlanan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Atlanan {
        private Long kaynakId;
        private String mesaj;
    }
}
