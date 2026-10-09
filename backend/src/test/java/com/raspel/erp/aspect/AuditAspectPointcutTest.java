package com.raspel.erp.aspect;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Denetim izi (audit) pointcut'inin hangi controller metotlarını yakaladığını
 * doğrular.
 *
 * <p>REDTEAM/Wave 2.2: pointcut metot <b>isim örüntüsüne</b> dayanıyordu ve
 * depo stok giriş/çıkış/transfer, stok düzeltme, POS gün sonu, açılış devri,
 * mali dönem kilidi, bordro hesabı/onayı ve masraf talebi onayı bu örüntüye
 * uymadığı için denetim izi bırakmıyordu.
 *
 * <p>Bu test, AOP'nin ifadeyi gerçekten nasıl değerlendirdiğini kullanır; metot
 * adlarında 'contains' aramaz.
 */
class AuditAspectPointcutTest {

    private final AspectJExpressionPointcut pointcut = build();

    private static AspectJExpressionPointcut build() {
        try {
            Method m = AuditAspect.class.getDeclaredMethod("kritikPointcut");
            org.aspectj.lang.annotation.Pointcut p = m.getAnnotation(org.aspectj.lang.annotation.Pointcut.class);
            AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
            pc.setExpression(p.value());
            return pc;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean eslesiyor(Class<?> tip, String metotAdi) {
        Method hedef = null;
        for (Method m : tip.getDeclaredMethods()) {
            if (m.getName().equals(metotAdi)) { hedef = m; break; }
        }
        if (hedef == null) throw new IllegalStateException("Metot yok: " + tip.getSimpleName() + "#" + metotAdi);
        return pointcut.matches(hedef, tip);
    }

    @Test
    void depoStokHareketleriDenetimAltinda() {
        Class<?> depo = sinifBul("com.raspel.erp.controller.sube.DepoController");
        assertTrue(eslesiyor(depo, "stokEkle"), "depo stok girişi denetim izi birakmamali");
        assertTrue(eslesiyor(depo, "stokCikar"), "depo stok çıkışı denetim izi birakmamali");
        assertTrue(eslesiyor(depo, "stokTransfer"), "depo transferi denetim izi birakmamali");
    }

    @Test
    void stokDuzeltmeSayimVeAcilisDenetimAltinda() {
        Class<?> duzeltme = sinifBul("com.raspel.erp.controller.envanter.StokDuzeltmeController");
        assertTrue(eslesiyor(duzeltme, "duzelt"), "stok düzeltmesi denetim izi birakmamali");

        Class<?> sayim = sinifBul("com.raspel.erp.controller.envanter.StokSayimController");
        assertTrue(eslesiyor(sayim, "tara"), "stok sayım taraması denetim izi birakmamali");

        Class<?> hareket = sinifBul("com.raspel.erp.controller.finans.HareketController");
        assertTrue(eslesiyor(hareket, "acilis"), "açılış devri denetim izi birakmamali");
    }

    @Test
    void donemKilidiDenetimAltinda() {
        Class<?> donem = sinifBul("com.raspel.erp.controller.sistem.DonemController");
        assertTrue(eslesiyor(donem, "kilitle"), "dönem kilitleme denetim izi birakmamali");
        assertTrue(eslesiyor(donem, "kilidiAc"), "dönem kilidi açma denetim izi birakmamali");
        assertTrue(eslesiyor(donem, "yilSonuKapat"), "mali yıl sonu kapanışı denetim izi birakmamali");
    }

    @Test
    void bordroVeOnayDenetimAltinda() {
        Class<?> bordro = sinifBul("com.raspel.erp.controller.ik.MaasBordroController");
        assertTrue(eslesiyor(bordro, "hesapla"), "bordro hesabı denetim izi birakmamali");
        assertTrue(eslesiyor(bordro, "onayla"), "bordro onayı denetim izi birakmamali");
        assertTrue(eslesiyor(bordro, "onayKaldir"), "bordro onay kaldırma denetim izi birakmamali");
        assertTrue(eslesiyor(bordro, "topluUret"), "toplu bordro üretimi denetim izi birakmamali");

        Class<?> talep = sinifBul("com.raspel.erp.controller.ik.PersonelMasrafTalepController");
        assertTrue(eslesiyor(talep, "onayla"), "masraf talebi onayı denetim izi birakmamali");
        assertTrue(eslesiyor(talep, "reddet"), "masraf talebi reddi denetim izi birakmamali");
    }

    @Test
    void posGunSonuDenetimAltinda() {
        Class<?> pos = sinifBul("com.raspel.erp.controller.finans.PosTerminaliController");
        assertTrue(eslesiyor(pos, "gunSonu"), "POS gün sonu denetim izi birakmamali");
    }

    @Test
    void okumaUclariDenetimDisiKalmali() {
        // Denetim izi gürültü olmamalı: saf okuma uçları yakalanmamalı.
        Class<?> rapor = sinifBul("com.raspel.erp.controller.sistem.RaporController");
        assertFalse(eslesiyor(rapor, "karlilikDetay"), "okuma ucu denetim altina girmemeli");
        assertFalse(eslesiyor(rapor, "karlilikDetay"), "okuma ucu denetim altina girmemeli");
    }

    @Test
    void mevcutKapsamKorunuyor() {
        // Önceden kapsananlar geriye dönük bozulmamalı.
        Class<?> fatura = sinifBul("com.raspel.erp.controller.ticaret.FaturaController");
        assertTrue(eslesiyor(fatura, "faturaOlustur"), "fatura oluşturma denetim altında olmalı");
        assertTrue(eslesiyor(fatura, "iptaliGeriAl"), "fatura iptal geri alma denetim altında olmalı");
    }

    @Test
    void sifreKasasiArsivlemeDenetimAltinda() {
        // Wave 4: arsivleme (soft delete) ve geri alma denetim izi bırakmalı.
        // "Kim arşivledi / kim geri aldı" sorusu cevaplanabilmeli.
        Class<?> kasa = sinifBul("com.raspel.erp.controller.sistem.SifreKasaController");
        assertTrue(eslesiyor(kasa, "arsivle"), "arsivleme denetim izi birakmamali");
        assertTrue(eslesiyor(kasa, "arsivleGeriAl"), "arsivden geri alma denetim izi birakmamali");
    }

    private static Class<?> sinifBul(String ad) {
        try {
            return Class.forName(ad);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Sınıf bulunamadı: " + ad, e);
        }
    }
}