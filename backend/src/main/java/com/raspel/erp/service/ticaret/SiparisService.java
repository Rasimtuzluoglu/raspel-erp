package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.SiparisDTO;
import com.raspel.erp.dto.ticaret.SiparisKalemDTO;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.ticaret.SiparisKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.SiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.EmailService;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.service.sistem.SeriNoServisi;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.ik.PersonelRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SiparisService {

    private final SiparisRepository siparisRepository;
    private final SiparisKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final FaturaService faturaService;
    private final SeriNoServisi seriNoServisi;
    private final BildirimService bildirimService;
        private final EmailService emailService;
    private final TenantChecker tenantChecker;
    private final GorevRepository gorevRepository;
    private final PersonelRepository personelRepository;
    private final KullaniciRepository kullaniciRepository;
    private final com.raspel.erp.service.ticaret.TeslimatService teslimatService;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    private final com.raspel.erp.repository.muhasebe.IrsaliyeRepository irsaliyeRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

    @Transactional(readOnly = true)
    public Page<SiparisDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<Siparis> sayfa = siparisRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable);
        List<Siparis> siparisler = sayfa.getContent();

        // N+1 önlemi: kalemleri ve carileri tek sorguda topla
        List<Long> siparisIdler = siparisler.stream().map(Siparis::getId).collect(Collectors.toList());
        Map<Long, List<SiparisKalem>> kalemHaritasi = siparisIdler.isEmpty() ? Map.of()
                : kalemRepository.findBySiparisIdIn(siparisIdler).stream()
                        .collect(Collectors.groupingBy(SiparisKalem::getSiparisId));
        Set<Long> cariIdler = siparisler.stream().map(Siparis::getCariHesapId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> cariAdlari = cariIdler.isEmpty() ? Map.of()
                : cariHesapRepository.findAllById(cariIdler).stream()
                        .collect(Collectors.toMap(c -> c.getId(), c -> c.getAd()));

        return sayfa.map(s -> entityToDTO(s, kalemHaritasi, cariAdlari));
    }

    @Transactional(readOnly = true)
    public SiparisDTO getir(Long id) {
        Siparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        return entityToDTO(s);
    }

    public SiparisDTO olustur(SiparisDTO dto, Long sirketId) {
        donemService.kilitKontrol(sirketId, dto.getTarih() != null ? dto.getTarih() : java.time.LocalDate.now(), "sipariş oluşturma");
        String siparisNo = dto.getSiparisNo() != null && !dto.getSiparisNo().isBlank()
                ? dto.getSiparisNo()
                : seriNoServisi.siparisNoUret(sirketId);
        String durum = dto.getDurum() != null && !dto.getDurum().isBlank() ? dto.getDurum() : "SIPARIS";
        durumDogrula(durum);
        Siparis s = Siparis.builder()
                .siparisNo(siparisNo).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).tur("SATIS")
                .durum(durum)
                .aciklama(dto.getAciklama())
                .teslimatAdresi(dto.getTeslimatAdresi())
                .sirketId(sirketId)
                .build();
        // Toplamlar istemciye guvenilmez; kalemlerden sunucuda hesaplanir.
        toplamlariHesapla(s, dto.getKalemler());
        s = siparisRepository.save(s);
        if (dto.getKalemler() != null) {
            for (SiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar()).build());
            }
        }
        if (sirketId != null) {
            Long bildirimSirketId = sirketId;
            java.math.BigDecimal bildirimTutar = s.getGenelToplam();
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "SIPARIS",
                    "Yeni Sipariş: " + siparisNo,
                    "Tutar: " + bildirimTutar + " ₺"));
        }
        cacheYardimci.temizle("dashboard");
        return entityToDTO(s);
    }

    public SiparisDTO guncelle(Long id, SiparisDTO dto) {
        Siparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURA_KESILDI".equals(s.getDurum())) {
            throw new BusinessException("Faturası kesilmiş sipariş doğrudan düzenlenemez");
        }
        // Hem mevcut hem yeni tarih kilitli dönemde olmamalı.
        donemService.kilitKontrol(s.getSirketId(), s.getTarih() != null ? s.getTarih() : java.time.LocalDate.now(), "sipariş güncelleme");
        if (dto.getTarih() != null) {
            donemService.kilitKontrol(s.getSirketId(), dto.getTarih(), "sipariş güncelleme");
        }
        s.setSiparisNo(dto.getSiparisNo());
        s.setTarih(dto.getTarih());
        s.setCariHesapId(dto.getCariHesapId());
        if (dto.getTur() != null) s.setTur(dto.getTur());
        if (dto.getDurum() != null) {
            durumDogrula(dto.getDurum());
            s.setDurum(dto.getDurum());
        }
        if (dto.getTeslimatAdresi() != null) s.setTeslimatAdresi(dto.getTeslimatAdresi());
        s.setAciklama(dto.getAciklama());
        if (dto.getKalemler() != null) {
            // Kalemler verildiyse toplamlar sunucuda yeniden hesaplanir (istemciye guvenilmez).
            toplamlariHesapla(s, dto.getKalemler());
        }
        s = siparisRepository.save(s);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteBySiparisId(s.getId());
            for (SiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar()).build());
            }
        }
        cacheYardimci.temizle("dashboard");
        return entityToDTO(s);
    }

    public SiparisDTO durumGuncelle(Long id, String durum) {
        durumDogrula(durum);
        Siparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        String eskiDurum = s.getDurum();

        // Sofurlu siparislerde teslim, imzali teslimat akisiyla tamamlanmalidir; durum
        // menusunden dogrudan TESLIM_EDILDI yapilirsa teslim ispati kaybolur.
        if ("TESLIM_EDILDI".equals(durum) && s.getDriverId() != null && !"TESLIM_EDILDI".equals(eskiDurum)) {
            throw new BusinessException(
                    "Şoförlü siparişlerde imzalı teslim zorunludur. Teslimatı Saha Portalı / Teslimatlar ekranından imza ile tamamlayın.");
        }

        // Sipariş geri alınıyorsa (FATURA_KESILDI -> başka durum): bağlı fatura varsa iptal et,
        // aksi halde yeniden faturalama çift fatura + çift stok düşümüne yol açar.
        if ("FATURA_KESILDI".equals(eskiDurum) && !"FATURA_KESILDI".equals(durum)) {
            var bagliFaturalar = faturaRepository.findBySiparisId(s.getId()).stream()
                    .filter(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL)
                    .collect(java.util.stream.Collectors.toList());
            for (Fatura bf : bagliFaturalar) {
                try {
                    faturaService.faturaDurumGuncelle(bf.getId(), "IPTAL");
                } catch (Exception e) {
                    throw new com.raspel.erp.exception.BusinessException(
                            "Siparişe bağlı fatura iptal edilemedi (" + bf.getFaturaNumarasi() + "): " + e.getMessage());
                }
            }
        }

        if ("FATURA_KESILDI".equals(durum) && !"FATURA_KESILDI".equals(eskiDurum)) {
            // Aynı sipariş için hâlihazırda (iptal olmayan) fatura varsa tekrar kesme.
            boolean mevcutFatura = faturaRepository.findBySiparisId(s.getId()).stream()
                    .anyMatch(f -> f.getDurum() != Fatura.FaturaDurum.IPTAL);
            if (mevcutFatura) {
                throw new com.raspel.erp.exception.BusinessException(
                        "Bu sipariş için zaten fatura kesilmiş. Önce mevcut faturayı iptal edin.");
            }

            // Siparişe bağlı kesilmiş irsaliye varsa fatura ona bağlanır; böylece stok
            // ikinci kez düşülmez (irsaliye stoğu zaten işledi). Birden fazla kesilmiş
            // irsaliye varsa hangisinin faturalanacağı belirsizdir; kullanıcı irsaliyeden
            // fatura kesmelidir.
            var kesilmisIrsaliyeler = irsaliyeRepository
                    .findBySirketIdAndSiparisId(s.getSirketId(), s.getId()).stream()
                    .filter(x -> "KESILDI".equals(x.getDurum()))
                    .collect(java.util.stream.Collectors.toList());
            if (kesilmisIrsaliyeler.size() > 1) {
                throw new com.raspel.erp.exception.BusinessException(
                        "Bu siparişe ait birden fazla kesilmiş irsaliye var; fatura irsaliye ekranından kesilmelidir.");
            }
            Long bagliIrsaliyeId = kesilmisIrsaliyeler.isEmpty() ? null : kesilmisIrsaliyeler.get(0).getId();

            List<SiparisKalem> kalemler = kalemRepository.findBySiparisId(s.getId());
            List<FaturaKalemDTO> faturaKalemler = new java.util.ArrayList<>();
            for (SiparisKalem k : kalemler) {
                faturaKalemler.add(FaturaKalemDTO.builder()
                        .aciklama(k.getAciklama() != null ? k.getAciklama() : "")
                        .adet(k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE)
                        .birimFiyat(k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO)
                        .kdvOrani(k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani)
                        .tutar(k.getTutar() != null ? k.getTutar() : BigDecimal.ZERO)
                        .stokId(k.getStokId())
                        .build());
            }

            FaturaDTO faturaDTO = FaturaDTO.builder()
                    .tarih(java.time.LocalDate.now())
                    .tur("SATIS")
                    .durum("KESILDI")
                    .cariHesapId(s.getCariHesapId())
                    .siparisId(s.getId())
                    .irsaliyeId(bagliIrsaliyeId)
                    // Sipariş dönüşümünde müşteriye otomatik e-posta gönderilmez; kullanıcı
                    // Faturalar ekranından bilinçli olarak "Gönder" diyebilir.
                    .emailGonder(false)
                    .aciklama("Sipariş #" + s.getSiparisNo() + " dönüşümü")
                    .araToplam(s.getAraToplam())
                    .kdv(s.getKdv())
                    .genelToplam(s.getGenelToplam())
                    .kalemler(faturaKalemler)
                    .build();

            FaturaDTO olusanFatura = faturaService.faturaOlustur(faturaDTO, s.getSirketId(), null, null);
            if (bagliIrsaliyeId != null) {
                // İrsaliyeye oluşan fatura bağlanır; irsaliye→fatura dönüşümüyle aynı iz.
                irsaliyeRepository.findById(bagliIrsaliyeId).ifPresent(ir -> {
                    ir.setFaturaId(olusanFatura.getId());
                    irsaliyeRepository.save(ir);
                });
            }
            log.info("Sipariş #{} için fatura oluşturuldu", s.getSiparisNo());
            if (s.getDriverId() != null) {
                try {
                    teslimatService.siparisFaturaBagla(s.getId(), olusanFatura.getId(),
                            olusanFatura.getFaturaNumarasi(), s.getSirketId());
                } catch (Exception e) {
                    log.warn("Sipariş teslimatı faturaya bağlanamadı ({}): {}", s.getSiparisNo(), e.getMessage());
                }
            }
        }

        s.setDurum(durum);
        SiparisDTO sonuc = entityToDTO(siparisRepository.save(s));
        if ("TESLIM_EDILDI".equals(durum)) {
            try {
                teslimatService.siparisTeslimEdildi(s.getId(), s.getSirketId());
            } catch (Exception e) {
                log.warn("Sipariş teslimatı güncellenemedi ({}): {}", s.getSiparisNo(), e.getMessage());
            }
        }
        if (s.getCariHesapId() != null) {
            cariHesapRepository.findById(s.getCariHesapId())
                    .filter(c -> c.getEmail() != null && !c.getEmail().isBlank())
                    .ifPresent(c -> {
                        String eposta = c.getEmail();
                        String siparisNo = s.getSiparisNo();
                        com.raspel.erp.support.AfterCommitExecutor.calistir(() -> emailService.siparisBildirimiGonder(eposta, siparisNo, durum));
                    });
        }
        cacheYardimci.temizle("dashboard");
        return sonuc;
    }

    public void sil(Long id) {
        Siparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        if ("FATURA_KESILDI".equals(s.getDurum())) {
            throw new BusinessException("Faturası kesilmiş sipariş doğrudan silinemez");
        }
        // Bağlı (iptal olmayan) irsaliye varsa sipariş silinemez; aksi halde irsaliye
        // siparişsiz kalır ve stok/cari izi kopar.
        boolean bagliIrsaliyeVar = irsaliyeRepository
                .findBySirketIdAndSiparisId(s.getSirketId(), s.getId()).stream()
                .anyMatch(x -> !"IPTAL".equals(x.getDurum()));
        if (bagliIrsaliyeVar) {
            throw new BusinessException("Bu siparişe bağlı irsaliye var; önce irsaliyeyi iptal edin.");
        }
        kalemRepository.deleteBySiparisId(id);
        siparisRepository.deleteById(id);
        cacheYardimci.temizle("dashboard");
    }

    /**
     * Siparişten iş emri oluşturur ve bir personele atar.
     */
    public Gorev isEmriOlustur(Long siparisId, Long personelId, String aciklama) {
        Siparis s = siparisRepository.findById(siparisId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", siparisId));
        tenantChecker.check(s.getSirketId(), "Sipariş");

        String personelAd = null;
        if (personelId != null) {
            personelAd = personelRepository.findById(personelId)
                    .map(p -> (p.getAd() != null ? p.getAd() : "") + " " + (p.getSoyad() != null ? p.getSoyad() : ""))
                    .orElse(null);
        }

        Gorev gorev = Gorev.builder()
                .ad("İş Emri: " + s.getSiparisNo())
                .aciklama(aciklama != null ? aciklama : "Sipariş #" + s.getSiparisNo() + " için iş emri")
                .durum("YAPILACAK")
                .atanan(personelAd != null ? personelAd.trim() : null)
                .baslangic(java.time.LocalDate.now())
                .siparisId(siparisId)
                .personelId(personelId)
                .build();
        Gorev kaydedilen = gorevRepository.save(gorev);
        log.info("Sipariş #{} için iş emri oluşturuldu: {}", s.getSiparisNo(), kaydedilen.getId());
        return kaydedilen;
    }

    private SiparisDTO entityToDTO(Siparis s) {
        List<SiparisKalem> kalemEntities = kalemRepository.findBySiparisId(s.getId());
        Map<Long, String> stokAdlari = stokAdlariYukle(kalemEntities);
        String cariAdi = cariAdi(s);
        Map<Long, List<SiparisKalem>> kalemHaritasi = Map.of(s.getId(), kalemEntities);
        Map<Long, String> cariAdlari = cariAdi != null ? Map.of(s.getCariHesapId(), cariAdi) : Map.of();
        return entityToDTO(s, kalemHaritasi, cariAdlari, stokAdlari);
    }

    private SiparisDTO entityToDTO(Siparis s, Map<Long, List<SiparisKalem>> kalemHaritasi, Map<Long, String> cariAdlari) {
        Map<Long, String> stokAdlari = stokAdlariYukle(kalemHaritasi.getOrDefault(s.getId(), List.of()));
        return entityToDTO(s, kalemHaritasi, cariAdlari, stokAdlari);
    }

    private Map<Long, String> stokAdlariYukle(List<SiparisKalem> kalemEntities) {
        List<Long> stokIdler = kalemEntities.stream()
                .map(SiparisKalem::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, Stok::getAd));
    }

    private String cariAdi(Siparis s) {
        return s.getCariHesapId() != null
                ? cariHesapRepository.findById(s.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                : null;
    }

    private SiparisDTO entityToDTO(Siparis s, Map<Long, List<SiparisKalem>> kalemHaritasi, Map<Long, String> cariAdlari, Map<Long, String> stokAdlari) {
        List<SiparisKalem> kalemEntities = kalemHaritasi.getOrDefault(s.getId(), List.of());
        String cariAdi = s.getCariHesapId() != null ? cariAdlari.get(s.getCariHesapId()) : null;

        List<SiparisKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> SiparisKalemDTO.builder().id(k.getId()).siparisId(k.getSiparisId())
                        .stokId(k.getStokId()).stokAdi(k.getStokId() != null ? stokAdlari.get(k.getStokId()) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim())
                        .birimFiyat(k.getBirimFiyat()).kdvOrani(k.getKdvOrani()).tutar(k.getTutar()).build())
                .collect(Collectors.toList());
        return SiparisDTO.builder().id(s.getId()).siparisNo(s.getSiparisNo()).tarih(s.getTarih())
                .cariHesapId(s.getCariHesapId()).cariHesapAdi(cariAdi)
                .tur(s.getTur()).durum(s.getDurum()).aciklama(s.getAciklama())
                .teslimatAdresi(s.getTeslimatAdresi())
                .driverId(s.getDriverId()).driverAd(s.getDriverAd())
                .araToplam(s.getAraToplam()).kdv(s.getKdv()).genelToplam(s.getGenelToplam())
                .sirketId(s.getSirketId()).olusturmaTarihi(s.getOlusturmaTarihi()).kalemler(kalemler).build();
    }

    /**
     * Siparişe şoför atar ve bu atamayı Teslimatlar'a da yansıtır (sipariş bazlı teslimat oluşturur/günceller).
     */
    @Transactional
    public SiparisDTO soforAta(Long id, Long driverId, Long sirketId) {
        Siparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");

        String driverAd = null;
        if (driverId != null) {
            Kullanici surucu = kullaniciRepository.findById(driverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", driverId));
            if (!"DRIVER".equalsIgnoreCase(surucu.getRole())) {
                throw new BusinessException("Seçilen kullanıcı şoför (DRIVER) değil");
            }
            driverAd = surucu.getDisplayName() != null ? surucu.getDisplayName() : surucu.getUsername();
        }
        s.setDriverId(driverId);
        s.setDriverAd(driverAd);
        s = siparisRepository.save(s);

        if (driverId != null) {
            String musteriAdi = null;
            String adres = null;
            if (s.getCariHesapId() != null) {
                CariHesap cari = cariHesapRepository.findById(s.getCariHesapId()).orElse(null);
                if (cari != null) {
                    musteriAdi = cari.getAd();
                    adres = cari.getAdres();
                }
            }
            try {
                teslimatService.siparisTeslimatiUpsert(s.getId(), driverId, sirketId, musteriAdi, adres);
            } catch (Exception e) {
                log.warn("Sipariş teslimatı oluşturulamadı ({}): {}", s.getSiparisNo(), e.getMessage());
            }
            String bildirimSiparisNo = s.getSiparisNo();
            String bildirimSoforAd = driverAd != null ? driverAd : "";
            Long bildirimSirketId = sirketId;
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "TESLIMAT",
                    "Şoför atandı: " + bildirimSiparisNo,
                    "Şoför: " + bildirimSoforAd));
        }
        return entityToDTO(s);
    }

    /** Gecerli siparis durumlari. Serbest metin kabul edilmez; rapor/onay sayaclari bozulmaz. */
    private static final java.util.Set<String> GECERLI_DURUMLAR = java.util.Set.of(
            "TEKLIF", "SIPARIS", "BEKLIYOR", "HAZIRLANIYOR", "YOLDA",
            "TESLIM_EDILDI", "FATURA_KESILDI", "IPTAL");

    private void durumDogrula(String durum) {
        if (durum == null || !GECERLI_DURUMLAR.contains(durum)) {
            throw new BusinessException("Geçersiz sipariş durumu: " + durum);
        }
    }

    /**
     * Siparis toplamlarini kalemlerden KDV-dahil kanonik modelle (FaturaTutar) hesaplar
     * ve kalem tutarlarini yazar. Bos kalem listesi reddedilir; boylece sifir tutarli
     * siparis/fatura olusmaz.
     */
    private void toplamlariHesapla(Siparis s, List<SiparisKalemDTO> kalemler) {
        if (kalemler == null || kalemler.isEmpty()) {
            throw new BusinessException("Siparişe en az bir kalem eklenmelidir");
        }
        List<com.raspel.erp.util.FaturaTutar.Satir> satirlar = new java.util.ArrayList<>();
        for (SiparisKalemDTO k : kalemler) {
            BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ONE;
            if (miktar.signum() <= 0) {
                throw new BusinessException("Kalem miktarı 0'dan büyük olmalıdır");
            }
            BigDecimal birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
            com.raspel.erp.util.FaturaTutar.Satir satir =
                    com.raspel.erp.util.FaturaTutar.satir(birimFiyat, miktar, BigDecimal.ZERO, kdvOrani);
            satirlar.add(satir);
            k.setMiktar(miktar);
            k.setKdvOrani(kdvOrani);
            k.setTutar(satir.brut());
        }
        com.raspel.erp.util.FaturaTutar.Belge belge =
                com.raspel.erp.util.FaturaTutar.belge(satirlar, BigDecimal.ZERO);
        s.setAraToplam(belge.araToplam());
        s.setKdv(belge.kdv());
        s.setGenelToplam(belge.genelToplam());
    }
}