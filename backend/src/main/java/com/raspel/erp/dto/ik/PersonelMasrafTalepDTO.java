package com.raspel.erp.dto.ik;

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
    private String tur;
    private String kategori;
    private BigDecimal tutar;
    private String paraBirimi;
    private LocalDate tarih;
    private String aciklama;
    private String belgeUrl;
    private String durum;
    private String onaylayan;
    private String onayNotu;
    private LocalDateTime olusturmaTarihi;
}
