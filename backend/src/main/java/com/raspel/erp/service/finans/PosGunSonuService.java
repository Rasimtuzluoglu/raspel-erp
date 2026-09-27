package com.raspel.erp.service.finans;

import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.PosGunSonu;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.exception.BusinessException;
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
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    // Self-invocation'da @Transactional proxy'si devreye girmediği için programatik tx kullanılır.
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

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

            BigDecimal komisyon = hareketler.stream()
                    .filter(h -> bugun.equals(h.getHareketTarihi()) && h.getKomisyonTutar() != null)
                    .map(Hareket::getKomisyonTutar)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Perakende (hızlı satış) kart tahsilatları cari hareket oluşturmaz; POS bağlı
            // faturalardan toplanır. Aynı faturaya bağlı cari tahsilat varsa çift sayılmaz.
            java.util.Set<Long> cariTahsilatliFaturaIdler = hareketler.stream()
                    .filter(h -> h.getFaturaId() != null)
                    .map(Hareket::getFaturaId)
                    .collect(java.util.stream.Collectors.toSet());
            List<com.raspel.erp.entity.ticaret.Fatura> posFaturalar =
                    faturaRepository.findBySirketIdAndTarihBetween(sirketId, bugun, bugun).stream()
                            .filter(f -> p.getId().equals(f.getPosTerminaliId())
                                    && f.getTur() == com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS
                                    && f.getDurum() != com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.IPTAL
                                    && "KART".equalsIgnoreCase(f.getOdemeYontemi())
                                    && f.getOdenenTutar() != null && f.getOdenenTutar().signum() > 0
                                    && !cariTahsilatliFaturaIdler.contains(f.getId()))
                            .toList();
            BigDecimal faturaTutar = posFaturalar.stream()
                    .map(f -> f.getOdenenTutar() != null ? f.getOdenenTutar() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal faturaKomisyon = posFaturalar.stream()
                    .map(f -> f.getKomisyonTutar() != null ? f.getKomisyonTutar() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            tutar = tutar.add(faturaTutar);
            komisyon = komisyon.add(faturaKomisyon);

            if (tutar.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Banka tanımlı olmayan POS atlanır; banka tanımlı ama kayıt yoksa işlem açıkça reddedilir
            // (sessizce "başarılı" görünüp bankaya aktarılmaması finansal tutarsızlığa yol açar).
            Banka banka = bankaRepository.findById(p.getBankaId())
                    .orElseThrow(() -> new BusinessException(
                            "Talep edilen POS'un bankası bulunamadı (POS: " + p.getAd()
                                    + ", bankaId: " + p.getBankaId() + "). Gün sonu işlemi durduruldu."));
            // Bankaya POS komisyonu düşülerek NET tutar geçer; brüt yazılırsa bakiye şişer.
            BigDecimal net = tutar.subtract(komisyon);
            if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
            banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).add(net));
            bankaRepository.save(banka);

            try {
                // (pos_id, tarih) unique kısıt + flush: eşzamanlı iki gün sonu çalışmasında
                // ikinci kayıt DB seviyesinde reddedilir; banka bakiyesi iki kez artmaz.
                gunSonuRepository.saveAndFlush(PosGunSonu.builder()
                        .posId(p.getId()).sirketId(sirketId)
                        .tutar(tutar).komisyon(komisyon).tarih(bugun)
                        .build());
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Başka bir eşzamanlı çalışma bu POS için gün sonunu zaten işledi.
                // Banka bakiyesi bu transaction içinde şişmesin diye işlemi geri alma sinyali ver.
                log.warn("POS gün sonu zaten işlenmiş (POS: {}, tarih: {}); eşzamanlı çalışma nedeniyle atlandı",
                        p.getAd(), bugun);
                throw e;
            }

            sonuc.add(Map.of(
                    "posId", p.getId(),
                    "posAd", p.getAd(),
                    "bankaId", p.getBankaId(),
                    "tutar", tutar,
                    "komisyon", komisyon,
                    "net", net
            ));
            log.info("POS gün sonu: {} -> brüt {} TL, komisyon {} TL, banka net {} TL",
                    p.getAd(), tutar, komisyon, net);
        }
        return sonuc;
    }

    @Transactional(readOnly = true)
    public List<PosGunSonu> rapor(Long sirketId) {
        return gunSonuRepository.findTop100BySirketIdOrderByTarihDesc(sirketId);
    }

    @Scheduled(cron = "0 0 23 * * *")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "posGunSonu", lockAtMostFor = "PT20M", lockAtLeastFor = "PT1M")
    public void otomatikGunSonu() {
        try {
            sirketRepository.findByAktifTrue().forEach(s -> {
                try {
                    // Her şirket için ayrı ve atomik transaction (banka bakiyesi + gün sonu kaydı).
                    transactionTemplate.execute(status -> gunSonuIsle(s.getId()));
                } catch (Exception e) {
                    log.warn("POS gün sonu çalıştırılamadı ({}): {}", s.getAd(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("POS gün sonu planlaması çalıştırılamadı: {}", e.getMessage());
        }
    }
}
