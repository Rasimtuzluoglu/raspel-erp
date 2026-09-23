package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.CariSonUrunDTO;
import com.raspel.erp.dto.ticaret.CariUrunFiyatDTO;
import com.raspel.erp.repository.ticaret.CariUrunFiyatProjeksiyon;
import com.raspel.erp.dto.envanter.StokFiyatGecmisiDTO;
import com.raspel.erp.repository.ticaret.StokFiyatGecmisiProjeksiyon;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.TcmbKurService;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.sistem.EmailService;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.CariSonUrunProjeksiyon;
import com.raspel.erp.service.sistem.PdfRaporService;
import com.raspel.erp.service.sistem.SeriNoServisi;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final CariHesapService cariHesapService;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final DepoRepository depoRepository;
    private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    private final SeriNoServisi seriNoServisi;
    private final BildirimService bildirimService;
        private final EmailService emailService;
    private final PdfRaporService pdfRaporService;
    private final SirketRepository sirketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final TcmbKurService tcmbKurService;
    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    private final com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.repository.muhasebe.IrsaliyeRepository irsaliyeRepository;
    private final FaturaGecmisService faturaGecmisService;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.service.ticaret.IskontoMotoruService iskontoMotoruService;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

    @Transactional(readOnly = true)
    public Page<FaturaDTO> tumFaturalariGetir(Long sirketId, Pageable pageable) {
        return sayfaDTOyaCevir(faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable));
    }

    @Transactional(readOnly = true)
    public Page<FaturaDTO> ara(Long sirketId, String q, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return sayfaDTOyaCevir(faturaRepository.ara(sirketId, like, pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "faturalar", key = "T(com.raspel.erp.config.TenantChecker).tenantKey(#id)")
    public FaturaDTO faturaGetir(Long id) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        return entityDTOyeCevir(fatura);
    }

    /**
     * Bir cari hesabin son faturasini dondurur (kopyalama icin). Fatura yoksa null.
     */
    @Transactional(readOnly = true)
    public FaturaDTO cariSonFatura(Long cariId, Long sirketId) {
        return faturaRepository.findTopByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(cariId, sirketId)
                .map(this::entityDTOyeCevir)
                .orElse(null);
    }

    /**
     * Bir cari hesaba ait tüm faturaları (fişleri) sayfalı olarak döndürür.
     */
    @Transactional(readOnly = true)
    public Page<FaturaDTO> cariFaturalari(Long cariId, Long sirketId, Pageable pageable) {
        return sayfaDTOyaCevir(faturaRepository.findByCariHesapIdAndSirketIdOrderByTarihDesc(cariId, sirketId, pageable));
    }

    /**
     * Bir cari hesabın belirli bir ürünü geçmişte aldığı fiyatları döndürür.
     */
    @Transactional(readOnly = true)
    public CariUrunFiyatDTO cariUrunFiyatGecmisi(Long cariId, Long stokId, Long sirketId) {
        List<CariUrunFiyatProjeksiyon> gecmis = faturaKalemRepository.cariUrunFiyatGecmisi(
                cariId, stokId, sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI);

        List<CariUrunFiyatDTO.Kayit> kayitlar = gecmis.stream()
                .map(p -> CariUrunFiyatDTO.Kayit.builder()
                        .birimFiyat(p.getBirimFiyat())
                        .tarih(p.getTarih())
                        .faturaNumarasi(p.getFaturaNumarasi())
                        .adet(p.getAdet())
                        .build())
                .collect(Collectors.toList());

        BigDecimal sonFiyat = kayitlar.isEmpty() ? null : kayitlar.get(0).getBirimFiyat();
        return CariUrunFiyatDTO.builder()
                .gecmis(kayitlar)
                .sonFiyat(sonFiyat)
                .build();
    }

    /**
     * Cari hesabin son aldigi urunleri dondurur (SATIS + KESILDI faturalar).
     * En son alim tarihine gore siralanir.
     */
    @Transactional(readOnly = true)
    public List<CariSonUrunDTO> cariSonUrunler(Long cariId, Long sirketId, int limit) {
        List<CariSonUrunProjeksiyon> projeksiyonlar = faturaKalemRepository.cariSonUrunler(
                cariId, sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI);
        if (projeksiyonlar.isEmpty()) return List.of();

        int gercekLimit = Math.min(Math.max(limit, 1), 50);
        List<CariSonUrunProjeksiyon> sinirli = projeksiyonlar.stream()
                .limit(gercekLimit)
                .collect(Collectors.toList());

        List<Long> stokIdler = sinirli.stream()
                .map(CariSonUrunProjeksiyon::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Stok> stokMap = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s));

        // "Son fiyat": MAX(birimFiyat) yerine tarihe gore en yeni kalem fiyati.
        Map<Long, java.math.BigDecimal> sonFiyatMap = faturaKalemRepository.cariSonUrunFiyatlari(
                        cariId, sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI).stream()
                .filter(p -> p.getStokId() != null)
                .collect(Collectors.toMap(
                        com.raspel.erp.repository.ticaret.CariUrunSonFiyatProjeksiyon::getStokId,
                        com.raspel.erp.repository.ticaret.CariUrunSonFiyatProjeksiyon::getBirimFiyat,
                        (a, b) -> a));

        return sinirli.stream().map(p -> {
            Stok stok = stokMap.get(p.getStokId());
            return CariSonUrunDTO.builder()
                    .stokId(p.getStokId())
                    .stokKodu(stok != null ? stok.getStokKodu() : null)
                    .stokAd(stok != null ? stok.getAd() : null)
                    .sonAlisTarihi(p.getSonAlisTarihi())
                    .sonBirimFiyat(sonFiyatMap.getOrDefault(p.getStokId(), p.getSonBirimFiyat()))
                    .adet(p.getAdet())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Bir stogun son 5 alis fiyatini ve trend yonunu dondurur.
     */
    @Transactional(readOnly = true)
    public StokFiyatGecmisiDTO stokFiyatGecmisi(Long stokId, Long sirketId) {
        List<StokFiyatGecmisiProjeksiyon> gecmis = faturaKalemRepository.stokFiyatGecmisi(
                stokId, sirketId, Fatura.FaturaTur.ALIS, Fatura.FaturaDurum.KESILDI);

        BigDecimal guncelFiyat = stokRepository.findById(stokId)
                .map(s -> {
                    tenantChecker.check(s.getSirketId(), "Stok");
                    if (sirketId != null && s.getSirketId() != null && !sirketId.equals(s.getSirketId())) {
                        throw new ResourceNotFoundException("Stok bu sirkete ait degil");
                    }
                    return s.getFiyat();
                })
                .orElse(null);

        List<StokFiyatGecmisiDTO.Kayit> kayitlar = gecmis.stream()
                .limit(5)
                .map(p -> StokFiyatGecmisiDTO.Kayit.builder()
                        .birimFiyat(p.getBirimFiyat())
                        .tarih(p.getTarih())
                        .faturaNumarasi(p.getFaturaNumarasi())
                        .build())
                .collect(Collectors.toList());

        String trend = "STABIL";
        if (kayitlar.size() >= 2) {
            BigDecimal ilk = kayitlar.get(kayitlar.size() - 1).getBirimFiyat();
            BigDecimal son = kayitlar.get(0).getBirimFiyat();
            if (ilk != null && son != null && ilk.compareTo(BigDecimal.ZERO) != 0) {
                int karsilastirma = son.compareTo(ilk);
                if (karsilastirma > 0) trend = "ARTIS";
                else if (karsilastirma < 0) trend = "AZALIS";
            }
        }

        return StokFiyatGecmisiDTO.builder()
                .gecmis(kayitlar)
                .guncelFiyat(guncelFiyat)
                .trend(trend)
                .build();
    }

    public FaturaDTO faturaOlustur(FaturaDTO dto, Long sirketId, Long kullaniciId, String displayName) {
        log.info("Fatura oluşturuluyor - Tür: {}, sirketId: {}", dto.getTur(), sirketId);

        // Kilitli döneme ait tarihli belge oluşturulamaz.
        java.time.LocalDate faturaTarihi = dto.getTarih() != null ? dto.getTarih() : java.time.LocalDate.now();
        donemService.kilitKontrol(sirketId, faturaTarihi, "fatura oluşturma");

        CariHesap cariHesap = null;
        if (dto.getCariHesapId() != null) {
            cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", dto.getCariHesapId()));
            tenantChecker.check(cariHesap.getSirketId(), "Cari hesap");
            if (sirketId != null && cariHesap.getSirketId() != null && !sirketId.equals(cariHesap.getSirketId())) {
                throw new ResourceNotFoundException("Cari hesap bu sirkete ait degil");
            }
        }

        Fatura.FaturaTur tur;
        try {
            tur = Fatura.FaturaTur.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz fatura türü: " + dto.getTur());
        }

        String faturaNo = dto.getFaturaNumarasi() != null && !dto.getFaturaNumarasi().isBlank()
                ? dto.getFaturaNumarasi()
                : seriNoServisi.faturaNoUret(sirketId);

        List<Long> stokIdler = dto.getKalemler().stream()
                .map(FaturaKalemDTO::getStokId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Stok> stokHaritasi = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s, (a, b) -> a));
        // Güvenlik: çözülen stokların tamamı isteğin şirketine ait olmalıdır.
        for (Stok s : stokHaritasi.values()) {
            tenantChecker.check(s.getSirketId(), "Stok");
            if (sirketId != null && s.getSirketId() != null && !sirketId.equals(s.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
        }
        Map<Long, BigDecimal> agirlikHaritasi = stokHaritasi.values().stream()
                .filter(s -> s.getAgirlik() != null)
                .collect(Collectors.toMap(Stok::getId, Stok::getAgirlik));

        // Kalemde iskonto belirtilmediyse kademeli iskonto motorundan oran çözülür.
        java.time.LocalDate iskontoTarihi = dto.getTarih() != null ? dto.getTarih() : LocalDate.now();
        Long cariId = cariHesap != null ? cariHesap.getId() : null;
        // Birim fiyat KDV DAHİL kabul edilir; KDV matrahtan ayrıştırılır (FaturaTutar).
        List<FaturaKalem> kalemler = new ArrayList<>();
        List<com.raspel.erp.util.FaturaTutar.Satir> satirlar = new ArrayList<>();
        for (FaturaKalemDTO k : dto.getKalemler()) {
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
            BigDecimal iskontoOrani = k.getIskontoOrani();
            if (iskontoOrani == null) {
                Stok stok = k.getStokId() != null ? stokHaritasi.get(k.getStokId()) : null;
                iskontoOrani = iskontoMotoruService.iskontoHesapla(
                        sirketId, k.getStokId(), cariId,
                        stok != null ? stok.getKategori() : null,
                        k.getAdet(), iskontoTarihi);
                if (iskontoOrani == null) iskontoOrani = BigDecimal.ZERO;
            }
            com.raspel.erp.util.FaturaTutar.Satir satir = com.raspel.erp.util.FaturaTutar.satir(
                    k.getBirimFiyat(), k.getAdet(), iskontoOrani, kdvOrani);
            satirlar.add(satir);
            kalemler.add(FaturaKalem.builder()
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(kdvOrani)
                    .iskontoOrani(iskontoOrani)
                    .tutar(satir.brut())
                    .stokId(k.getStokId())
                    .agirlik(k.getStokId() != null ? agirlikHaritasi.get(k.getStokId()) : null)
                    .build());
        }

        // Genel iskonto: öncelik genelIskontoTutari, yoksa (POS) indirim alanı.
        BigDecimal genelIskonto = dto.getGenelIskontoTutari() != null ? dto.getGenelIskontoTutari()
                : (dto.getIndirim() != null ? dto.getIndirim() : BigDecimal.ZERO);
        com.raspel.erp.util.FaturaTutar.Belge belge = com.raspel.erp.util.FaturaTutar.belge(satirlar, genelIskonto);
        BigDecimal araToplam = belge.araToplam();
        BigDecimal kdv = belge.kdv();
        BigDecimal genelToplam = belge.genelToplam();

        BigDecimal odenenTutar = dto.getOdenenTutar() != null ? dto.getOdenenTutar() : BigDecimal.ZERO;
        if (odenenTutar.signum() < 0) odenenTutar = BigDecimal.ZERO;
        if (odenenTutar.compareTo(genelToplam) > 0) odenenTutar = genelToplam;
        BigDecimal kalanTutar = genelToplam.subtract(odenenTutar);
        // Ödeme durumu her zaman hesaplanır; istemciden gelen değere güvenilmez.
        String odemeDurumu = kalanTutar.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                : odenenTutar.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI";

        Fatura.FaturaDurum faturaDurum;
        try {
            faturaDurum = dto.getDurum() != null
                    ? Fatura.FaturaDurum.valueOf(dto.getDurum().toUpperCase())
                    : Fatura.FaturaDurum.TASLAK;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz durum: " + dto.getDurum());
        }

        Fatura fatura = Fatura.builder()
                .faturaNumarasi(faturaNo)
                .tarih(dto.getTarih() != null ? dto.getTarih() : LocalDate.now())
                .vadeTarihi(vadeTarihiHesapla(dto, cariHesap))
                .tur(tur)
                .durum(faturaDurum)
                .cariHesap(cariHesap)
                .aciklama(dto.getAciklama())
                .araToplam(araToplam)
                .kdv(kdv)
                .genelToplam(genelToplam)
                .genelIskontoTutari(genelIskonto)
                .odemeDurumu(odemeDurumu)
                .odenenTutar(odenenTutar)
                .kalanTutar(kalanTutar)
                .sirketId(sirketId)
                .olusturanKullaniciId(kullaniciId)
                .olusturanKullaniciAdi(displayName)
                .teslimEden(dto.getTeslimEden())
                .teslimDurumu(dto.getTeslimDurumu() != null ? dto.getTeslimDurumu() : "BEKLIYOR")
                .teslimNotu(dto.getTeslimNotu())
                .teslimFotograf(dto.getTeslimFotograf())
                .depoId(dto.getDepoId())
                .paraBirimi(dto.getParaBirimi() != null ? dto.getParaBirimi() : "TRY")
                .odemeYontemi(dto.getOdemeYontemi())
                .taksitKurum(dto.getTaksitKurum())
                .taksitTutar(dto.getTaksitTutar())
                .kasaId(dto.getKasaId())
                .bankaId(dto.getBankaId())
                .kartaBankaAktar(dto.getKartaBankaAktar())
                .irsaliyeId(dto.getIrsaliyeId())
                .build();

        kalemler.forEach(k -> k.setFatura(fatura));
        fatura.setKalemler(kalemler);

        Fatura kaydedilen = faturaRepository.save(fatura);

        faturaGecmisService.kaydet(kaydedilen, FaturaGecmisService.OLUSTUR,
                "Fatura oluşturuldu (" + kaydedilen.getDurum() + ")",
                null, faturaGecmisService.snapshot(kaydedilen));

        if (faturaDurum == Fatura.FaturaDurum.KESILDI) {
            // İrsaliye zaten stok çıkışı yaptıysa fatura tekrar düşmemeli (çift düşüm önlenir).
            if (!irsaliyeStokIslenmisMi(dto.getIrsaliyeId())) {
                List<Long> kritik = stokHareketleriIsle(fatura, stokYonu(tur), "Fatura #" + fatura.getFaturaNumarasi());
                if (tur == Fatura.FaturaTur.SATIS) {
                    kritikStokUyarisiGonder(kritik, sirketId);
                }
            } else {
                log.info("Fatura #{} irsaliye #{} üzerinden oluşturuldu; stok hareketi irsaliyede yapıldığı için tekrar düşülmedi",
                        fatura.getFaturaNumarasi(), dto.getIrsaliyeId());
            }
            cariBakiyeGuncelle(fatura, false);
        }

        // E-posta, DB transaction'ı commit edildikten SONRA gönderilir; boylece SMTP
        // gecikmesi transaction'i/kilitleri acik tutmaz. (Testte aktif transaction
        // yoksa AfterCommitExecutor gorevi hemen calistirir.)
        String emailGonderimDurumu = null;
        if (sirketId != null && cariHesap != null && cariHesap.getEmail() != null && !cariHesap.getEmail().isBlank()) {
            final String[] durum = new String[1];
            final String emailAdres = cariHesap.getEmail();
            final String emailFaturaNo = faturaNo;
            final String emailTutar = genelToplam.toString();
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> {
                boolean gonderildi = emailService.faturaBildirimiGonder(emailAdres, emailFaturaNo, emailTutar);
                durum[0] = gonderildi ? "GONDERILDI" : "GONDERILEMEDI";
                if (!gonderildi) {
                    log.warn("Fatura bildirim e-postası gönderilemedi (SMTP yapılandırılmamış veya hata): {}", emailFaturaNo);
                }
            });
            emailGonderimDurumu = durum[0] != null ? durum[0] : "GONDERILIYOR";
        }

        log.info("Fatura oluşturuldu - No: {}, ID: {}", faturaNo, kaydedilen.getId());
        if (sirketId != null) {
            Long bildirimSirketId = sirketId;
            String bildirimCariAd = cariHesap != null ? cariHesap.getAd() : null;
            java.math.BigDecimal bildirimTutar = genelToplam;
            com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "FATURA",
                    "Yeni Fatura: " + faturaNo,
                    "Tutar: " + bildirimTutar + " ₺" + (bildirimCariAd != null ? " - " + bildirimCariAd : ""),
                    displayName));
        }

        // Tahsilat yapılmışsa seçili hesaba giriş işle (kasa öncelikli).
        if (odenenTutar.compareTo(BigDecimal.ZERO) > 0) {
            if (kaydedilen.getKasaId() != null) {
                kasaGirisi(kaydedilen, odenenTutar);
            } else if (kaydedilen.getBankaId() != null) {
                // HAVALE ve (POS gün sonu dışı) KART tahsilatı banka hesabına aktarılır.
                bankaGirisi(kaydedilen, odenenTutar);
            }
        }
        FaturaDTO sonuc = entityDTOyeCevir(kaydedilen);
        sonuc.setEmailGonderimDurumu(emailGonderimDurumu);
        return sonuc;
    }

    /**
     * Tahsil edilen tutarı seçili kasaya giriş olarak işler.
     * Hata durumunda sessizce yutulmaz: kasa seçilip tahsilat yapıldıysa
     * kasa hareketi kaydedilmezse fatura oluşturma işlemi geri alınır.
     */
    /**
     * Tahsil edilen tutarı seçili banka hesabına alacak olarak işler (KART doğrudan aktarım).
     * Kasa ile aynı yaklaşımla idempotent değildir; yalnızca fatura oluşturmada bir kez çağrılır.
     */
    private void bankaGirisi(Fatura fatura, BigDecimal tutar) {
        com.raspel.erp.entity.finans.Banka banka = bankaRepository.findByIdForUpdate(fatura.getBankaId())
                .orElseThrow(() -> new BusinessException("Banka bulunamadı: " + fatura.getBankaId()));
        tenantChecker.check(banka.getSirketId(), "Banka");
        banka.setBakiye(banka.getBakiye() != null ? banka.getBakiye().add(tutar) : tutar);
        bankaRepository.save(banka);
        bankaHareketiRepository.save(com.raspel.erp.entity.finans.BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(fatura.getTarih() != null ? fatura.getTarih() : LocalDate.now())
                .aciklama("Satış (KART): " + fatura.getFaturaNumarasi())
                .borc(BigDecimal.ZERO)
                .alacak(tutar)
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(fatura.getSirketId())
                .build());
    }

    private void kasaGirisi(Fatura fatura, BigDecimal tutar) {
        Kasa kasa = kasaRepository.findByIdForUpdate(fatura.getKasaId())
                .orElseThrow(() -> new BusinessException("Kasa bulunamadı: " + fatura.getKasaId()));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setBakiye(kasa.getBakiye().add(tutar));
        kasaRepository.save(kasa);
        kasaHareketRepository.save(KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(tutar)
                .hareketTarihi(fatura.getTarih())
                .aciklama("Satış: " + fatura.getFaturaNumarasi())
                .build());
    }

    /** Fatura PDF'ini cari hesabın e-posta adresine gönderir. Gönderilemezse hata fırlatır. */
    public void gonderEmail(Long id) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        if (fatura.getCariHesap() == null || fatura.getCariHesap().getEmail() == null
                || fatura.getCariHesap().getEmail().isBlank()) {
            throw new BusinessException("Bu faturanın cari hesabında e-posta adresi tanımlı değil");
        }
        byte[] pdf = pdfRaporService.faturaRaporu(id);
        boolean gonderildi = emailService.faturaPdfGonder(
                fatura.getCariHesap().getEmail(),
                pdf,
                fatura.getFaturaNumarasi(),
                fatura.getGenelToplam() != null ? fatura.getGenelToplam().toString() : "0.00",
                fatura.getCariHesap().getAd());
        if (!gonderildi) {
            throw new BusinessException("E-posta gönderilemedi: SMTP yapılandırılmamış veya gönderim hatası");
        }
        try {
            if (fatura.getSirketId() != null) {
                Long bildirimSirketId = fatura.getSirketId();
                String alici = fatura.getCariHesap().getEmail();
                com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "FATURA",
                        "Fatura e-posta ile gönderildi: " + fatura.getFaturaNumarasi(),
                        "Alıcı: " + alici));
            }
        } catch (Exception e) {
            log.warn("Fatura gönderim bildirimi başarısız: {}", e.getMessage());
        }
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public FaturaDTO faturaDurumGuncelle(Long id, String yeniDurum) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        donemService.kilitKontrol(fatura.getSirketId(), fatura.getTarih(), "fatura durum güncelleme");

        Fatura.FaturaDurum durum;
        try {
            durum = Fatura.FaturaDurum.valueOf(yeniDurum.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz durum: " + yeniDurum);
        }

        String eskiDurum = fatura.getDurum() != null ? fatura.getDurum().name() : null;
        String durumOncekiSnapshot = faturaGecmisService.snapshot(fatura);

        if (fatura.getDurum() == Fatura.FaturaDurum.IPTAL) {
            throw new BusinessException("İptal edilmiş fatura güncellenemez");
        }

        // Kesilmiş faturadan geri donus (TASLAK/IPTAL): stok ve cari etkisi geri alinir.
        boolean geriAliniyor = fatura.getDurum() == Fatura.FaturaDurum.KESILDI
                && (durum == Fatura.FaturaDurum.TASLAK || durum == Fatura.FaturaDurum.IPTAL);

        // Ödeme yapılmış fatura geri alınamaz/iptal edilemez (bakiye/tahsilat tutarsızlığı önlenir)
        if (geriAliniyor
                && fatura.getOdenenTutar() != null
                && fatura.getOdenenTutar().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Ödeme yapılmış fatura geri alınamaz/iptal edilemez. Önce tahsilat/ödeme hareketlerini silin.");
        }

        if (durum == Fatura.FaturaDurum.KESILDI && fatura.getDurum() != Fatura.FaturaDurum.KESILDI) {
            List<Long> kritik = stokHareketleriIsle(fatura, stokYonu(fatura.getTur()), "Fatura #" + fatura.getFaturaNumarasi());
            cariBakiyeGuncelle(fatura, false);
            if (fatura.getTur() == Fatura.FaturaTur.SATIS) {
                kritikStokUyarisiGonder(kritik, fatura.getSirketId());
            }
        } else if (geriAliniyor) {
            stokHareketleriIsle(fatura, tersStokYonu(fatura.getTur()), "Fatura geri alındı #" + fatura.getFaturaNumarasi());
            cariBakiyeGuncelle(fatura, true);
        }

        fatura.setDurum(durum);
        Fatura guncellenen = faturaRepository.save(fatura);
        log.info("Fatura durumu güncellendi - ID: {}, Durum: {}", id, durum);
        faturaGecmisService.kaydet(guncellenen, FaturaGecmisService.DURUM,
                "Durum: " + eskiDurum + " → " + durum,
                durumOncekiSnapshot, faturaGecmisService.snapshot(guncellenen));
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public FaturaDTO faturaGuncelle(Long id, FaturaDTO dto) {
        log.info("Fatura düzenleniyor - ID: {}", id);
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        donemService.kilitKontrol(fatura.getSirketId(), fatura.getTarih(), "fatura düzenleme");
        // Yeni tarih de kilitli döneme denk gelmemeli (eski tarih kontrolü tek başına yetmez).
        if (dto.getTarih() != null) {
            donemService.kilitKontrol(fatura.getSirketId(), dto.getTarih(), "fatura düzenleme");
        }

        if (fatura.getDurum() == Fatura.FaturaDurum.IPTAL) {
            throw new BusinessException("İptal edilmiş fatura düzenlenemez");
        }
        boolean kesilmisti = fatura.getDurum() == Fatura.FaturaDurum.KESILDI;

        // Ödeme (tahsilat) yapılmış kesilmiş fatura revize edilemez (bakiye tutarsızlığı önlenir)
        if (kesilmisti && fatura.getOdenenTutar() != null
                && fatura.getOdenenTutar().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Ödeme yapılmış fatura revize edilemez. Önce tahsilat/ödeme hareketlerini silin.");
        }

        String duzenlemeOncekiSnapshot = faturaGecmisService.snapshot(fatura);

        // Kesilmiş faturanın eski durumu (stok + bakiye geri alma için)
        Map<Long, BigDecimal> eskiMiktarlar = new HashMap<>();
        BigDecimal eskiGenelToplam = fatura.getGenelToplam();
        Fatura.FaturaTur eskiTur = fatura.getTur();
        String eskiParaBirimi = fatura.getParaBirimi();
        Long eskiCariId = fatura.getCariHesap() != null ? fatura.getCariHesap().getId() : null;
        if (kesilmisti) {
            for (FaturaKalem k : fatura.getKalemler()) {
                if (k.getStokId() == null) continue;
                eskiMiktarlar.merge(k.getStokId(), (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO), BigDecimal::add);
            }
        }

        CariHesap cariHesap = null;
        if (dto.getCariHesapId() != null) {
            cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", dto.getCariHesapId()));
            tenantChecker.check(cariHesap.getSirketId(), "Cari hesap");
            if (fatura.getSirketId() != null && cariHesap.getSirketId() != null && !fatura.getSirketId().equals(cariHesap.getSirketId())) {
                throw new ResourceNotFoundException("Cari hesap bu sirkete ait degil");
            }
        }

        Fatura.FaturaTur tur;
        try {
            tur = Fatura.FaturaTur.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz fatura türü: " + dto.getTur());
        }

        List<Long> stokIdler = dto.getKalemler().stream()
                .map(FaturaKalemDTO::getStokId)
                .filter(sid -> sid != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, BigDecimal> agirlikHaritasi = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .filter(s -> s.getAgirlik() != null)
                        .collect(Collectors.toMap(Stok::getId, Stok::getAgirlik));
        // Güvenlik: güncellenen faturanın şirketi dışındaki stoklar reddedilir.
        for (Stok s : stokRepository.findAllById(stokIdler)) {
            tenantChecker.check(s.getSirketId(), "Stok");
            if (fatura.getSirketId() != null && s.getSirketId() != null && !fatura.getSirketId().equals(s.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
        }

        // Birim fiyat KDV DAHİL kabul edilir; KDV matrahtan ayrıştırılır (FaturaTutar).
        List<FaturaKalem> yeniKalemler = new ArrayList<>();
        List<com.raspel.erp.util.FaturaTutar.Satir> yeniSatirlar = new ArrayList<>();
        for (FaturaKalemDTO k : dto.getKalemler()) {
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
            BigDecimal iskontoOrani = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
            com.raspel.erp.util.FaturaTutar.Satir satir = com.raspel.erp.util.FaturaTutar.satir(
                    k.getBirimFiyat(), k.getAdet(), iskontoOrani, kdvOrani);
            yeniSatirlar.add(satir);
            yeniKalemler.add(FaturaKalem.builder()
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(kdvOrani)
                    .iskontoOrani(iskontoOrani)
                    .tutar(satir.brut())
                    .stokId(k.getStokId())
                    .agirlik(k.getStokId() != null ? agirlikHaritasi.get(k.getStokId()) : null)
                    .build());
        }

        BigDecimal genelIskonto = dto.getGenelIskontoTutari() != null ? dto.getGenelIskontoTutari()
                : (dto.getIndirim() != null ? dto.getIndirim() : BigDecimal.ZERO);
        com.raspel.erp.util.FaturaTutar.Belge belge = com.raspel.erp.util.FaturaTutar.belge(yeniSatirlar, genelIskonto);
        BigDecimal araToplam = belge.araToplam();
        BigDecimal kdv = belge.kdv();
        BigDecimal genelToplam = belge.genelToplam();

        BigDecimal odenenTutar = dto.getOdenenTutar() != null ? dto.getOdenenTutar() : BigDecimal.ZERO;
        if (odenenTutar.signum() < 0) odenenTutar = BigDecimal.ZERO;
        if (odenenTutar.compareTo(genelToplam) > 0) odenenTutar = genelToplam;
        BigDecimal kalanTutar = genelToplam.subtract(odenenTutar);
        // Ödeme durumu her zaman hesaplanır; istemciden gelen değere güvenilmez.
        String odemeDurumu = kalanTutar.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                : odenenTutar.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI";

        fatura.setCariHesap(cariHesap);
        fatura.setTur(tur);
        fatura.setTarih(dto.getTarih() != null ? dto.getTarih() : fatura.getTarih());
        fatura.setVadeTarihi(vadeTarihiHesapla(dto, cariHesap));
        fatura.setAciklama(dto.getAciklama());
        if (dto.getTeslimEden() != null) fatura.setTeslimEden(dto.getTeslimEden());
        if (dto.getTeslimDurumu() != null) fatura.setTeslimDurumu(dto.getTeslimDurumu());
        if (dto.getTeslimNotu() != null) fatura.setTeslimNotu(dto.getTeslimNotu());
        if (dto.getTeslimFotograf() != null) fatura.setTeslimFotograf(dto.getTeslimFotograf());
        if (dto.getDepoId() != null) fatura.setDepoId(dto.getDepoId());
        if (dto.getParaBirimi() != null) fatura.setParaBirimi(dto.getParaBirimi());
        fatura.setAraToplam(araToplam);
        fatura.setKdv(kdv);
        fatura.setGenelToplam(genelToplam);
        fatura.setGenelIskontoTutari(genelIskonto);
        fatura.setOdemeDurumu(odemeDurumu);
        fatura.setOdenenTutar(odenenTutar);
        fatura.setKalanTutar(kalanTutar);

        fatura.getKalemler().clear();
        yeniKalemler.forEach(k -> k.setFatura(fatura));
        fatura.getKalemler().addAll(yeniKalemler);

        if (kesilmisti) {
            // Stok farkını işle (revize)
            Map<Long, BigDecimal> yeniMiktarlar = new HashMap<>();
            for (FaturaKalem k : yeniKalemler) {
                if (k.getStokId() == null) continue;
                yeniMiktarlar.merge(k.getStokId(), (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO), BigDecimal::add);
            }
            stokFarkiIsle(fatura, eskiMiktarlar, yeniMiktarlar, tur);

            // Cari bakiye farkını işle (revize)
            bakiyeUygula(eskiCariId, eskiTur, eskiGenelToplam, eskiParaBirimi, true);
            bakiyeUygula(cariHesap != null ? cariHesap.getId() : null, tur, genelToplam,
                    dto.getParaBirimi() != null ? dto.getParaBirimi() : fatura.getParaBirimi(), false);
        }

        Fatura guncellenen = faturaRepository.save(fatura);
        log.info("Fatura düzenlendi - ID: {}, No: {}", id, guncellenen.getFaturaNumarasi());
        String duzenlemeYeniSnapshot = faturaGecmisService.snapshot(guncellenen);
        faturaGecmisService.kaydet(guncellenen, FaturaGecmisService.GUNCELLE,
                faturaGecmisService.diffOzet(duzenlemeOncekiSnapshot, duzenlemeYeniSnapshot),
                duzenlemeOncekiSnapshot, duzenlemeYeniSnapshot);
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public void faturaSil(Long id) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        if (fatura.getDurum() == Fatura.FaturaDurum.KESILDI) {
            throw new BusinessException("Kesilmiş fatura silinemez");
        }
        String silmeOncesiSnapshot = faturaGecmisService.snapshot(fatura);
        faturaRepository.deleteById(id);
        faturaGecmisService.kaydet(fatura, FaturaGecmisService.SIL, "Fatura silindi", silmeOncesiSnapshot, null);
    }

    /**
     * Verilen irsaliye kesilmiş (stok etkisi işlenmiş) ise true döner. Bu durumda aynı
     * kalemlerden fatura kesilirken stok ikinci kez düşülmez.
     */
    private boolean irsaliyeStokIslenmisMi(Long irsaliyeId) {
        if (irsaliyeId == null) return false;
        return irsaliyeRepository.findById(irsaliyeId)
                .map(i -> "KESILDI".equals(i.getDurum()))
                .orElse(false);
    }

    private String stokYonu(Fatura.FaturaTur tur) {
        return tur == Fatura.FaturaTur.ALIS ? "GIRIS" : "CIKIS";
    }

    private String tersStokYonu(Fatura.FaturaTur tur) {
        return tur == Fatura.FaturaTur.ALIS ? "CIKIS" : "GIRIS";
    }

    private boolean negatifStokIzinli(Long sirketId) {
        if (sirketId == null) return false;
        try {
            return sirketRepository.findById(sirketId)
                    .map(s -> Boolean.TRUE.equals(s.getNegatifStokIzni()))
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Negatif stok izni okunamadı (sirket {}): {}", sirketId, e.getMessage());
            return false;
        }
    }

    /**
     * Faturanın cari hesap bakiyesine etkisini uygular.
     * Kullanıcı görünümü: pozitif = alacak, negatif = borç.
     * Satış faturası müşteriyi borçlandırır (bakiye eksi), alış faturası tedarikçiye borç olarak artar.
     * ters=true iptal/geri alma için işareti çevirir.
     */
    private void cariBakiyeGuncelle(Fatura fatura, boolean ters) {
        if (fatura.getCariHesap() == null) return;
        // Peşin/ön ödeme düşülür: cariye yalnızca kalan (ödenmemiş) tutar borç yazılır,
        // aksi halde peşin satışta cari borç tam tutar kadar kalır.
        BigDecimal tutar = fatura.getKalanTutar() != null ? fatura.getKalanTutar() : BigDecimal.ZERO;
        // Dövizli fatura cari bakiyeye TL karşılığı olarak yansıtılır.
        tutar = tlKarsiliginaCevir(fatura, tutar);
        if (fatura.getTur() == Fatura.FaturaTur.SATIS) {
            // Satış: müşteri borçlanır -> bakiye negatif (borçlu)
            tutar = tutar.negate();
        }
        if (ters) {
            tutar = tutar.negate();
        }
        cariHesapService.bakiyeGuncelle(fatura.getCariHesap().getId(), tutar);
    }

    /**
     * Cari bakiyesine açık değerlerle etki uygular (revize için kullanılır).
     * Dövizli tutar TL karşılığına çevrilir.
     */
    private void bakiyeUygula(Long cariId, Fatura.FaturaTur tur, BigDecimal tutar, String paraBirimi, boolean ters) {
        if (cariId == null) return;
        BigDecimal deger = tlKarsiliginaCevir(tutar != null ? tutar : BigDecimal.ZERO, paraBirimi);
        if (tur == Fatura.FaturaTur.SATIS) {
            deger = deger.negate();
        }
        if (ters) {
            deger = deger.negate();
        }
        cariHesapService.bakiyeGuncelle(cariId, deger);
    }

    /**
     * Revize edilen faturanın eski/yeni kalem miktarları arasındaki farkı stoğa işler.
     * Maliyet fiyatına dokunmaz; yalnızca miktarı düzeltir ve hareket kaydı oluşturur.
     */
    private void stokFarkiIsle(Fatura fatura, Map<Long, BigDecimal> eski, Map<Long, BigDecimal> yeni, Fatura.FaturaTur tur) {
        Set<Long> stokIdler = new HashSet<>();
        stokIdler.addAll(eski.keySet());
        stokIdler.addAll(yeni.keySet());

        List<StokHareket> hareketler = new ArrayList<>();
        for (Long stokId : stokIdler) {
            BigDecimal eskiAdet = eski.getOrDefault(stokId, BigDecimal.ZERO);
            BigDecimal yeniAdet = yeni.getOrDefault(stokId, BigDecimal.ZERO);
            BigDecimal delta = yeniAdet.subtract(eskiAdet);
            if (delta.compareTo(BigDecimal.ZERO) == 0) continue;

            // SATIS'te miktar artınca stok düşer; ALIS'te artınca stok artar.
            BigDecimal stokDegisim = tur == Fatura.FaturaTur.SATIS ? delta.negate() : delta;

            Stok stok = stokRepository.findByIdForUpdate(stokId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
            // Güvenlik: revize edilen faturanın şirketi dışındaki stoklara dokunulamaz.
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (fatura.getSirketId() != null && stok.getSirketId() != null && !fatura.getSirketId().equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
            BigDecimal yeniMiktar = (stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO).add(stokDegisim);
            if (yeniMiktar.compareTo(BigDecimal.ZERO) < 0) {
                if (negatifStokIzinli(fatura.getSirketId())) {
                    log.warn("Negatif stok izni açık; revize ile stok eksiye düşüyor. Ürün: {}", stok.getAd());
                } else {
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                            + ", Mevcut: " + stok.getMiktar() + ", Revize sonrası: " + yeniMiktar);
                }
            }
            BigDecimal oncekiMiktar = yeniMiktar.subtract(stokDegisim);
            stok.setMiktar(yeniMiktar);
            stokRepository.save(stok);
            if (stokDegisim.signum() > 0) {
                maliyetService.girisIsle(stok, oncekiMiktar, stokDegisim, null,
                        fatura.getSirketId(), "FATURA", fatura.getId());
            } else {
                maliyetService.cikisIsle(stok, stokDegisim.abs(), yeniMiktar,
                        fatura.getSirketId(), "FATURA", fatura.getId());
            }

            String hareketTuru = stokDegisim.compareTo(BigDecimal.ZERO) >= 0 ? "GIRIS" : "CIKIS";
            hareketler.add(StokHareket.builder()
                    .stok(stok).tur(hareketTuru)
                    .miktar(stokDegisim.abs())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("Fatura revize #" + fatura.getFaturaNumarasi())
                    .cariHesap(fatura.getCariHesap())
                    .depoId(fatura.getDepoId())
                    .kaynakTip("FATURA").kaynakId(fatura.getId())
                    .build());
        }
        if (!hareketler.isEmpty()) {
            stokHareketRepository.saveAll(hareketler);
        }
        cacheYardimci.temizle("stoklar", "dashboard");
    }

    /**
     * Fatura para birimi TRY değilse birim fiyatı TL karşılığına çevirir (stok maliyeti TL tutulur).
     * Kur servisi başarısız olursa ham fiyat korunur.
     */
    private BigDecimal tlKarsiliginaCevir(Fatura fatura, BigDecimal tutar) {
        return tlKarsiliginaCevir(tutar, fatura.getParaBirimi());
    }

    private BigDecimal tlKarsiliginaCevir(BigDecimal tutar, String paraBirimi) {
        if (tutar == null) return BigDecimal.ZERO;
        if (paraBirimi == null || "TRY".equalsIgnoreCase(paraBirimi)) return tutar;
        try {
            return tcmbKurService.cevir(tutar, paraBirimi, "TRY");
        } catch (Exception e) {
            log.warn("Döviz kuru çevirilemedi ({}), ham fiyat kullanılıyor: {}", paraBirimi, e.getMessage());
            return tutar;
        }
    }

    private List<Long> stokHareketleriIsle(Fatura fatura, String tur, String aciklama) {
        List<Long> kritikStokIds = new ArrayList<>();
        List<StokHareket> hareketler = new ArrayList<>();
        // Depo bazli senkron: faturada depo secilmisse o, yoksa varsayilan aktif depo.
        Long depoId = depoStokService.coz(fatura.getDepoId(), fatura.getSirketId());
        for (FaturaKalem k : fatura.getKalemler()) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            // Güvenlik: faturanın şirketi dışındaki stoklara dokunulamaz.
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (fatura.getSirketId() != null && stok.getSirketId() != null && !fatura.getSirketId().equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
            Long seriId = null;
            if ("CIKIS".equals(tur)) {
                BigDecimal adet = (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO);
                if (stok.getMiktar().compareTo(adet) < 0) {
                    if (negatifStokIzinli(fatura.getSirketId())) {
                        log.warn("Negatif stok izni açık; stok eksiye düşüyor. Ürün: {} - Mevcut: {}, İstenen: {}",
                                stok.getAd(), stok.getMiktar(), adet);
                    } else {
                        throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                                + ", Mevcut: " + stok.getMiktar() + ", İstenen: " + adet);
                    }
                }
                stok.setMiktar(stok.getMiktar().subtract(adet));
                // FEFO seri/lot tüketimi (seri takibi varsa)
                var tuketilenSeriler = stokSeriService.fefoTuket(k.getStokId(), depoId, adet);
                if (tuketilenSeriler.size() == 1) seriId = tuketilenSeriler.get(0);
                // COGS anlık görüntüsü: satış anındaki ağırlıklı ortalama birim maliyet
                BigDecimal birimMaliyet = maliyetService.cikisIsle(stok, adet, stok.getMiktar(),
                        fatura.getSirketId(), "FATURA", fatura.getId());
                if (birimMaliyet == null) birimMaliyet = BigDecimal.ZERO;
                k.setBirimMaliyet(birimMaliyet);
                k.setMaliyetTutar(birimMaliyet.multiply(adet).setScale(2, RoundingMode.HALF_UP));
                faturaKalemRepository.save(k);
            } else {
                BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
                BigDecimal yeniMiktar = (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO);
                BigDecimal yeniBirimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
                yeniBirimFiyat = tlKarsiliginaCevir(fatura, yeniBirimFiyat);
                BigDecimal eskiFiyat = stok.getFiyat() != null ? stok.getFiyat() : BigDecimal.ZERO;

                stok.setMiktar(eskiMiktar.add(yeniMiktar));

                BigDecimal toplamMiktar = eskiMiktar.add(yeniMiktar);
                BigDecimal agirlikliOrtalama;
                if (toplamMiktar.compareTo(BigDecimal.ZERO) > 0) {
                    agirlikliOrtalama = eskiMiktar.multiply(eskiFiyat)
                            .add(yeniMiktar.multiply(yeniBirimFiyat))
                            .divide(toplamMiktar, 2, RoundingMode.HALF_UP);
                } else {
                    agirlikliOrtalama = yeniBirimFiyat;
                }
                stok.setFiyat(agirlikliOrtalama);
                stok.setTedarikciFiyat(yeniBirimFiyat);
                if (fatura.getCariHesap() != null) {
                    stok.setTedarikciId(fatura.getCariHesap().getId());
                }
                // Ağırlıklı ortalama maliyet motoru. İade/geri alma girişlerinde orijinal
                // kalem maliyeti varsa o kullanılır; normal alışta alış birim fiyatı kullanılır.
                BigDecimal girisBirimMaliyet = k.getBirimMaliyet() != null ? k.getBirimMaliyet() : yeniBirimFiyat;
                maliyetService.girisIsle(stok, eskiMiktar, yeniMiktar, girisBirimMaliyet,
                        fatura.getSirketId(), "FATURA", fatura.getId());
            }
            stokRepository.save(stok);
            // Depo stok senkronu (giris/çikis)
            BigDecimal depoDelta = "CIKIS".equals(tur)
                    ? (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO).negate()
                    : (k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO);
            depoStokService.guncelle(depoId, k.getStokId(), depoDelta);

            if (stok.getMinMiktar() != null && stok.getMiktar().compareTo(stok.getMinMiktar()) < 0) {
                log.warn("Kritik stok seviyesi! {} - Mevcut: {}, Minimum: {}", stok.getAd(), stok.getMiktar(), stok.getMinMiktar());
                kritikStokIds.add(stok.getId());
            }

            hareketler.add(StokHareket.builder()
                    .stok(stok).tur(tur)
                    .miktar((k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO))
                    .hareketTarihi(LocalDate.now())
                    .aciklama(aciklama)
                    .cariHesap(fatura.getCariHesap())
                    .depoId(depoId)
                    .seriId(seriId)
                    .kaynakTip("FATURA").kaynakId(fatura.getId())
                    .build());
        }
        // Hareketleri tek batch'te kaydet (kalem başına ayrı INSERT yerine)
        if (!hareketler.isEmpty()) {
            stokHareketRepository.saveAll(hareketler);
        }
        cacheYardimci.temizle("stoklar", "dashboard");
        return kritikStokIds;
    }

    private void kritikStokUyarisiGonder(List<Long> kritikStokIds, Long sirketId) {
        if (kritikStokIds == null || kritikStokIds.isEmpty()) return;
        try {
            String sirketEmail = null;
            if (sirketId != null) {
                sirketEmail = sirketRepository.findById(sirketId).map(Sirket::getEmail).orElse(null);
            }
            for (Long stokId : kritikStokIds) {
                Stok stok = stokRepository.findById(stokId).orElse(null);
                if (stok == null) continue;
                if (sirketId != null) {
                    Long bildirimSirketId = sirketId;
                    com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(bildirimSirketId, "STOK",
                            "Kritik Stok: " + stok.getAd(),
                            "Mevcut: " + stok.getMiktar() + ", Minimum: " + stok.getMinMiktar()));
                }
                if (sirketEmail != null && !sirketEmail.isBlank()) {
                    String bildirimEmail = sirketEmail;
                    com.raspel.erp.support.AfterCommitExecutor.calistir(() -> emailService.stokUyarisiGonder(bildirimEmail, stok.getAd(),
                            stok.getMiktar() != null ? stok.getMiktar().toString() : "0",
                            stok.getBirim() != null ? stok.getBirim() : ""));
                }
            }
        } catch (Exception e) {
            log.warn("Kritik stok uyarısı gönderilemedi: {}", e.getMessage());
        }
    }

    /**
     * Faturanın vade tarihini hesaplar.
     * Öncelik: DTO'da açıkça verilen vade tarihi -> Cari hesabın ödeme vadesi -> Fatura tarihi.
     */
    private LocalDate vadeTarihiHesapla(FaturaDTO dto, CariHesap cariHesap) {
        if (dto.getVadeTarihi() != null) return dto.getVadeTarihi();
        LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : LocalDate.now();
        if (cariHesap != null && cariHesap.getOdemeVadesi() != null) {
            return tarih.plusDays(cariHesap.getOdemeVadesi());
        }
        return tarih;
    }

    private FaturaDTO entityDTOyeCevir(Fatura fatura) {
        Map<Long, Stok> stokHaritasi = stokRepository.findAllById(
                fatura.getKalemler().stream()
                        .map(FaturaKalem::getStokId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Stok::getId, s -> s, (s1, s2) -> s1));
        Map<Long, String> depoHaritasi = fatura.getDepoId() == null ? Map.of()
                : depoRepository.findAllById(List.of(fatura.getDepoId())).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.sube.Depo::getId, com.raspel.erp.entity.sube.Depo::getAd, (a, b) -> a));
        Map<Long, String> kasaHaritasi = fatura.getKasaId() == null ? Map.of()
                : kasaRepository.findAllById(List.of(fatura.getKasaId())).stream()
                        .collect(Collectors.toMap(Kasa::getId, Kasa::getAd, (a, b) -> a));
        return entityDTOyeCevir(fatura, stokHaritasi, depoHaritasi, kasaHaritasi);
    }

    /**
     * Sayfa içindeki tüm faturaların kalemlerindeki stokları tek sorguda yükler,
     * böylece fatura başına ayrı stok sorgusu (N+1) çalışmaz.
     */
    private Page<FaturaDTO> sayfaDTOyaCevir(Page<Fatura> sayfa) {
        Set<Long> stokIdler = new HashSet<>();
        Set<Long> depoIdler = new HashSet<>();
        Set<Long> kasaIdler = new HashSet<>();
        for (Fatura f : sayfa.getContent()) {
            for (FaturaKalem k : f.getKalemler()) {
                if (k.getStokId() != null) stokIdler.add(k.getStokId());
            }
            if (f.getDepoId() != null) depoIdler.add(f.getDepoId());
            if (f.getKasaId() != null) kasaIdler.add(f.getKasaId());
        }
        Map<Long, Stok> stokHaritasi = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s, (s1, s2) -> s1));
        // Depo ve kasa adlari sayfa basina tek sorguda (fatura basina 2 sorgu yerine).
        Map<Long, String> depoHaritasi = depoIdler.isEmpty() ? Map.of()
                : depoRepository.findAllById(depoIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.sube.Depo::getId, com.raspel.erp.entity.sube.Depo::getAd, (a, b) -> a));
        Map<Long, String> kasaHaritasi = kasaIdler.isEmpty() ? Map.of()
                : kasaRepository.findAllById(kasaIdler).stream()
                        .collect(Collectors.toMap(Kasa::getId, Kasa::getAd, (a, b) -> a));
        return sayfa.map(f -> entityDTOyeCevir(f, stokHaritasi, depoHaritasi, kasaHaritasi));
    }

    private FaturaDTO entityDTOyeCevir(Fatura fatura, Map<Long, Stok> stokHaritasi,
                                       Map<Long, String> depoHaritasi, Map<Long, String> kasaHaritasi) {

        List<FaturaKalemDTO> kalemDTO = fatura.getKalemler().stream().map(k -> {
            String stokAd = null;
            String stokKodu = null;
            if (k.getStokId() != null && stokHaritasi.containsKey(k.getStokId())) {
                Stok s = stokHaritasi.get(k.getStokId());
                stokAd = s.getAd();
                stokKodu = s.getStokKodu();
            }
            return FaturaKalemDTO.builder()
                    .id(k.getId())
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(k.getKdvOrani())
                    .iskontoOrani(k.getIskontoOrani())
                    .tutar(k.getTutar())
                    .stokId(k.getStokId())
                    .stokAd(stokAd)
                    .stokKodu(stokKodu)
                    .agirlik(k.getAgirlik())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal toplamAgirlik = fatura.getKalemler().stream()
                .map(k -> k.getAgirlik() != null
                        ? k.getAgirlik().multiply((k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FaturaDTO.builder()
                .id(fatura.getId())
                .faturaNumarasi(fatura.getFaturaNumarasi())
                .tarih(fatura.getTarih())
                .vadeTarihi(fatura.getVadeTarihi())
                .tur(fatura.getTur().name())
                .durum(fatura.getDurum().name())
                .cariHesapId(fatura.getCariHesap() != null ? fatura.getCariHesap().getId() : null)
                .cariHesapAd(fatura.getCariHesap() != null ? fatura.getCariHesap().getAd() : null)
                .aciklama(fatura.getAciklama())
                .araToplam(fatura.getAraToplam())
                .kdv(fatura.getKdv())
                .genelToplam(fatura.getGenelToplam())
                .genelIskontoTutari(fatura.getGenelIskontoTutari())
                .toplamAgirlik(toplamAgirlik)
                .odemeDurumu(fatura.getOdemeDurumu())
                .odenenTutar(fatura.getOdenenTutar())
                .kalanTutar(fatura.getKalanTutar())
                .kalemler(kalemDTO)
                .olusturmaTarihi(fatura.getOlusturmaTarihi())
                .olusturanKullaniciId(fatura.getOlusturanKullaniciId())
                .olusturanKullaniciAdi(fatura.getOlusturanKullaniciAdi())
                .teslimEden(fatura.getTeslimEden())
                .teslimDurumu(fatura.getTeslimDurumu())
                .teslimNotu(fatura.getTeslimNotu())
                .teslimFotograf(fatura.getTeslimFotograf())
                .depoId(fatura.getDepoId())
                .depoAd(fatura.getDepoId() != null ? depoHaritasi.get(fatura.getDepoId()) : null)
                .paraBirimi(fatura.getParaBirimi())
                .odemeYontemi(fatura.getOdemeYontemi())
                .taksitKurum(fatura.getTaksitKurum())
                .bankaId(fatura.getBankaId())
                .kartaBankaAktar(fatura.getKartaBankaAktar())
                .taksitTutar(fatura.getTaksitTutar())
                .kasaId(fatura.getKasaId())
                .kasaAd(fatura.getKasaId() != null ? kasaHaritasi.get(fatura.getKasaId()) : null)
                .irsaliyeId(fatura.getIrsaliyeId())
                .build();
    }
}