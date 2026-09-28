package com.raspel.erp.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Sınırsız büyüyebilen liste uçlarına geriye uyumlu (opt-in) sayfalama eklemek
 * için yardımcılar.
 *
 * <p>İstemci {@code page}/{@code size} parametrelerinden en az birini gönderirse
 * uç nokta Spring Data {@code Page} döndürür; hiç göndermezse eski davranış
 * korunur ve tam liste döner. Böylece mevcut çağrılar bozulmadan kademeli
 * geçiş yapılabilir.
 */
public final class SayfalamaUtil {

    /** Parametresiz istekte kullanılan varsayılan sayfa boyutu. */
    public static final int VARSAYILAN_BOYUT = 50;

    /** Tek istekte döndürülebilecek en büyük sayfa boyutu (bellek koruması). */
    public static final int MAKS_BOYUT = 200;

    private SayfalamaUtil() {
    }

    /** {@code page} veya {@code size} verildiyse sayfalı yanıt üretilmelidir. */
    public static boolean sayfaliMi(Integer page, Integer size) {
        return page != null || size != null;
    }

    /** İstemci parametrelerini sınırlandırılmış bir {@link Pageable}'a çözer. */
    public static Pageable coz(Integer page, Integer size, Sort sort) {
        int sayfa = (page == null || page < 0) ? 0 : page;
        int boyut = (size == null || size <= 0) ? VARSAYILAN_BOYUT : Math.min(size, MAKS_BOYUT);
        return PageRequest.of(sayfa, boyut, sort);
    }
}
