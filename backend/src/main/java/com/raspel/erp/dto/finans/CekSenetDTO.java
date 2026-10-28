package com.raspel.erp.dto.finans;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CekSenetDTO {
    private Long id;
    @NotBlank(message = "Çek/senet türü seçilmelidir")
    private String tur;
    private Long cariHesapId;

    /** Tahsil edilecek fatura (opsiyonel). */
    private Long faturaId;
    private String cariHesapAdi;
    private String bankaAdi;
    private String sube;
    private String cekNo;
    private String hesapNo;
    @NotNull(message = "Vade tarihi girilmelidir")
    private LocalDate vadeTarihi;
    private LocalDate kesinmeTarihi;
    @NotNull(message = "Tutar girilmelidir")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    private BigDecimal tutar;
    private String durum;
    private String aciklama;
    private Long sirketId;
    private LocalDateTime olusturmaTarihi;

    // --- Tahsil bağları (iptal ters kaydı için) ---
    private Long tahsilatHareketId;
    private Long kasaHareketId;
    private Long bankaHareketiId;

    // --- Ciro / teminat / protesto ---
    private Long ciroCariId;
    private String ciroCariAd;
    private LocalDateTime ciroTarihi;
    private String ciroNotu;
    private LocalDateTime teminatTarihi;
    private String teminatAciklama;
    private LocalDateTime protestoTarihi;

    // --- Liste/detay zenginleştirme ---
    private String faturaNo;
    private String kasaAd;
    private String bankaAd;

    /** Detayda dönen durum geçmişi (zaman çizelgesi). */
    private List<CekSenetDurumLogDTO> gecmis;
}
