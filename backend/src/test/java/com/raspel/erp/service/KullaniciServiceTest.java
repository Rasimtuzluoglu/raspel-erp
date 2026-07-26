package com.raspel.erp.service;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.dto.KullaniciDTO;
import com.raspel.erp.dto.LoginRequest;
import com.raspel.erp.dto.LoginResponse;
import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.entity.Sirket;
import com.raspel.erp.repository.KullaniciRepository;
import com.raspel.erp.repository.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KullaniciServiceTest {

    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private SirketRepository sirketRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private KullaniciService kullaniciService;

    private Kullanici createKullanici(Long id) {
        Kullanici k = new Kullanici();
        k.setId(id);
        k.setUsername("testuser" + id);
        k.setPassword("encoded");
        k.setDisplayName("Test User");
        k.setRole("USER");
        k.setActive(true);
        k.setOlusturmaTarihi(LocalDateTime.now());
        return k;
    }

    @Test
    void tumunuGetir_returnsAllUsers() {
        when(kullaniciRepository.findAll()).thenReturn(List.of(createKullanici(1L), createKullanici(2L)));
        List<KullaniciDTO> result = kullaniciService.tumunuGetir();
        assertEquals(2, result.size());
    }

    @Test
    void getir_returnsUser() {
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(createKullanici(1L)));
        KullaniciDTO result = kullaniciService.getir(1L);
        assertEquals("testuser1", result.getUsername());
    }

    @Test
    void getir_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.getir(99L));
    }

    @Test
    void olustur_createsUser() {
        KullaniciDTO dto = KullaniciDTO.builder().username("newuser").displayName("New").password("pass").build();
        when(kullaniciRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        Kullanici saved = createKullanici(1L);
        saved.setUsername("newuser");
        saved.setDisplayName("New");
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(saved);
        KullaniciDTO result = kullaniciService.olustur(dto);
        assertEquals("newuser", result.getUsername());
    }

    @Test
    void olustur_throwsWhenUsernameExists() {
        KullaniciDTO dto = KullaniciDTO.builder().username("existing").displayName("Existing").build();
        assertThrows(RuntimeException.class, () -> kullaniciService.olustur(dto));
    }

    @Test
    void guncelle_updatesUser() {
        Kullanici existing = createKullanici(1L);
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(existing));
        KullaniciDTO dto = KullaniciDTO.builder().displayName("Updated").active(false).role("ADMIN").build();
        when(kullaniciRepository.save(any(Kullanici.class))).thenReturn(existing);
        KullaniciDTO result = kullaniciService.guncelle(1L, dto);
        assertEquals("Updated", result.getDisplayName());
    }

    @Test
    void guncelle_throwsWhenNotFound() {
        when(kullaniciRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kullaniciService.guncelle(99L, new KullaniciDTO()));
    }

    @Test
    void sil_deletesUser() {
        when(kullaniciRepository.existsById(1L)).thenReturn(true);
        kullaniciService.sil(1L);
        verify(kullaniciRepository).deleteById(1L);
    }

    @Test
    void sil_throwsWhenNotFound() {
        when(kullaniciRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> kullaniciService.sil(99L));
    }

    @Test
    void giris_successfulLogin() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(any(Kullanici.class), any(), any())).thenReturn("token");
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");
        LoginResponse resp = kullaniciService.giris(req);
        assertEquals("testuser1", resp.getUsername());
        assertEquals("token", resp.getToken());
    }

    @Test
    void giris_throwsWhenInvalidPassword() {
        Kullanici k = createKullanici(1L);
        k.setPassword("encoded");
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("wrong");
        assertThrows(RuntimeException.class, () -> kullaniciService.giris(req));
    }

    @Test
    void giris_throwsWhenInactive() {
        Kullanici k = createKullanici(1L);
        k.setActive(false);
        when(kullaniciRepository.findByUsername("testuser1")).thenReturn(Optional.of(k));
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser1");
        req.setPassword("pass");
        assertThrows(RuntimeException.class, () -> kullaniciService.giris(req));
    }
}
