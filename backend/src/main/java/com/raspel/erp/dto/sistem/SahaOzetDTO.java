package com.raspel.erp.dto.sistem;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Saha portalı "Bugün" özeti ({@code GET /api/saha/ozet}).
 *
 * <p>Tüm sayımlar oturum kullanıcısına (sürücü) göre daraltılır; şirket
 * geneli veri dönmez.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SahaOzetDTO {

    private LocalDate tarih;
    private Teslimat teslimat;
    private Ziyaret ziyaret;
    private Talepler talepler;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Teslimat {
        private long bekleyen;
        private long yolda;
        private long bugunTeslimEdilen;
        private long geciken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ziyaret {
        private long bugun;
        private LocalDateTime sonZiyaretTarihi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Talepler {
        private long bekleyenIzin;
        private long bekleyenMasraf;
    }
}
