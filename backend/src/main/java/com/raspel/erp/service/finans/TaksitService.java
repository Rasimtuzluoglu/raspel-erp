package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.TaksitDTO;
import com.raspel.erp.dto.finans.TaksitOdeDTO;
import com.raspel.erp.dto.finans.TaksitPlanDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Taksit;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.TaksitRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaksitService {

    private final TaksitRepository taksitRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;

    public static final String DURUM_BEKLEMEDE = "BEKLEMEDE";
    public static final String DURUM_ODENDI = "ODENDI";
    private static final DateTimeFormatter PLAN_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Transactional(readOnly = true)
    public Page<TaksitDTO> listele(Long sirketId, Long cariId, String durum,
                                   LocalDate baslangic, LocalDate bitis, Pageable pageable) {
        return listele(sirketId, cariId, durum, baslangic, bitis, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaksitDTO> listele(Long sirketId, Long cariId, String durum,
                                   LocalDate baslangic, LocalDate bitis, String q, Pageable pageable) {
        Specification<Taksit> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sirketId"), sirketId));
            if (cariId != null) {
                predicates.add(cb.equal(root.get("cariHesap").get("id"), cariId));
            }
            if (durum != null && !durum.isBlank() && !"TUMU".equalsIgnoreCase(durum)) {
                predicates.add(cb.equal(root.get("odemeDurumu"), durum.trim().toUpperCase()));
            }
            if (baslangic != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("vadeTarihi"), baslangic));
            }
            if (bitis != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("vadeTarihi"), bitis));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cariHesap").get("ad")),
                        "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return taksitRepository.findAll(spec, pageable).map(this::toDTO);
    }

    /** Belirtilen gun icinde vadesi gelecek (veya gecmis) odenmemis taksitler. */
    @Transactional(readOnly = true)
    public List<TaksitDTO> yaklasan(Long sirketId, int gun) {
        LocalDate bugun = LocalDate.now();
        LocalDate bitis = bugun.plusDays(Math.max(0, gun));
        return taksitRepository
                .findBySirketIdAndOdemeDurumuNotAndVadeTarihiBetweenOrderByVadeTarihiAsc(
                        sirketId, DURUM_ODENDI, bugun.minusYears(5), bitis)
                .stream()
                .filter(t -> t.getVadeTarihi() == null || !t.getVadeTarihi().isAfter(bitis))
                .map(this::toDTO)
                .toList();
    }

    /** Plan bazlı özet listesi (opsiyonel cari adı araması). */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.finans.TaksitPlanOzetDTO> planlar(Long sirketId, String q) {
        if (sirketId == null) {
            return List.of();
        }
        String arama = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(java.util.Locale.ROOT);
        return taksitRepository.planOzetleri(sirketId, arama).stream()
                .map(row -> {
                    BigDecimal toplam = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
                    BigDecimal odenen = row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO;
                    return com.raspel.erp.dto.finans.TaksitPlanOzetDTO.builder()
                            .planNo((String) row[0])
                            .cariAd((String) row[1])
                            .kurum((String) row[2])
                            .taksitSayisi(((Number) row[3]).longValue())
                            .odenenAdet(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                            .toplamTutar(toplam)
                            .odenenTutar(odenen)
                            .kalanTutar(toplam.subtract(odenen))
                            .ilkVade((LocalDate) row[7])
                            .sonVade((LocalDate) row[8])
                            .build();
                })
                .toList();
    }

    /** Plan detayı: plana ait kalemler. */
    @Transactional(readOnly = true)
    public List<TaksitDTO> planDetay(Long sirketId, String planNo) {
        List<Taksit> kalemler = taksitRepository.findBySirketIdAndPlanNoOrderByTaksitNoAsc(sirketId, planNo);
        if (kalemler.isEmpty()) {
            throw new ResourceNotFoundException("Taksit planı bulunamadı: " + planNo);
        }
        return kalemler.stream().map(this::toDTO).toList();
    }

    /** Belirtilen ay icindeki tum taksitler (takvim gorunumu icin). */
    @Transactional(readOnly = true)
    public List<TaksitDTO> takvim(Long sirketId, int yil, int ay) {
        LocalDate bas = LocalDate.of(yil, ay, 1);
        LocalDate bit = bas.withDayOfMonth(bas.lengthOfMonth());
        return taksitRepository
                .findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(sirketId, bas, bit)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.raspel.erp.dto.finans.TaksitOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.finans.TaksitOzetDTO.builder()
                    .bekleyenToplam(BigDecimal.ZERO).gecikmisToplam(BigDecimal.ZERO)
                    .buAyToplam(BigDecimal.ZERO).odenenBuYilToplam(BigDecimal.ZERO).build();
        }
        LocalDate bugun = LocalDate.now();
        LocalDate ayBas = bugun.withDayOfMonth(1);
        LocalDate ayBit = bugun.withDayOfMonth(bugun.lengthOfMonth());
        LocalDate yilBas = bugun.withDayOfYear(1);
        LocalDate yilBit = bugun.withDayOfYear(bugun.lengthOfYear());
        return com.raspel.erp.dto.finans.TaksitOzetDTO.builder()
                .bekleyenToplam(taksitRepository.sumBekleyenTutar(sirketId))
                .bekleyenAdet(taksitRepository.countBekleyen(sirketId))
                .gecikmisToplam(taksitRepository.sumGecikmisTutar(sirketId, bugun))
                .gecikmisAdet(taksitRepository.countGecikmis(sirketId, bugun))
                .buAyToplam(taksitRepository.sumBuAyTutar(sirketId, ayBas, ayBit))
                .odenenBuYilToplam(taksitRepository.sumOdenenTutar(sirketId, yilBas, yilBit))
                .odenenBuYilAdet(taksitRepository.countOdenen(sirketId, yilBas, yilBit))
                .build();
    }

    @Transactional
    public List<TaksitDTO> planOlustur(TaksitPlanDTO dto, Long sirketId) {
        if (dto.getToplamTutar() == null || dto.getToplamTutar().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Toplam tutar 0'dan büyük olmalıdır");
        }
        if (dto.getTaksitSayisi() == null || dto.getTaksitSayisi() < 1) {
            throw new BusinessException("Taksit sayısı en az 1 olmalidir");
        }
        CariHesap cari = cariHesapRepository.findById(dto.getCariId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariId()));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");

        int adet = dto.getTaksitSayisi();
        int periyot = dto.getPeriyotAy() != null && dto.getPeriyotAy() > 0 ? dto.getPeriyotAy() : 1;
        LocalDate baslangic = dto.getBaslangicTarihi() != null ? dto.getBaslangicTarihi() : LocalDate.now();
        BigDecimal toplam = dto.getToplamTutar().setScale(2, RoundingMode.HALF_UP);
        BigDecimal taksitTutari = toplam.divide(BigDecimal.valueOf(adet), 2, RoundingMode.DOWN);

        String planNo = "TKS-" + LocalDateTime.now().format(PLAN_NO_FORMAT)
                + "-" + ThreadLocalRandom.current().nextInt(100, 1000);

        List<Taksit> kalemler = new ArrayList<>();
        BigDecimal dagitilan = BigDecimal.ZERO;
        for (int i = 0; i < adet; i++) {
            BigDecimal tutar = (i == adet - 1) ? toplam.subtract(dagitilan) : taksitTutari;
            dagitilan = dagitilan.add(tutar);
            kalemler.add(Taksit.builder()
                    .sirketId(sirketId)
                    .cariHesap(cari)
                    .faturaId(dto.getFaturaId())
                    .planNo(planNo)
                    .kurum(dto.getKurum())
                    .taksitNo(i + 1)
                    .taksitSayisi(adet)
                    .vadeTarihi(baslangic.plusMonths((long) periyot * i))
                    .tutar(tutar)
                    .odemeDurumu(DURUM_BEKLEMEDE)
                    .aciklama(dto.getAciklama())
                    .build());
        }
        List<Taksit> kaydedilen = taksitRepository.saveAll(kalemler);
        log.info("Taksit plani olusturuldu -> Cari: {}, Plan: {}, Kalem: {}, Toplam: {}",
                cari.getAd(), planNo, adet, toplam);
        return kaydedilen.stream().map(this::toDTO).toList();
    }

    @Transactional
    public TaksitDTO ode(Long id, TaksitOdeDTO dto, Long sirketId) {
        Taksit taksit = taksitRepository.findByIdAndSirketId(id, sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Taksit", id));
        if (DURUM_ODENDI.equals(taksit.getOdemeDurumu())) {
            throw new BusinessException("Bu taksit zaten odendi");
        }
        taksit.setOdemeDurumu(DURUM_ODENDI);
        taksit.setOdemeTarihi(dto != null && dto.getOdemeTarihi() != null
                ? dto.getOdemeTarihi() : LocalDate.now());
        if (dto != null) {
            if (dto.getHareketId() != null) {
                taksit.setHareketId(dto.getHareketId());
            }
            if (dto.getAciklama() != null && !dto.getAciklama().isBlank()) {
                taksit.setAciklama(dto.getAciklama());
            }
        }
        Taksit kaydedilen = taksitRepository.save(taksit);
        log.info("Taksit odendi -> id: {}, tutar: {}", kaydedilen.getId(), kaydedilen.getTutar());
        return toDTO(kaydedilen);
    }

    @Transactional
public void sil(Long id, Long sirketId) {
          Taksit taksit = taksitRepository.findByIdAndSirketId(id, sirketId)
                  .orElseThrow(() -> new ResourceNotFoundException("Taksit", id));
          // REDTEAM/Faz1.6: Ödenmiş taksit kalemi silinebiliyordu. Bu, para
          // hareketi olan bir kaydın denetim izini yok eder: takvimden
          // "ödendi" işaretlenmiş, bankada/cari defterinde karşılığı olan bir
          // kalem sessizce kayboluyordu.
          // REDTEAM/Faz1.2 sonrası taksitler gerçek tahsilat hareketine
          // bağlandığı için (hareketId) bu koruma zorunludur: hareketi olan
          // kalem silinirse hareket yetim (orphan) kalır.
          if (DURUM_ODENDI.equals(taksit.getOdemeDurumu())) {
              throw new BusinessException(
                      "Ödenmiş taksit kalemi silinemez (ödeme durumu: ODENDI). "
                      + "Önce ilgili tahsilat hareketini geri alın.");
          }
          if (taksit.getHareketId() != null) {
              throw new BusinessException(
                      "Bu taksit bir tahsilat hareketine bağlı (hareketId="
                              + taksit.getHareketId() + "); önce hareketi geri alın.");
          }
          taksitRepository.delete(taksit);
      }

@Transactional
    public void planSil(String planNo, Long sirketId) {
          // REDTEAM/Faz1.6: planSil taksitleri toptan sildiği için tekil
          // silmedeki korumayı baypas ediyordu; içinde ödenmiş kalem varsa
          // sessizce kayboluyordu. Aynı iki kural burada da uygulanır.
          long odenmisSayisi = taksitRepository.countByPlanNoAndSirketIdAndOdemeDurumu(
                  planNo, sirketId, DURUM_ODENDI);
          if (odenmisSayisi > 0) {
              throw new BusinessException(
                      "Bu planda " + odenmisSayisi + " adet ödenmiş taksit var. "
                      + "Ödenmiş kalemler silinemez; önce ilgili tahsilat hareketlerini geri alın.");
          }
          long hareketliSayisi = taksitRepository.countByPlanNoAndSirketIdAndHareketIdIsNotNull(
                  planNo, sirketId);
          if (hareketliSayisi > 0) {
              throw new BusinessException(
                      "Bu planda " + hareketliSayisi
                              + " adet taksit tahsilat hareketine bağlı; önce hareketleri geri alın.");
          }
          taksitRepository.deleteByPlanNoAndSirketId(planNo, sirketId);
          log.info("Taksit plani silindi -> plan: {}", planNo);
      }

    private TaksitDTO toDTO(Taksit t) {
        long gecikme = 0;
        if (!DURUM_ODENDI.equals(t.getOdemeDurumu()) && t.getVadeTarihi() != null
                && t.getVadeTarihi().isBefore(LocalDate.now())) {
            gecikme = ChronoUnit.DAYS.between(t.getVadeTarihi(), LocalDate.now());
        }
        return TaksitDTO.builder()
                .id(t.getId())
                .sirketId(t.getSirketId())
                .cariId(t.getCariHesap() != null ? t.getCariHesap().getId() : null)
                .cariAd(t.getCariHesap() != null ? t.getCariHesap().getAd() : null)
                .faturaId(t.getFaturaId())
                .hareketId(t.getHareketId())
                .planNo(t.getPlanNo())
                .kurum(t.getKurum())
                .taksitNo(t.getTaksitNo())
                .taksitSayisi(t.getTaksitSayisi())
                .vadeTarihi(t.getVadeTarihi())
                .tutar(t.getTutar())
                .odemeDurumu(t.getOdemeDurumu())
                .odemeTarihi(t.getOdemeTarihi())
                .aciklama(t.getAciklama())
                .gecikmeGunu(gecikme)
                .olusturmaTarihi(t.getOlusturmaTarihi())
                .build();
    }
}
