package com.raspel.erp.dto.sistem;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISorguSonucDTO {
    private String soru;
    private String cevapMetni;
    private String grafikTipi; // "bar", "doughnut", "line", "none"
    private Map<String, Object> grafikVerisi; // labels, datasets
    private List<Map<String, Object>> tabloVerisi; // headers, rows
    private String intent; // "CIRO_MUSTERI", "VADESI_GELEN", "STOK_DURUM", "LIKIDITE", "GENEL"
}
