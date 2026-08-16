package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sunucu hatalarında yöneticiye e-posta ile otomatik uyarı gönderir.
 * Spam'i önlemek için bildirimler arası en az 5 dakika beklenir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HataBildirimService {

    private final EmailService emailService;
    private final SirketRepository sirketRepository;

    @Value("${app.alert.email:}")
    private String alertEmail;

    private volatile long sonBildirimZamani = 0;
    private static final long MIN_ARALIK_MS = 5 * 60 * 1000;

    public void hataBildir(Long sirketId, String tur, String mesaj, String endpoint) {
        long simdi = System.currentTimeMillis();
        if (simdi - sonBildirimZamani < MIN_ARALIK_MS) {
            return;
        }
        sonBildirimZamani = simdi;

        String adres = alertEmail != null && !alertEmail.isBlank() ? alertEmail.trim() : null;
        if (adres == null && sirketId != null) {
            adres = sirketRepository.findById(sirketId)
                    .map(Sirket::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .orElse(null);
        }
        if (adres == null) {
            log.info("Hata uyarısı için alıcı e-posta bulunamadı (sirketId={})", sirketId);
            return;
        }

        String konu = "RasPel ERP - Sistem Hatası Uyarısı";
        String icerik = "Sistemde bir hata oluştu.\n\n"
                + "Tür: " + tur + "\n"
                + "Mesaj: " + (mesaj != null ? mesaj : "-") + "\n"
                + "Uç nokta: " + (endpoint != null ? endpoint : "-") + "\n\n"
                + "Detaylar için Sistem Durumu ekranındaki 'Son Hatalar' bölümüne bakın.";

        try {
            emailService.emailGonder(adres, konu, icerik);
            log.info("Hata uyarısı e-postası gönderildi -> {}", adres);
        } catch (Exception e) {
            log.warn("Hata uyarısı e-postası gönderilemedi: {}", e.getMessage());
        }
    }
}
