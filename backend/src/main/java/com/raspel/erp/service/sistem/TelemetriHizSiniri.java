package com.raspel.erp.service.sistem;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Frontend hata telemetrisi için kullanıcı başına sabit pencereli hız sınırı.
 *
 * <p>Bellek içidir (çok replikalı ortamda her replika kendi sayacını tutar);
 * amaç kötüye kullanım/log şişmesini engellemektir, kesin kota değildir.</p>
 */
@Component
public class TelemetriHizSiniri {

    static final int VARSAYILAN_LIMIT = 30;
    static final long VARSAYILAN_PENCERE_MS = Duration.ofHours(1).toMillis();
    private static final int AZAMI_KULLANICI = 10_000;

    private final int limit;
    private final long pencereMs;
    private final LongSupplier saat;
    private final Map<Long, Pencere> pencereler = new ConcurrentHashMap<>();

    public TelemetriHizSiniri() {
        this(VARSAYILAN_LIMIT, VARSAYILAN_PENCERE_MS, System::currentTimeMillis);
    }

    /** Testler için saat ve pencere enjekte edilebilir. */
    TelemetriHizSiniri(int limit, long pencereMs, LongSupplier saat) {
        this.limit = limit;
        this.pencereMs = pencereMs;
        this.saat = saat;
    }

    /** true: istek kabul edilebilir; false: pencere doldu (429). */
    public boolean izinVar(Long kullaniciId) {
        if (kullaniciId == null) return false;
        long simdi = saat.getAsLong();
        if (pencereler.size() > AZAMI_KULLANICI) suresiDolanlariTemizle(simdi);

        Pencere sonuc = pencereler.compute(kullaniciId, (id, mevcut) -> {
            if (mevcut == null || simdi - mevcut.baslangic() >= pencereMs) {
                return new Pencere(simdi, 1);
            }
            return new Pencere(mevcut.baslangic(), mevcut.sayi() + 1);
        });
        return sonuc.sayi() <= limit;
    }

    private void suresiDolanlariTemizle(long simdi) {
        pencereler.entrySet().removeIf(giris -> simdi - giris.getValue().baslangic() >= pencereMs);
    }

    private record Pencere(long baslangic, int sayi) {
    }
}
