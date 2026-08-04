package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @Scheduled(cron = "0 0 8 * * *")
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

            emailService.odemeHatimlaticiGonder(
                    cari.getEmail(),
                    fatura.getFaturaNumarasi(),
                    fatura.getGenelToplam() != null ? fatura.getGenelToplam().toString() : "0.00",
                    fatura.getKalanTutar().toString(),
                    vade.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    cari.getAd());
            gonderilen++;
        }
        log.info("Vadesi geçen hatırlatıcı tamamlandı - Gönderilen: {}", gonderilen);
    }
}
