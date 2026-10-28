package com.raspel.erp.entity.sistem;

/**
 * Kategori kapsamı. Tek registry içinde hangi modüle ait olduğunu belirtir.
 *
 * <ul>
 *   <li>{@code KASA_GELIR} / {@code KASA_GIDER} — kasa hareket kategorileri</li>
 *   <li>{@code STOK} — stok kartı ve iskonto kuralı kategorileri</li>
 *   <li>{@code MASRAF} — finans masraf kategorileri (adlar büyük harf)</li>
 *   <li>{@code BUTCE} — finans bütçe kategorileri (adlar büyük harf)</li>
 * </ul>
 */
public enum KategoriKapsam {
    KASA_GELIR,
    KASA_GIDER,
    STOK,
    MASRAF,
    BUTCE;

    /** Kasa kapsamı mı? (legacy {@code tur} alanı bu kapsamlarda doludur) */
    public boolean kasaKapsami() {
        return this == KASA_GELIR || this == KASA_GIDER;
    }

    /** Ad büyük harfe normalize edilir (masraf/bütçe eşleşmesi korunur). */
    public boolean buyukHarf() {
        return this == MASRAF || this == BUTCE;
    }

    /** Legacy {@code tur} değerinden kapsam çözümü (GELIR/GIDER). */
    public static KategoriKapsam turdan(String tur) {
        if (tur == null || tur.isBlank()) return null;
        String t = tur.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (t) {
            case "GELIR", "KASA_GELIR" -> KASA_GELIR;
            case "GIDER", "KASA_GIDER" -> KASA_GIDER;
            default -> null;
        };
    }

    /** Legacy {@code tur} karşılığı (kasa dışı kapsamlarda null). */
    public String turKarsiligi() {
        return switch (this) {
            case KASA_GELIR -> "GELIR";
            case KASA_GIDER -> "GIDER";
            default -> null;
        };
    }
}
