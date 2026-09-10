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
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.SirketHedefRepository;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.entity.sistem.SirketHedef;

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
    private final FaturaRepository faturaRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final BankaRepository bankaRepository;
    private final KasaRepository kasaRepository;
    private final MasrafRepository masrafRepository;
    private final SirketHedefRepository sirketHedefRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'dashboard:' + #sirketId")
    public DashboardDTO dashboardVerileriGetir(Long sirketId) {
        log.debug("Dashboard verileri getiriliyor... sirketId: {}", sirketId);

        Long toplamCariSayisi = safeGet(() -> cariHesapService.toplamCariSayisiGetir(sirketId), 0L);
        BigDecimal toplamBakiye = safeGet(() -> cariHesapService.toplamBakiyeGetir(sirketId), BigDecimal.ZERO);
        List<HareketDTO> sonHareketler = safeGetList(() -> hareketService.sonHareketleriGetir(5, sirketId), Collections.emptyList());

        Long aktifCalisan = safeGet(() -> personelRepository.countByAktifTrueAndSirketId(sirketId), 0L);
        Long bugunIzinli = safeGet(() -> personelIzinRepository.countBugunIzinliAndSirketId(LocalDate.now(), sirketId), 0L);
        var ayBaslangic = LocalDate.now().withDayOfMonth(1);
        var ayBitis = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Long buAyIseBaslayacak = safeGet(() -> personelRepository.countBySirketIdAndIseGirisTarihiBetween(sirketId, ayBaslangic, ayBitis), 0L);

        Long bugunkuSiparis = safeGet(() -> siparisRepository.countBySirketIdAndTarih(sirketId, LocalDate.now()), 0L);
        Long bekleyenTeslimat = safeGet(() -> siparisRepository.countBySirketIdAndDurumNot(sirketId, "TAMAMLANDI"), 0L);
        BigDecimal iadeOrani = BigDecimal.ZERO;

        long toplamStok = safeGet(() -> stokRepository.countBySirketId(sirketId), 0L);
        long toplamCikis = safeGet(() -> stokHareketRepository.countByStokSirketIdAndTur(sirketId, "CIKIS"), 0L);
        BigDecimal stokDevirHizi = toplamStok > 0
                ? BigDecimal.valueOf(toplamCikis).divide(BigDecimal.valueOf(toplamStok), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardDTO.EnCokSatanDTO> enCokSatanlar = safeGetList(() -> stokHareketRepository.enCokSatanlarBySirket(sirketId), Collections.emptyList())
                .stream().limit(5)
                .map(m -> DashboardDTO.EnCokSatanDTO.builder()
                        .stokAd((String) m.get("stokAd"))
                        .stokKodu((String) m.get("stokKodu"))
                        .satisMiktari((BigDecimal) m.get("satisMiktari"))
                        .build())
                .collect(Collectors.toList());

        BigDecimal pozitifBakiye = safeGet(() -> cariHesapService.toplamPozitifBakiyeGetir(sirketId), BigDecimal.ZERO);
        BigDecimal negatifBakiye = safeGet(() -> cariHesapService.toplamNegatifBakiyeGetir(sirketId), BigDecimal.ZERO);

        BigDecimal bugunkuTahsilat = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.TAHSILAT, LocalDate.now(), sirketId), BigDecimal.ZERO);
        BigDecimal bugunkuOdeme = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.ODEME, LocalDate.now(), sirketId), BigDecimal.ZERO);
        Long bekleyenIzinSayisi = safeGet(() -> personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId), 0L);

        var onIkiAyOnce = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        var aylikGelirGider = safeGetList(() -> hareketRepository.aylikGelirGider(onIkiAyOnce, sirketId), Collections.emptyList())
                .stream().map(row -> DashboardDTO.AylikGelirGiderDTO.builder()
                        .ay((String) row[0])
                        .gelir((BigDecimal) row[1])
                        .gider((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        var yediGunOnce = LocalDate.now().minusDays(6);
        var gunlukNakitAkisi = safeGetList(() -> hareketRepository.gunlukNakitAkisi(yediGunOnce, sirketId), Collections.emptyList())
                .stream().map(row -> DashboardDTO.GunlukNakitAkisiDTO.builder()
                        .gun((String) row[0])
                        .gelir((BigDecimal) row[1])
                        .gider((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        var odemeDurumlari = List.of("ODENDI", "IPTAL");
        var bugun = LocalDate.now();
        List<DashboardDTO.VadeBildirimiDTO> vadesiGecenFaturalar = safeGetList(
                () -> faturaRepository.findVadesiGecen(sirketId, Fatura.FaturaDurum.KESILDI, odemeDurumlari, bugun)
                        .stream().map(this::vadeDTOyaCevir).collect(Collectors.toList()),
                Collections.emptyList());
        List<DashboardDTO.VadeBildirimiDTO> vadesiYaklasanFaturalar = safeGetList(
                () -> faturaRepository.findVadesiYaklasan(sirketId, Fatura.FaturaDurum.KESILDI, odemeDurumlari,
                                bugun, bugun.plusDays(7))
                        .stream().map(this::vadeDTOyaCevir).collect(Collectors.toList()),
                Collections.emptyList());

        Long toplamFatura = safeGet(() -> faturaRepository.countBySirketId(sirketId), 0L);
        Long kesilenFatura = safeGet(() -> faturaRepository.countBySirketIdAndDurum(sirketId, Fatura.FaturaDurum.KESILDI), 0L);
        BigDecimal toplamBankaBakiye = safeGet(() -> bankaRepository.sumBakiyeBySirketId(sirketId), BigDecimal.ZERO);
        BigDecimal toplamKasaBakiye = safeGet(() -> kasaRepository.sumBakiyeBySirketId(sirketId), BigDecimal.ZERO);
        Long kritikStokSayisi = safeGet(() -> stokRepository.countKritikStokBySirketId(sirketId), 0L);

        List<DashboardDTO.KritikStokDTO> kritikStoklar = safeGetList(
                () -> stokRepository.kritikStoklar(sirketId).stream()
                        .limit(6)
                        .map(s -> DashboardDTO.KritikStokDTO.builder()
                                .id(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd())
                                .miktar(s.getMiktar()).birim(s.getBirim()).minMiktar(s.getMinMiktar()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());

        // Aylık hedef ve gerçekleşen ciro
        LocalDate ayBas = LocalDate.now().withDayOfMonth(1);
        LocalDate aySon = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Optional<SirketHedef> hedefOpt = safeGet(
                () -> sirketHedefRepository.findBySirketIdAndYilAndAy(sirketId, LocalDate.now().getYear(), LocalDate.now().getMonthValue()),
                Optional.empty());
        BigDecimal hedefCiro = hedefOpt.map(SirketHedef::getHedefCiro).orElse(BigDecimal.ZERO);
        BigDecimal hedefKar = hedefOpt.map(SirketHedef::getHedefKar).orElse(BigDecimal.ZERO);

        BigDecimal gerceklesenCiro = safeGet(() -> {
            return faturaRepository.findBySirketIdAndTarihBetween(sirketId, ayBas, aySon).stream()
                    .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }, BigDecimal.ZERO);

        BigDecimal toplamAlisMaliyeti = safeGet(() -> {
            return faturaRepository.findBySirketIdAndTarihBetween(sirketId, ayBas, aySon).stream()
                    .filter(f -> f.getTur() == Fatura.FaturaTur.ALIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                    .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }, BigDecimal.ZERO);

        BigDecimal toplamMasraflar = safeGet(() -> {
            return masrafRepository.findBySirketIdAndTarihBetween(sirketId, ayBas, aySon).stream()
                    .map(Masraf::getTutar).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }, BigDecimal.ZERO);

        BigDecimal gerceklesenKar = gerceklesenCiro.subtract(toplamAlisMaliyeti).subtract(toplamMasraflar);
        BigDecimal ciroIlerlemeYuzdesi = hedefCiro.compareTo(BigDecimal.ZERO) > 0
                ? gerceklesenCiro.multiply(BigDecimal.valueOf(100)).divide(hedefCiro, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // En çok borçlu ve alacaklı cariler
        List<DashboardDTO.CariOzetDTO> enCokBorcCariler = safeGetList(
                () -> cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                        .filter(c -> c.getBakiye() != null && c.getBakiye().compareTo(BigDecimal.ZERO) < 0)
                        .sorted((a, b) -> a.getBakiye().compareTo(b.getBakiye()))
                        .limit(5)
                        .map(c -> DashboardDTO.CariOzetDTO.builder().cariAd(c.getAd()).tutar(c.getBakiye().abs()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());

        List<DashboardDTO.CariOzetDTO> enCokAlacakCariler = safeGetList(
                () -> cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                        .filter(c -> c.getBakiye() != null && c.getBakiye().compareTo(BigDecimal.ZERO) > 0)
                        .sorted((a, b) -> b.getBakiye().compareTo(a.getBakiye()))
                        .limit(5)
                        .map(c -> DashboardDTO.CariOzetDTO.builder().cariAd(c.getAd()).tutar(c.getBakiye()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());

        List<DashboardDTO.KategoriSatisDTO> kategoriSatislari = safeGetList(
                () -> faturaKalemRepository.kategoriSatislari(sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI)
                        .stream()
                        .map(row -> DashboardDTO.KategoriSatisDTO.builder()
                                .kategori((String) row[0])
                                .tutar((BigDecimal) row[1])
                                .build())
                        .collect(Collectors.toList()),
                Collections.emptyList());

        List<DashboardDTO.AlacakYasDTO> alacakYaslandirma = alacakYaslandirmaHesapla(sirketId);

        BigDecimal toplamStokDegeri = safeGet(
                () -> stokRepository.findBySirketIdOrderByAd(sirketId).stream()
                        .map(s -> (s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO)
                                .multiply(s.getFiyat() != null ? s.getFiyat() : BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO);

        String ozet = ozetOlustur(gerceklesenCiro, kritikStokSayisi, vadesiGecenFaturalar, enCokBorcCariler);

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
                .toplamStok(toplamStok)
                .kritikStokSayisi(kritikStokSayisi)
                .kritikStoklar(kritikStoklar)
                .toplamFatura(toplamFatura)
                .kesilenFatura(kesilenFatura)
                .toplamBankaBakiye(toplamBankaBakiye)
                .toplamKasaBakiye(toplamKasaBakiye)
                .hedefCiro(hedefCiro)
                .gerceklesenCiro(gerceklesenCiro)
                .ciroIlerlemeYuzdesi(ciroIlerlemeYuzdesi)
                .hedefKar(hedefKar)
                .gerceklesenKar(gerceklesenKar)
                .stokDevirHizi(stokDevirHizi)
                .enCokSatanlar(enCokSatanlar)
                .pozitifBakiye(pozitifBakiye)
                .negatifBakiye(negatifBakiye)
                .bugunkuTahsilat(bugunkuTahsilat)
                .bugunkuOdeme(bugunkuOdeme)
                .bekleyenIzinSayisi(bekleyenIzinSayisi)
                .aylikGelirGider(aylikGelirGider)
                .gunlukNakitAkisi(gunlukNakitAkisi)
                .vadesiGecenFaturalar(vadesiGecenFaturalar)
                .vadesiYaklasanFaturalar(vadesiYaklasanFaturalar)
                .enCokBorcCariler(enCokBorcCariler)
                .enCokAlacakCariler(enCokAlacakCariler)
                .kategoriSatislari(kategoriSatislari)
                .alacakYaslandirma(alacakYaslandirma)
                .toplamStokDegeri(toplamStokDegeri)
                .ozet(ozet)
                .build();
    }

    private List<DashboardDTO.AlacakYasDTO> alacakYaslandirmaHesapla(Long sirketId) {
        List<String> odemeDurumlari = List.of("ODENDI", "IPTAL");
        LocalDate bugun = LocalDate.now();
        BigDecimal vadesiGecen = safeGet(
                () -> faturaRepository.findVadesiGecen(sirketId, Fatura.FaturaDurum.KESILDI, odemeDurumlari, bugun).stream()
                        .map(f -> f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO);
        return List.of(
                DashboardDTO.AlacakYasDTO.builder().aralik("Vadesi Geçti").tutar(vadesiGecen).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("0-30 Gün")
                        .tutar(kalanTopla(sirketId, odemeDurumlari, bugun, bugun.plusDays(30))).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("31-60 Gün")
                        .tutar(kalanTopla(sirketId, odemeDurumlari, bugun.plusDays(31), bugun.plusDays(60))).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("60+ Gün")
                        .tutar(kalanTopla(sirketId, odemeDurumlari, bugun.plusDays(61), bugun.plusDays(3650))).build());
    }

    private BigDecimal kalanTopla(Long sirketId, List<String> odemeDurumlari, LocalDate bas, LocalDate bit) {
        return safeGet(
                () -> faturaRepository.findVadesiYaklasan(sirketId, Fatura.FaturaDurum.KESILDI, odemeDurumlari, bas, bit).stream()
                        .map(f -> f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.ZERO);
    }

    private String ozetOlustur(BigDecimal ciro, Long kritikStok, List<DashboardDTO.VadeBildirimiDTO> vadesiGecen,
                               List<DashboardDTO.CariOzetDTO> enCokBorc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bu ay kesilen satış cirosu ").append(ciro != null ? ciro.toPlainString() : "0").append(" TL.");
        if (kritikStok != null && kritikStok > 0) {
            sb.append(" ").append(kritikStok).append(" ürün kritik stok seviyesinde; sipariş planlaması önerilir.");
        }
        if (vadesiGecen != null && !vadesiGecen.isEmpty()) {
            sb.append(" ").append(vadesiGecen.size()).append(" faturanın vadesi geçmiş durumda.");
        }
        if (enCokBorc != null && !enCokBorc.isEmpty()) {
            sb.append(" En yüksek borçlu cari: ").append(enCokBorc.get(0).getCariAd()).append(".");
        }
        return sb.toString();
    }

    private DashboardDTO.VadeBildirimiDTO vadeDTOyaCevir(Fatura f) {
        return DashboardDTO.VadeBildirimiDTO.builder()
                .faturaId(f.getId())
                .faturaNumarasi(f.getFaturaNumarasi())
                .cariHesapAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                .cariTelefon(f.getCariHesap() != null ? f.getCariHesap().getTelefon() : null)
                .vadeTarihi(f.getVadeTarihi())
                .kalanTutar(f.getKalanTutar())
                .build();
    }

    private <T> T safeGet(SafeSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Dashboard verisi alinamadi (varsayilan kullanilacak): {}", e.getMessage());
            return defaultValue;
        }
    }

    private <T> List<T> safeGetList(SafeSupplier<List<T>> supplier, List<T> defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Dashboard listesi alinamadi (varsayilan kullanilacak): {}", e.getMessage());
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface SafeSupplier<T> {
        T get();
    }
}