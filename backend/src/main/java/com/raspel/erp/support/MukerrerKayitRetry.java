package com.raspel.erp.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Uygulama tarafinda uretilen belge numaralari (fatura/siparis/teklif) es zamanli
 * isteklerde ayni olusup UNIQUE kisit ihlali verebilir. Bu yardimci, yalnizca
 * "mukerrer numara" kaynakli ihlallerde islemi kis bir backoff ile yeniden dener;
 * diger veri butunlugu hatalarini oldugu gibi firlatir.
 */
@Slf4j
public final class MukerrerKayitRetry {

    private MukerrerKayitRetry() {
    }

    public static <T> T calistir(Supplier<T> islem, Predicate<DataIntegrityViolationException> mukerrerMi) {
        return calistir(islem, mukerrerMi, 6);
    }

    public static <T> T calistir(Supplier<T> islem,
                                 Predicate<DataIntegrityViolationException> mukerrerMi,
                                 int maksDeneme) {
        DataIntegrityViolationException son = null;
        for (int deneme = 0; deneme < maksDeneme; deneme++) {
            try {
                return islem.get();
            } catch (DataIntegrityViolationException e) {
                if (!mukerrerMi.test(e)) throw e;
                son = e;
                log.warn("Mukerrer belge numarasi, yeniden deneniyor ({}/{}): {}",
                        deneme + 1, maksDeneme, e.getMostSpecificCause().getMessage());
                try {
                    Thread.sleep(15L * (deneme + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw son;
    }

    /** Kisit adini (veya mesaj parcasi) iceriyorsa mukerrer kayittir. */
    public static boolean kisitMi(DataIntegrityViolationException e, String kisitAdi) {
        String m = e.getMessage();
        return m != null && m.contains(kisitAdi);
    }
}
