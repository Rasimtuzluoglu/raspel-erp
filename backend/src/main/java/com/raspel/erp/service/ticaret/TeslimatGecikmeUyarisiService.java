package com.raspel.erp.service.ticaret;

import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.service.sistem.BildirimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Beklenen teslim tarihi geçmiş ve hâlâ aktif (BEKLEMEDE/YOLDA) durumdaki teslimatlar
 * için gecikme uyarısı üretir. Her teslimat için yalnızca bir kez bildirim gönderilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeslimatGecikmeUyarisiService {

    private final TeslimatService teslimatService;
    private final BildirimService bildirimService;

    @Scheduled(cron = "0 30 7 * * *")
    public void gecikmisTeslimatlariBildir() {
        try {
            List<Teslimat> gecikenler = teslimatService.gecikmisTeslimatlar();
            for (Teslimat t : gecikenler) {
                try {
                    if (t.getSirketId() != null) {
                        bildirimService.bildirimGonder(t.getSirketId(), "TESLIMAT",
                                "Teslimat gecikti: " + (t.getMusteriAdi() != null ? t.getMusteriAdi() : ("#" + t.getFaturaNumarasi())),
                                "Beklenen teslim: " + t.getBeklenenTeslimTarihi()
                                        + (t.getTeslimatAdresi() != null ? " - " + t.getTeslimatAdresi() : ""));
                    }
                    teslimatService.gecikmeBildirildiIsaretle(t.getId());
                } catch (Exception e) {
                    log.warn("Teslimat gecikme bildirimi islenemedi ({}): {}", t.getId(), e.getMessage());
                }
            }
            if (!gecikenler.isEmpty()) {
                log.info("Teslimat gecikme uyarisi tamamlandi - Bildirilen: {}", gecikenler.size());
            }
        } catch (Exception e) {
            log.warn("Teslimat gecikme kontrolu calistirilamadi: {}", e.getMessage());
        }
    }
}
