package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SirketHedefDTO;
import com.raspel.erp.dto.sistem.YoneticiKokpitDTO;
import com.raspel.erp.dto.sistem.YoneticiKokpitDTO.*;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.entity.sistem.SirketHedef;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.SirketHedefRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class YoneticiKokpitService {

    private final SirketHedefRepository sirketHedefRepository;
    private final FaturaRepository faturaRepository;
    private final MasrafRepository masrafRepository;
    private final CariHesapRepository cariHesapRepository;
    private final KasaRepository kasaRepository;
    private final BankaRepository bankaRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public YoneticiKokpitDTO getKokpitVerileri(Long sirketId, Integer yil, Integer ay) {
        LocalDate bugun = LocalDate.now();
        int aktifYil = (yil != null) ? yil : bugun.getYear();
        int aktifAy = (ay != null) ? ay : bugun.getMonthValue();

        LocalDate baslangic = LocalDate.of(aktifYil, aktifAy, 1);
        LocalDate bitis = baslangic.withDayOfMonth(baslangic.lengthOfMonth());
        int toplamGun = baslangic.lengthOfMonth();
        int gecenGun = (aktifYil == bugun.getYear() && aktifAy == bugun.getMonthValue()) ? bugun.getDayOfMonth() : toplamGun;
        int kalanGun = Math.max(0, toplamGun - gecenGun);

        // 1. Hedefleri Getir
        Optional<SirketHedef> hedefOpt = sirketHedefRepository.findBySirketIdAndYilAndAy(sirketId, aktifYil, aktifAy);
        BigDecimal hedefCiro = hedefOpt.map(SirketHedef::getHedefCiro).orElse(BigDecimal.ZERO);
        BigDecimal hedefKar = hedefOpt.map(SirketHedef::getHedefKar).orElse(BigDecimal.ZERO);
        int hedefYeniMusteri = hedefOpt.map(SirketHedef::getHedefYeniMusteri).orElse(0);
        int hedefSatisAdedi = hedefOpt.map(SirketHedef::getHedefSatisAdedi).orElse(0);

        // 2. Faturaları Getir ve Hesapla
        List<Fatura> donemFaturalari = (sirketId != null)
                ? faturaRepository.findBySirketIdAndTarihBetween(sirketId, baslangic, bitis)
                : Collections.emptyList();

        List<Fatura> satisFaturalari = donemFaturalari.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        List<Fatura> alisFaturalari = donemFaturalari.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.ALIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        BigDecimal gerceklesenCiro = satisFaturalari.stream()
                .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal toplamAlisMaliyeti = alisFaturalari.stream()
                .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Masrafları Getir
        List<Masraf> donemMasraflari = (sirketId != null)
                ? masrafRepository.findBySirketIdAndTarihBetween(sirketId, baslangic, bitis)
                : Collections.emptyList();

        BigDecimal toplamMasraflar = donemMasraflari.stream()
                .map(m -> m.getTutar() != null ? m.getTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Kâr & Marj Hesaplamaları
        BigDecimal gerceklesenKar = gerceklesenCiro.subtract(toplamAlisMaliyeti).subtract(toplamMasraflar);

        double ciroIlerlemeYuzdesi = (hedefCiro.compareTo(BigDecimal.ZERO) > 0)
                ? gerceklesenCiro.multiply(BigDecimal.valueOf(100)).divide(hedefCiro, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double karIlerlemeYuzdesi = (hedefKar.compareTo(BigDecimal.ZERO) > 0)
                ? gerceklesenKar.multiply(BigDecimal.valueOf(100)).divide(hedefKar, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double netKarMarji = (gerceklesenCiro.compareTo(BigDecimal.ZERO) > 0)
                ? gerceklesenKar.multiply(BigDecimal.valueOf(100)).divide(gerceklesenCiro, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        BigDecimal kalanCiro = hedefCiro.subtract(gerceklesenCiro);
        if (kalanCiro.compareTo(BigDecimal.ZERO) < 0) kalanCiro = BigDecimal.ZERO;

        BigDecimal gunlukOrtalamaCiro = (gecenGun > 0)
                ? gerceklesenCiro.divide(BigDecimal.valueOf(gecenGun), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. Likidite, Alacak ve Borç
        BigDecimal kasaToplam = (sirketId != null)
                ? kasaRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(k -> k.getBakiye() != null ? k.getBakiye() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        BigDecimal bankaToplam = (sirketId != null)
                ? bankaRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(b -> b.getBakiye() != null ? b.getBakiye() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        BigDecimal kasaBankaToplam = kasaToplam.add(bankaToplam);

        BigDecimal toplamAlacak = (sirketId != null) ? safeNull(cariHesapRepository.toplamPozitifBakiyeBySirketId(sirketId)) : BigDecimal.ZERO;
        BigDecimal toplamBorc = (sirketId != null) ? safeNull(cariHesapRepository.toplamNegatifBakiyeBySirketId(sirketId)).abs() : BigDecimal.ZERO;

        // 6. Top 5 Müşteri
        Map<CariHesap, List<Fatura>> cariGrup = satisFaturalari.stream()
                .filter(f -> f.getCariHesap() != null)
                .collect(Collectors.groupingBy(Fatura::getCariHesap));

        List<TopMusteriDTO> topMusteriler = cariGrup.entrySet().stream()
                .map(e -> {
                    BigDecimal cariCiro = e.getValue().stream()
                            .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return TopMusteriDTO.builder()
                            .cariId(e.getKey().getId())
                            .unvan(e.getKey().getAd())
                            .toplamCiro(cariCiro)
                            .faturaSayisi(e.getValue().size())
                            .build();
                })
                .sorted((a, b) -> b.getToplamCiro().compareTo(a.getToplamCiro()))
                .limit(5)
                .collect(Collectors.toList());

        // 7. Top 5 Karlı / Satan Ürün
        Map<Long, List<FaturaKalem>> stokGrup = satisFaturalari.stream()
                .filter(f -> f.getKalemler() != null)
                .flatMap(f -> f.getKalemler().stream())
                .filter(k -> k.getStokId() != null)
                .collect(Collectors.groupingBy(FaturaKalem::getStokId));

        List<TopUrunDTO> topUrunler = stokGrup.entrySet().stream()
                .map(e -> {
                    Long stokId = e.getKey();
                    List<FaturaKalem> kalemler = e.getValue();
                    Optional<Stok> stokOpt = stokRepository.findById(stokId);
                    String stokKodu = stokOpt.map(Stok::getStokKodu).orElse("STK-" + stokId);
                    String stokAdi = stokOpt.map(Stok::getAd).orElse(kalemler.get(0).getAciklama());

                    BigDecimal toplamMiktar = kalemler.stream()
                            .map(k -> k.getAdet() != null ? BigDecimal.valueOf(k.getAdet()) : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal toplamTutar = kalemler.stream()
                            .map(k -> k.getTutar() != null ? k.getTutar() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TopUrunDTO.builder()
                            .stokId(stokId)
                            .stokKodu(stokKodu)
                            .stokAdi(stokAdi)
                            .satisMiktari(toplamMiktar)
                            .toplamCiro(toplamTutar)
                            .build();
                })
                .sorted((a, b) -> b.getToplamCiro().compareTo(a.getToplamCiro()))
                .limit(5)
                .collect(Collectors.toList());

        // 8. Kritik Vadesi Geçen Alacaklar
        List<Fatura> vadesiGecenler = (sirketId != null)
                ? faturaRepository.findVadesiGecen(sirketId, Fatura.FaturaDurum.KESILDI, List.of("ODENDI", "IPTAL"), bugun)
                : Collections.emptyList();

        BigDecimal vadesiGecenAlacak = vadesiGecenler.stream()
                .map(f -> f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<KritikAlacakDTO> kritikAlacaklar = vadesiGecenler.stream()
                .filter(f -> f.getCariHesap() != null)
                .map(f -> {
                    long gecikme = (f.getVadeTarihi() != null) ? ChronoUnit.DAYS.between(f.getVadeTarihi(), bugun) : 0;
                    return KritikAlacakDTO.builder()
                            .cariId(f.getCariHesap().getId())
                            .unvan(f.getCariHesap().getAd())
                            .bakiye(f.getKalanTutar())
                            .telefon(f.getCariHesap().getTelefon())
                            .email(f.getCariHesap().getEmail())
                            .vadeTarihi(f.getVadeTarihi())
                            .gecikmeGunu(gecikme)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getGecikmeGunu(), a.getGecikmeGunu()))
                .limit(10)
                .collect(Collectors.toList());

        // 9. Günlük Ciro Trendi (1. günden son güne)
        Map<Integer, BigDecimal> gunlukCiroMap = satisFaturalari.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getTarih().getDayOfMonth(),
                        Collectors.reducing(BigDecimal.ZERO, f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO, BigDecimal::add)
                ));

        List<GunlukCiroDTO> gunlukTrend = new ArrayList<>();
        for (int gun = 1; gun <= toplamGun; gun++) {
            BigDecimal ciro = gunlukCiroMap.getOrDefault(gun, BigDecimal.ZERO);
            gunlukTrend.add(GunlukCiroDTO.builder()
                    .gun(gun)
                    .tarih(String.format("%02d.%02d", gun, aktifAy))
                    .ciro(ciro)
                    .build());
        }

        return YoneticiKokpitDTO.builder()
                .yil(aktifYil)
                .ay(aktifAy)
                .ayinGunu(gecenGun)
                .aydakiToplamGun(toplamGun)
                .kalanGun(kalanGun)
                .gerceklesenCiro(gerceklesenCiro)
                .hedefCiro(hedefCiro)
                .ciroIlerlemeYuzdesi(ciroIlerlemeYuzdesi)
                .kalanCiro(kalanCiro)
                .gunlukOrtalamaCiro(gunlukOrtalamaCiro)
                .toplamAlisMaliyeti(toplamAlisMaliyeti)
                .toplamMasraflar(toplamMasraflar)
                .gerceklesenKar(gerceklesenKar)
                .hedefKar(hedefKar)
                .karIlerlemeYuzdesi(karIlerlemeYuzdesi)
                .netKarMarji(netKarMarji)
                .toplamSatisAdedi(satisFaturalari.size())
                .hedefSatisAdedi(hedefSatisAdedi)
                .yeniMusteriSayisi(topMusteriler.size())
                .hedefYeniMusteri(hedefYeniMusteri)
                .kasaBankaToplam(kasaBankaToplam)
                .toplamAlacak(toplamAlacak)
                .vadesiGecenAlacak(vadesiGecenAlacak)
                .toplamBorc(toplamBorc)
                .topMusteriler(topMusteriler)
                .topUrunler(topUrunler)
                .kritikAlacaklar(kritikAlacaklar)
                .gunlukCiroTrendi(gunlukTrend)
                .build();
    }

    public SirketHedefDTO hedefKaydet(SirketHedefDTO dto, Long sirketId) {
        LocalDate bugun = LocalDate.now();
        int yil = (dto.getYil() != null) ? dto.getYil() : bugun.getYear();
        int ay = (dto.getAy() != null) ? dto.getAy() : bugun.getMonthValue();

        SirketHedef hedef = sirketHedefRepository.findBySirketIdAndYilAndAy(sirketId, yil, ay)
                .orElse(SirketHedef.builder()
                        .sirketId(sirketId)
                        .yil(yil)
                        .ay(ay)
                        .build());

        if (dto.getHedefCiro() != null) hedef.setHedefCiro(dto.getHedefCiro());
        if (dto.getHedefKar() != null) hedef.setHedefKar(dto.getHedefKar());
        if (dto.getHedefYeniMusteri() != null) hedef.setHedefYeniMusteri(dto.getHedefYeniMusteri());
        if (dto.getHedefSatisAdedi() != null) hedef.setHedefSatisAdedi(dto.getHedefSatisAdedi());
        if (dto.getNotlar() != null) hedef.setNotlar(dto.getNotlar());

        hedef = sirketHedefRepository.save(hedef);

        return SirketHedefDTO.builder()
                .id(hedef.getId())
                .sirketId(hedef.getSirketId())
                .yil(hedef.getYil())
                .ay(hedef.getAy())
                .hedefCiro(hedef.getHedefCiro())
                .hedefKar(hedef.getHedefKar())
                .hedefYeniMusteri(hedef.getHedefYeniMusteri())
                .hedefSatisAdedi(hedef.getHedefSatisAdedi())
                .notlar(hedef.getNotlar())
                .guncellemeTarihi(hedef.getGuncellemeTarihi())
                .build();
    }

    private BigDecimal safeNull(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
