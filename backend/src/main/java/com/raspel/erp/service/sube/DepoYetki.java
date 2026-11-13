package com.raspel.erp.service.sube;

import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Depo islem yetkisi: ADMIN veya depo sorumlusu.
 *
 * Sorumlu atanmamissa islem yalniz ADMIN'e aittir; goruntuleme kisitlanmaz.
 */
@Component
@RequiredArgsConstructor
public class DepoYetki {

    private final KullaniciRepository kullaniciRepository;

    public boolean islemYapabilir(Depo depo, Long kullaniciId) {
        if (depo == null || kullaniciId == null) return false;
        if (depo.getSorumluKullaniciId() != null
                && depo.getSorumluKullaniciId().equals(kullaniciId)) {
            return true;
        }
        return kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole()))
                .orElse(false);
    }
}
