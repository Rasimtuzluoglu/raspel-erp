package com.raspel.erp.service.muhasebe;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeKalemDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeOzetDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeTeslimIstek;
import com.raspel.erp.dto.muhasebe.TopluFaturaSonucDTO;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.muhasebe.IrsaliyeDurumLog;
import com.raspel.erp.entity.muhasebe.IrsaliyeKalem;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeDurumLogRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IrsaliyeService {

    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeDurumLogRepository irsaliyeDurumLogRepository;
    private final IrsaliyeKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SiparisRepository siparisRepository;
    private final TeslimatRepository teslimatRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    private final com.raspel.erp.service.ticaret.FaturaService faturaService;
    private final com.raspel.erp.service.ticaret.TeslimatService teslimatService;
    private final PlatformTransactionManager transactionManager;

    private static final String TASLAK = "TASLAK";
    private static final String HAZIRLANDI = "HAZIRLANDI";
    private static final String SEVK_EDILDI = "SEVK_EDILDI";
    private static final String TESLIM_EDILDI = "TESLIM_EDILDI";
    private static final String IPTAL = "IPTAL";
    /** Eski istemcilerin gönderdiği durum adı; yeni modelde SEVK_EDILDI. */
    private static final String KESILDI = "KESILDI";

    @Transactional(readOnly = true)
    public Page<IrsaliyeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<Irsaliye> sayfa = irsaliyeRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable);
        List<IrsaliyeDTO> dtolar = sayfa.getContent().stream().map(this::temelDTO).collect(Collectors.toList());
        zenginlestir(sayfa.getContent(), dtolar);
        return new PageImpl<>(dtolar, pageable, sayfa.getTotalElements());
    }

    /**
     * Sunucu taraflı irsaliye listesi. {@code q} irsaliye no veya cari adında arar;
     * tarih aralığı, durum, tür, cari ve faturalı/faturasız filtresi desteklenir.
     */
    @Transactional(readOnly = true)
    public Page<IrsaliyeDTO> filtreli(Long sirketId, String q, String durum, String tur, Long cariId,
                                      LocalDate bas, LocalDate bit, Boolean faturali, Pageable pageable) {
        String qq = com.raspel.erp.util.AramaTemizleyici.like(q);
        String dd = (durum == null || durum.isBlank()) ? null : durum.trim().toUpperCase(Locale.ROOT);
        String tt = (tur == null || tur.isBlank()) ? null : tur.trim().toUpperCase(Locale.ROOT);
        Page<Irsaliye> sayfa = irsaliyeRepository.filtreliGetir(sirketId, qq, dd, tt, cariId, bas, bit,
                faturali, pageable);
        List<IrsaliyeDTO> dtolar = sayfa.getContent().stream().map(this::temelDTO).collect(Collectors.toList());
        zenginlestir(sayfa.getContent(), dtolar);
        return new PageImpl<>(dtolar, pageable, sayfa.getTotalElements());
    }

    /**
     * Liste KPI özeti. Durum sayımları verilen tarih aralığında (verilmezse tüm
     * zamanlarda) hesaplanır; {@code teslimEdilenBuAy} aralık verilmediyse içinde
     * bulunulan ayı kullanır.
     */
    @Transactional(readOnly = true)
    public IrsaliyeOzetDTO ozet(Long sirketId, LocalDate bas, LocalDate bit) {
        LocalDate bugun = LocalDate.now();
        LocalDate teslimBas;
        LocalDate teslimBit;
        if (bas != null && bit != null) {
            teslimBas = bas;
            teslimBit = bit;
        } else {
            teslimBas = bugun.withDayOfMonth(1);
            teslimBit = bugun.withDayOfMonth(bugun.lengthOfMonth());
        }
        return IrsaliyeOzetDTO.builder()
                .taslak(irsaliyeRepository.countDurum(sirketId, TASLAK, bas, bit))
                .hazirlanan(irsaliyeRepository.countDurum(sirketId, HAZIRLANDI, bas, bit))
                .sevkEdilen(irsaliyeRepository.countDurum(sirketId, SEVK_EDILDI, bas, bit))
                .iptal(irsaliyeRepository.countDurum(sirketId, IPTAL, bas, bit))
                .teslimEdilenBuAy(irsaliyeRepository.countTeslimEdilen(sirketId,
                        teslimBas.atStartOfDay(), teslimBit.plusDays(1).atStartOfDay()))
                .faturasiz(irsaliyeRepository.countFaturasiz(sirketId, bas, bit))
                .build();
    }

    @Transactional(readOnly = true)
    public IrsaliyeDTO getir(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        IrsaliyeDTO dto = entityToDTO(i);
        dto.setGecmis(loglariGetir(i.getId()));
        return dto;
    }

    public IrsaliyeDTO olustur(IrsaliyeDTO dto, Long sirketId) {
        donemService.kilitKontrol(sirketId,
                dto.getTarih() != null ? dto.getTarih() : LocalDate.now(), "irsaliye oluşturma");
        Irsaliye i = Irsaliye.builder()
                .irsaliyeNo(dto.getIrsaliyeNo()).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).faturaId(dto.getFaturaId())
                .siparisId(dto.getSiparisId())
                .durum(TASLAK).tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .aciklama(dto.getAciklama()).sirketId(sirketId).depoId(dto.getDepoId()).build();
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        logYaz(i, null, i.getDurum(), "OLUSTUR", null, "İrsaliye oluşturuldu");
        return entityToDTO(i);
    }

    /**
     * İrsaliyeyi faturaya dönüştürür.
     *
     * <p>İptal dışındaki her durumda fatura kesilebilir. {@code TASLAK}/{@code HAZIRLANDI}
     * bir irsaliye için önce aynı transaction içinde otomatik sevk çalıştırılır
     * (şoför/adres yoksa anlaşılır hata döner; stok hareketi bu durumda hiç
     * başlamaz). İrsaliye stoğu sevk anında işlendiği için faturaya
     * {@code irsaliyeId} bağlanır ve fatura kesilirken stok tekrar düşülmez
     * (çift düşüm önlenir).
     */
    @Transactional
    public IrsaliyeDTO faturayaDonustur(Long id, Long sirketId) {
        return faturayaDonustur(id, sirketId, null);
    }

    @Transactional
    public IrsaliyeDTO faturayaDonustur(Long id, Long sirketId, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        if (IPTAL.equals(i.getDurum())) {
            throw new BusinessException("İptal edilmiş irsaliye faturaya dönüştürülemez.");
        }
        if (canliFaturaVar(i)) {
            var mevcut = faturaRepository.findById(i.getFaturaId()).orElse(null);
            throw new BusinessException("Bu irsaliye zaten faturaya dönüştürülmüş: "
                    + (mevcut != null ? mevcut.getFaturaNumarasi() : i.getFaturaId()));
        }
        if (!SEVK_EDILDI.equals(i.getDurum()) && !TESLIM_EDILDI.equals(i.getDurum())) {
            // TASLAK veya HAZIRLANDI: fatura öncesi otomatik sevk (aynı transaction).
            if (TASLAK.equals(i.getDurum())) {
                hazirla(id, kullaniciId);
                i = kilitliGetir(id);
            }
            sevk(id, mevcutSevkIstegi(i), kullaniciId);
            i = irsaliyeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        }
        List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
        List<com.raspel.erp.dto.ticaret.FaturaKalemDTO> faturaKalemler = new java.util.ArrayList<>();
        for (IrsaliyeKalem k : kalemler) {
            // İrsaliye kaleminde fiyat/KDV yok; stok kartından satış fiyatı ve KDV oranı çözülür.
            java.math.BigDecimal birimFiyat = java.math.BigDecimal.ZERO;
            java.math.BigDecimal kdv = new java.math.BigDecimal("20");
            if (k.getStokId() != null) {
                var stok = stokRepository.findById(k.getStokId()).orElse(null);
                if (stok != null) {
                    birimFiyat = stok.getSatisFiyati() != null ? stok.getSatisFiyati()
                            : (stok.getFiyat() != null ? stok.getFiyat() : java.math.BigDecimal.ZERO);
                    if (stok.getKdvOrani() != null) kdv = stok.getKdvOrani();
                }
            }
            faturaKalemler.add(com.raspel.erp.dto.ticaret.FaturaKalemDTO.builder()
                    .aciklama(k.getAciklama() != null ? k.getAciklama() : "")
                    .adet(k.getMiktar() != null ? k.getMiktar() : java.math.BigDecimal.ONE)
                    .birimFiyat(birimFiyat).kdvOrani(kdv).stokId(k.getStokId()).build());
        }
        com.raspel.erp.dto.ticaret.FaturaDTO faturaDTO = com.raspel.erp.dto.ticaret.FaturaDTO.builder()
                .tarih(java.time.LocalDate.now())
                .tur("SATIS").durum("KESILDI")
                .cariHesapId(i.getCariHesapId())
                .irsaliyeId(i.getId())
                .siparisId(i.getSiparisId())
                .depoId(i.getDepoId())
                .aciklama("İrsaliye #" + i.getIrsaliyeNo() + " dönüşümü")
                .kalemler(faturaKalemler)
                .build();
        var olusan = faturaService.faturaOlustur(faturaDTO, i.getSirketId(), kullaniciId, null);
        i.setFaturaId(olusan.getId());
        i = irsaliyeRepository.save(i);
        teslimatService.faturaBaglaByIrsaliye(i.getId(), olusan.getId(), olusan.getFaturaNumarasi());
        logYaz(i, i.getDurum(), i.getDurum(), "FATURA", kullaniciId,
                "Fatura #" + olusan.getFaturaNumarasi() + " oluşturuldu");
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(i);
    }

    /**
     * Toplu fatura: seçilen irsaliyeler aynı cariye ait olmalıdır. İptal edilmiş ve
     * canlı faturası olan kayıtlar atlanır; her irsaliye bağımsız transaction'da
     * faturalanır ve kısmi hata sonuç özetinde raporlanır.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TopluFaturaSonucDTO topluFatura(List<Long> ids, Long sirketId, Long kullaniciId) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Faturaya dönüştürülecek irsaliye seçilmedi.");
        }
        List<Long> benzersiz = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (benzersiz.isEmpty()) {
            throw new BusinessException("Faturaya dönüştürülecek irsaliye seçilmedi.");
        }
        List<Irsaliye> kayitlar = irsaliyeRepository.findAllById(benzersiz);
        if (kayitlar.size() != benzersiz.size()) {
            throw new BusinessException("Seçilen irsaliyelerden bazıları bulunamadı.");
        }
        for (Irsaliye i : kayitlar) {
            if (sirketId != null && !sirketId.equals(i.getSirketId())) {
                throw new BusinessException("İrsaliye #" + i.getIrsaliyeNo() + " bu şirkete ait değil.");
            }
        }
        // Aynı cari kuralı: tek bir farklı cari bile tüm isteği reddeder (kısmi fatura oluşmaz).
        Set<Long> cariler = new HashSet<>();
        for (Irsaliye i : kayitlar) {
            if (i.getCariHesapId() == null) {
                throw new BusinessException("Cari hesabı olmayan irsaliye toplu faturalanamaz: #" + i.getIrsaliyeNo());
            }
            cariler.add(i.getCariHesapId());
        }
        if (cariler.size() > 1) {
            throw new BusinessException("Toplu fatura için seçilen irsaliyelerin tümü aynı cariye ait olmalıdır.");
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        List<TopluFaturaSonucDTO.Basarili> basarili = new ArrayList<>();
        List<TopluFaturaSonucDTO.Atlanan> atlanan = new ArrayList<>();
        for (Irsaliye i : kayitlar) {
            if (IPTAL.equals(i.getDurum())) {
                atlanan.add(TopluFaturaSonucDTO.Atlanan.builder()
                        .irsaliyeId(i.getId()).irsaliyeNo(i.getIrsaliyeNo())
                        .sebep("İptal edilmiş irsaliye").build());
                continue;
            }
            if (canliFaturaVar(i)) {
                atlanan.add(TopluFaturaSonucDTO.Atlanan.builder()
                        .irsaliyeId(i.getId()).irsaliyeNo(i.getIrsaliyeNo())
                        .sebep("İrsaliye zaten faturaya dönüştürülmüş").build());
                continue;
            }
            try {
                IrsaliyeDTO sonuc = template.execute(status ->
                        faturayaDonustur(i.getId(), i.getSirketId(), kullaniciId));
                if (sonuc == null) {
                    throw new BusinessException("Fatura oluşturulamadı");
                }
                basarili.add(TopluFaturaSonucDTO.Basarili.builder()
                        .irsaliyeId(sonuc.getId()).irsaliyeNo(sonuc.getIrsaliyeNo())
                        .faturaId(sonuc.getFaturaId()).faturaNo(sonuc.getFaturaNumarasi()).build());
            } catch (Exception e) {
                log.warn("Toplu fatura: irsaliye #{} atlandı: {}", i.getIrsaliyeNo(), e.getMessage());
                atlanan.add(TopluFaturaSonucDTO.Atlanan.builder()
                        .irsaliyeId(i.getId()).irsaliyeNo(i.getIrsaliyeNo())
                        .sebep(hataMesaji(e)).build());
            }
        }
        return TopluFaturaSonucDTO.builder().basarili(basarili).atlanan(atlanan).build();
    }

    private String hataMesaji(Exception e) {
        String mesaj = e.getMessage();
        return (mesaj == null || mesaj.isBlank()) ? "Fatura oluşturulamadı" : mesaj;
    }

    public IrsaliyeDTO guncelle(Long id, IrsaliyeDTO dto) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye düzenleme");
        if (dto.getTarih() != null) {
            donemService.kilitKontrol(i.getSirketId(), dto.getTarih(), "irsaliye düzenleme");
        }
        i.setIrsaliyeNo(dto.getIrsaliyeNo());
        i.setTarih(dto.getTarih());
        i.setCariHesapId(dto.getCariHesapId());
        i.setFaturaId(dto.getFaturaId());
        if (dto.getSiparisId() != null) i.setSiparisId(dto.getSiparisId());
        if (dto.getTur() != null) i.setTur(dto.getTur());
        if (dto.getDepoId() != null) i.setDepoId(dto.getDepoId());

        // REDTEAM C5: `durum` alanı DTO'dan doğrudan yazılıyordu. Saldırı
        // zinciri: TASLAK oluştur (stok HİÇ ETKİLENMEZ) -> PUT ile KESILDI yap
        // (stok yine etkilenmez, bu yol stok düşürmüyor) -> /durum=IPTAL
        // (stok +miktar GERİ EKLENİR) => satış olmadan BEDAVA STOK.
        // Durum yalnızca `durumGuncelle` uçlarından değişebilir.
        String istenenDurum = dto.getDurum();
        if (istenenDurum != null && !istenenDurum.equals(i.getDurum())) {
            throw new BusinessException("İrsaliye durumu değiştirilemez. Durum değişikliği için "
                    + "'PUT /api/irsaliyeler/{id}/durum' ucunu kullanın. "
                    + "Mevcut: " + i.getDurum() + ", istenen: " + istenenDurum);
        }

        i.setAciklama(dto.getAciklama());
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteByIrsaliyeId(i.getId());
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    /**
     * Eski durum ucu (geriye uyum). {@code KESILDI} isteği yeni modelde sevk anlamına
     * gelir: TASLAK otomatik hazırlanır, HAZIRLANDI mevcut sevk alanlarıyla sevk
     * edilir (şoför yoksa anlaşılır hata), SEVK_EDILDI idempotent kabul edilir.
     */
    public IrsaliyeDTO durumGuncelle(Long id, String durum) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye durum güncelleme");
        if (durum == null || durum.isBlank()) {
            throw new BusinessException("İrsaliye durumu belirtilmedi.");
        }
        return switch (durum) {
            case KESILDI -> legacySevkEt(i);
            case SEVK_EDILDI -> legacySevkEt(i);
            case TESLIM_EDILDI -> teslim(id, IrsaliyeTeslimIstek.builder().build(), null);
            case IPTAL -> iptal(id, null, null);
            case TASLAK -> taslagaAl(i);
            case HAZIRLANDI -> legacyHazirla(i);
            default -> throw new BusinessException("Geçersiz irsaliye durumu: " + durum);
        };
    }

    /** KESILDI/SEVK_EDILDI isteği: gerekirse hazırla + sevk et, sevk edilmişse idempotent. */
    private IrsaliyeDTO legacySevkEt(Irsaliye i) {
        if (SEVK_EDILDI.equals(i.getDurum())) {
            return entityToDTO(i);
        }
        if (TASLAK.equals(i.getDurum())) {
            hazirla(i.getId(), null);
            i = kilitliGetir(i.getId());
        } else if (!HAZIRLANDI.equals(i.getDurum())) {
            throw new BusinessException("Bu irsaliye sevk edilemez (durum: " + i.getDurum() + ")");
        }
        if (i.getSoforId() == null) {
            throw new BusinessException("İrsaliye sevk etmek için şoför seçilmelidir.");
        }
        return sevk(i.getId(), mevcutSevkIstegi(i), null);
    }

    private IrsaliyeDTO legacyHazirla(Irsaliye i) {
        if (HAZIRLANDI.equals(i.getDurum())) {
            return entityToDTO(i);
        }
        if (!TASLAK.equals(i.getDurum())) {
            throw new BusinessException("Yalnızca taslak irsaliye hazırlanabilir (durum: " + i.getDurum() + ")");
        }
        return hazirla(i.getId(), null);
    }

    /** Eski istemcilerin "durum=TASLAK" isteği: sevk edilmişse stok iadesiyle geri alınır. */
    private IrsaliyeDTO taslagaAl(Irsaliye i) {
        if (TASLAK.equals(i.getDurum())) {
            return entityToDTO(i);
        }
        if (IPTAL.equals(i.getDurum())) {
            i.setDurum(TASLAK);
            return entityToDTO(irsaliyeRepository.save(i));
        }
        if (TESLIM_EDILDI.equals(i.getDurum())) {
            throw new BusinessException("Teslim edilmiş irsaliye taslağa çevrilemez; yalnızca iptal edilebilir.");
        }
        if (SEVK_EDILDI.equals(i.getDurum())) {
            stokIadeIsle(i, "İrsaliye geri alındı");
            teslimatService.irsaliyeIptal(i.getTeslimatId());
            i.setSevkTarihi(null);
            cacheYardimci.commitSonrasiTemizle("stoklar", "dashboard");
        }
        String eski = i.getDurum();
        i.setDurum(TASLAK);
        Irsaliye kayit = irsaliyeRepository.save(i);
        logYaz(kayit, eski, TASLAK, "SEVKI_GERI_AL", null, null);
        return entityToDTO(kayit);
    }

    /** Mevcut irsaliye alanlarından sevk isteği üretir (legacy/otomatik sevk). */
    private com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek mevcutSevkIstegi(Irsaliye i) {
        return com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek.builder()
                .plaka(i.getPlaka())
                .soforId(i.getSoforId())
                .teslimatAdresi(i.getTeslimatAdresi())
                .tasiyiciFirma(i.getTasiyiciFirma())
                .koliAdedi(i.getKoliAdedi())
                .desi(i.getDesi())
                .brutKg(i.getBrutKg())
                .build();
    }

    @Transactional
    public IrsaliyeDTO sevk(Long id, com.raspel.erp.dto.muhasebe.IrsaliyeSevkIstek istek, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye sevki");
        if (!HAZIRLANDI.equals(i.getDurum())) {
            throw new BusinessException("Yalnızca hazırlanmış irsaliye sevk edilebilir (durum: " + i.getDurum() + ")");
        }
        if (istek == null || istek.getSoforId() == null) {
            throw new BusinessException("Sevk için şoför seçilmelidir");
        }
        String adres = istek.getTeslimatAdresi();
        if (adres == null || adres.isBlank()) {
            adres = i.getCariHesapId() != null
                    ? cariHesapRepository.findById(i.getCariHesapId()).map(c -> c.getAdres()).orElse(null)
                    : null;
        }
        if (adres == null || adres.isBlank()) {
            throw new BusinessException("Teslimat adresi zorunludur");
        }
        ciftDusumKontrol(i);
        i.setPlaka(istek.getPlaka());
        i.setSoforId(istek.getSoforId());
        i.setTeslimatAdresi(adres.trim());
        i.setTasiyiciFirma(istek.getTasiyiciFirma());
        i.setKoliAdedi(istek.getKoliAdedi());
        i.setDesi(istek.getDesi());
        i.setBrutKg(istek.getBrutKg());

        stokCikisIsle(i);
        var teslimat = teslimatService.irsaliyedenOlustur(i);
        i.setTeslimatId(teslimat.getId());
        String eski = i.getDurum();
        i.setDurum(SEVK_EDILDI);
        i.setSevkTarihi(LocalDateTime.now());
        Irsaliye kayit = irsaliyeRepository.save(i);
        logYaz(kayit, eski, SEVK_EDILDI, "SEVK", kullaniciId, null);
        cacheYardimci.commitSonrasiTemizle("stoklar", "dashboard");
        return entityToDTO(kayit);
    }

    /** @param istek teslim alan/tarih; null ise tarih şimdi alınır. */
    @Transactional
    public IrsaliyeDTO teslim(Long id, IrsaliyeTeslimIstek istek, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye teslimi");
        if (!SEVK_EDILDI.equals(i.getDurum())) {
            throw new BusinessException("Yalnızca sevk edilmiş irsaliye teslim edilebilir (durum: " + i.getDurum() + ")");
        }
        String teslimAlan = istek != null ? istek.getTeslimAlan() : null;
        LocalDateTime tarih = istek != null && istek.getTeslimTarihi() != null
                ? istek.getTeslimTarihi() : LocalDateTime.now();
        if (teslimAlan != null && !teslimAlan.isBlank()) {
            i.setTeslimAlan(teslimAlan.trim());
        }
        i.setTeslimTarihi(tarih);
        String eski = i.getDurum();
        i.setDurum(TESLIM_EDILDI);
        Irsaliye kayit = irsaliyeRepository.save(i);
        teslimatService.irsaliyeTeslimEdildi(kayit.getTeslimatId(), kayit.getTeslimAlan(), tarih);
        logYaz(kayit, eski, TESLIM_EDILDI, "TESLIM", kullaniciId, null);
        return entityToDTO(kayit);
    }

    /**
     * Sevki geri alır: stok/maliyet hareketi ters kayıtla düzeltilir, otomatik
     * teslimat iptal edilir ve irsaliye yeniden HAZIRLANDI olur. Canlı (iptal
     * edilmemiş) fatura varsa işlem reddedilir.
     */
    @Transactional
    public IrsaliyeDTO sevkiGeriAl(Long id, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye sevk iadesi");
        if (!SEVK_EDILDI.equals(i.getDurum())) {
            throw new BusinessException("Yalnızca sevk edilmiş irsaliyenin sevki geri alınabilir (durum: "
                    + i.getDurum() + ")");
        }
        if (canliFaturaVar(i)) {
            throw new BusinessException("Bu irsaliyeye bağlı canlı fatura var. Önce faturayı iptal edin.");
        }
        stokIadeIsle(i, "Sevk geri alındı");
        teslimatService.irsaliyeIptal(i.getTeslimatId());
        String eski = i.getDurum();
        i.setDurum(HAZIRLANDI);
        i.setSevkTarihi(null);
        i.setTeslimAlan(null);
        i.setTeslimTarihi(null);
        Irsaliye kayit = irsaliyeRepository.save(i);
        logYaz(kayit, eski, HAZIRLANDI, "SEVKI_GERI_AL", kullaniciId, null);
        cacheYardimci.commitSonrasiTemizle("stoklar", "dashboard");
        return entityToDTO(kayit);
    }

    /**
     * İrsaliyeyi iptal eder. Taslak/hazırlanmış kayıtta stok etkisi yoktur; sevk
     * edilmiş veya teslim edilmiş kayıtta canlı fatura yoksa stok iadesi yapılır ve
     * teslimat iptal edilir.
     */
    @Transactional
    public IrsaliyeDTO iptal(Long id, String aciklama, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye iptali");
        if (IPTAL.equals(i.getDurum())) {
            throw new BusinessException("İrsaliye zaten iptal edilmiş.");
        }
        if (SEVK_EDILDI.equals(i.getDurum()) || TESLIM_EDILDI.equals(i.getDurum())) {
            if (canliFaturaVar(i)) {
                throw new BusinessException("Bu irsaliyeye bağlı canlı fatura var. Önce faturayı iptal edin.");
            }
            stokIadeIsle(i, "İrsaliye iptal");
            teslimatService.irsaliyeIptal(i.getTeslimatId());
            i.setSevkTarihi(null);
            cacheYardimci.commitSonrasiTemizle("stoklar", "dashboard");
        } else if (!TASLAK.equals(i.getDurum()) && !HAZIRLANDI.equals(i.getDurum())) {
            throw new BusinessException("İrsaliye durumu iptale uygun değil: " + i.getDurum());
        }
        String eski = i.getDurum();
        i.setDurum(IPTAL);
        Irsaliye kayit = irsaliyeRepository.save(i);
        logYaz(kayit, eski, IPTAL, "IPTAL", kullaniciId, aciklama);
        return entityToDTO(kayit);
    }

    @Transactional
    public IrsaliyeDTO hazirla(Long id, Long kullaniciId) {
        Irsaliye i = kilitliGetir(id);
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye hazırlama");
        if (!TASLAK.equals(i.getDurum())) {
            throw new BusinessException("Yalnızca taslak irsaliye hazırlanabilir (durum: " + i.getDurum() + ")");
        }
        String eski = i.getDurum();
        i.setDurum(HAZIRLANDI);
        Irsaliye kayit = irsaliyeRepository.save(i);
        logYaz(kayit, eski, HAZIRLANDI, "HAZIRLA", kullaniciId, null);
        return entityToDTO(kayit);
    }

    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.muhasebe.IrsaliyeDurumLogDTO> gecmis(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        return loglariGetir(i.getId());
    }

    private List<com.raspel.erp.dto.muhasebe.IrsaliyeDurumLogDTO> loglariGetir(Long irsaliyeId) {
        return irsaliyeDurumLogRepository.findByIrsaliyeIdOrderByOlusturmaTarihiAscIdAsc(irsaliyeId).stream()
                .map(l -> com.raspel.erp.dto.muhasebe.IrsaliyeDurumLogDTO.builder()
                        .id(l.getId())
                        .eskiDurum(l.getEskiDurum())
                        .yeniDurum(l.getYeniDurum())
                        .islem(l.getIslem())
                        .aciklama(l.getAciklama())
                        .kullaniciId(l.getKullaniciId())
                        .olusturmaTarihi(l.getOlusturmaTarihi())
                        .build())
                .toList();
    }

    private void logYaz(Irsaliye i, String eskiDurum, String yeniDurum, String islem,
                        Long kullaniciId, String aciklama) {
        irsaliyeDurumLogRepository.save(IrsaliyeDurumLog.builder()
                .irsaliyeId(i.getId())
                .sirketId(i.getSirketId())
                .eskiDurum(eskiDurum)
                .yeniDurum(yeniDurum)
                .islem(islem)
                .kullaniciId(kullaniciId)
                .aciklama(aciklama)
                .build());
    }

    public void sil(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        donemService.kilitKontrol(i.getSirketId(), i.getTarih(), "irsaliye silme");
        if (KESILDI.equals(i.getDurum()) || SEVK_EDILDI.equals(i.getDurum())
                || TESLIM_EDILDI.equals(i.getDurum())) {
            throw new BusinessException("Sevk edilmiş irsaliye doğrudan silinemez, önce iptal edilmelidir");
        }
        kalemRepository.deleteByIrsaliyeId(id);
        irsaliyeRepository.deleteById(id);
    }

    /** Kilitli + tenant doğrulanmış irsaliye okuması. */
    private Irsaliye kilitliGetir(Long id) {
        Irsaliye i = irsaliyeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        return i;
    }

    /**
     * Çift düşüm koruması: siparişin canlı faturası veya irsaliyeye bağlı canlı
     * fatura varsa sevk stoğu ikinci kez düşürür. Sevk öncesi reddedilir.
     */
    private void ciftDusumKontrol(Irsaliye i) {
        if (i.getSiparisId() != null) {
            boolean canliFatura = faturaRepository.findBySiparisId(i.getSiparisId()).stream()
                    .anyMatch(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL);
            if (canliFatura) {
                throw new BusinessException(
                        "Bu sipariş için zaten fatura kesilmiş. İrsaliye kesmek stoğu ikinci kez düşürür; "
                                + "önce bağlı faturayı iptal edin.");
            }
        }
        if (canliFaturaVar(i)) {
            throw new BusinessException("Bu irsaliyeye bağlı kesilmiş fatura var; irsaliye yeniden kesilemez.");
        }
    }

    /** İrsaliyeye bağlı fatura var ve iptal edilmemişse true. */
    private boolean canliFaturaVar(Irsaliye i) {
        if (i.getFaturaId() == null) return false;
        return faturaRepository.findById(i.getFaturaId())
                .map(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL)
                .orElse(false);
    }

    private void stokCikisIsle(Irsaliye i) {
        List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
        Long depoId = depoStokService.coz(i.getDepoId(), i.getSirketId());
        for (IrsaliyeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            tenantChecker.check(stok.getSirketId(), "Stok");
            BigDecimal adet = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
            if ("SATIS".equals(i.getTur()) && stok.getMiktar().compareTo(adet) < 0) {
                throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                        + ", Mevcut: " + stok.getMiktar() + ", İstenen: " + adet);
            }
            if ("SATIS".equals(i.getTur())) {
                stok.setMiktar(stok.getMiktar().subtract(adet));
                maliyetService.cikisIsle(stok, adet, stok.getMiktar(), i.getSirketId(), "IRSALIYE", i.getId());
            } else {
                BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                stok.setMiktar(stok.getMiktar().add(adet));
                maliyetService.girisIsle(stok, eskiMiktar, adet, null, i.getSirketId(), "IRSALIYE", i.getId());
            }
            stokRepository.save(stok);
            Long seriId = null;
            if ("SATIS".equals(i.getTur())) {
                var seriler = stokSeriService.fefoTuket(stok.getId(), depoId, adet);
                if (seriler.size() == 1) seriId = seriler.get(0);
            }
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok)
                    .tur("SATIS".equals(i.getTur()) ? "CIKIS" : "GIRIS")
                    .miktar(adet)
                    .hareketTarihi(i.getTarih() != null ? i.getTarih() : LocalDate.now())
                    .aciklama("İrsaliye #" + i.getIrsaliyeNo())
                    .depoId(depoId)
                    .seriId(seriId)
                    .kaynakTip("IRSALIYE").kaynakId(i.getId())
                    .build());
            depoStokService.guncelle(depoId, stok.getId(),
                    "SATIS".equals(i.getTur()) ? adet.negate() : adet);
        }
    }

    /**
     * Sevk edilmiş irsaliyenin stok etkisini ters kayıtla geri alır
     * (SATIS -> giriş, ALIS -> çıkış). Maliyet ve depo stoğu da düzeltilir.
     */
    private void stokIadeIsle(Irsaliye i, String sebep) {
        List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
        Long depoId = depoStokService.coz(i.getDepoId(), i.getSirketId());
        for (IrsaliyeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            // REDTEAM D-05: Geri alma (stok iadesi) yolunda da tenant doğrulaması.
            tenantChecker.check(stok.getSirketId(), "Stok");
            BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
            if ("SATIS".equals(i.getTur())) {
                BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                stok.setMiktar(stok.getMiktar().add(miktar));
                maliyetService.girisIsle(stok, eskiMiktar, miktar, null, i.getSirketId(), "IRSALIYE", i.getId());
            } else {
                stok.setMiktar(stok.getMiktar().subtract(miktar));
                maliyetService.cikisIsle(stok, miktar, stok.getMiktar(), i.getSirketId(), "IRSALIYE", i.getId());
            }
            stokRepository.save(stok);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok)
                    .tur("SATIS".equals(i.getTur()) ? "GIRIS" : "CIKIS")
                    .miktar(miktar)
                    .hareketTarihi(i.getTarih() != null ? i.getTarih() : LocalDate.now())
                    .aciklama(sebep + " #" + i.getIrsaliyeNo())
                    .depoId(depoId)
                    .kaynakTip("IRSALIYE").kaynakId(i.getId())
                    .build());
            depoStokService.guncelle(depoId, stok.getId(),
                    "SATIS".equals(i.getTur()) ? miktar : miktar.negate());
        }
    }

    private IrsaliyeDTO entityToDTO(Irsaliye i) {
        IrsaliyeDTO dto = temelDTO(i);
        zenginlestir(List.of(i), List.of(dto));
        return dto;
    }

    private IrsaliyeDTO temelDTO(Irsaliye i) {
        List<IrsaliyeKalem> kalemEntities = kalemRepository.findByIrsaliyeId(i.getId());

        List<Long> stokIdler = kalemEntities.stream()
                .map(IrsaliyeKalem::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> stokAdlari = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, Stok::getAd));

        String cariAdi = i.getCariHesapId() != null
                ? cariHesapRepository.findById(i.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                : null;

        List<IrsaliyeKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> IrsaliyeKalemDTO.builder().id(k.getId())
                        .stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokAdlari.get(k.getStokId()) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim()).build())
                .collect(Collectors.toList());
        return IrsaliyeDTO.builder().id(i.getId()).irsaliyeNo(i.getIrsaliyeNo()).tarih(i.getTarih())
                .cariHesapId(i.getCariHesapId())
                .cariHesapAdi(cariAdi)
                .faturaId(i.getFaturaId()).durum(i.getDurum()).tur(i.getTur())
                .siparisId(i.getSiparisId())
                .aciklama(i.getAciklama()).sirketId(i.getSirketId()).depoId(i.getDepoId())
                .olusturmaTarihi(i.getOlusturmaTarihi()).kalemler(kalemler)
                .plaka(i.getPlaka()).soforId(i.getSoforId())
                .teslimAlan(i.getTeslimAlan()).teslimTarihi(i.getTeslimTarihi())
                .teslimatAdresi(i.getTeslimatAdresi())
                .koliAdedi(i.getKoliAdedi()).desi(i.getDesi()).brutKg(i.getBrutKg())
                .tasiyiciFirma(i.getTasiyiciFirma()).sevkTarihi(i.getSevkTarihi())
                .teslimatId(i.getTeslimatId())
                .build();
    }

    /**
     * Liste DTO'larını toplu olarak zenginleştirir (şoför adı, sipariş no, fatura no,
     * teslimat durumu). Tek tek sorgu atmak yerine id bazlı toplu okuma yapılır.
     */
    private void zenginlestir(List<Irsaliye> kayitlar, List<IrsaliyeDTO> dtolar) {
        if (kayitlar.isEmpty() || kayitlar.size() != dtolar.size()) return;

        Set<Long> soforIdler = kayitlar.stream().map(Irsaliye::getSoforId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> soforAdlari = new HashMap<>();
        if (!soforIdler.isEmpty()) {
            for (Kullanici k : kullaniciRepository.findAllById(soforIdler)) {
                soforAdlari.put(k.getId(),
                        k.getDisplayName() != null ? k.getDisplayName() : k.getUsername());
            }
        }

        Set<Long> siparisIdler = kayitlar.stream().map(Irsaliye::getSiparisId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> siparisNolari = new HashMap<>();
        if (!siparisIdler.isEmpty()) {
            for (Siparis s : siparisRepository.findAllById(siparisIdler)) {
                siparisNolari.put(s.getId(), s.getSiparisNo());
            }
        }

        Set<Long> faturaIdler = kayitlar.stream().map(Irsaliye::getFaturaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> faturaNumaralari = new HashMap<>();
        if (!faturaIdler.isEmpty()) {
            for (Fatura f : faturaRepository.findAllByIdIn(faturaIdler)) {
                faturaNumaralari.put(f.getId(), f.getFaturaNumarasi());
            }
        }

        Set<Long> irsaliyeIdler = kayitlar.stream().map(Irsaliye::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> teslimatDurumlari = new HashMap<>();
        if (!irsaliyeIdler.isEmpty()) {
            for (Teslimat t : teslimatRepository.findByIrsaliyeIdIn(irsaliyeIdler)) {
                teslimatDurumlari.put(t.getIrsaliyeId(), t.getDurum());
            }
        }

        for (int idx = 0; idx < kayitlar.size(); idx++) {
            Irsaliye i = kayitlar.get(idx);
            IrsaliyeDTO dto = dtolar.get(idx);
            if (i.getSoforId() != null) dto.setSoforAd(soforAdlari.get(i.getSoforId()));
            if (i.getSiparisId() != null) dto.setSiparisNo(siparisNolari.get(i.getSiparisId()));
            if (i.getFaturaId() != null) dto.setFaturaNumarasi(faturaNumaralari.get(i.getFaturaId()));
            dto.setTeslimatDurum(teslimatDurumlari.get(i.getId()));
        }
    }
}
