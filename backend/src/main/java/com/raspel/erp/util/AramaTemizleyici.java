package com.raspel.erp.util;

/**
 * {@code LIKE} desenleri için arama terimi temizleme ve normalleştirme.
 *
 * <p><b>1) Joker karakter kaçışı:</b> Kullanıcı arama kutusuna yazdığı metin
 * doğrudan {@code LIKE :q} desenine giriyordu. {@code %} ve {@code _}
 * karakterleri SQL jokeridir:
 * <ul>
 *   <li>{@code q="a%"} -> "a ile başlayan HER ŞEY" (istenen: "a%" içeren kayıtlar)</li>
 *   <li>{@code q="_"} -> "tek karakterlik TÜM KAYITLAR"</li>
 * </ul>
 * Yani bir kullanıcı istemeden tüm tabloyu getirebilirdi (veri sızdırma /
 * kaynak tüketme). Bu karakterler artık kaçışlanır.
 *
 * <p><b>2) Minimum uzunluk:</b> 1 karakterlik arama büyük tabloda neredeyse
 * tüm satırları döndürür ve kullanıcıya bilgi vermez ("her şey eşleşti").
 * Trigram indexler de tek karakter için kullanılamaz. Bu yüzden 2 karakterin
 * altındaki aramalar <b>boş sonuç</b> döner.
 *
 * <p><b>Boş girdi ile kısa girdi AYRIMI (önemli):</b> Kullanıcı arama kutusuna
 * hiçbir şey yazmadığında filtre uygulanmaz ({@code null} -> tüm liste). Ancak
 * tek karakter yazdığında {@code null} dönersek kullanıcı "filtreledim sanıp"
 * tüm listeyi görürdü — bu yanıltıcıdır ve büyük tablolarda tüm kayıtları
 * belleğe çeker. Bu yüzden kısa girdi {@link #HIC_ESLESMEYEN} desenine çevrilir:
 * kullanıcı boş sonuç görür ve daha fazla karakter yazmayı anlar.
 *
 * <p><b>3) Uzunluk sınırı:</b> Aşırı uzun arama terimleri (gigabayt string)
 * gereksiz bellek ve CPU tüketir; 100 karakterle sınırlanır.
 *
 * <p><b>SQL injection değildir:</b> Değerler JPA parametre bind ile gider
 * (güvenle taşınır). Buradaki işlem SQL semantiğine müdahale eder.
 */
public final class AramaTemizleyici {

    /** Bu kısalttan kısa aramalar sonuç üretmez. */
    public static final int MIN_ARAMA_UZUNLUK = 2;

    /** Maksimum kabul edilen arama terimi uzunluğu. */
    public static final int MAKS_ARAMA_UZUNLUK = 100;

    /**
     * Hiçbir kayıtla eşleşmeyen desen (kullanıcı 1 karakter yazdığında).
     *
     * <p>Bileşenler bilinçli olarak seçildi:
     * <ul>
     *   <li>{@link #HIC_ESLESMEYEN_RAR} {@link #MIN_ARAMA_UZUNLUK} karakterden
     *       uzun olduğu için bir arama terimi olarak <b>hiçbir zaman</b>
     *       üretilemez (kısaltma olsa bile alt sınır korunur).</li>
     *   <li>İçinde joker karakter ({@code %} veya {@code _}) <b>yoktur</b>;
     *       aksi halde desenin kendisi joker'e dönüşürdü.</li>
     *   <li>Tırnak/boşluk/bolus yoktur; SQL'de sorun çıkarmaz.</li>
     * </ul>
     */
    private static final String HIC_ESLESMEYEN_RAR = "hicEslesmeyenNadirDizi";
    private static final String HIC_ESLESMEYEN = "%" + HIC_ESLESMEYEN_RAR + "%";

    private AramaTemizleyici() {
    }

    /**
     * Arama terimini {@code LIKE} deseni için hazırlar.
     *
     * @param q kullanıcı girdisi (ham)
     * @return {@code null} yalnızca <b>terim boşsa</b>; aksi halde kaçışlanmış
     *         {@code %değer%} deseni ya da {@link #HIC_ESLESMEYEN}
     */
    public static String like(String q) {
        String temiz = temizle(q);
        if (temiz == null) {
            // Girdi boşsa filtre uygulanmaz (null döner).
            return bosMu(q) ? null : HIC_ESLESMEYEN;
        }
        return "%" + kacisJoker(temiz.toLowerCase(java.util.Locale.ROOT)) + "%";
    }

    private static boolean bosMu(String q) {
        return q == null || q.trim().isEmpty();
    }

    /**
     * Arama terimini normalize eder: kırpar, kısalta ve gereksiz karakterleri
     * atar.
     *
     * @return normalize edilmiş metin veya {@code null}
     */
    public static String temizle(String q) {
        if (q == null) return null;
        String s = q.trim();
        if (s.isEmpty()) return null;
        if (s.length() > MAKS_ARAMA_UZUNLUK) {
            s = s.substring(0, MAKS_ARAMA_UZUNLUK);
        }
        // Kontrol karakterleri JDBC'de geçersizdir ve anlamsızdır.
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isISOControl(c)) sb.append(c);
        }
        String sonuc = sb.toString().trim();
        if (sonuc.length() < MIN_ARAMA_UZUNLUK) return null;
        return sonuc;
    }

    /**
     * LIKE joker karakterlerini kaçışlar.
     *
     * <p><b>Önemli:</b> JPQL içinde standart SQL {@code ESCAPE} kullanımı için
     * desende ters eğik çizgi kullanılır. Hibernate/H2/PostgreSQL'in üçünde de
     * {@code ESCAPE '\'} ile birlikte çalışır; sorgularda mutlaka
     * {@code LIKE :q ESCAPE '\'} yazılmalıdır, aksi halde kaçış ters çevrilir.
     */
    private static String kacisJoker(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}