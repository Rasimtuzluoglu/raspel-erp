package com.raspel.erp.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisProvider;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        when(redisProvider.getIfAvailable()).thenReturn(null); // in-memory fallback
        filter = new LoginRateLimitFilter(redisProvider);
    }

    /** Başarısız (401) giriş denemesi gönderir. */
    private MockHttpServletResponse basarisizDeneme(String ip, String kullaniciAdi) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/kullanicilar/giris");
        request.setRequestURI("/api/kullanicilar/giris");
        request.setRemoteAddr(ip);
        String govde = "{\"username\":\"" + kullaniciAdi + "\",\"password\":\"x\"}";
        request.setContent(govde.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(401);
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void korumaliOlmayanYol_dogrudanGecer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/faturalar");
        request.setRequestURI("/api/faturalar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(eq(request), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void getMetoduSayilmaz() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/kullanicilar/giris");
        request.setRequestURI("/api/kullanicilar/giris");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void besBasarisizDenemedenSonra_429Doner() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(401, basarisizDeneme("10.0.0.1", "ali").getStatus());
        }

        MockHttpServletResponse altinci = basarisizDeneme("10.0.0.1", "ali");

        assertEquals(429, altinci.getStatus());
        assertTrue(altinci.getContentAsString().contains("Çok fazla deneme"));
        assertTrue(altinci.getContentType().contains("application/json"));
    }

    @Test
    void basariliGirisSayaciSifirlar() throws Exception {
        for (int i = 0; i < 4; i++) {
            basarisizDeneme("10.0.0.2", "veli");
        }

        // Başarılı giriş (200) sayaçları sıfırlar
        MockHttpServletRequest okReq = new MockHttpServletRequest("POST", "/api/kullanicilar/giris");
        okReq.setRequestURI("/api/kullanicilar/giris");
        okReq.setRemoteAddr("10.0.0.2");
        okReq.setContent("{\"username\":\"veli\",\"password\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse okRes = new MockHttpServletResponse();
        filter.doFilter(okReq, okRes, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));

        // Sonraki 4 başarısız deneme hâlâ engellenmemeli
        for (int i = 0; i < 4; i++) {
            assertEquals(401, basarisizDeneme("10.0.0.2", "veli").getStatus());
        }
    }

    @Test
    void kullaniciAdiBazliKoruma_farkliIplerdenBloklar() throws Exception {
        // Aynı kullanıcı, farklı güvenilir-proxy IP'leri: kullanıcı adı sayacı birikir
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/kullanicilar/giris");
            request.setRequestURI("/api/kullanicilar/giris");
            request.setRemoteAddr("127.0.0.1"); // güvenilir proxy
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            request.setContent("{\"username\":\"hedef\",\"password\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(401));
            assertEquals(401, response.getStatus());
        }

        // Yeni bir IP'den aynı kullanıcı adı artık bloklu
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/kullanicilar/giris");
        request.setRequestURI("/api/kullanicilar/giris");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.200");
        request.setContent("{\"username\":\"hedef\",\"password\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(401));

        assertEquals(429, response.getStatus());
    }

    @Test
    void sifreSifirlamaUcuDaKorunur() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/kullanicilar/sifre-sifirlama-talebi");
        request.setRequestURI("/api/kullanicilar/sifre-sifirlama-talebi");
        request.setRemoteAddr("10.0.0.9");
        request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(401);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, chain);
        }
        MockHttpServletResponse blok = new MockHttpServletResponse();
        filter.doFilter(request, blok, chain);

        assertEquals(429, blok.getStatus());
    }

    /** Güvenilir proxy (127.0.0.1) arkasından, sahte XFF öneki + gerçek istemci gönderir. */
    private MockHttpServletResponse basarisizDenemeXff(String gercekIp, String sahteOnek, String kullaniciAdi) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/kullanicilar/giris");
        request.setRequestURI("/api/kullanicilar/giris");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", sahteOnek + ", " + gercekIp);
        request.setContent(("{\"username\":\"" + kullaniciAdi + "\",\"password\":\"x\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(401));
        return response;
    }

    @Test
    void xffSahteOnekYoksayilir_gercekSonElemanKullanilir() throws Exception {
        // Faz 0.3: aynı sahte önek (1.2.3.4) ile 5 farklı gerçek istemciden başarısız
        // deneme. İlk (sahte) değer kullanılsaydı hepsi aynı sayaca düşer ve 6. istek
        // engellenirdi. Son (gerçek) değer kullanıldığı için 6. istek farklı IP'dedir.
        for (int i = 0; i < 5; i++) {
            assertEquals(401, basarisizDenemeXff("203.0.113." + (10 + i), "1.2.3.4", "kullanici" + i).getStatus());
        }
        // Yeni gerçek istemci: engellenmemeli (sahte önek yok sayıldı)
        assertEquals(401, basarisizDenemeXff("203.0.113.50", "1.2.3.4", "yeniKullanici").getStatus());

        // Aynı gerçek istemciden 6. deneme engellenmeli
        for (int i = 0; i < 5; i++) {
            basarisizDenemeXff("203.0.113.60", "1.2.3.4", "kullaniciB" + i);
        }
        assertEquals(429, basarisizDenemeXff("203.0.113.60", "1.2.3.4", "kullaniciC").getStatus());
    }

    @Test
    void guvenilirProxyYapilandirmasi_xffYalnizcaListedekindenGelirseKullanilir() {
        ReflectionTestUtils.setField(filter, "trustedProxiesConfig", "10.9.9.9");
        ReflectionTestUtils.setField(filter, "trustedProxyCache", null);

        MockHttpServletRequest listedenGelen = new MockHttpServletRequest();
        listedenGelen.setRemoteAddr("10.9.9.9");
        listedenGelen.addHeader("X-Forwarded-For", "203.0.113.7");
        assertEquals("203.0.113.7", ReflectionTestUtils.invokeMethod(filter, "getClientIp", listedenGelen));

        MockHttpServletRequest listedeOlmayan = new MockHttpServletRequest();
        listedeOlmayan.setRemoteAddr("8.8.8.8");
        listedeOlmayan.addHeader("X-Forwarded-For", "203.0.113.7");
        assertEquals("8.8.8.8", ReflectionTestUtils.invokeMethod(filter, "getClientIp", listedeOlmayan));
    }

    @Test
    void cidrYapilandirmasi_altAgiKapsar() {
        ReflectionTestUtils.setField(filter, "trustedProxiesConfig", "172.18.0.0/16");
        ReflectionTestUtils.setField(filter, "trustedProxyCache", null);

        MockHttpServletRequest cidrde = new MockHttpServletRequest();
        cidrde.setRemoteAddr("172.18.5.4");
        cidrde.addHeader("X-Forwarded-For", "203.0.113.9");
        assertEquals("203.0.113.9", ReflectionTestUtils.invokeMethod(filter, "getClientIp", cidrde));

        MockHttpServletRequest disinda = new MockHttpServletRequest();
        disinda.setRemoteAddr("172.19.0.1");
        disinda.addHeader("X-Forwarded-For", "203.0.113.9");
        assertEquals("172.19.0.1", ReflectionTestUtils.invokeMethod(filter, "getClientIp", disinda));
    }
}
