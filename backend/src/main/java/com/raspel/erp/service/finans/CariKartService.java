package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CariKartDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariFiyat;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.Taksit;
import com.raspel.erp.entity.ticaret.CariFirsat;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariFiyatRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.TaksitRepository;
import com.raspel.erp.repository.sistem.NotRepository;
import com.raspel.erp.repository.ticaret.CariFirsatRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CariKartService {

    private final CariHesapRepository cariHesapRepository;
    private final FaturaRepository faturaRepository;
    private final SiparisRepository siparisRepository;
    private final IadeRepository iadeRepository;
    private final NotRepository notRepository;
    private final CariFirsatRepository cariFirsatRepository;
    private final CariFiyatRepository cariFiyatRepository;
    private final HareketRepository hareketRepository;
    private final StokRepository stokRepository;
    private final TaksitRepository taksitRepository;
    private final TenantChecker tenantChecker;

    private static final int SON_KAYIT_LIMIT = 5;

    @Transactional(readOnly = true)
    public CariKartDTO kartGetir(Long cariId, Long sirketId) {
        CariHesap cari = cariHesapRepository.findById(cariId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");

        List<Fatura> faturalar = faturaRepository
                .findByCariHesapIdAndSirketIdOrderByTarihDesc(cariId, sirketId, Pageable.unpaged())
                .getContent();
        List<Siparis> siparisler = siparisRepository.findByCariHesapId(cariId).stream()
                .filter(s -> sirketId == null || sirketId.equals(s.getSirketId()))
                .collect(Collectors.toList());
        List<Long> faturaIds = faturalar.stream().map(Fatura::getId).collect(Collectors.toList());
        List<Iade> iadeler = faturaIds.isEmpty() ? List.of()
                : iadeRepository.findByFaturaIdInAndSirketId(faturaIds, sirketId);
        List<Not> notlar = notRepository.findByCariHesapIdOrderByOlusturmaTarihiDesc(cariId);
        List<CariFirsat> firsatlar = cariFirsatRepository
                .findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(sirketId, cariId);
        List<CariFiyat> fiyatlar = cariFiyatRepository.findByCariHesapIdOrderByStokId(cariId);
        List<Hareket> hareketler = hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(cariId);
        List<Taksit> taksitler = taksitRepository.findBySirketIdAndCariHesapIdOrderByVadeTarihiAsc(sirketId, cariId);

        Map<Long, Stok> stokMap = stokMapHazirla(fiyatlar);

        return CariKartDTO.builder()
                .cariId(cari.getId())
                .cariAd(cari.getAd())
                .telefon(cari.getTelefon())
                .email(cari.getEmail())
                .tur(cari.getTur())
                .aktif(cari.getAktif())
                .temsilciId(cari.getTemsilciId())
                .temsilciAd(cari.getTemsilciAd())
                .kredi(krediDurumu(cari))
                .ozet(ozet(faturalar, siparisler, iadeler, hareketler))
                .sonFaturalar(faturalar.stream().limit(SON_KAYIT_LIMIT).map(this::faturaOzet).collect(Collectors.toList()))
                .sonSiparisler(siparisler.stream()
                        .sorted(Comparator.comparing(Siparis::getTarih, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(SON_KAYIT_LIMIT).map(this::siparisOzet).collect(Collectors.toList()))
                .sonIadeler(iadeler.stream()
                        .sorted(Comparator.comparing(Iade::getTarih, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(SON_KAYIT_LIMIT).map(this::iadeOzet).collect(Collectors.toList()))
                .firsatlar(firsatlar.stream().map(this::firsatOzet).collect(Collectors.toList()))
                .notlar(notlar.stream().map(this::notOzet).collect(Collectors.toList()))
                .ozelFiyatlar(fiyatlar.stream().map(f -> fiyatOzet(f, stokMap.get(f.getStokId()))).collect(Collectors.toList()))
                .taksitler(taksitler.stream().map(this::taksitOzet).collect(Collectors.toList()))
                .build();
    }

    private Map<Long, Stok> stokMapHazirla(List<CariFiyat> fiyatlar) {
        List<Long> stokIds = fiyatlar.stream().map(CariFiyat::getStokId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (stokIds.isEmpty()) return Map.of();
        return stokRepository.findAllById(stokIds).stream()
                .collect(Collectors.toMap(Stok::getId, s -> s));
    }

    private CariKartDTO.KrediDurumu krediDurumu(CariHesap cari) {
        BigDecimal limit = cari.getKrediLimiti();
        BigDecimal bakiye = cari.getBakiye() != null ? cari.getBakiye() : BigDecimal.ZERO;
        BigDecimal borc = bakiye.max(BigDecimal.ZERO);
        BigDecimal kullanilabilir = limit != null ? limit.subtract(borc) : null;
        BigDecimal risk = (limit != null && limit.signum() > 0)
                ? borc.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP)
                : null;
        boolean limitAsimi = limit != null && borc.compareTo(limit) > 0;
        return CariKartDTO.KrediDurumu.builder()
                .krediLimiti(limit)
                .bakiye(bakiye)
                .kullanilabilirKredi(kullanilabilir)
                .riskOrani(risk)
                .limitAsimi(limitAsimi)
                .build();
    }

    private CariKartDTO.Ozet ozet(List<Fatura> faturalar, List<Siparis> siparisler,
                                   List<Iade> iadeler, List<Hareket> hareketler) {
        List<Fatura> gecerliFaturalar = faturalar.stream()
                .filter(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL).collect(Collectors.toList());
        List<Iade> gecerliIadeler = iadeler.stream()
                .filter(i -> !"IPTAL".equalsIgnoreCase(i.getDurum())).collect(Collectors.toList());

        BigDecimal faturaToplam = gecerliFaturalar.stream()
                .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal kalanTutar = gecerliFaturalar.stream()
                .map(f -> f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal siparisToplam = siparisler.stream()
                .map(s -> s.getGenelToplam() != null ? s.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iadeToplam = gecerliIadeler.stream()
                .map(i -> i.getTutar() != null ? i.getTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tahsilatToplam = hareketler.stream()
                .filter(h -> h.getTur() == Hareket.HareketTuru.TAHSILAT)
                .map(h -> h.getTutar() != null ? h.getTutar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CariKartDTO.Ozet.builder()
                .siparisSayisi(siparisler.size())
                .siparisToplam(siparisToplam)
                .faturaSayisi(gecerliFaturalar.size())
                .faturaToplam(faturaToplam)
                .kalanTutar(kalanTutar)
                .iadeSayisi(gecerliIadeler.size())
                .iadeToplam(iadeToplam)
                .tahsilatToplam(tahsilatToplam)
                .build();
    }

    private CariKartDTO.FaturaOzet faturaOzet(Fatura f) {
        return CariKartDTO.FaturaOzet.builder()
                .id(f.getId())
                .faturaNumarasi(f.getFaturaNumarasi())
                .tarih(f.getTarih())
                .tur(f.getTur() != null ? f.getTur().name() : null)
                .durum(f.getDurum() != null ? f.getDurum().name() : null)
                .odemeDurumu(f.getOdemeDurumu())
                .genelToplam(f.getGenelToplam())
                .kalanTutar(f.getKalanTutar())
                .build();
    }

    private CariKartDTO.SiparisOzet siparisOzet(Siparis s) {
        return CariKartDTO.SiparisOzet.builder()
                .id(s.getId())
                .siparisNo(s.getSiparisNo())
                .tarih(s.getTarih())
                .durum(s.getDurum())
                .genelToplam(s.getGenelToplam())
                .build();
    }

    private CariKartDTO.IadeOzet iadeOzet(Iade i) {
        return CariKartDTO.IadeOzet.builder()
                .id(i.getId())
                .tur(i.getTur())
                .tarih(i.getTarih())
                .tutar(i.getTutar())
                .durum(i.getDurum())
                .build();
    }

    private CariKartDTO.FirsatOzet firsatOzet(CariFirsat f) {
        return CariKartDTO.FirsatOzet.builder()
                .id(f.getId())
                .ad(f.getAd())
                .durum(f.getDurum())
                .kaynak(f.getKaynak())
                .deger(f.getDeger())
                .tahminiKapanis(f.getTahminiKapanis())
                .build();
    }

    private CariKartDTO.NotOzet notOzet(Not n) {
        return CariKartDTO.NotOzet.builder()
                .id(n.getId())
                .baslik(n.getBaslik())
                .icerik(n.getIcerik())
                .onemDerecesi(n.getOnemDerecesi())
                .olusturmaTarihi(n.getOlusturmaTarihi())
                .build();
    }

    private CariKartDTO.FiyatOzet fiyatOzet(CariFiyat f, Stok s) {
        return CariKartDTO.FiyatOzet.builder()
                .id(f.getId())
                .stokId(f.getStokId())
                .stokAd(s != null ? s.getAd() : null)
                .stokKodu(s != null ? s.getStokKodu() : null)
                .fiyat(f.getFiyat())
                .build();
    }

    private CariKartDTO.TaksitOzet taksitOzet(Taksit t) {
        long gecikme = 0;
        if (!"ODENDI".equals(t.getOdemeDurumu()) && t.getVadeTarihi() != null
                && t.getVadeTarihi().isBefore(java.time.LocalDate.now())) {
            gecikme = java.time.temporal.ChronoUnit.DAYS.between(t.getVadeTarihi(), java.time.LocalDate.now());
        }
        return CariKartDTO.TaksitOzet.builder()
                .id(t.getId())
                .planNo(t.getPlanNo())
                .taksitNo(t.getTaksitNo())
                .taksitSayisi(t.getTaksitSayisi())
                .kurum(t.getKurum())
                .vadeTarihi(t.getVadeTarihi())
                .tutar(t.getTutar())
                .odemeDurumu(t.getOdemeDurumu())
                .gecikmeGunu(gecikme)
                .build();
    }
}
