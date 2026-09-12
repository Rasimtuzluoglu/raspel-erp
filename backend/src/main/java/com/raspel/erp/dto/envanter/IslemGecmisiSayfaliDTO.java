package com.raspel.erp.dto.envanter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** İşlem geçmişi için sayfalı yanıt: geçerli sayfa satırları + toplam kayıt sayısı. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IslemGecmisiSayfaliDTO {
    private List<IslemSatirDTO> satirlar;
    private long toplam;
    private int sayfa;
    private int boyut;
    private int toplamSayfa;
}