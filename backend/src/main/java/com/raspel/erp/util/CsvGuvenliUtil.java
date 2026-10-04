package com.raspel.erp.util;

/**
 * CSV dışa aktarımı için formül enjeksiyonu (CSV injection) koruması.
 *
 * <p><b>Saldırı:</b> Excel/LibreOffice/Sheets bir CSV hücresinde
 * {@code =}, {@code +}, {@code -}, {@code @} ile başlayan değeri FORMÜL olarak
 * değerlendirir. Dışa aktarılan veri kullanıcı tarafından girebiliyorsa
 * (cari adı, açıklama, stok adı) saldırgan şu formülü yazabilir:
 * <pre>
 *   =HYPERLINK("http://kotu.example","tikla")   → kullanıcı veri sızdırır
 *   =cmd|'/C calc'!A0                          → LibreOffice'de komut çalıştırır
 *   @SUM(1+9)*cmd|'/C calc'!A0                 → @ ile başlayan varyant
 * </pre>
 * Bu, uygulamanın kullanıcı verisini kullanıcının makinesinde <b>kod
 * çalıştırmasına</b> yol açar.
 *
 * <p><b>Bu sınıfın çözdüğü ek açıklar:</b> Uygulamada 3 kopya {@code csvSafe}
 * vardı ve üçü de yalnızca <b>ilk karaktere</b> bakıyordu. Aşağıdaki
 * varyantlar tümünü atlatıyordu:
 * <ul>
 *   <li>{@code " =1+1"} — Excel öncü boşlukları yok sayar, yine formül çalışır.</li>
 *   <li>{@code "\t=1+1"} / {@code "\r=1+1"} — sekme/SAT başlangıcı kontrolü atlatır.</li>
 * </ul>
 * Bu yüzden kontrol <b>baştaki boşluk/tab/satır sonu karakterleri atlandıktan
 * sonra</b> yapılır. Ayrıca bu karakterler CSV satır/sütun yapısını bozduğu
 * için nötrleştirilir.
 *
 * <p><b>Kullanım:</b> Alan zaten tırnak içine alınıyorsa
 * {@link #deger(String)} (tırnaklı) kullanılmalıdır.
 */
public final class CsvGuvenliUtil {

    /** Formül tetikleyici karakterler. */
    private static final char[] TETIKLEYICI = {'=', '+', '-', '@'};

    /** Hücre yapısını bozan ve formül kontrolünü atlatmaya yarayan karakterler. */
    private static final String KONTROL_KARAKTERLERI = "\t\r\n";

    private CsvGuvenliUtil() {
    }

    /**
     * Değeri CSV formül enjeksiyonuna karşı korur; tırnak içine almaz.
     * Çağıran zaten tırnak ekliyorsa bunu kullanmamalıdır.
     *
     * @return güvenli düz metin (null için boş string)
     */
    public static String deger(String deger) {
        if (deger == null) return "";
        String s = deger;

        // 1) Hücre yapısını bozan karakterleri temizle.
        s = temizleKontrolKarakterleri(s);

        // 2) Baştaki boşluk/sekme/satır sonlarını atla; Excel bunları yok sayar,
        //    bu yüzden kontrol buradan sonra yapılmalıdır.
        String kontrolEdilecek = atlaBasBosluk(s);

        // 3) Formül tetikleyicisi -> apostrof ile nötrleştir.
        if (!kontrolEdilecek.isEmpty() && tetikleyiciMi(kontrolEdilecek.charAt(0))) {
            s = "'" + s;
        }

        // 4) Tırnak kaçışı (CSV ayırıcı olarak da kullanılır).
        return s.replace("\"", "\"\"");
    }

    /**
     * Değeri tamamen korumalı ve tırnak içine alınmış CSV alanına çevirir.
     *
     * @return {@code "değer"} biçiminde alan
     */
    public static String alan(String deger) {
        if (deger == null || deger.isBlank()) return "\"\"";
        return "\"" + deger(deger.trim()) + "\"";
    }

    private static boolean tetikleyiciMi(char c) {
        for (char t : TETIKLEYICI) {
            if (t == c) return true;
        }
        return false;
    }

    private static String temizleKontrolKarakterleri(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (KONTROL_KARAKTERLERI.indexOf(c) >= 0) {
                // Sekme/SAT/SATIR sonu hücre dışına çıkabilir ve formül kontrolünü
                // atlatabilirdi; düz boşluğa çevrilir.
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String atlaBasBosluk(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }
}