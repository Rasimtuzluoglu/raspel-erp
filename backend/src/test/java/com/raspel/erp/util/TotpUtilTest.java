package com.raspel.erp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpUtilTest {

    private static final String TEST_SECRET = "JBSWY3DPEHPK3PXP"; // RFC 6238 örnek anahtarı (ASCII "12345678901234567890")

    @Test
    void testGenerateSecret_Base32Format() {
        String secret = TotpUtil.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
        assertTrue(secret.matches("[A-Z2-7]+"), "Secret base32 karakterlerinden oluşmalı");
    }

    @Test
    void testGenerateCode_AltıHaneUretir() {
        String code = TotpUtil.generateCode(TEST_SECRET, System.currentTimeMillis());
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testValidate_DogruKodKabulEdilir() {
        long now = System.currentTimeMillis();
        String code = TotpUtil.generateCode(TEST_SECRET, now);
        assertTrue(TotpUtil.validate(TEST_SECRET, code, now));
    }

    @Test
    void testValidate_YanlisKodReddedilir() {
        assertFalse(TotpUtil.validate(TEST_SECRET, "000000", System.currentTimeMillis()));
    }

    @Test
    void testValidate_NullGirdilerReddedilir() {
        assertFalse(TotpUtil.validate(null, "123456", System.currentTimeMillis()));
        assertFalse(TotpUtil.validate(TEST_SECRET, null, System.currentTimeMillis()));
    }

    @Test
    void testBase32_IleriGeriDonusum() {
        byte[] data = new byte[]{0x48, 0x65, 0x6c, 0x6c, 0x6f}; // "Hello"
        String encoded = TotpUtil.base32Encode(data);
        byte[] decoded = TotpUtil.base32Decode(encoded);
        assertArrayEquals(data, decoded);
    }

    @Test
    void testOtpAuthUri_Format() {
        String uri = TotpUtil.otpauthUri("RasPelERP", "admin", TEST_SECRET);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("issuer=RasPelERP"));
        assertTrue(uri.contains("secret=" + TEST_SECRET));
    }
}
