package com.raspel.erp.service.sistem;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KullaniciServiceTest {

    @Mock
    private KullaniciRepository kullaniciRepository;
    @Mock
    private SirketRepository sirketRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AktifOturumService aktifOturumService;

    @InjectMocks
    private KullaniciService kullaniciService;

    @Test
    void bildirimTercihleriGetir_virgulluStringiListeyeCevirir() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN")
                .bildirimTercihleri("FATURA,HATA").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGetir(1L);

        assertEquals(2, sonuc.size());
        assertEquals("FATURA", sonuc.get(0));
        assertEquals("HATA", sonuc.get(1));
    }

    @Test
    void bildirimTercihleriGetir_bosIseBosListeDoner() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGetir(1L);

        assertNotNull(sonuc);
        assertTrue(sonuc.isEmpty());
    }

    @Test
    void bildirimTercihleriGuncelle_listeyiKaydeder() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").role("ADMIN").build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        List<String> sonuc = kullaniciService.bildirimTercihleriGuncelle(1L, List.of("FATURA", "ANOMALI"));

        assertEquals(2, sonuc.size());
        assertEquals("FATURA,ANOMALI", k.getBildirimTercihleri());
        verify(kullaniciRepository).save(k);
    }
}
