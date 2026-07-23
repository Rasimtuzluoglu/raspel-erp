package com.raspel.erp.service;

import com.raspel.erp.dto.DashboardDTO;
import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.repository.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CariHesapService cariHesapService;
    private final HareketService hareketService;
    private final SiparisRepository siparisRepository;
    private final PersonelRepository personelRepository;
    private final PersonelIzinRepository personelIzinRepository;
    private final StokHareketRepository stokHareketRepository;
    private final StokRepository stokRepository;

    @Cacheable(value = "dashboard", key = "'veriler'")
    @CircuitBreaker(name = "dashboardService", fallbackMethod = "dashboardFallback")
    public DashboardDTO dashboardVerileriGetir() {
        log.debug("Dashboard verileri getiriliyor...");

        Long toplamCariSayisi = cariHesapService.toplamCariSayisiGetir();
        var toplamBakiye = cariHesapService.toplamBakiyeGetir();
        List<HareketDTO> sonHareketler = hareketService.sonHareketleriGetir(5);

        Long aktifCalisan = personelRepository.countByAktifTrue();
        Long bugunIzinli = personelIzinRepository.countBugunIzinli(LocalDate.now());
        var ayBaslangic = LocalDate.now().withDayOfMonth(1);
        var ayBitis = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Long buAyIseBaslayacak = personelRepository.countByIseGirisTarihiBetween(ayBaslangic, ayBitis);

        Long bugunkuSiparis = siparisRepository.countByTarih(LocalDate.now());
        Long bekleyenTeslimat = siparisRepository.countByDurumNot("TAMAMLANDI");
        BigDecimal iadeOrani = BigDecimal.ZERO;

        long toplamStok = stokRepository.count();
        long toplamCikis = stokHareketRepository.countByTur("CIKIS");
        BigDecimal stokDevirHizi = toplamStok > 0
                ? BigDecimal.valueOf(toplamCikis).divide(BigDecimal.valueOf(toplamStok), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardDTO.EnCokSatanDTO> enCokSatanlar = stokHareketRepository.enCokSatanlar().stream()
                .limit(5)
                .map(m -> DashboardDTO.EnCokSatanDTO.builder()
                        .stokAd((String) m.get("stokAd"))
                        .stokKodu((String) m.get("stokKodu"))
                        .satisMiktari((BigDecimal) m.get("satisMiktari"))
                        .build())
                .collect(Collectors.toList());

        var pozitifBakiye = cariHesapService.toplamPozitifBakiyeGetir();
        var negatifBakiye = cariHesapService.toplamNegatifBakiyeGetir();

        return DashboardDTO.builder()
                .toplamCariSayisi(toplamCariSayisi)
                .toplamBakiye(toplamBakiye)
                .sonHareketler(sonHareketler)
                .aktifCalisan(aktifCalisan)
                .bugunIzinli(bugunIzinli)
                .buAyIseBaslayacak(buAyIseBaslayacak)
                .bugunkuSiparis(bugunkuSiparis)
                .bekleyenTeslimat(bekleyenTeslimat)
                .iadeOrani(iadeOrani)
                .stokDevirHizi(stokDevirHizi)
                .enCokSatanlar(enCokSatanlar)
                .pozitifBakiye(pozitifBakiye)
                .negatifBakiye(negatifBakiye)
                .build();
    }

    public DashboardDTO dashboardFallback(Throwable t) {
        log.warn("Dashboard circuit breaker calisti: {}", t.getMessage());
        return DashboardDTO.builder()
                .toplamCariSayisi(0L).toplamBakiye(BigDecimal.ZERO)
                .sonHareketler(Collections.emptyList())
                .enCokSatanlar(Collections.emptyList())
                .build();
    }
}
