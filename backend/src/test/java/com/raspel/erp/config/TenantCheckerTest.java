package com.raspel.erp.config;

import com.raspel.erp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class TenantCheckerTest {

    private final TenantChecker tenantChecker = new TenantChecker();

    @BeforeEach
    void setUp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("sirketId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getCurrentSirketId_requestAttrsindenOkur() {
        assertEquals(1L, tenantChecker.getCurrentSirketId());
    }

    @Test
    void check_ayniSirket_izinVerir() {
        assertDoesNotThrow(() -> tenantChecker.check(1L, "Stok"));
    }

    @Test
    void check_farkliSirket_ResourceNotFoundFirlatir() {
        assertThrows(ResourceNotFoundException.class, () -> tenantChecker.check(2L, "Stok"));
    }

    @Test
    void check_nullEntitySirket_izinVerir() {
        assertDoesNotThrow(() -> tenantChecker.check(null, "Stok"));
    }

    @Test
    void checkSirketId_ayniSirket_izinVerir() {
        assertDoesNotThrow(() -> tenantChecker.checkSirketId(1L, "Personel"));
    }

    @Test
    void checkSirketId_farkliSirket_ResourceNotFoundFirlatir() {
        assertThrows(ResourceNotFoundException.class, () -> tenantChecker.checkSirketId(3L, "Personel"));
    }

    @Test
    void requestContextYoksa_kontrolYapilmaz() {
        RequestContextHolder.resetRequestAttributes();
        assertDoesNotThrow(() -> tenantChecker.check(5L, "Stok"));
        assertDoesNotThrow(() -> tenantChecker.checkSirketId(5L, "Stok"));
        assertNull(tenantChecker.getCurrentSirketId());
    }
}
