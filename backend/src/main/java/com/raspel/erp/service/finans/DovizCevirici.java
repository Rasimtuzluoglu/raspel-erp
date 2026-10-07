package com.raspel.erp.service.finans;

import com.raspel.erp.service.sistem.TcmbKurService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Rapor/dışa aktarma üretimi sırasında TRY tutarlarını seçilen para birimine
 * çeviren istek-yerel (thread-local) bağlam.
 *
 * <p>Doküman üreticileri (PDF/Excel) onlarca tutarı derinlerde biçimlendirir;
 * her imzayı değiştirmek yerine, istek başında hedef para birimi bir kez
 * ayarlanır ve para biçimlendirme noktaları bu bağlamı okur. Hedef TRY/null
 * olduğunda hiçbir dönüşüm yapılmaz (mevcut davranış korunur).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DovizCevirici {

    private static final ThreadLocal<String> HEDEF = new ThreadLocal<>();

    private final TcmbKurService tcmbKurService;

    public void basla(String kod) {
        HEDEF.set(kod == null || kod.isBlank() ? null : kod.trim().toUpperCase(Locale.ROOT));
    }

    public void temizle() {
        HEDEF.remove();
    }

    public String hedef() {
        return HEDEF.get();
    }

    /** Hedef TRY değilse TRY tutarını çevirir; kur yoksa ham değeri döndürür. */
    public BigDecimal cevir(BigDecimal tryTutar) {
        String kod = HEDEF.get();
        if (tryTutar == null || kod == null || "TRY".equals(kod)) {
            return tryTutar;
        }
        try {
            return tcmbKurService.cevir(tryTutar, "TRY", kod);
        } catch (Exception e) {
            log.warn("Rapor para birimi çevrilemedi ({}): {}", kod, e.getMessage());
            return tryTutar;
        }
    }
}
