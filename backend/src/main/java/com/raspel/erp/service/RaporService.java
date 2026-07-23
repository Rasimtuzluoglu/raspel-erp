package com.raspel.erp.service;

import com.raspel.erp.dto.*;
import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Fatura;
import com.raspel.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaporService {

    private final CariHesapRepository cariHesapRepository;
    private final HareketRepository hareketRepository;
    private final FaturaRepository faturaRepository;
    private final CariHesapService cariHesapService;
    private final HareketService hareketService;

    public RaporDTO.CariEkstreDTO cariEkstreGetir(Long cariHesapId, LocalDate baslangic, LocalDate bitis) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new RuntimeException("Cari hesap bulunamadı"));

        List<HareketDTO> hareketler = hareketRepository
                .findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(cariHesapId, baslangic, bitis)
                .stream().map(hareketService::entityDTOyeCevir).collect(Collectors.toList());

        BigDecimal donemBasi = cari.getBakiye();
        BigDecimal donemSonu = donemBasi;
        for (HareketDTO h : hareketler) {
            if ("TAHSILAT".equals(h.getTur())) donemSonu = donemSonu.add(h.getTutar());
            else donemSonu = donemSonu.subtract(h.getTutar());
        }

        return RaporDTO.CariEkstreDTO.builder()
                .cariAd(cari.getAd()).donemBasBakiye(cari.getBakiye())
                .donemSonBakiye(donemSonu).hareketler(hareketler).build();
    }

    public RaporDTO.GelirGiderOzetDTO gelirGiderOzeti(LocalDate baslangic, LocalDate bitis) {
        var hareketler = hareketRepository.findByHareketTarihiBetweenOrderByHareketTarihiAsc(baslangic, bitis);

        BigDecimal toplamGelir = hareketler.stream()
                .filter(h -> "TAHSILAT".equals(h.getTur()))
                .map(h -> h.getTutar()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal toplamGider = hareketler.stream()
                .filter(h -> "ÖDEME".equals(h.getTur()))
                .map(h -> h.getTutar()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = toplamGelir.subtract(toplamGider);

        Map<String, BigDecimal> aylik = new LinkedHashMap<>();
        for (var h : hareketler) {
            String ay = h.getHareketTarihi().getYear() + "-" + String.format("%02d", h.getHareketTarihi().getMonthValue());
            BigDecimal ek = "TAHSILAT".equals(h.getTur()) ? h.getTutar() : h.getTutar().negate();
            aylik.merge(ay, ek, BigDecimal::add);
        }

        List<Map<String, Object>> aylikDagilim = aylik.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ay", e.getKey());
            m.put("net", e.getValue());
            return m;
        }).collect(Collectors.toList());

        return RaporDTO.GelirGiderOzetDTO.builder()
                .toplamGelir(toplamGelir).toplamGider(toplamGider).netKarZarar(net)
                .aylikDagilim(aylikDagilim).build();
    }

    public RaporDTO.KdvRaporDTO kdvRaporu(LocalDate baslangic, LocalDate bitis) {
        List<Fatura> faturalar = faturaRepository.findAllByOrderByTarihDesc().stream()
                .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .filter(f -> !f.getTarih().isBefore(baslangic) && !f.getTarih().isAfter(bitis))
                .collect(Collectors.toList());

        BigDecimal cikisKdv = faturalar.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS)
                .map(Fatura::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal girisKdv = faturalar.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.ALIS)
                .map(Fatura::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);

        return RaporDTO.KdvRaporDTO.builder()
                .toplamKdvCikis(cikisKdv).toplamKdvGiris(girisKdv)
                .kdvFarki(cikisKdv.subtract(girisKdv)).build();
    }

    public List<RaporDTO.YaslandirmaDTO> yaslandirmaRaporu() {
        LocalDate bugun = LocalDate.now();
        return cariHesapRepository.findAll().stream()
                .filter(c -> c.getBakiye().compareTo(BigDecimal.ZERO) < 0)
                .map(c -> {
                    int gun = (int) ChronoUnit.DAYS.between(c.getGuncellemeTarihi().toLocalDate(), bugun);
                    String aralik;
                    if (gun <= 30) aralik = "0-30 Gün";
                    else if (gun <= 60) aralik = "31-60 Gün";
                    else if (gun <= 90) aralik = "61-90 Gün";
                    else aralik = "90+ Gün";

                    return RaporDTO.YaslandirmaDTO.builder()
                            .cariAd(c.getAd()).bakiye(c.getBakiye().abs()).gun(gun).aralik(aralik).build();
                })
                .sorted(Comparator.comparingInt(RaporDTO.YaslandirmaDTO::getGun).reversed())
                .collect(Collectors.toList());
    }
}
