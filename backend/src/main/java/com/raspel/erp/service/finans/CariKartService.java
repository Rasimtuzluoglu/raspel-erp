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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        Long tenantId = sirketId != null ? sirketId : cari.getSirketId();
        if (tenantId == null) {
            throw new ResourceNotFoundException("Cari Hesap", cariId);
        }

        // Ağır listeler yerine yalnızca son N kayıt; toplamlar SQL agregasyonu.
        List<Fatura> faturalar = faturaRepository
                .findByCariHesapIdAndSirketIdOrderByTarihDesc(cariId, tenantId,
                        org.springframework.data.domain.PageRequest.of(0, SON_KAYIT_LIMIT))
                .getContent();
        List<Siparis> siparisler = siparisRepository
                .findTop5ByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(cariId, tenantId);
        List<Iade> iadeler = iadeRepository
                .findTop5ByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(cariId, tenantId);
        List<Hareket> hareketler = hareketRepository
                .findTop5BySirketIdAndCariHesapIdOrderByHareketTarihiDescIdDesc(tenantId, cariId);

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
                .ozet(ozet(
                        faturaRepository.cariFaturaOzet(cariId, tenantId),
                        siparisRepository.cariSiparisOzet(cariId, tenantId),
                        iadeRepository.cariIadeOzet(cariId, tenantId),
                        hareketRepository.cariTahsilatToplam(cariId, tenantId)))
                .notSayisi(notRepository.countBySirketIdAndCariHesapId(tenantId, cariId))
                .firsatSayisi(cariFirsatRepository.countBySirketIdAndCariHesapId(tenantId, cariId))
                .ozelFiyatSayisi(cariFiyatRepository.countBySirketIdAndCariHesapId(tenantId, cariId))
                .taksitSayisi(taksitRepository.countBySirketIdAndCariHesapId(tenantId, cariId))
                .sonFaturalar(faturalar.stream().map(this::faturaOzet).collect(Collectors.toList()))
                .sonSiparisler(siparisler.stream().map(this::siparisOzet).collect(Collectors.toList()))
                .sonIadeler(iadeler.stream().map(this::iadeOzet).collect(Collectors.toList()))
                .notlar(List.of())
                .firsatlar(List.of())
                .ozelFiyatlar(List.of())
                .taksitler(List.of())
                .sonHareketler(hareketler.stream().map(this::hareketOzet).collect(Collectors.toList()))
                .build();
    }

    /** Cariyi tenant kontrolüyle yükler ve etkin şirket id'sini döndürür. */
    private Long cariTenantId(Long cariId, Long sirketId) {
        CariHesap cari = cariHesapRepository.findById(cariId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        Long tenantId = sirketId != null ? sirketId : cari.getSirketId();
        if (tenantId == null) {
            throw new ResourceNotFoundException("Cari Hesap", cariId);
        }
        return tenantId;
    }

    /** Sekme sayfalarında aşırı büyük istekleri engeller. */
    private org.springframework.data.domain.Pageable sayfaSinirla(
            org.springframework.data.domain.Pageable pageable) {
        int sayfa = pageable != null ? pageable.getPageNumber() : 0;
        int boyut = pageable != null ? pageable.getPageSize() : 10;
        boyut = Math.min(Math.max(boyut, 1), 50);
        org.springframework.data.domain.Sort sirala = pageable != null
                ? pageable.getSort() : org.springframework.data.domain.Sort.unsorted();
        return org.springframework.data.domain.PageRequest.of(sayfa, boyut, sirala);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CariKartDTO.NotOzet> notlar(
            Long cariId, Long sirketId, org.springframework.data.domain.Pageable pageable) {
        Long tenantId = cariTenantId(cariId, sirketId);
        return notRepository
                .findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(
                        tenantId, cariId, sayfaSinirla(pageable))
                .map(this::notOzet);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CariKartDTO.FirsatOzet> firsatlar(
            Long cariId, Long sirketId, org.springframework.data.domain.Pageable pageable) {
        Long tenantId = cariTenantId(cariId, sirketId);
        return cariFirsatRepository
                .findBySirketIdAndCariHesapIdOrderByOlusturmaTarihiDesc(
                        tenantId, cariId, sayfaSinirla(pageable))
                .map(this::firsatOzet);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CariKartDTO.FiyatOzet> ozelFiyatlar(
            Long cariId, Long sirketId, org.springframework.data.domain.Pageable pageable) {
        Long tenantId = cariTenantId(cariId, sirketId);
        org.springframework.data.domain.Page<CariFiyat> sayfa = cariFiyatRepository
                .findBySirketIdAndCariHesapIdOrderByStokId(tenantId, cariId, sayfaSinirla(pageable));
        Map<Long, Stok> stokMap = stokMapHazirla(sayfa.getContent());
        return sayfa.map(f -> fiyatOzet(f, stokMap.get(f.getStokId())));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CariKartDTO.TaksitOzet> taksitler(
            Long cariId, Long sirketId, org.springframework.data.domain.Pageable pageable) {
        Long tenantId = cariTenantId(cariId, sirketId);
        return taksitRepository
                .findBySirketIdAndCariHesapIdOrderByVadeTarihiAsc(
                        tenantId, cariId, sayfaSinirla(pageable))
                .map(this::taksitOzet);
    }

    private CariKartDTO.HareketOzet hareketOzet(Hareket h) {
        return CariKartDTO.HareketOzet.builder()
                .id(h.getId())
                .tur(h.getTur() != null ? h.getTur().name() : null)
                .tarih(h.getHareketTarihi())
                .tutar(h.getTutar())
                .aciklama(h.getAciklama())
                .faturaId(h.getFaturaId())
                .odemeYontemi(h.getOdemeYontemi())
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
        // Isaret kurali (kod genelinde tutarli): negatif bakiye = musteri bize borclu,
        // pozitif bakiye = musteri alacakli. Bu nedenle borc yalnizca negatif bakiyeden olusur;
        // alacakli (pozitif) cari riskli/limit-asimi sayilmaz.
        BigDecimal borc = bakiye.signum() < 0 ? bakiye.negate() : BigDecimal.ZERO;
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

    private CariKartDTO.Ozet ozet(com.raspel.erp.dto.finans.CariFaturaOzet faturaOzet,
                                   com.raspel.erp.dto.finans.CariTutarOzet siparisOzet,
                                   com.raspel.erp.dto.finans.CariTutarOzet iadeOzet,
                                   BigDecimal tahsilatToplam) {
        long faturaSayisi = faturaOzet != null && faturaOzet.adet() != null ? faturaOzet.adet() : 0L;
        BigDecimal faturaToplam = faturaOzet != null && faturaOzet.tutar() != null
                ? faturaOzet.tutar() : BigDecimal.ZERO;
        BigDecimal kalanTutar = faturaOzet != null && faturaOzet.kalan() != null
                ? faturaOzet.kalan() : BigDecimal.ZERO;
        long siparisSayisi = siparisOzet != null && siparisOzet.adet() != null ? siparisOzet.adet() : 0L;
        BigDecimal siparisToplam = siparisOzet != null && siparisOzet.tutar() != null
                ? siparisOzet.tutar() : BigDecimal.ZERO;
        long iadeSayisi = iadeOzet != null && iadeOzet.adet() != null ? iadeOzet.adet() : 0L;
        BigDecimal iadeToplam = iadeOzet != null && iadeOzet.tutar() != null
                ? iadeOzet.tutar() : BigDecimal.ZERO;

        return CariKartDTO.Ozet.builder()
                .siparisSayisi(siparisSayisi)
                .siparisToplam(siparisToplam)
                .faturaSayisi(faturaSayisi)
                .faturaToplam(faturaToplam)
                .kalanTutar(kalanTutar)
                .iadeSayisi(iadeSayisi)
                .iadeToplam(iadeToplam)
                .tahsilatToplam(tahsilatToplam != null ? tahsilatToplam : BigDecimal.ZERO)
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
