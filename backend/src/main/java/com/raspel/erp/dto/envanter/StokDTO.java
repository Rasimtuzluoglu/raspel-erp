package com.raspel.erp.dto.envanter;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.raspel.erp.entity.envanter.Stok;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StokDTO {
    private Long id;
    private String stokKodu;
    @NotBlank(message = "Stok adı boş olamaz")
    private String ad;
    private String birim;
    @NotNull(message = "Fiyat girilmelidir")
    @DecimalMin(value = "0", message = "Fiyat 0'dan küçük olamaz")
    private BigDecimal fiyat;
    private BigDecimal satisFiyati;
    private BigDecimal miktar;
    private BigDecimal minMiktar;
    private BigDecimal kdvOrani;
    private String stokGrubu;
    private String barkod;
    private String rafNo;
    private String marka;
    private BigDecimal agirlik;
    private String kategori;
    private String aciklama;
    private String fotoUrl;
    private String birim2;
    private BigDecimal cevrimKatsayisi;
    private Long tedarikciId;
    private String tedarikciAd;
    private String tedarikciStokKodu;
    private BigDecimal tedarikciFiyat;
    private String maliyetYontemi;
    private LocalDateTime olusturmaTarihi;
}