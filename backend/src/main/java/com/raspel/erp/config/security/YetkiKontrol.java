package com.raspel.erp.config.security;

import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Endpoint seviyesinde yetki kodu kontrolü. Kullanıcının rolüne bağlı yetki kodlarını
 * (STOK_DELETE, FATURA_DELETE vb.) çözer. SpEL ile kullanılır:
 * {@code @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'STOK_DELETE')")}
 * Rolü ADMIN olan kullanıcı her zaman geçer; mevcut rol kontrolleri geriye uyumlu kalır.
 */
@Component("yetkiKontrol")
@RequiredArgsConstructor
@Slf4j
public class YetkiKontrol {

    private final KullaniciRepository kullaniciRepository;
    private final RolRepository rolRepository;

    public boolean kontrol(Authentication authentication, String kod) {
        if (authentication == null || kod == null || kod.isBlank()) return false;
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (admin) return true;
        try {
            var kullanici = kullaniciRepository.findByUsername(authentication.getName()).orElse(null);
            if (kullanici == null || kullanici.getRole() == null) {
                log.warn("Yetki kontrolü: kullanıcı veya rol bilgisi yok ({} / {}), yetki {} reddedildi",
                        authentication.getName(), kullanici == null ? "yok" : "rol yok", kod);
                return false;
            }
            var rol = rolRepository.findByAd(kullanici.getRole()).orElse(null);
            if (rol == null) {
                // Fail-closed (güvenli) ama TEŞHİS EDİLEBİLİR olmalı. `sistem.rol`
                // tablosu seed verisiz kurulduğunda (ya da seed job'ı çalışmadığında)
                // yetkisi olan kullanıcılar da sessizce reddediliyordu ve nedeni
                // yalnızca log.warn ile belli oluyordu.
                log.warn("Yetki kontrolü: '{}' rolü sistem.rol tablosunda tanımlı değil. "
                                + "Yetki {} reddedildi. Migration seed'inin çalıştığından emin olun.",
                        kullanici.getRole(), kod);
                return false;
            }
            return rol.getYetkiler().stream()
                    .anyMatch(y -> kod.equalsIgnoreCase(y.getKod()));
        } catch (Exception e) {
            // DB kesintisi tüm yazma işlemlerini kilitler; en azından teşhis edilebilir olsun.
            log.error("Yetki kodu kontrol edilemedi ({}), kullanıcı: {}", kod, authentication.getName(), e);
            return false;
        }
    }
}
