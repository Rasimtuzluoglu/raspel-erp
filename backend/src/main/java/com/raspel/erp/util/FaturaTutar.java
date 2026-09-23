package com.raspel.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Fatura/satış tutar hesabı — TEK KANONİK KAYNAK.
 *
 * <p>Model: <b>birim fiyat KDV DAHİL</b>dir (perakende etiket fiyatı). KDV, iskonto
 * sonrası brüt tutardan ayrıştırılır; böylece ekranda/fişte gösterilen ve tahsil
 * edilen tutar ile kayıtlı {@code genelToplam} birebir aynı olur.
 *
 * <p>Değişmezler: {@code araToplam + kdv == genelToplam} ve
 * {@code genelToplam == brutToplam - genelIskonto} (yuvarlama toleransıyla).
 */
public final class FaturaTutar {

    private static final BigDecimal YUZ = BigDecimal.valueOf(100);

    private FaturaTutar() {
    }

    /** Satır bazında KDV hariç net, KDV ve KDV dahil (iskonto sonrası) brüt tutar. */
    public record Satir(BigDecimal net, BigDecimal kdv, BigDecimal brut) {
    }

    /** Belge bazında ara toplam (KDV hariç matrah), KDV ve genel toplam. */
    public record Belge(BigDecimal araToplam, BigDecimal kdv, BigDecimal genelToplam, BigDecimal brutToplam) {
    }

    /**
     * KDV dahil birim fiyattan satır tutarlarını hesaplar.
     *
     * @param birimFiyatKdvDahil KDV dahil birim fiyat
     * @param adet               miktar
     * @param iskontoOrani       satır iskonto oranı (yüzde, null/0 olabilir)
     * @param kdvOrani           KDV oranı (yüzde)
     */
    public static Satir satir(BigDecimal birimFiyatKdvDahil, BigDecimal adet,
                              BigDecimal iskontoOrani, BigDecimal kdvOrani) {
        BigDecimal bf = nz(birimFiyatKdvDahil);
        BigDecimal ad = nz(adet);
        BigDecimal isk = nz(iskontoOrani);
        BigDecimal kdv = nz(kdvOrani);

        BigDecimal brut = bf.multiply(ad);
        BigDecimal iskontoTutari = brut.multiply(isk).divide(YUZ, 2, RoundingMode.HALF_UP);
        BigDecimal iskontoluBrut = brut.subtract(iskontoTutari);
        if (iskontoluBrut.signum() < 0) iskontoluBrut = BigDecimal.ZERO;

        // net = iskontoluBrut / (1 + kdv/100); kdv = iskontoluBrut - net
        BigDecimal bolen = BigDecimal.ONE.add(kdv.divide(YUZ, 6, RoundingMode.HALF_UP));
        BigDecimal net = iskontoluBrut.divide(bolen, 2, RoundingMode.HALF_UP);
        BigDecimal kdvTutari = iskontoluBrut.subtract(net);
        return new Satir(net, kdvTutari, iskontoluBrut);
    }

    /**
     * Satırlardan genel iskonto (KDV dahil tutar) sonrası belge toplamlarını üretir.
     * Genel iskonto, net ve KDV'ye aynı oranda yansıtılır; {@code araToplam + kdv == genelToplam} korunur.
     */
    public static Belge belge(List<Satir> satirlar, BigDecimal genelIskontoKdvDahil) {
        BigDecimal brutToplam = BigDecimal.ZERO;
        BigDecimal netToplam = BigDecimal.ZERO;
        BigDecimal kdvToplam = BigDecimal.ZERO;
        for (Satir s : satirlar) {
            brutToplam = brutToplam.add(s.brut());
            netToplam = netToplam.add(s.net());
            kdvToplam = kdvToplam.add(s.kdv());
        }

        BigDecimal iskonto = nz(genelIskontoKdvDahil);
        if (iskonto.signum() < 0) iskonto = BigDecimal.ZERO;
        if (iskonto.compareTo(brutToplam) > 0) iskonto = brutToplam;

        if (brutToplam.signum() == 0 || iskonto.signum() == 0) {
            return new Belge(netToplam, kdvToplam, netToplam.add(kdvToplam), brutToplam);
        }
        BigDecimal oran = brutToplam.subtract(iskonto).divide(brutToplam, 8, RoundingMode.HALF_UP);
        BigDecimal araToplam = netToplam.multiply(oran).setScale(2, RoundingMode.HALF_UP);
        BigDecimal kdv = kdvToplam.multiply(oran).setScale(2, RoundingMode.HALF_UP);
        return new Belge(araToplam, kdv, araToplam.add(kdv), brutToplam);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
