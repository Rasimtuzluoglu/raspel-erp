package com.raspel.erp.dto.sistem;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Bir kategorinin modül bazlı kullanım sayısı. Silme engeli ve birleştirme
 * öncesi özet bu veriyi kullanır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriKullanimDTO {

    private Long kategoriId;
    private String ad;
    private long toplam;

    @Builder.Default
    private List<Detay> detaylar = new ArrayList<>();

    /** Modül adı (STOK, KASA, MASRAF, BUTCE, ISKONTO) ve kayıt adedi. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detay {
        private String modul;
        private long adet;
    }
}
