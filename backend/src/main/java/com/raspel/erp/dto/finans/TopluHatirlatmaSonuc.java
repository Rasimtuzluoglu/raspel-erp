package com.raspel.erp.dto.finans;

import lombok.*;
import java.util.List;

/**
 * Toplu hatırlatma sonucu. {@code gonderilen}/{@code atlanan} cari sayısıdır;
 * {@code hatalar} gönderim başarısızlıklarını açıklar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopluHatirlatmaSonuc {

    private int gonderilen;
    private int atlanan;
    private List<String> hatalar;
}
