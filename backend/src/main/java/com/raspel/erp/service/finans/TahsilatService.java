package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TahsilatService {

    private final FaturaRepository faturaRepository;
    private final EmailService emailService;

    private static final List<String> ODENDI_DURUMLARI = List.of("ODENDI", "IPTAL");
    private static final DateTimeFormatter VADE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public TahsilatDTO ozetGetir(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        List<Fatura> faturalar = faturaRepository.findTahsilatEdilecek(
                sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI);

        List<Fatura> cariBasli = faturalar.stream()
                .filter(f -> f.getCariHesap() != null)
                .collect(Collectors.toList());

        Map<Long, List<Fatura>> cariyeGore = cariBasli.stream()
                .collect(Collectors.groupingBy(f -> f.getCariHesap().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<TahsilatDTO.CariOzet> cariler = cariyeGore.entrySet().stream()
                .map(e -> cariOzetiOlustur(e.getValue(), bugun))
                .sorted(Comparator.comparingInt(TahsilatDTO.CariOzet::getMaxGecikmeGunu).reversed())
                .collect(Collectors.toList());

        BigDecimal toplamAlacak = cariBasli.stream()
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vadesiGecmisToplam = cariBasli.stream()
                .filter(f -> gecikmeGunu(vade(f), bugun) > 0)
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vadesiYaklasanToplam = cariBasli.stream()
                .filter(f -> {
                    int gun = gecikmeGunu(vade(f), bugun);
                    return gun <= 0 && gun >= -30;
                })
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int gecikmisCariSayisi = (int) cariler.stream()
                .filter(c -> c.getGecikmisAlacak() != null && c.getGecikmisAlacak().signum() > 0)
                .count();

        return TahsilatDTO.builder()
                .toplamAlacak(toplamAlacak)
                .vadesiGecmisToplam(vadesiGecmisToplam)
                .vadesiYaklasanToplam(vadesiYaklasanToplam)
                .acikFaturaSayisi(cariBasli.size())
                .gecikmisCariSayisi(gecikmisCariSayisi)
                .cariler(cariler)
                .build();
    }

    @Transactional(readOnly = true)
    public int hatirlat(Long cariId, Long sirketId) {
        List<Fatura> faturalar = faturaRepository.findTahsilatEdilecek(
                sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI);

        List<Fatura> cariFaturalari = faturalar.stream()
                .filter(f -> f.getCariHesap() != null && f.getCariHesap().getId().equals(cariId))
                .collect(Collectors.toList());

        if (cariFaturalari.isEmpty()) {
            throw new ResourceNotFoundException("Cari için ödenmemiş fatura bulunamadı", cariId);
        }

        CariHesap cari = cariFaturalari.get(0).getCariHesap();
        if (cari.getEmail() == null || cari.getEmail().isBlank()) {
            throw new BusinessException("Bu cari hesap için e-posta adresi tanımlı değil");
        }

        int gonderilen = 0;
        for (Fatura f : cariFaturalari) {
            LocalDate vade = vade(f);
            emailService.odemeHatimlaticiGonder(
                    cari.getEmail(),
                    f.getFaturaNumarasi(),
                    f.getGenelToplam() != null ? f.getGenelToplam().toString() : "0.00",
                    f.getKalanTutar() != null ? f.getKalanTutar().toString() : "0.00",
                    vade != null ? vade.format(VADE_FORMAT) : "-",
                    cari.getAd());
            gonderilen++;
        }
        log.info("Tahsilat hatırlatması gönderildi -> Cari: {}, Fatura sayısı: {}", cari.getAd(), gonderilen);
        return gonderilen;
    }

    private TahsilatDTO.CariOzet cariOzetiOlustur(List<Fatura> faturalar, LocalDate bugun) {
        CariHesap cari = faturalar.get(0).getCariHesap();

        BigDecimal toplam = faturalar.stream()
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gecikmis = faturalar.stream()
                .filter(f -> gecikmeGunu(vade(f), bugun) > 0)
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int maxGecikme = faturalar.stream()
                .mapToInt(f -> gecikmeGunu(vade(f), bugun))
                .max().orElse(0);

        List<TahsilatDTO.FaturaOzet> faturaOzetleri = faturalar.stream()
                .sorted(Comparator.comparing(f -> vade(f), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(f -> TahsilatDTO.FaturaOzet.builder()
                        .faturaId(f.getId())
                        .faturaNumarasi(f.getFaturaNumarasi())
                        .vadeTarihi(vade(f))
                        .kalanTutar(f.getKalanTutar())
                        .gecikmeGunu(gecikmeGunu(vade(f), bugun))
                        .build())
                .collect(Collectors.toList());

        return TahsilatDTO.CariOzet.builder()
                .cariId(cari.getId())
                .cariAd(cari.getAd())
                .telefon(cari.getTelefon())
                .email(cari.getEmail())
                .toplamAlacak(toplam)
                .gecikmisAlacak(gecikmis)
                .faturaSayisi(faturalar.size())
                .maxGecikmeGunu(maxGecikme)
                .aralik(aralik(maxGecikme))
                .faturalar(faturaOzetleri)
                .build();
    }

    private LocalDate vade(Fatura f) {
        if (f.getVadeTarihi() != null) return f.getVadeTarihi();
        CariHesap cari = f.getCariHesap();
        if (cari != null && cari.getOdemeVadesi() != null) {
            return f.getTarih().plusDays(cari.getOdemeVadesi());
        }
        return f.getTarih();
    }

    private int gecikmeGunu(LocalDate vade, LocalDate bugun) {
        if (vade == null) return 0;
        return (int) ChronoUnit.DAYS.between(vade, bugun);
    }

    private String aralik(int gun) {
        if (gun <= 0) return "Vadesi Gelmemiş";
        if (gun <= 30) return "0-30 Gün";
        if (gun <= 60) return "31-60 Gün";
        if (gun <= 90) return "61-90 Gün";
        return "90+ Gün";
    }
}
