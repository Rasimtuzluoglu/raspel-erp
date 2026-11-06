package com.raspel.erp.service.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.dto.sistem.PasskeyGirisRequest;
import com.raspel.erp.dto.sistem.PasskeyKayitRequest;
import com.raspel.erp.dto.sistem.PasskeySecenekDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Passkey;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.PasskeyRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasskeyServiceTest {

    @Mock private PasskeyRepository passkeyRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KullaniciService kullaniciService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private WebAuthnManager webAuthnManager;

    private PasskeyService service;
    private Kullanici kullanici;

    @BeforeEach
    void kur() {
        service = new PasskeyService(passkeyRepository, kullaniciRepository, passwordEncoder,
                kullaniciService, redisTemplate, new ObjectMapper(), new ObjectConverter(),
                webAuthnManager);
        ReflectionTestUtils.setField(service, "rpId", "localhost");
        ReflectionTestUtils.setField(service, "originsRaw", "http://localhost");

        kullanici = new Kullanici();
        kullanici.setId(1L);
        kullanici.setUsername("testuser");
        kullanici.setDisplayName("Test User");
        kullanici.setPassword("encoded");
        kullanici.setActive(true);
        kullanici.setSirketId(5L);
    }

    @Test
    void kayitSecenekleri_parolaDogrulanirVeSecenekDoner() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(kullanici));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of());

        PasskeySecenekDTO sonuc = service.kayitSecenekleri(1L, "pass");

        assertNotNull(sonuc.getChallengeId());
        assertEquals("localhost", ((Map<?, ?>) sonuc.getPublicKey().get("rp")).get("id"));
        assertEquals("testuser", ((Map<?, ?>) sonuc.getPublicKey().get("user")).get("name"));
        assertNotNull(sonuc.getPublicKey().get("challenge"));
        assertFalse(sonuc.getPublicKey().containsKey("excludeCredentials"));
    }

    @Test
    void kayitSecenekleri_yanlisParolayiReddeder() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(kullanici));
        when(passwordEncoder.matches("yanlis", "encoded")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.kayitSecenekleri(1L, "yanlis"));
    }

    @Test
    void kayitSecenekleri_enFazlaOnPasskey() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(kullanici));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(1L))
                .thenReturn(java.util.Collections.nCopies(10, new Passkey()));

        assertThrows(BusinessException.class, () -> service.kayitSecenekleri(1L, "pass"));
    }

    @Test
    void girisSecenekleri_discoverableChallengeDoner() {
        PasskeySecenekDTO sonuc = service.girisSecenekleri();

        assertNotNull(sonuc.getChallengeId());
        assertEquals("localhost", sonuc.getPublicKey().get("rpId"));
        assertEquals("required", sonuc.getPublicKey().get("userVerification"));
        assertFalse(sonuc.getPublicKey().containsKey("allowCredentials"));
    }

    @Test
    void kaydet_gecersizChallengeReddeder() {
        PasskeyKayitRequest req = PasskeyKayitRequest.builder()
                .challengeId("yok").credential(Map.of("id", "x")).build();

        assertThrows(BusinessException.class, () -> service.kaydet(1L, req));
        verify(passkeyRepository, never()).save(any());
    }

    @Test
    void giris_gecersizChallengeReddeder() {
        PasskeyGirisRequest req = PasskeyGirisRequest.builder()
                .challengeId("yok").credential(Map.of("id", "x")).build();

        assertThrows(BusinessException.class, () -> service.giris(req));
    }

    @Test
    void listele_kullaniciyaAitKayitlariDoner() {
        Passkey p = new Passkey();
        p.setId(3L);
        p.setEtiket("Telefon");
        p.setTransports("internal");
        p.setOlusturmaTarihi(LocalDateTime.now());
        when(passkeyRepository.findByKullaniciIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of(p));

        var liste = service.listele(1L);

        assertEquals(1, liste.size());
        assertEquals("Telefon", liste.get(0).getEtiket());
    }

    @Test
    void sil_baskasininKaydiniSilemez() {
        when(passkeyRepository.findByIdAndKullaniciId(3L, 1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.sil(1L, 3L));
    }

    @Test
    void adminSil_tumKayitlariSiler() {
        service.adminSil(1L);
        verify(passkeyRepository).deleteByKullaniciId(1L);
    }
}
