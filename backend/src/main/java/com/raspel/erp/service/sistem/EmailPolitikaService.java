package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * E-posta gönderim politikası. Kullanıcı girdisiyle serbest alıcıya e-posta
 * gönderilmesini (relay/spam kötüye kullanımı) engeller: alıcı, şirketin kayıtlı
 * cari veya kullanıcı e-postalarından biri olmalıdır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPolitikaService {

    private final CariHesapRepository cariHesapRepository;
    private final KullaniciRepository kullaniciRepository;

    /**
     * Alıcının şirket içinde kayıtlı olup olmadığını doğrular; değilse reddeder.
     * ADMIN kullanıcılar için alan adı allowlist'i genişletilebilir (opsiyonel env).
     */
    @Transactional(readOnly = true)
    public void aliciDogrula(String alici, Long sirketId) {
        if (alici == null || alici.isBlank() || !alici.contains("@")) {
            throw new BusinessException("Geçerli bir e-posta adresi giriniz");
        }
        if (sirketId == null) {
            throw new BusinessException("E-posta gönderimi için şirket bağlamı bulunamadı");
        }
        String hedef = alici.trim().toLowerCase(Locale.ROOT);
        Set<String> izinli = kayitliEpostalar(sirketId);
        if (!izinli.contains(hedef)) {
            throw new BusinessException(
                    "Yalnızca şirkete kayıtlı cari/kullanıcı e-posta adreslerine gönderim yapılabilir: " + alici);
        }
    }

    private Set<String> kayitliEpostalar(Long sirketId) {
        Set<String> epostalar = new HashSet<>();
        for (CariHesap c : cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId)) {
            if (c.getEmail() != null && !c.getEmail().isBlank()) {
                epostalar.add(c.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        for (Kullanici k : kullaniciRepository.findBySirketId(sirketId, Pageable.unpaged()).getContent()) {
            if (k.getEmail() != null && !k.getEmail().isBlank()) {
                epostalar.add(k.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        return epostalar;
    }
}
