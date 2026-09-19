package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuncellemeServiceTest {

    private RestTemplate restTemplate;
    private GuncellemeService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        service = new GuncellemeService(restTemplate);
        ReflectionTestUtils.setField(service, "surum", "1.26.0");
        ReflectionTestUtils.setField(service, "repo", "Rasimtuzluoglu/raspel-erp");
        ReflectionTestUtils.setField(service, "branch", "main");
    }

    @Test
    void durum_yeniReleaseVarsaGuncellemeVarTrue() {
        when(restTemplate.getForObject(contains("/releases/latest"), eq(Map.class)))
                .thenReturn(Map.of("tag_name", "v1.27.0"));
        when(restTemplate.getForObject(contains("/commits"), eq(List.class)))
                .thenReturn(List.of(Map.of("sha", "abcdef1234567890", "commit",
                        Map.of("message", "feat: x", "author", Map.of("date", "2026-09-20T00:00:00Z")))));

        Map<String, Object> r = service.durum();

        assertEquals("1.26.0", r.get("mevcutSurum"));
        assertEquals("v1.27.0", r.get("sonSurum"));
        assertEquals(true, r.get("guncellemeVar"));
        assertEquals("abcdef1", r.get("sonCommit"));
    }

    @Test
    void durum_ayniSurumdeGuncellemeVarFalse() {
        when(restTemplate.getForObject(contains("/releases/latest"), eq(Map.class)))
                .thenReturn(Map.of("tag_name", "v1.26.0"));
        when(restTemplate.getForObject(contains("/commits"), eq(List.class))).thenReturn(List.of());

        Map<String, Object> r = service.durum();

        assertEquals(false, r.get("guncellemeVar"));
    }

    @Test
    void durum_releaseYoksaCommitYineOkunur() {
        when(restTemplate.getForObject(contains("/releases/latest"), eq(Map.class)))
                .thenThrow(new RuntimeException("404"));
        when(restTemplate.getForObject(contains("/commits"), eq(List.class)))
                .thenReturn(List.of(Map.of("sha", "1234567")));

        Map<String, Object> r = service.durum();

        assertNull(r.get("sonSurum"));
        assertEquals("1234567", r.get("sonCommit"));
    }

    @Test
    void durum_githubErisilemezseHataAlaniDoner() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("timeout"));
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenThrow(new RuntimeException("timeout"));

        Map<String, Object> r = service.durum();

        assertEquals("1.26.0", r.get("mevcutSurum"));
        assertTrue(r.containsKey("hata"));
    }
}
