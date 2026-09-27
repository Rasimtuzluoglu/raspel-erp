package com.raspel.erp.service.envanter;

import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sirket ici EAN-13 barkod uretimi.
 *
 * Format: 869 (Turkiye GS1 on eki) + 9 haneli sirket ici sira + kontrol hanesi.
 * Sira, sirketteki en buyuk 869'lu barkoddan turetilir; cakisma olursa sonraki
 * siraya gecilir. Gercek GS1 kayitlariyla cakismamasi icin bu numaralar yalnizca
 * kurum ici etiketleme amaciyla kullanilmalidir.
 */
@Service
@RequiredArgsConstructor
public class BarkodUretService {

    private static final String ON_EK = "869";
    private static final long SIRA_MOD = 1_000_000_000L;

    private final StokRepository stokRepository;

    public String ean13Uret(Long sirketId) {
        return ean13Uret(sirketId, java.util.Set.of());
    }

    /**
     * @param haricTutulanlar ayni toplu islemde uretilen ve henuz DB'ye yazilmamis
     *                       barkodlar (mukerrer uretimi onler)
     */
    public String ean13Uret(Long sirketId, java.util.Set<String> haricTutulanlar) {
        long sira = sonSira(sirketId);
        for (int deneme = 0; deneme < 50; deneme++) {
            sira++;
            String aday = ean13(sira);
            if (!haricTutulanlar.contains(aday) && !stokRepository.existsBySirketIdAndBarkod(sirketId, aday)) {
                return aday;
            }
        }
        throw new BusinessException("Otomatik barkod üretilemedi; lütfen tekrar deneyin.");
    }

    private long sonSira(Long sirketId) {
        String max = stokRepository.maxEan13Barkod(sirketId);
        if (max == null || max.length() != 13) return 0;
        try {
            return Long.parseLong(max.substring(3, 12));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 12 haneli govde + EAN-13 kontrol hanesi. */
    static String ean13(long sira) {
        String govde = ON_EK + String.format("%09d", Math.floorMod(sira, SIRA_MOD));
        return govde + kontrolHanesi(govde);
    }

    /** EAN-13: tek konumlar x1, cift konumlar x3 toplanir; 10'a tamamlanir. */
    static int kontrolHanesi(String ilk12) {
        int toplam = 0;
        for (int i = 0; i < 12; i++) {
            int hane = ilk12.charAt(i) - '0';
            toplam += (i % 2 == 0) ? hane : hane * 3;
        }
        return (10 - (toplam % 10)) % 10;
    }
}
