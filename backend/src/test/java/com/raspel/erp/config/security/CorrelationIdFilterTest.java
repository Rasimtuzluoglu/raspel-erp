package com.raspel.erp.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void gelenKimlikKullanilirVeYanitaEklenir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            // İstek işlenirken MDC'de kimlik görünür olmalı.
            assertEquals("abc-123", MDC.get(CorrelationIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertEquals("abc-123", response.getHeader(CorrelationIdFilter.HEADER));
        // İstek sonrası MDC temizlenir (thread pool kirlenmez).
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void kimlikYoksaUretilir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void asiriUzunKimlikYenidenUretilir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "x".repeat(200));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(id);
        assertTrue(id.length() <= 64);
    }
}
