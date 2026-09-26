package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.AjandaHatirlatici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.sistem.AjandaHatirlaticiRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vadesi geçen alacaklar için otomatik ödeme hatırlatıcısı gönderir.
 * Her gün sabah 08:00'de çalışır; SATIS faturalarında kalan tutarı olan ve
 * vadesi geçmiş kayıtlar için cari hesabın e-posta adresine hatırlatma yollar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HatirlaticiService {

    private final FaturaRepository faturaRepository;
    private final EmailService emailService;
    private final BildirimService bildirimService;
    private final AjandaHatirlaticiRepository ajandaHatirlaticiRepository;

    @Scheduled(cron = "0 0 8 * * *")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "hatirlatici", lockAtMostFor = "PT20M", lockAtLeastFor = "PT1M")
    public void vadesiGecenHatirlaticiGonder() {
        List<Fatura> faturalar;
        try {
            faturalar = faturaRepository.findByTurAndOdemeDurumuNotIn(Fatura.FaturaTur.SATIS, List.of("ODENDI", "IPTAL"));
        } catch (Exception e) {
            log.warn("Faturalar listelenemedi: {}", e.getMessage());
            return;
        }

        int gonderilen = 0;
        for (Fatura fatura : faturalar) {
            if (fatura.getKalanTutar() == null || fatura.getKalanTutar().signum() <= 0) continue;
            CariHesap cari = fatura.getCariHesap();
            if (cari == null || cari.getEmail() == null || cari.getEmail().isBlank()) continue;

            LocalDate vade = fatura.getTarih().plusDays(cari.getOdemeVadesi() != null ? cari.getOdemeVadesi() : 0);
            if (!vade.isBefore(LocalDate.now())) continue;

            boolean gonderildi = emailService.odemeHatimlaticiGonder(
                    cari.getEmail(),
                    fatura.getFaturaNumarasi(),
                    fatura.getGenelToplam() != null ? fatura.getGenelToplam().toString() : "0.00",
                    fatura.getKalanTutar().toString(),
                    vade.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    cari.getAd());
            if (!gonderildi) {
                log.warn("Vade hatırlatma e-postası gönderilemedi -> {}", cari.getEmail());
            }

            if (fatura.getSirketId() != null) {
                bildirimService.bildirimGonder(fatura.getSirketId(), "VADE",
                        "Vadesi geçen fatura: " + fatura.getFaturaNumarasi(),
                        cari.getAd() + " - Kalan: " + fatura.getKalanTutar() + " ₺");
            }
            gonderilen++;
        }
        log.info("Vadesi geçen hatırlatıcı tamamlandı - Gönderilen: {}", gonderilen);
    }

    /**
     * Zamanı gelmiş kişisel ajanda hatırlatıcıları için bildirim üretir ve
     * hatırlatıcıyı "bildirildi" olarak işaretler. Her 15 dakikada bir çalışır.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "ajandaHatirlatici", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void ajandaHatirlaticiGonder() {
        List<AjandaHatirlatici> bekleyenler;
        try {
            bekleyenler = ajandaHatirlaticiRepository
                    .findByBildirildiFalseAndHatirlatmaZamaniLessThanEqual(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Ajanda hatırlatıcıları listelenemedi: {}", e.getMessage());
            return;
        }
        int gonderilen = 0;
        for (AjandaHatirlatici h : bekleyenler) {
            try {
                if (h.getSirketId() != null) {
                    bildirimService.bildirimGonder(h.getSirketId(), "AJANDA",
                            "Hatırlatıcı: " + h.getBaslik(),
                            "Belirlediğiniz hatırlatıcı zamanı geldi.");
                }
                h.setBildirildi(true);
                ajandaHatirlaticiRepository.save(h);
                gonderilen++;
            } catch (Exception e) {
                log.warn("Ajanda hatırlatıcı bildirimi gönderilemedi (id: {}): {}", h.getId(), e.getMessage());
            }
        }
        log.info("Ajanda hatırlatıcı tamamlandı - Gönderilen: {}", gonderilen);
    }
}
