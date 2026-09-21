package com.raspel.erp.dto.ik;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonelMasrafTalepDTO {
    private Long id;
    private Long personelId;
    private String personelAdi;
    private String departman;
    private Long kullaniciId;
    private String kullaniciAdi;
    private Long sirketId;
    @NotBlank(message = "Masraf türü zorunludur")
    private String tur;
    private String kategori;
    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0.01", message = "Tutar sıfırdan büyük olmalıdır")
    private BigDecimal tutar;
    private String paraBirimi;
    @NotNull(message = "Tarih zorunludur")
    private LocalDate tarih;
    @Size(max = 1000, message = "Aciklama en fazla 1000 karakter olabilir")
    private String aciklama;
    private String belgeUrl;
    private String durum;
    private String onaylayan;
    private String onayNotu;
    private LocalDateTime olusturmaTarihi;
}
