package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.*;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Teklif;
import com.raspel.erp.entity.ticaret.TeklifKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.TeklifKalemRepository;
import com.raspel.erp.repository.ticaret.TeklifRepository;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.SeriNoServisi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TeklifService {

    private final TeklifRepository teklifRepository;
    private final TeklifKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final SiparisService siparisService;
    private final FaturaService faturaService;
    private final SeriNoServisi seriNoServisi;
    private final BildirimService bildirimService;
        private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<TeklifDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<Teklif> sayfa = teklifRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable);
        List<Teklif> teklifler = sayfa.getContent();
        if (teklifler.isEmpty()) return sayfa.map(this::entityToDTO);

        // N+1 onlemi: kalem, stok ve cari verilerini toplu sorgularla getir.
        List<Long> teklifIdler = teklifler.stream().map(Teklif::getId).collect(Collectors.toList());
        Map<Long, List<TeklifKalem>> kalemMap = kalemRepository.findByTeklifIdIn(teklifIdler).stream()
                .collect(Collectors.groupingBy(TeklifKalem::getTeklifId));

        Map<Long, Stok> stokMap = stokMapOlustur(kalemMap.values().stream()
                .flatMap(List::stream).map(TeklifKalem::getStokId).collect(Collectors.toSet()));
        Map<Long, CariHesap> cariMap = cariMapOlustur(teklifler.stream()
                .map(Teklif::getCariHesapId).collect(Collectors.toSet()));

        return sayfa.map(t -> entityToDTO(t,
                kalemMap.getOrDefault(t.getId(), List.of()), stokMap, cariMap));
    }

    @Transactional(readOnly = true)
    public TeklifDTO getir(Long id) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");
        return entityToDTO(t);
    }

    public TeklifDTO olustur(TeklifDTO dto, Long sirketId) {
        if (dto.getCariHesapId() == null) {
            throw new BusinessException("Cari hesap seçilmelidir");
        }
        CariHesap cari = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", dto.getCariHesapId()));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        if (dto.getKalemler() == null || dto.getKalemler().isEmpty()) {
            throw new BusinessException("En az bir kalem eklenmelidir");
        }
        for (TeklifKalemDTO k : dto.getKalemler()) {
            if (k.getAciklama() == null || k.getAciklama().isBlank()) {
                throw new BusinessException("Kalem açıklaması boş olamaz");
            }
        }
        String teklifNo = dto.getTeklifNo() != null && !dto.getTeklifNo().isBlank()
                ? dto.getTeklifNo()
                : seriNoServisi.teklifNoUret(sirketId);

        BigDecimal araToplam = BigDecimal.ZERO;
        BigDecimal kdvToplam = BigDecimal.ZERO;

        if (dto.getKalemler() != null) {
            for (TeklifKalemDTO k : dto.getKalemler()) {
                BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE;
                BigDecimal birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
                BigDecimal iskontoOrani = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : new BigDecimal("20");

                BigDecimal brut = miktar.multiply(birimFiyat);
                BigDecimal iskontoTutar = brut.multiply(iskontoOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal net = brut.subtract(iskontoTutar);
                // Teklifte satır iskontosu yalnız netten düşer; KDV brüt üzerinden hesaplanır.
                BigDecimal kalemKdv = brut.multiply(kdvOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                k.setTutar(net);
                araToplam = araToplam.add(net);
                kdvToplam = kdvToplam.add(kalemKdv);
            }
        }

        BigDecimal iskontoOrani = dto.getIskontoOrani() != null ? dto.getIskontoOrani() : BigDecimal.ZERO;
        BigDecimal genelIskontoTutari = araToplam.multiply(iskontoOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAraToplam = araToplam.subtract(genelIskontoTutari);
        BigDecimal genelToplam = netAraToplam.add(kdvToplam);

        Teklif t = Teklif.builder()
                .teklifNo(teklifNo)
                .revizyonNo(dto.getRevizyonNo() != null ? dto.getRevizyonNo() : 0)
                .tarih(dto.getTarih() != null ? dto.getTarih() : LocalDate.now())
                .gecerlilikTarihi(dto.getGecerlilikTarihi())
                .cariHesapId(dto.getCariHesapId())
                .tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .araToplam(araToplam)
                .kdv(kdvToplam)
                .iskontoOrani(iskontoOrani)
                .iskontoTutari(genelIskontoTutari)
                .genelToplam(genelToplam)
                .paraBirimi(dto.getParaBirimi() != null ? dto.getParaBirimi() : "TRY")
                .teslimatSarti(dto.getTeslimatSarti())
                .odemeSarti(dto.getOdemeSarti())
                .garantiSarti(dto.getGarantiSarti())
                .notlar(dto.getNotlar())
                .sirketId(sirketId)
                .build();

        t = teklifRepository.save(t);

        if (dto.getKalemler() != null) {
            for (TeklifKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(TeklifKalem.builder()
                        .teklifId(t.getId())
                        .stokId(k.getStokId())
                        .aciklama(k.getAciklama())
                        .miktar(k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE)
                        .birim(k.getBirim() != null ? k.getBirim() : "Adet")
                        .birimFiyat(k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO)
                        .iskontoOrani(k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO)
                        .kdvOrani(k.getKdvOrani() != null ? k.getKdvOrani() : new BigDecimal("20"))
                        .tutar(k.getTutar() != null ? k.getTutar() : BigDecimal.ZERO)
                        .build());
            }
        }

        if (sirketId != null) {
            Long bildirimSirketId = sirketId;
            java.math.BigDecimal bildirimTutar = genelToplam;
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "TEKLIF",
                    "Yeni Satış Teklifi: " + teklifNo,
                    "Tutar: " + bildirimTutar + " ₺"));
        }

        return entityToDTO(t);
    }

    public TeklifDTO guncelle(Long id, TeklifDTO dto) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");

        if ("SIPARISE_DONUSTU".equals(t.getDurum()) || "FATURALASTI".equals(t.getDurum())) {
            throw new BusinessException("Siparişe veya faturaya dönüştürülmüş teklifler güncellenemez.");
        }

        t.setTarih(dto.getTarih() != null ? dto.getTarih() : t.getTarih());
        t.setGecerlilikTarihi(dto.getGecerlilikTarihi());
        t.setCariHesapId(dto.getCariHesapId());
        if (dto.getDurum() != null) t.setDurum(dto.getDurum());
        t.setTeslimatSarti(dto.getTeslimatSarti());
        t.setOdemeSarti(dto.getOdemeSarti());
        t.setGarantiSarti(dto.getGarantiSarti());
        t.setNotlar(dto.getNotlar());
        t.setParaBirimi(dto.getParaBirimi());

        kalemRepository.deleteByTeklifId(t.getId());

        BigDecimal araToplam = BigDecimal.ZERO;
        BigDecimal kdvToplam = BigDecimal.ZERO;

        if (dto.getKalemler() != null) {
            for (TeklifKalemDTO k : dto.getKalemler()) {
                if (k.getAciklama() == null || k.getAciklama().isBlank()) {
                    throw new BusinessException("Kalem açıklaması boş olamaz");
                }
                BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE;
                BigDecimal birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
                BigDecimal iskontoOrani = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : new BigDecimal("20");

                BigDecimal brut = miktar.multiply(birimFiyat);
                BigDecimal iskontoTutar = brut.multiply(iskontoOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal net = brut.subtract(iskontoTutar);
                BigDecimal kalemKdv = brut.multiply(kdvOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                k.setTutar(net);
                araToplam = araToplam.add(net);
                kdvToplam = kdvToplam.add(kalemKdv);

                kalemRepository.save(TeklifKalem.builder()
                        .teklifId(t.getId())
                        .stokId(k.getStokId())
                        .aciklama(k.getAciklama())
                        .miktar(miktar)
                        .birim(k.getBirim())
                        .birimFiyat(birimFiyat)
                        .iskontoOrani(iskontoOrani)
                        .kdvOrani(kdvOrani)
                        .tutar(net)
                        .build());
            }
        }

        BigDecimal iskontoOrani = dto.getIskontoOrani() != null ? dto.getIskontoOrani() : BigDecimal.ZERO;
        BigDecimal genelIskontoTutari = araToplam.multiply(iskontoOrani).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAraToplam = araToplam.subtract(genelIskontoTutari);
        BigDecimal genelToplam = netAraToplam.add(kdvToplam);

        t.setAraToplam(araToplam);
        t.setIskontoOrani(iskontoOrani);
        t.setIskontoTutari(genelIskontoTutari);
        t.setKdv(kdvToplam);
        t.setGenelToplam(genelToplam);

        return entityToDTO(teklifRepository.save(t));
    }

    public void sil(Long id) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");

        if ("SIPARISE_DONUSTU".equals(t.getDurum()) || "FATURALASTI".equals(t.getDurum())) {
            throw new BusinessException("Siparişe veya faturaya dönüştürülmüş teklifler silinemez.");
        }

        kalemRepository.deleteByTeklifId(t.getId());
        teklifRepository.delete(t);
    }

    public TeklifDTO durumGuncelle(Long id, String yeniDurum) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");
        t.setDurum(yeniDurum);
        return entityToDTO(teklifRepository.save(t));
    }

    public TeklifDTO revizyonOlustur(Long id) {
        Teklif eski = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(eski.getSirketId(), "Teklif");

        List<TeklifKalem> eskiKalemler = kalemRepository.findByTeklifId(id);

        int yeniRevizyon = (eski.getRevizyonNo() != null ? eski.getRevizyonNo() : 0) + 1;
        if (teklifRepository.existsBySirketIdAndTeklifNoAndRevizyonNo(
                eski.getSirketId(), eski.getTeklifNo(), yeniRevizyon)) {
            throw new BusinessException("Bu teklifin " + yeniRevizyon + ". revizyonu zaten mevcut");
        }

        Teklif yeni = Teklif.builder()
                .teklifNo(eski.getTeklifNo())
                .revizyonNo(yeniRevizyon)
                .tarih(LocalDate.now())
                .gecerlilikTarihi(eski.getGecerlilikTarihi())
                .cariHesapId(eski.getCariHesapId())
                .tur(eski.getTur())
                .durum("TASLAK")
                .araToplam(eski.getAraToplam())
                .kdv(eski.getKdv())
                .iskontoOrani(eski.getIskontoOrani())
                .iskontoTutari(eski.getIskontoTutari())
                .genelToplam(eski.getGenelToplam())
                .paraBirimi(eski.getParaBirimi())
                .teslimatSarti(eski.getTeslimatSarti())
                .odemeSarti(eski.getOdemeSarti())
                .garantiSarti(eski.getGarantiSarti())
                .notlar(eski.getNotlar())
                .sirketId(eski.getSirketId())
                .build();

        yeni = teklifRepository.save(yeni);

        for (TeklifKalem k : eskiKalemler) {
            kalemRepository.save(TeklifKalem.builder()
                    .teklifId(yeni.getId())
                    .stokId(k.getStokId())
                    .aciklama(k.getAciklama())
                    .miktar(k.getMiktar())
                    .birim(k.getBirim())
                    .birimFiyat(k.getBirimFiyat())
                    .iskontoOrani(k.getIskontoOrani())
                    .kdvOrani(k.getKdvOrani())
                    .tutar(k.getTutar())
                    .build());
        }

        return entityToDTO(yeni);
    }

    public SiparisDTO sipariseDonustur(Long id) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");

        List<TeklifKalem> kalemler = kalemRepository.findByTeklifId(id);

        List<SiparisKalemDTO> siparisKalemleri = kalemler.stream().map(k -> SiparisKalemDTO.builder()
                .stokId(k.getStokId())
                .aciklama(k.getAciklama())
                .miktar(k.getMiktar())
                .birim(k.getBirim())
                .birimFiyat(k.getBirimFiyat())
                .kdvOrani(k.getKdvOrani())
                .tutar(k.getTutar())
                .build()).collect(Collectors.toList());

        SiparisDTO siparisDTO = SiparisDTO.builder()
                .cariHesapId(t.getCariHesapId())
                .tarih(LocalDate.now())
                .tur("SATIS")
                .durum("SIPARIS")
                .aciklama("Teklif No: " + t.getTeklifNo() + " (Rev." + t.getRevizyonNo() + ") referansıyla oluşturuldu.")
                .araToplam(t.getAraToplam())
                .kdv(t.getKdv())
                .genelToplam(t.getGenelToplam())
                .kalemler(siparisKalemleri)
                .build();

        SiparisDTO olusanSiparis = siparisService.olustur(siparisDTO, t.getSirketId());

        t.setDurum("SIPARISE_DONUSTU");
        teklifRepository.save(t);

        return olusanSiparis;
    }

    public FaturaDTO faturayaDonustur(Long id, Long kullaniciId) {
        Teklif t = teklifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teklif", id));
        tenantChecker.check(t.getSirketId(), "Teklif");

        List<TeklifKalem> kalemler = kalemRepository.findByTeklifId(id);

        List<FaturaKalemDTO> faturaKalemleri = kalemler.stream().map(k -> FaturaKalemDTO.builder()
                .stokId(k.getStokId())
                .aciklama(k.getAciklama())
                .adet(k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE)
                .birimFiyat(k.getBirimFiyat())
                .iskontoOrani(k.getIskontoOrani())
                .kdvOrani(k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.valueOf(20))
                .tutar(k.getTutar())
                .build()).collect(Collectors.toList());

        FaturaDTO faturaDTO = FaturaDTO.builder()
                .cariHesapId(t.getCariHesapId())
                .tarih(LocalDate.now())
                .tur("SATIS")
                .durum("KESILDI")
                .aciklama("Teklif No: " + t.getTeklifNo() + " referansıyla oluşturuldu.")
                .araToplam(t.getAraToplam())
                .kdv(t.getKdv())
                .genelIskontoTutari(t.getIskontoTutari())
                .genelToplam(t.getGenelToplam())
                .paraBirimi(t.getParaBirimi())
                .kalemler(faturaKalemleri)
                .build();

        FaturaDTO olusanFatura = faturaService.faturaOlustur(faturaDTO, t.getSirketId(), kullaniciId, "Teklif Dönüşümü");

        t.setDurum("FATURALASTI");
        teklifRepository.save(t);

        return olusanFatura;
    }

    private TeklifDTO entityToDTO(Teklif t) {
        List<TeklifKalem> kalemler = kalemRepository.findByTeklifId(t.getId());
        Map<Long, Stok> stokMap = stokMapOlustur(kalemler.stream()
                .map(TeklifKalem::getStokId).collect(Collectors.toSet()));
        Map<Long, CariHesap> cariMap = cariMapOlustur(new HashSet<>(java.util.Collections.singletonList(t.getCariHesapId())));
        return entityToDTO(t, kalemler, stokMap, cariMap);
    }

    private Map<Long, Stok> stokMapOlustur(java.util.Collection<Long> stokIdler) {
        List<Long> gecerli = stokIdler.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (gecerli.isEmpty()) return Map.of();
        return stokRepository.findAllById(gecerli).stream()
                .collect(Collectors.toMap(Stok::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, CariHesap> cariMapOlustur(java.util.Collection<Long> cariIdler) {
        List<Long> gecerli = cariIdler.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (gecerli.isEmpty()) return Map.of();
        return cariHesapRepository.findAllById(gecerli).stream()
                .collect(Collectors.toMap(CariHesap::getId, c -> c, (a, b) -> a));
    }

    private TeklifDTO entityToDTO(Teklif t, List<TeklifKalem> kalemler,
                                  Map<Long, Stok> stokMap, Map<Long, CariHesap> cariMap) {
        List<TeklifKalemDTO> kalemDTOs = kalemler.stream().map(k -> {
            Stok stok = k.getStokId() != null ? stokMap.get(k.getStokId()) : null;
            return TeklifKalemDTO.builder()
                    .id(k.getId())
                    .teklifId(k.getTeklifId())
                    .stokId(k.getStokId())
                    .stokKodu(stok != null ? stok.getStokKodu() : null)
                    .aciklama(k.getAciklama())
                    .miktar(k.getMiktar())
                    .birim(k.getBirim())
                    .birimFiyat(k.getBirimFiyat())
                    .iskontoOrani(k.getIskontoOrani())
                    .kdvOrani(k.getKdvOrani())
                    .tutar(k.getTutar())
                    .build();
        }).collect(Collectors.toList());

        String cariAd = null;
        String cariVergiNo = null;
        String cariVergiDairesi = null;
        String cariTelefon = null;
        String cariEmail = null;
        String cariAdres = null;

        CariHesap c = t.getCariHesapId() != null ? cariMap.get(t.getCariHesapId()) : null;
        if (c != null) {
            cariAd = c.getAd();
            cariVergiNo = c.getVergiNumarasi();
            cariVergiDairesi = c.getVergiDairesi();
            cariTelefon = c.getTelefon();
            cariEmail = c.getEmail();
            cariAdres = c.getAdres();
        }

        return TeklifDTO.builder()
                .id(t.getId())
                .teklifNo(t.getTeklifNo())
                .revizyonNo(t.getRevizyonNo())
                .tarih(t.getTarih())
                .gecerlilikTarihi(t.getGecerlilikTarihi())
                .cariHesapId(t.getCariHesapId())
                .cariHesapAdi(cariAd)
                .cariVergiNo(cariVergiNo)
                .cariVergiDairesi(cariVergiDairesi)
                .cariTelefon(cariTelefon)
                .cariEmail(cariEmail)
                .cariAdres(cariAdres)
                .tur(t.getTur())
                .durum(t.getDurum())
                .araToplam(t.getAraToplam())
                .kdv(t.getKdv())
                .iskontoOrani(t.getIskontoOrani())
                .iskontoTutari(t.getIskontoTutari())
                .genelToplam(t.getGenelToplam())
                .paraBirimi(t.getParaBirimi())
                .teslimatSarti(t.getTeslimatSarti())
                .odemeSarti(t.getOdemeSarti())
                .garantiSarti(t.getGarantiSarti())
                .notlar(t.getNotlar())
                .sirketId(t.getSirketId())
                .olusturmaTarihi(t.getOlusturmaTarihi())
                .kalemler(kalemDTOs)
                .build();
    }
}
