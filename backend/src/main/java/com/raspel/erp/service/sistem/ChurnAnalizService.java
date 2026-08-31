package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.ChurnRiskDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Müşteri kayıp (churn) riski skorlama.
 * Son işlem tarihine göre: uzun süre işlem yapmayan müşteriler kayıp riski taşır.
 * Skor = son işlem gün sayısı + işlem sayısı/ciro ağırlıklarına göre hesaplanır.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ChurnAnalizService {

    private final CariHesapRepository cariHesapRepository;
    private final HareketRepository hareketRepository;
    private final FaturaRepository faturaRepository;

    private static final int YUKSEK_ESIK_GUN = 90;
    private static final int ORTA_ESIK_GUN = 45;

    public List<ChurnRiskDTO> churnRiskiAnaliz(Long sirketId) {
        List<CariHesap> cariler = cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId);
        List<Hareket> hareketler = hareketRepository.findBySirketIdOrderByHareketTarihiDesc(sirketId, Pageable.unpaged()).getContent();
        List<Fatura> faturalar = faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, Pageable.unpaged()).getContent();

        Map<Long, List<Hareket>> hareketByCari = hareketler.stream()
                .filter(h -> h.getCariHesap() != null)
                .collect(Collectors.groupingBy(h -> h.getCariHesap().getId()));
        Map<Long, List<Fatura>> faturaByCari = faturalar.stream()
                .filter(f -> f.getCariHesap() != null)
                .collect(Collectors.groupingBy(f -> f.getCariHesap().getId()));

        LocalDate bugun = LocalDate.now();
        List<ChurnRiskDTO> sonuc = new ArrayList<>();

        for (CariHesap c : cariler) {
            Long cariId = c.getId();
            List<Hareket> h = hareketByCari.getOrDefault(cariId, List.of());
            List<Fatura> f = faturaByCari.getOrDefault(cariId, List.of());

            LocalDate sonIslem = null;
            if (!h.isEmpty()) {
                sonIslem = h.stream().map(Hareket::getHareketTarihi).max(LocalDate::compareTo).orElse(null);
            }
            for (Fatura ft : f) {
                if (ft.getTarih() != null && (sonIslem == null || ft.getTarih().isAfter(sonIslem))) {
                    sonIslem = ft.getTarih();
                }
            }

            // Hiç işlem yoksa bilgi eksik, atla (churn hesaplanamaz).
            if (sonIslem == null) continue;

            int gunOnce = (int) ChronoUnit.DAYS.between(sonIslem, bugun);
            int islemSayisi = h.size() + f.size();
            BigDecimal ciro = f.stream()
                    .map(Fatura::getGenelToplam).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int skor = skorHesapla(gunOnce, islemSayisi);
            String seviye = skor >= 70 ? "YUKSEK" : skor >= 40 ? "ORTA" : "DUSUK";
            String oneri = oneriUret(seviye, gunOnce);

            sonuc.add(ChurnRiskDTO.builder()
                    .cariId(cariId)
                    .cariAd(c.getAd())
                    .seviye(seviye)
                    .skor(skor)
                    .sonIslemGunOnce(gunOnce)
                    .toplamIslemSayisi((long) islemSayisi)
                    .toplamCiro(ciro)
                    .oneri(oneri)
                    .build());
        }

        sonuc.sort(Comparator.comparingInt(ChurnRiskDTO::getSkor).reversed());
        return sonuc;
    }

    private int skorHesapla(int gunOnce, int islemSayisi) {
        int gunSkor;
        if (gunOnce >= YUKSEK_ESIK_GUN) {
            gunSkor = 100;
        } else if (gunOnce >= ORTA_ESIK_GUN) {
            gunSkor = 70;
        } else {
            gunSkor = Math.max(0, 40 - (gunOnce / 2));
        }
        // Yüksek işlem hacmi riski bir miktar azaltır.
        if (islemSayisi >= 20) gunSkor -= 15;
        else if (islemSayisi >= 10) gunSkor -= 5;
        return Math.max(0, Math.min(100, gunSkor));
    }

    private String oneriUret(String seviye, int gunOnce) {
        return switch (seviye) {
            case "YUKSEK" -> "Müşteri " + gunOnce + " gündür işlem yapmamış. İletişime geçin veya kampanya önerin.";
            case "ORTA" -> "Müşteri etkileşimi azalıyor. Periyodik hatırlatma veya teklif gönderin.";
            default -> "Müşteri aktif görünüyor.";
        };
    }
}
