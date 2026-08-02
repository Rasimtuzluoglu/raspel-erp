package com.raspel.erp.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * RFC 6238 TOTP uygulaması. Yalnızca JDK yerleşik kripto (HmacSHA1) kullanır.
 * Google Authenticator / Authy gibi uygulamalarla uyumlu 6 haneli kod üretir.
 */
public final class TotpUtil {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";

    private TotpUtil() {}

    public static String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        String secret = base32Encode(bytes).replace("=", "");
        return secret.substring(0, Math.min(secret.length(), 32));
    }

    public static String generateCode(String base32Secret, long timestampMillis) {
        long counter = timestampMillis / 1000 / TIME_STEP_SECONDS;
        return generateCounterCode(base32Secret, counter);
    }

    public static boolean validate(String base32Secret, String code, long timestampMillis) {
        if (base32Secret == null || code == null) return false;
        String normalized = code.trim();
        for (long drift = -1; drift <= 1; drift++) {
            long counter = timestampMillis / 1000 / TIME_STEP_SECONDS + drift;
            String expected = generateCounterCode(base32Secret, counter);
            if (constantTimeEquals(expected, normalized)) return true;
        }
        return false;
    }

    private static String generateCounterCode(String base32Secret, long counter) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] message = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(message);

            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("TOTP üretilemedi", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ---------- RFC 4648 Base32 ----------
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32_ALPHABET.charAt((value >>> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            result.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1f));
        }
        return result.toString();
    }

    public static byte[] base32Decode(String input) {
        String clean = input.toUpperCase().replace(" ", "").replace("=", "");
        ByteBuffer output = ByteBuffer.allocate(clean.length() * 5 / 8 + 8);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) continue;
            buffer = (buffer << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                output.put((byte) ((buffer >>> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        byte[] result = new byte[output.position()];
        System.arraycopy(output.array(), 0, result, 0, result.length);
        return result;
    }

    public static String otpauthUri(String issuer, String username, String secret) {
        String label = issuer + ":" + username;
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }
}
