package com.raspel.erp.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM şifreleme yardımcıları. Yedek dosyalarının bulut (MinIO) kopyalarını
 * şifrelemek için kullanılır. Çıktı formatı: [12 bayt IV] + [şifreli veri]
 * (GCM tag = 128 bit, otomatik ekli). Anahtar env'den gelen herhangi uzunlukta bir
 * dize olabilir; AES geçerli anahtar uzunluğuna (16/24/32 bayt) SHA-256 ile normalize edilir.
 */
public final class AesGcmUtil {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private AesGcmUtil() {}

    /**
     * Anahtar dizesini sabit 32 baytlık (AES-256) anahtara normalize eder.
     */
    private static byte[] normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Şifreleme anahtarı tanımlı değil (APP_BACKUP_ENCRYPTION_KEY / AI_ENCRYPTION_KEY)");
        }
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 bulunamadı", e);
        }
    }

    public static byte[] encrypt(byte[] plain, String key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(normalizeKey(key), ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plain);
            byte[] ivAndEncrypted = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length);
            return ivAndEncrypted;
        } catch (Exception e) {
            throw new IllegalStateException("Yedek şifrelenemedi: " + e.getMessage(), e);
        }
    }

    public static byte[] decrypt(byte[] ivAndEncrypted, String key) {
        try {
            if (ivAndEncrypted.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Geçersiz şifreli veri");
            }
            byte[] iv = Arrays.copyOfRange(ivAndEncrypted, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(ivAndEncrypted, GCM_IV_LENGTH, ivAndEncrypted.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(normalizeKey(key), ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Yedek çözülemedi: " + e.getMessage(), e);
        }
    }
}