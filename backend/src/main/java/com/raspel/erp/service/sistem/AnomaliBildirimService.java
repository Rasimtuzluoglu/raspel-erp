package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anomali tespit sonuçlarını otomatik olarak bildirir (e-posta ve/veya Slack).
 * Günlük tarama yapar; veriye dayalı gerçek anomalilerde yöneticiyi uyarır.
 * Simülasyon amaçlı "güvenlik" anomalileri bildirime dahil edilmez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomaliBildirimService {

    private static final Set<String> BILDIRILECEK_TURLER = Set.of(
            "MUKERRER_FATURA", "MUKERRER_ODEME", "ANORMAL_MASRAF"
    );

    private final AnomaliTespitEngine anomaliTespitEngine;
    private final SirketRepository sirketRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${app.alert.email:}")
    private String alertEmail;

    @Value("${app.alert.slack-webhook:}")
    private String slackWebhook;

    @Scheduled(cron = "0 0 6 * * *")
    public void gunlukAnomaliTarama() {
        List<Sirket> sirketler = sirketRepository.findByAktifTrue();
        for (Sirket s : sirketler) {
            try {
                List<AnomaliDTO> anomaliler = anomaliTespitEngine.anomalileriTara(s.getId());
                List<AnomaliDTO> bildirilecek = anomaliler.stream()
                        .filter(a -> BILDIRILECEK_TURLER.contains(a.getTur()))
                        .collect(Collectors.toList());
                if (!bildirilecek.isEmpty()) {
                    anomaliBildir(s, bildirilecek);
                }
            } catch (Exception e) {
                log.warn("Anomali taraması başarısız ({}): {}", s.getAd(), e.getMessage());
            }
        }
    }

    public void anomaliBildir(Sirket sirket, List<AnomaliDTO> anomaliler) {
        if (anomaliler == null || anomaliler.isEmpty()) return;

        String ozet = anomaliler.stream()
                .map(a -> String.format("- [%s] %s: %s", a.getSeviye(), a.getBaslik(), a.getAciklama()))
                .collect(Collectors.joining("\n"));

        emailGonder(sirket, anomaliler.size(), ozet);
        slackGonder(sirket, anomaliler.size(), ozet);
    }

    private void emailGonder(Sirket sirket, int adet, String ozet) {
        String adres = alertEmail != null && !alertEmail.isBlank() ? alertEmail.trim() : null;
        if (adres == null && sirket != null) {
            adres = sirket.getEmail();
        }
        if (adres == null || adres.isBlank()) {
            log.info("Anomali bildirimi için alıcı e-posta bulunamadı (sirket={})", sirket != null ? sirket.getAd() : "?");
            return;
        }
        try {
            String konu = String.format("RasPel ERP - %d adet anomali tespit edildi", adet);
            String icerik = "Aşağıdaki şüpheli durumlar tespit edildi:\n\n" + ozet
                    + "\n\nDetaylar için Anomaliler ekranına bakın.";
            emailService.emailGonder(adres, konu, icerik);
            log.info("Anomali uyarısı e-postası gönderildi -> {}", adres);
        } catch (Exception e) {
            log.warn("Anomali uyarısı e-postası gönderilemedi: {}", e.getMessage());
        }
    }

    private void slackGonder(Sirket sirket, int adet, String ozet) {
        if (slackWebhook == null || slackWebhook.isBlank()) return;
        try {
            String text = String.format("*RasPel ERP — %d anomali tespit edildi*\n%s", adet, ozet);
            String payload = "{\"text\":\"" + escapeJson(text) + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(slackWebhook, new HttpEntity<>(payload, headers), String.class);
            log.info("Anomali uyarısı Slack'e gönderildi");
        } catch (Exception e) {
            log.warn("Anomali uyarısı Slack'e gönderilemedi: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
