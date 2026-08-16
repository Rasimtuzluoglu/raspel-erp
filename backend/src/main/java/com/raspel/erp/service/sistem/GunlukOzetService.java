package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Her sabah 07:00'de aktif firmalara günlük özet e-postası gönderir.
 * Özet: kritik stok sayısı, vadesi geçen fatura sayısı ve toplam tutarı.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GunlukOzetService {

    private final SirketRepository sirketRepository;
    private final StokRepository stokRepository;
    private final FaturaRepository faturaRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 7 * * *")
    public void gunlukOzetGonder() {
        List<Sirket> sirketler = sirketRepository.findByAktifTrue();
        for (Sirket s : sirketler) {
            try {
                if (s.getEmail() == null || s.getEmail().isBlank()) continue;
                ozetGonder(s);
            } catch (Exception e) {
                log.warn("Günlük özet gönderilemedi ({}) : {}", s.getAd(), e.getMessage());
            }
        }
    }

    private void ozetGonder(Sirket s) {
        Long sirketId = s.getId();
        int kritikStok = stokRepository.kritikStoklar(sirketId).size();
        List<Fatura> vadesiGecen = faturaRepository.findVadesiGecen(
                sirketId, Fatura.FaturaDurum.KESILDI, List.of("ODENDI", "IPTAL"), LocalDate.now());
        int vadesiGecenSayisi = vadesiGecen.size();

        String konu = "RasPel ERP - Günlük Özet (" + LocalDate.now() + ")";
        String icerik = "Merhaba,\n\n"
                + "Bugünkü özetiniz:\n"
                + "- Kritik stok seviyesindeki ürün: " + kritikStok + "\n"
                + "- Vadesi geçen fatura: " + vadesiGecenSayisi + "\n\n"
                + "Detaylar için sisteme giriş yapabilirsiniz.\n\n"
                + "RasPel ERP";

        emailService.emailGonder(s.getEmail(), konu, icerik);
        log.info("Günlük özet gönderildi -> {} ({})", s.getAd(), s.getEmail());
    }
}
