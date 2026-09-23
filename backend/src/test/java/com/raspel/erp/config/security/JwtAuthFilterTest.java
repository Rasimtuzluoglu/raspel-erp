package com.raspel.erp.config.security;

import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.service.sistem.AktifOturumService;
import com.raspel.erp.service.sistem.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private AktifOturumService aktifOturumService;
    @Mock private ApiTokenService apiTokenService;
    @Mock private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, userDetailsService, kullaniciRepository,
                aktifOturumService, apiTokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails kullaniciDetay() {
        return org.springframework.security.core.userdetails.User
                .withUsername("ali").password("p")
                .authorities("ROLE_ADMIN").build();
    }

    @Test
    void tokenYoksa_zincyZinciriDevamEder_veAuthYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(eq(request), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void gecerliBearerToken_attributesVeAuthAyarlanir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer tok");
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("tok")).thenReturn("jti1");
        when(aktifOturumService.iptalEdilmis("jti1")).thenReturn(false);
        when(aktifOturumService.aktifOturumMu(5L, "jti1")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tok")).thenReturn(5L);
        when(jwtUtil.getSirketIdFromToken("tok")).thenReturn(7L);
        when(jwtUtil.getDisplayNameFromToken("tok")).thenReturn("Ali");
        when(jwtUtil.getUsernameFromToken("tok")).thenReturn("ali");
        when(jwtUtil.getTokenVersionFromToken("tok")).thenReturn(0L);
        when(kullaniciRepository.findByUsername("ali"))
                .thenReturn(Optional.of(Kullanici.builder().tokenVersion(0L).build()));
        when(userDetailsService.loadUserByUsername("ali")).thenReturn(kullaniciDetay());

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertEquals(5L, request.getAttribute("kullaniciId"));
        assertEquals(7L, request.getAttribute("sirketId"));
        assertEquals("Ali", request.getAttribute("displayName"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void gecersizToken_authYok_zincyDevamEder() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer bad");
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void jwtCookie_headerYoksaKullanilir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.setCookies(new Cookie("jwt", "cookieTok"));
        when(jwtUtil.validateToken("cookieTok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("cookieTok")).thenReturn("jtiC");
        when(aktifOturumService.iptalEdilmis("jtiC")).thenReturn(false);
        when(aktifOturumService.aktifOturumMu(9L, "jtiC")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("cookieTok")).thenReturn(9L);
        when(jwtUtil.getSirketIdFromToken("cookieTok")).thenReturn(3L);
        when(jwtUtil.getDisplayNameFromToken("cookieTok")).thenReturn("Veli");
        when(jwtUtil.getUsernameFromToken("cookieTok")).thenReturn("veli");
        when(jwtUtil.getTokenVersionFromToken("cookieTok")).thenReturn(0L);
        when(kullaniciRepository.findByUsername("veli"))
                .thenReturn(Optional.of(Kullanici.builder().tokenVersion(0L).build()));
        when(userDetailsService.loadUserByUsername("veli")).thenReturn(kullaniciDetay());

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertEquals(9L, request.getAttribute("kullaniciId"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void iptalEdilmisOturum_authYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer tok");
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("tok")).thenReturn("jti1");
        when(aktifOturumService.iptalEdilmis("jti1")).thenReturn(true);

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void gecerliOturumDegilse_authYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer tok");
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("tok")).thenReturn("jti1");
        when(aktifOturumService.iptalEdilmis("jti1")).thenReturn(false);
        when(jwtUtil.getUserIdFromToken("tok")).thenReturn(5L);
        when(aktifOturumService.aktifOturumMu(5L, "jti1")).thenReturn(false);

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenVersionUyusmazsa_authYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer tok");
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("tok")).thenReturn("jti1");
        when(aktifOturumService.iptalEdilmis("jti1")).thenReturn(false);
        when(aktifOturumService.aktifOturumMu(5L, "jti1")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tok")).thenReturn(5L);
        when(jwtUtil.getSirketIdFromToken("tok")).thenReturn(7L);
        when(jwtUtil.getDisplayNameFromToken("tok")).thenReturn("Ali");
        when(jwtUtil.getUsernameFromToken("tok")).thenReturn("ali");
        when(jwtUtil.getTokenVersionFromToken("tok")).thenReturn(0L);
        when(kullaniciRepository.findByUsername("ali"))
                .thenReturn(Optional.of(Kullanici.builder().tokenVersion(5L).build()));

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void kullaniciPasifse_authYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer tok");
        when(jwtUtil.validateToken("tok")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("tok")).thenReturn("jti1");
        when(aktifOturumService.iptalEdilmis("jti1")).thenReturn(false);
        when(aktifOturumService.aktifOturumMu(5L, "jti1")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tok")).thenReturn(5L);
        when(jwtUtil.getSirketIdFromToken("tok")).thenReturn(7L);
        when(jwtUtil.getDisplayNameFromToken("tok")).thenReturn("Ali");
        when(jwtUtil.getUsernameFromToken("tok")).thenReturn("ali");
        when(jwtUtil.getTokenVersionFromToken("tok")).thenReturn(0L);
        when(kullaniciRepository.findByUsername("ali"))
                .thenReturn(Optional.of(Kullanici.builder().tokenVersion(0L).build()));
        when(userDetailsService.loadUserByUsername("ali")).thenReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername("ali").password("p").disabled(true).authorities("ROLE_ADMIN").build());

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void kisiselErisimTokeni_gecerliyseAuthAyarlanir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer raspel_pat_abc");
        Kullanici k = Kullanici.builder().id(4L).username("patuser").displayName("PAT")
                .sirketId(2L).active(true).build();
        when(apiTokenService.tokenIleKullaniciBul("raspel_pat_abc")).thenReturn(k);
        when(userDetailsService.loadUserByUsername("patuser")).thenReturn(kullaniciDetay());

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertEquals(4L, request.getAttribute("kullaniciId"));
        assertEquals(2L, request.getAttribute("sirketId"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(apiTokenService).sonKullanimGuncelle("raspel_pat_abc");
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void kisiselErisimTokeni_gecersizse_authYok() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.addHeader("Authorization", "Bearer raspel_pat_bad");
        when(apiTokenService.tokenIleKullaniciBul("raspel_pat_bad")).thenReturn(null);

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void prometheusScrapeToken_rolVerir() throws Exception {
        ReflectionTestUtils.setField(filter, "metricsScrapeToken", "gizli");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("X-Metrics-Token", "gizli");

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PROMETHEUS".equals(a.getAuthority())));
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void prometheusYanlisToken_rolVerilmez() throws Exception {
        ReflectionTestUtils.setField(filter, "metricsScrapeToken", "gizli");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("X-Metrics-Token", "yanlis");

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
