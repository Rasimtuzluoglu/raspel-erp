package com.raspel.erp.dto.envanter;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceteDTO {
    private Long id;
    private Long sirketId;
    private String ad;
    private Long urunId;
    private String urunAd;
    private String aciklama;
    private List<ReceteKalemDTO> kalemler;
}
