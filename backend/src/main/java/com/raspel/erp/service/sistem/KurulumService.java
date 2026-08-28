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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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

    private static final long KURULUM_KILIT_ID = 72495632L;

    private final SirketRepository sirketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final KullaniciService kullaniciService;
    private final DataSource dataSource;

    @Transactional(readOnly = true)
    public Map<String, Object> durum() {
        boolean kurulumGerekli = sirketRepository.count() == 0;
        return Map.of("kurulumGerekli", kurulumGerekli);
    }

    /**
     * Çoklu instance / eşzamanlı istek senaryosunda kurulumun yalnızca bir kez
     * çalışması PostgreSQL advisory lock ile garanti edilir (TOCTOU koruması).
     */
    @Transactional
    public LoginResponse kurulumYap(KurulumDTO dto) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT pg_try_advisory_lock(" + KURULUM_KILIT_ID + ")")) {
                if (!rs.next() || !rs.getBoolean(1)) {
                    throw new BusinessException("Kurulum şu anda başka bir istek tarafından yürütülüyor, lütfen tekrar deneyin");
                }
            }
            return kurulumYapKilitli(dto);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Kurulum kilidi alınamadı: {}", e.getMessage());
            return kurulumYapKilitli(dto);
        }
    }

    private LoginResponse kurulumYapKilitli(KurulumDTO dto) {
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
