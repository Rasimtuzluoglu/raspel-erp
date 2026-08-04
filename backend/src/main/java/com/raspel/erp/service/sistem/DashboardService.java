package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.Hareket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CariHesapService cariHesapService;
    private final HareketService hareketService;
    private final HareketRepository hareketRepository;
    private final SiparisRepository siparisRepository;
    private final PersonelRepository personelRepository;
    private final PersonelIzinRepository personelIzinRepository;
    private final StokHareketRepository stokHareketRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'dashboard:' + #sirketId")
    public DashboardDTO dashboardVerileriGetir(Long sirketId) {
        log.debug("Dashboard verileri getiriliyor... sirketId: {}", sirketId);

        Long toplamCariSayisi = safeGet(() -> cariHesapService.toplamCariSayisiGetir(sirketId), 0L);
        BigDecimal toplamBakiye = safeGet(() -> cariHesapService.toplamBakiyeGetir(sirketId), BigDecimal.ZERO);
        List<HareketDTO> sonHareketler = safeGetList(() -> hareketService.sonHareketleriGetir(5), Collections.emptyList());

        Long aktifCalisan = safeGet(() -> personelRepository.countByAktifTrue(), 0L);
        Long bugunIzinli = safeGet(() -> personelIzinRepository.countBugunIzinli(LocalDate.now()), 0L);
        var ayBaslangic = LocalDate.now().withDayOfMonth(1);
        var ayBitis = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Long buAyIseBaslayacak = safeGet(() -> personelRepository.countByIseGirisTarihiBetween(ayBaslangic, ayBitis), 0L);

        Long bugunkuSiparis = safeGet(() -> siparisRepository.countByTarih(LocalDate.now()), 0L);
        Long bekleyenTeslimat = safeGet(() -> siparisRepository.countByDurumNot("TAMAMLANDI"), 0L);
        BigDecimal iadeOrani = BigDecimal.ZERO;

        long toplamStok = safeGet(() -> stokRepository.count(), 0L);
        long toplamCikis = safeGet(() -> stokHareketRepository.countByTur("CIKIS"), 0L);
        BigDecimal stokDevirHizi = toplamStok > 0
                ? BigDecimal.valueOf(toplamCikis).divide(BigDecimal.valueOf(toplamStok), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardDTO.EnCokSatanDTO> enCokSatanlar = safeGetList(() -> stokHareketRepository.enCokSatanlar(), Collections.emptyList())
                .stream().limit(5)
                .map(m -> DashboardDTO.EnCokSatanDTO.builder()
                        .stokAd((String) m.get("stokAd"))
                        .stokKodu((String) m.get("stokKodu"))
                        .satisMiktari((BigDecimal) m.get("satisMiktari"))
                        .build())
                .collect(Collectors.toList());

        BigDecimal pozitifBakiye = safeGet(() -> cariHesapService.toplamPozitifBakiyeGetir(sirketId), BigDecimal.ZERO);
        BigDecimal negatifBakiye = safeGet(() -> cariHesapService.toplamNegatifBakiyeGetir(sirketId), BigDecimal.ZERO);

        BigDecimal bugunkuTahsilat = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.TAHSILAT, LocalDate.now()), BigDecimal.ZERO);
        BigDecimal bugunkuOdeme = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.ODEME, LocalDate.now()), BigDecimal.ZERO);
        Long bekleyenIzinSayisi = safeGet(() -> personelIzinRepository.countByDurum("BEKLEMEDE"), 0L);

        var altiAyOnce = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        var aylikGelirGider = safeGetList(() -> hareketRepository.aylikGelirGider(altiAyOnce), Collections.emptyList())
                .stream().map(row -> DashboardDTO.AylikGelirGiderDTO.builder()
                        .ay((String) row[0])
                        .gelir((BigDecimal) row[1])
                        .gider((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

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
                .bugunkuTahsilat(bugunkuTahsilat)
                .bugunkuOdeme(bugunkuOdeme)
                .bekleyenIzinSayisi(bekleyenIzinSayisi)
                .aylikGelirGider(aylikGelirGider)
                .build();
    }

    private <T> T safeGet(SafeSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Dashboard verisi alinirken hata: {}", e.getMessage(), e);
            return defaultValue;
        }
    }

    private <T> List<T> safeGetList(SafeSupplier<List<T>> supplier, List<T> defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Dashboard listesi alinirken hata: {}", e.getMessage(), e);
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface SafeSupplier<T> {
        T get();
    }
}