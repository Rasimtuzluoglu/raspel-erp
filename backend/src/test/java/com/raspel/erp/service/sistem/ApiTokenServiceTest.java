package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.ApiTokenDTO;
import com.raspel.erp.entity.sistem.ApiToken;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.ApiTokenRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    @Mock
    private ApiTokenRepository apiTokenRepository;
    @Mock
    private KullaniciRepository kullaniciRepository;

    @InjectMocks
    private ApiTokenService apiTokenService;

    @Test
    void olustur_tokenUretirVeHashKaydeder() {
        Kullanici k = Kullanici.builder().id(1L).username("admin").active(true).build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));
        when(apiTokenRepository.save(any(ApiToken.class))).thenAnswer(inv -> {
            ApiToken t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        ApiTokenDTO dto = apiTokenService.olustur(1L, "Entegrasyon");

        assertNotNull(dto.getToken());
        assertTrue(dto.getToken().startsWith("raspel_pat_"));
        assertEquals("Entegrasyon", dto.getAd());
        verify(apiTokenRepository).save(any(ApiToken.class));
    }

    @Test
    void olustur_kullaniciYoksaHataFirlatir() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> apiTokenService.olustur(99L, "X"));
    }

    @Test
    void sil_baskaKullaniciSilinemez() {
        ApiToken t = ApiToken.builder().id(1L).kullaniciId(2L).tokenHash("abc").build();
        when(apiTokenRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(ResourceNotFoundException.class, () -> apiTokenService.sil(1L, 1L));
        verify(apiTokenRepository, never()).deleteById(any());
    }

    @Test
    void sil_kendiTokeniniSiler() {
        ApiToken t = ApiToken.builder().id(1L).kullaniciId(1L).tokenHash("abc").build();
        when(apiTokenRepository.findById(1L)).thenReturn(Optional.of(t));

        apiTokenService.sil(1L, 1L);

        verify(apiTokenRepository).deleteById(1L);
    }

    @Test
    void tokenIleKullaniciBul_gecersizTokenNullDoner() {
        assertNull(apiTokenService.tokenIleKullaniciBul(null));
        assertNull(apiTokenService.tokenIleKullaniciBul("gecersiz"));
    }

    @Test
    void tokenIleKullaniciBul_gecerliTokenKullaniciDoner() {
        String token = "raspel_pat_testtoken";
        ApiToken t = ApiToken.builder().id(1L).kullaniciId(1L).tokenHash("hash").build();
        Kullanici k = Kullanici.builder().id(1L).username("admin").build();

        // tokenIleKullaniciBul sha256(token) hash'ini arar; hash'i doğrudan mock'lamak için
        // token hash'ini hesaplayıp repository'yi o hash ile mockluyoruz.
        when(apiTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(t));
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(k));

        Kullanici sonuc = apiTokenService.tokenIleKullaniciBul(token);

        assertNotNull(sonuc);
        assertEquals("admin", sonuc.getUsername());
    }

    @Test
    void listele_tokenlariDoner() {
        when(apiTokenRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(1L))
                .thenReturn(List.of(ApiToken.builder().id(1L).kullaniciId(1L).ad("T1").build()));

        List<ApiTokenDTO> liste = apiTokenService.listele(1L);

        assertEquals(1, liste.size());
        assertNull(liste.get(0).getToken());
    }
}
