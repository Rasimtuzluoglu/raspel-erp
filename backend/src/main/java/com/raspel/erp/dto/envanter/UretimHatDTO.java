package com.raspel.erp.dto.envanter;

import lombok.*;

/** Üretim hattı / iş merkezi ve anlık doluluk özeti. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UretimHatDTO {
    private Long id;
    private String ad;
    private String aciklama;
    private Integer kapasite;
    private Boolean aktif;
    /** Hattaki açık (TASLAK + URETIMDE) emir sayısı. */
    private long aktifEmir;
    /** Hattaki geciken emir sayısı. */
    private long gecikenEmir;
    /** aktifEmir / kapasite * 100 (0-100 arası sınırlandırılır). */
    private int dolulukYuzde;
}
