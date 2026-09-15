package com.raspel.erp.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmUtilTest {

    private static final String KEY = "gizli-yedek-anahtari-2026";

    @Test
    void encryptDecrypt_yuvarlakCalisir() {
        byte[] duz = "RasPel ERP yedek verisi".getBytes(StandardCharsets.UTF_8);
        byte[] sifreli = AesGcmUtil.encrypt(duz, KEY);
        byte[] cozulen = AesGcmUtil.decrypt(sifreli, KEY);
        assertArrayEquals(duz, cozulen);
    }

    @Test
    void encrypt_ivIcerir_veVeridenBuyuktur() {
        byte[] duz = new byte[64];
        byte[] sifreli = AesGcmUtil.encrypt(duz, KEY);
        assertTrue(sifreli.length > duz.length, "IV + GCM tag nedeniyle çıktı girdiden büyük olmalı");
        assertTrue(sifreli.length - duz.length >= 28, "12 bayt IV + 16 bayt tag");
    }

    @Test
    void encrypt_ayniveriFarkliIverIleFarkliCikar() {
        byte[] duz = "aynı veri".getBytes(StandardCharsets.UTF_8);
        byte[] ilk = AesGcmUtil.encrypt(duz, KEY);
        byte[] ikinci = AesGcmUtil.encrypt(duz, KEY);
        assertFalse(Arrays.equals(ilk, ikinci), "Her şifrelemede rastgele IV üretilmeli");
    }

    @Test
    void decrypt_yanlisAnahtarlaBasarisizOlur() {
        byte[] sifreli = AesGcmUtil.encrypt("veri".getBytes(StandardCharsets.UTF_8), KEY);
        assertThrows(IllegalStateException.class, () -> AesGcmUtil.decrypt(sifreli, "yanlis-anahtar"));
    }

    @Test
    void decrypt_kisaVeriIcinHataFirlatir() {
        assertThrows(IllegalStateException.class, () -> AesGcmUtil.decrypt(new byte[4], KEY));
    }

    @Test
    void encrypt_bosAnahtarIcinHataFirlatir() {
        assertThrows(IllegalStateException.class,
                () -> AesGcmUtil.encrypt(new byte[8], "   "));
    }

    @Test
    void decrypt_onceSifrelenenDuzgunCozulur() {
        byte[] veri = ("1234567890-" + "x".repeat(1000)).getBytes(StandardCharsets.UTF_8);
        byte[] sifreli = AesGcmUtil.encrypt(veri, KEY);
        assertEquals(new String(veri, StandardCharsets.UTF_8),
                new String(AesGcmUtil.decrypt(sifreli, KEY), StandardCharsets.UTF_8));
    }
}