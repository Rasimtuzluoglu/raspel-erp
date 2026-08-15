package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.KurulumDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * İlk kurulum akışı.
 * Sistemde henüz firma yoksa kullanıcı firma bilgilerini girip kendi yönetici hesabını oluşturur;
 * sonrasında otomatik giriş yapılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KurulumService {

    private final SirketRepository sirketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final KullaniciService kullaniciService;

    @Transactional(readOnly = true)
    public Map<String, Object> durum() {
        boolean kurulumGerekli = sirketRepository.count() == 0;
        return Map.of("kurulumGerekli", kurulumGerekli);
    }

    @Transactional
    public LoginResponse kurulumYap(KurulumDTO dto) {
        if (sirketRepository.count() > 0) {
            throw new BusinessException("Kurulum zaten tamamlanmış. Giriş yaparak devam edebilirsiniz.");
        }

        Sirket sirket = sirketRepository.save(Sirket.builder()
                .ad(dto.getAd().trim())
                .vergiNo(dto.getVergiNo().trim())
                .vergiDairesi(dto.getVergiDairesi())
                .adres(dto.getAdres())
                .telefon(dto.getTelefon())
                .email(dto.getEmail())
                .webSite(dto.getWebSite())
                .aktif(true)
                .build());
        log.info("İlk kurulum: firma oluşturuldu -> {} (ID: {})", sirket.getAd(), sirket.getId());

        String displayName = dto.getAdminDisplayName() != null && !dto.getAdminDisplayName().isBlank()
                ? dto.getAdminDisplayName().trim()
                : sirket.getAd() + " Yöneticisi";

        KullaniciDTO adminDTO = KullaniciDTO.builder()
                .username(dto.getAdminUsername().trim())
                .password(dto.getAdminPassword())
                .displayName(displayName)
                .companyName(sirket.getAd())
                .sirketId(sirket.getId())
                .sirketIds(List.of(sirket.getId()))
                .role("ADMIN")
                .build();
        Kullanici admin = kullaniciRepository.findById(kullaniciService.olustur(adminDTO).getId())
                .orElseThrow(() -> new BusinessException("Yönetici hesabı oluşturulamadı"));
        log.info("İlk kurulum: yönetici hesabı oluşturuldu -> {}", admin.getUsername());

        return kullaniciService.giris(LoginRequest.builder()
                .username(dto.getAdminUsername().trim())
                .password(dto.getAdminPassword())
                .build());
    }
}
