package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raspel.erp.dto.sistem.AktifOturumDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AktifOturumServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AktifOturumService aktifOturumService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    private AktifOturumDTO ornekOturum() {
        return AktifOturumDTO.builder()
                .jti("jti-1").kullaniciId(1L).kullaniciAdi("admin").sirketId(4L).ip("127.0.0.1").build();
    }

    @Test
    void testOturumKaydet_RedisEYazar() {
        aktifOturumService.oturumKaydet("jti-1", 1L, "admin", 4L, "127.0.0.1", Duration.ofHours(1));

        verify(valueOps, times(1)).set(eq("session:jti-1"), anyString(), eq(Duration.ofHours(1)));
        verify(setOps, times(1)).add("session:user:1", "jti-1");
    }

    @Test
    void testOturumGetir_VarolanOturumuDondurur() throws Exception {
        String json = objectMapper.writeValueAsString(ornekOturum());
        when(valueOps.get("session:jti-1")).thenReturn(json);

        AktifOturumDTO sonuc = aktifOturumService.oturumGetir("jti-1");

        assertNotNull(sonuc);
        assertEquals("jti-1", sonuc.getJti());
        assertEquals(1L, sonuc.getKullaniciId());
    }

    @Test
    void testOturumGetir_BulunamazsaNullDoner() {
        when(valueOps.get("session:yok")).thenReturn(null);

        assertNull(aktifOturumService.oturumGetir("yok"));
    }

    @Test
    void testOturumIptal_KaraListeyeEkler() throws Exception {
        String json = objectMapper.writeValueAsString(ornekOturum());
        when(valueOps.get("session:jti-1")).thenReturn(json);

        aktifOturumService.oturumIptal("jti-1");

        verify(setOps, times(1)).remove("session:user:1", "jti-1");
        verify(redisTemplate, times(1)).delete("session:jti-1");
        verify(valueOps, times(1)).set(eq("session:revoked:jti-1"), eq("1"), any(Duration.class));
    }

    @Test
    void testIptalEdilmis_KaraListedeIseTrue() {
        when(redisTemplate.hasKey("session:revoked:jti-1")).thenReturn(true);

        assertTrue(aktifOturumService.iptalEdilmis("jti-1"));
    }

    @Test
    void testAktifOturumlar_KendiOturumlariniListeler() throws Exception {
        String json = objectMapper.writeValueAsString(ornekOturum());
        when(setOps.members("session:user:1")).thenReturn(Set.of("jti-1"));
        when(valueOps.get("session:jti-1")).thenReturn(json);

        List<AktifOturumDTO> sonuc = aktifOturumService.aktifOturumlar(1L, false);

        assertEquals(1, sonuc.size());
        assertEquals("jti-1", sonuc.get(0).getJti());
    }
}
