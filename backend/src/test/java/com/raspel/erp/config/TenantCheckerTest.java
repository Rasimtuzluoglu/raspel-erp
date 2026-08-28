package com.raspel.erp.config;

import com.raspel.erp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class TenantCheckerTest {

    private final TenantChecker tenantChecker = new TenantChecker();

    @AfterEach
    void temizle() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void oturumSirket(Long sirketId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("sirketId", sirketId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void check_ayniSirketIcinGecerli() {
        oturumSirket(1L);
        assertDoesNotThrow(() -> tenantChecker.check(1L, "Test"));
    }

    @Test
    void check_farkliSirketIcinReddedilir() {
        oturumSirket(1L);
        assertThrows(ResourceNotFoundException.class, () -> tenantChecker.check(2L, "Test"));
    }

    @Test
    void tenantKey_sirketBilgisiIcerir() {
        oturumSirket(3L);
        assertEquals("42:3", TenantChecker.tenantKey(42L));
    }

    @Test
    void tenantKey_farkliSirketIcinFarklidir() {
        oturumSirket(3L);
        String anahtar3 = TenantChecker.tenantKey(42L);
        oturumSirket(9L);
        String anahtar9 = TenantChecker.tenantKey(42L);
        assertNotEquals(anahtar3, anahtar9);
    }

    @Test
    void tenantKey_requestYoksaSadeceIdDoner() {
        assertEquals("42", TenantChecker.tenantKey(42L));
    }
}
