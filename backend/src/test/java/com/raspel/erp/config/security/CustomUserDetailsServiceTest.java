package com.raspel.erp.config.security;

import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.entity.sistem.Yetki;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private RolRepository rolRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    void kullaniciYoksa_usernameNotFoundFirlatir() {
        when(kullaniciRepository.findByUsername("yok")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("yok"));
    }

    @Test
    void kullaniciRoluyleBirlikteYuklenir() {
        Kullanici k = Kullanici.builder()
                .username("ali").password("hash").role("SATIS").active(true).build();
        when(kullaniciRepository.findByUsername("ali")).thenReturn(Optional.of(k));
        when(rolRepository.findByAd("SATIS")).thenReturn(Optional.empty());

        UserDetails ud = service.loadUserByUsername("ali");

        assertEquals("ali", ud.getUsername());
        assertEquals("hash", ud.getPassword());
        assertTrue(ud.isEnabled());
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> "ROLE_SATIS".equals(a.getAuthority())));
    }

    @Test
    void rolYetkileri_authorityOlarakEklenir() {
        Kullanici k = Kullanici.builder()
                .username("ali").password("hash").role("MUHASEBE").active(true).build();
        Rol rol = Rol.builder().ad("MUHASEBE").yetkiler(Set.of(
                Yetki.builder().kod("FATURA_READ").build(),
                Yetki.builder().kod("FATURA_WRITE").build(),
                Yetki.builder().kod("  ").build()
        )).build();
        when(kullaniciRepository.findByUsername("ali")).thenReturn(Optional.of(k));
        when(rolRepository.findByAd("MUHASEBE")).thenReturn(Optional.of(rol));

        UserDetails ud = service.loadUserByUsername("ali");

        assertTrue(ud.getAuthorities().stream().anyMatch(a -> "ROLE_MUHASEBE".equals(a.getAuthority())));
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> "FATURA_READ".equals(a.getAuthority())));
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> "FATURA_WRITE".equals(a.getAuthority())));
        // Bos yetki kodu eklenmez
        assertTrue(ud.getAuthorities().stream().noneMatch(a -> a.getAuthority().isBlank()));
    }

    @Test
    void rolBosIseVarsayilanUserRolu() {
        Kullanici k = Kullanici.builder()
                .username("ali").password("hash").role(null).active(true).build();
        when(kullaniciRepository.findByUsername("ali")).thenReturn(Optional.of(k));
        when(rolRepository.findByAd("USER")).thenReturn(Optional.empty());

        UserDetails ud = service.loadUserByUsername("ali");

        assertTrue(ud.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
    }

    @Test
    void pasifKullanici_enabledFalse() {
        Kullanici k = Kullanici.builder()
                .username("ali").password("hash").role("USER").active(false).build();
        when(kullaniciRepository.findByUsername("ali")).thenReturn(Optional.of(k));
        when(rolRepository.findByAd("USER")).thenReturn(Optional.empty());

        UserDetails ud = service.loadUserByUsername("ali");

        assertFalse(ud.isEnabled());
    }
}
