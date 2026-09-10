package com.raspel.erp.service.finans;

import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.PosGunSonu;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.PosGunSonuRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gün içinde POS'tan çekilen tutarları gün sonunda ilgili banka hesabına aktarır.
 * Her POS için günde bir kez çalışır (idempotent).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PosGunSonuService {

    private final PosTerminaliRepository posRepository;
    private final HareketRepository hareketRepository;
    private final BankaRepository bankaRepository;
    private final PosGunSonuRepository gunSonuRepository;
    private final SirketRepository sirketRepository;

    @Transactional
    public List<Map<String, Object>> gunSonuIsle(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        List<Map<String, Object>> sonuc = new ArrayList<>();
        for (PosTerminali p : posRepository.findBySirketIdAndAktifTrueOrderByAd(sirketId)) {
            if (p.getBankaId() == null) continue;
            if (gunSonuRepository.existsByPosIdAndTarih(p.getId(), bugun)) continue;

            List<Hareket> hareketler = hareketRepository.findBySirketIdAndPosTerminaliIdOrderByHareketTarihiDesc(sirketId, p.getId());
            BigDecimal tutar = hareketler.stream()
                    .filter(h -> bugun.equals(h.getHareketTarihi()) && h.getTutar() != null)
                    .map(Hareket::getTutar)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (tutar.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal komisyon = hareketler.stream()
                    .filter(h -> bugun.equals(h.getHareketTarihi()) && h.getKomisyonTutar() != null)
                    .map(Hareket::getKomisyonTutar)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            bankaRepository.findById(p.getBankaId()).ifPresent(banka -> {
                banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).add(tutar));
                bankaRepository.save(banka);
            });

            gunSonuRepository.save(PosGunSonu.builder()
                    .posId(p.getId()).sirketId(sirketId)
                    .tutar(tutar).komisyon(komisyon).tarih(bugun)
                    .build());

            sonuc.add(Map.of(
                    "posId", p.getId(),
                    "posAd", p.getAd(),
                    "bankaId", p.getBankaId(),
                    "tutar", tutar,
                    "komisyon", komisyon
            ));
            log.info("POS gün sonu: {} -> {} TL (komisyon: {} TL)", p.getAd(), tutar, komisyon);
        }
        return sonuc;
    }

    @Transactional(readOnly = true)
    public List<PosGunSonu> rapor(Long sirketId) {
        return gunSonuRepository.findTop100BySirketIdOrderByTarihDesc(sirketId);
    }

    @Scheduled(cron = "0 0 23 * * *")
    public void otomatikGunSonu() {
        try {
            sirketRepository.findByAktifTrue().forEach(s -> {
                try {
                    gunSonuIsle(s.getId());
                } catch (Exception e) {
                    log.warn("POS gün sonu çalıştırılamadı ({}): {}", s.getAd(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("POS gün sonu planlaması çalıştırılamadı: {}", e.getMessage());
        }
    }
}
