package com.raspel.erp.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ApiVersionAliasFilterTest {

    private final ApiVersionAliasFilter filter = new ApiVersionAliasFilter();

    @Test
    void v1YoluApiYoluOlarakNormalizeEdilir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cari-hesaplar");
        request.setRequestURI("/api/v1/cari-hesaplar");
        AtomicReference<String> gorulenUri = new AtomicReference<>();
        FilterChain chain = (req, res) -> gorulenUri.set(((jakarta.servlet.http.HttpServletRequest) req).getRequestURI());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("/api/cari-hesaplar", gorulenUri.get());
    }

    @Test
    void v1OlmayanYolDegismez() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cari-hesaplar");
        request.setRequestURI("/api/cari-hesaplar");
        AtomicReference<String> gorulenUri = new AtomicReference<>();
        FilterChain chain = (req, res) -> gorulenUri.set(((jakarta.servlet.http.HttpServletRequest) req).getRequestURI());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("/api/cari-hesaplar", gorulenUri.get());
    }
}
