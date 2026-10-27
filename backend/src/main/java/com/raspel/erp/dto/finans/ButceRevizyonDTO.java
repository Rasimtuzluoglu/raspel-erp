package com.raspel.erp.dto.finans;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bütçe revizyon geçmişi satırı. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButceRevizyonDTO {
    private Long id;
    private Long butceId;
    private Integer revizyon;
    private String islem;
    private String durum;
    private String aciklama;
    private BigDecimal tutar;
    private Long degistirenKullaniciId;
    private LocalDateTime olusturmaTarihi;
}
