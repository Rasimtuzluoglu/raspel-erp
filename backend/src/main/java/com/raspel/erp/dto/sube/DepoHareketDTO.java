package com.raspel.erp.dto.sube;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Depo stok hareketi (gorunum). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepoHareketDTO {
    private Long id;
    private LocalDateTime tarih;
    private LocalDate hareketTarihi;
    private String tur;
    private String kaynakTip;
    private Long stokId;
    private String stokKod;
    private String stokAd;
    private BigDecimal miktar;
    private String kullaniciAd;
    private String aciklama;
}
