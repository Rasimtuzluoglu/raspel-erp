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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YetkiKontrolTest {

    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private RolRepository rolRepository;
    @InjectMocks private YetkiKontrol yetkiKontrol;

    private UsernamePasswordAuthenticationToken auth(String rol) {
        return new UsernamePasswordAuthenticationToken("kullanici", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    void adminHerKoduGecebilir() {
        assertTrue(yetkiKontrol.kontrol(auth("ADMIN"), "STOK_DELETE"));
    }

    @Test
    void rolYetkileriKodaGoreDegerlendirilir() {
        Kullanici k = new Kullanici();
        k.setUsername("kullanici");
        k.setRole("DEPO");
        Rol rol = new Rol();
        rol.setAd("DEPO");
        rol.setYetkiler(Set.of(Yetki.builder().kod("STOK_DELETE").build()));
        when(kullaniciRepository.findByUsername("kullanici")).thenReturn(Optional.of(k));
        when(rolRepository.findByAd("DEPO")).thenReturn(Optional.of(rol));

        assertTrue(yetkiKontrol.kontrol(auth("USER"), "STOK_DELETE"));
        assertFalse(yetkiKontrol.kontrol(auth("USER"), "FATURA_DELETE"));
    }

    @Test
    void kullaniciBulunamazsaReddeder() {
        when(kullaniciRepository.findByUsername("kullanici")).thenReturn(Optional.empty());
        assertFalse(yetkiKontrol.kontrol(auth("USER"), "STOK_DELETE"));
    }

    @Test
    void bosKodReddeder() {
        assertFalse(yetkiKontrol.kontrol(auth("USER"), " "));
        assertFalse(yetkiKontrol.kontrol(null, "STOK_DELETE"));
    }
}
