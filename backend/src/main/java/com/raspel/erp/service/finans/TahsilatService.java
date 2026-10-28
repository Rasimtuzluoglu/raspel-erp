package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.finans.TahsilatHedefDTO;
import com.raspel.erp.dto.finans.TahsilatIslemDTO;
import com.raspel.erp.dto.finans.TahsilatPerformansDTO;
import com.raspel.erp.dto.finans.TopluHatirlatmaSonuc;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.entity.finans.TahsilatHedef;
import com.raspel.erp.entity.finans.TahsilatIslem;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import com.raspel.erp.repository.finans.TahsilatHedefRepository;
import com.raspel.erp.repository.finans.TahsilatIslemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.EmailService;
import com.raspel.erp.service.finans.HareketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TahsilatService {

    private final FaturaRepository faturaRepository;
    private final EmailService emailService;
    private final CariHesapRepository cariHesapRepository;
    private final HareketService hareketService;
    private final HareketRepository hareketRepository;
    private final PosTerminaliRepository posTerminaliRepository;
    private final TaksitService taksitService;
    private final com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    private final com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    private final com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    private final com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    private final TahsilatIslemRepository tahsilatIslemRepository;
    private final TahsilatHedefRepository tahsilatHedefRepository;
    private final com.raspel.erp.config.TenantChecker tenantChecker;
    private final com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;
    private final com.raspel.erp.service.sistem.DonemService donemService;

    private static final List<String> ODENDI_DURUMLARI = List.of("ODENDI", "IPTAL");
    private static final List<String> GECERLI_ODEME_YONTEMLERI = List.of("NAKIT", "KART", "TAKSIT", "HAVALE");
    private static final DateTimeFormatter VADE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public TahsilatDTO ozetGetir(Long sirketId) {
        return ozetGetir(sirketId, null, null, null, null);
    }

    /**
     * Yaşlandırma özeti. Sunucu filtreleri yalnız {@code cariler} listesini
     * daraltır; KPI toplamları (toplam alacak, vadesi geçmiş, yaklaşan) global
     * kalır (spec §6).
     */
    @Transactional(readOnly = true)
    public TahsilatDTO ozetGetir(Long sirketId, String q, String aralik,
                                 Boolean sadeceGecikmis, BigDecimal minTutar) {
        LocalDate bugun = LocalDate.now();
        List<Fatura> faturalar = faturaRepository.findTahsilatEdilecek(
                sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI);

        List<Fatura> cariBasli = faturalar.stream()
                .filter(f -> f.getCariHesap() != null)
                .collect(Collectors.toList());

        Map<Long, List<Fatura>> cariyeGore = cariBasli.stream()
                .collect(Collectors.groupingBy(f -> f.getCariHesap().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<TahsilatDTO.CariOzet> tumCariler = cariyeGore.entrySet().stream()
                .map(e -> cariOzetiOlustur(e.getValue(), bugun))
                .collect(Collectors.toList());

        List<TahsilatDTO.CariOzet> cariler = tumCariler.stream()
                .filter(c -> cariFiltresineUyar(c, q, aralik, sadeceGecikmis, minTutar))
                .sorted(Comparator.comparingInt(TahsilatDTO.CariOzet::getMaxGecikmeGunu).reversed())
                .collect(Collectors.toList());

        BigDecimal toplamAlacak = cariBasli.stream()
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vadesiGecmisToplam = cariBasli.stream()
                .filter(f -> gecikmeGunu(vade(f), bugun) > 0)
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vadesiYaklasanToplam = cariBasli.stream()
                .filter(f -> {
                    int gun = gecikmeGunu(vade(f), bugun);
                    return gun <= 0 && gun >= -30;
                })
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int gecikmisCariSayisi = (int) tumCariler.stream()
                .filter(c -> c.getGecikmisAlacak() != null && c.getGecikmisAlacak().signum() > 0)
                .count();

        return TahsilatDTO.builder()
                .toplamAlacak(toplamAlacak)
                .vadesiGecmisToplam(vadesiGecmisToplam)
                .vadesiYaklasanToplam(vadesiYaklasanToplam)
                .acikFaturaSayisi(cariBasli.size())
                .gecikmisCariSayisi(gecikmisCariSayisi)
                .cariler(cariler)
                .build();
    }

    private boolean cariFiltresineUyar(TahsilatDTO.CariOzet cari, String q, String aralik,
                                      Boolean sadeceGecikmis, BigDecimal minTutar) {
        if (q != null && !q.isBlank()) {
            String arama = q.trim().toLowerCase(Locale.ROOT);
            boolean adEslesir = cari.getCariAd() != null
                    && cari.getCariAd().toLowerCase(Locale.ROOT).contains(arama);
            boolean telefonEslesir = cari.getTelefon() != null
                    && cari.getTelefon().replace(" ", "").contains(arama.replace(" ", ""));
            if (!adEslesir && !telefonEslesir) return false;
        }
        if (aralik != null && !aralik.isBlank() && !aralikEslesir(cari.getAralik(), aralik)) {
            return false;
        }
        if (Boolean.TRUE.equals(sadeceGecikmis) && cari.getMaxGecikmeGunu() <= 0) {
            return false;
        }
        if (minTutar != null && (cari.getToplamAlacak() == null
                || cari.getToplamAlacak().compareTo(minTutar) < 0)) {
            return false;
        }
        return true;
    }

    private boolean aralikEslesir(String cariAralik, String istenen) {
        if (cariAralik == null) return false;
        String a = cariAralik.replace(" Gün", "").trim();
        String b = istenen.replace(" Gün", "").trim();
        return a.equalsIgnoreCase(b);
    }

    /**
     * Tahsilat girişi: cariye ait ödenmemiş faturalara (en eski vade öncelikli) girilen tutarı dağıtır.
     * Her tahsis için ayrı TAHSILAT hareketi oluşturur; fatura ödeme durumunu ve cari bakiyeyi günceller.
     */
    @Transactional
    public Map<String, Object> tahsilatGir(Long cariId, BigDecimal tutar, String odemeYontemi,
                                           String taksitKurum, BigDecimal taksitTutar, String aciklama,
                                           LocalDate hareketTarihi, Long sirketId,
                                           Long posTerminaliId, BigDecimal komisyonTutar, LocalDate valorTarihi) {
        return tahsilatGir(cariId, tutar, odemeYontemi, taksitKurum, taksitTutar, aciklama,
                hareketTarihi, sirketId, posTerminaliId, komisyonTutar, valorTarihi, null, null, null, null);
    }

    /**
     * Tahsilat girişi (taksit kalem atamalı): {@code taksitId} verilirse, tahsilat kaydedildikten
     * sonra ilgili taksit plan kalemi "ödendi" olarak işaretlenir.
     */
    @Transactional
    public Map<String, Object> tahsilatGir(Long cariId, BigDecimal tutar, String odemeYontemi,
                                           String taksitKurum, BigDecimal taksitTutar, String aciklama,
                                           LocalDate hareketTarihi, Long sirketId,
                                           Long posTerminaliId, BigDecimal komisyonTutar, LocalDate valorTarihi,
                                           Long taksitId) {
        return tahsilatGir(cariId, tutar, odemeYontemi, taksitKurum, taksitTutar, aciklama,
                hareketTarihi, sirketId, posTerminaliId, komisyonTutar, valorTarihi, taksitId, null, null, null);
    }

    /**
     * Tahsilat girişi (kasa/banka hesabına işlemeli, kullanıcı bilgisi olmadan).
     */
    @Transactional
    public Map<String, Object> tahsilatGir(Long cariId, BigDecimal tutar, String odemeYontemi,
                                           String taksitKurum, BigDecimal taksitTutar, String aciklama,
                                           LocalDate hareketTarihi, Long sirketId,
                                           Long posTerminaliId, BigDecimal komisyonTutar, LocalDate valorTarihi,
                                           Long taksitId, Long kasaId, Long bankaId) {
        return tahsilatGir(cariId, tutar, odemeYontemi, taksitKurum, taksitTutar, aciklama,
                hareketTarihi, sirketId, posTerminaliId, komisyonTutar, valorTarihi, taksitId, kasaId, bankaId, null);
    }

    /**
     * Tahsilat girişi. İşlem başında tek bir {@link TahsilatIslem} oluşturulur; fatura
     * dağıtımı ve fazla/avans için üretilen tüm {@code Hareket} satırları bu işleme bağlanır.
     * Herhangi bir adım hata verirse tek transaction geri alınır (işlem de yazılmaz).
     */
    @Transactional
    public Map<String, Object> tahsilatGir(Long cariId, BigDecimal tutar, String odemeYontemi,
                                           String taksitKurum, BigDecimal taksitTutar, String aciklama,
                                           LocalDate hareketTarihi, Long sirketId,
                                           Long posTerminaliId, BigDecimal komisyonTutar, LocalDate valorTarihi,
                                           Long taksitId, Long kasaId, Long bankaId, Long kullaniciId) {
        if (tutar == null || tutar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Tahsilat tutarı 0'dan büyük olmalıdır");
        }
        // Dönem kilidi. Güvenlik zaten hareketOlustur içinde uygulanıyor
        // (her tahsilat en az bir hareket yazar); buradaki kontrol iki işe yarar:
        //   1) Kullanıcı "Tahsilat Al" dedi ama ekranda "hareket oluşturma
        //      yapılamaz" görüyordu. Artık ne yaptığını söyleyen bir mesaj alır.
        //   2) Fatura taraması ve olası kısmi yazmalar başlamadan hızlıca
        //      reddeder.
        donemService.kilitKontrol(sirketId,
                hareketTarihi != null ? hareketTarihi : LocalDate.now(), "tahsilat");
        CariHesap cari = cariHesapRepository.findById(cariId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariId));

        // Ödeme yöntemi geçerli mi?
        if (odemeYontemi != null && !odemeYontemi.isBlank() && !GECERLI_ODEME_YONTEMLERI.contains(odemeYontemi.toUpperCase())) {
            throw new BusinessException("Geçersiz ödeme yöntemi: " + odemeYontemi);
        }
        if ("TAKSIT".equalsIgnoreCase(odemeYontemi)
                && (taksitKurum == null || taksitKurum.isBlank()
                || taksitTutar == null || taksitTutar.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("Taksit seçildiğinde taksit kurumu ve çekilen tutar girilmelidir");
        }

        // POS terminali adını çöz (kart tek çekim için)
        String posAd = null;
        if (posTerminaliId != null) {
            posAd = posTerminaliRepository.findById(posTerminaliId)
                    .map(PosTerminali::getAd)
                    .orElseThrow(() -> new ResourceNotFoundException("POS Terminali", posTerminaliId));
        }

        // Açık faturaları vade öncelikli al (yalnızca bu cariye ait faturalar)
        List<Fatura> acikFaturalar = faturaRepository.findTahsilatEdilecekByCari(
                        sirketId, cariId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI)
                .stream()
                .sorted(Comparator.comparing(this::vade, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Kasa/banka hesabina giris (secildiyse). Ayni tutar iki hesaba birden yazilamaz.
        if (kasaId != null && bankaId != null) {
            throw new BusinessException("Aynı tahsilat hem kasaya hem bankaya işlenemez; tek hesap seçin");
        }
        // POS tahsilati gun sonunda POS'un banka hesabina aktarilir; burada bankaya
        // yazilirsa tutar iki kez sayilir (cift yazim).
        boolean bankaYazilir = bankaId != null && posTerminaliId == null;

        // Tahsilat işlemi (batch). Aynı çağrıda üretilen tüm cari hareketleri
        // bu kayda bağlanır; iptal bütünlüğü bu id üzerinden sağlanır.
        LocalDate islemTarihi = hareketTarihi != null ? hareketTarihi : LocalDate.now();
        TahsilatIslem islem = tahsilatIslemRepository.save(TahsilatIslem.builder()
                .sirketId(sirketId)
                .cariId(cariId)
                .tarih(islemTarihi)
                .tutar(tutar)
                .odemeYontemi(odemeYontemi != null ? odemeYontemi.toUpperCase(Locale.ROOT) : null)
                .kasaId(kasaId)
                .bankaId(bankaId)
                .durum(TahsilatIslem.DURUM_AKTIF)
                .olusturanKullaniciId(kullaniciId)
                .build());
        Long tahsilatIslemId = islem.getId();

        BigDecimal kalan = tutar;
        List<Long> uygulananFaturalar = new ArrayList<>();
        Long ilkHareketId = null;
        for (Fatura f : acikFaturalar) {
            if (kalan.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal faturaKalan = f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO;
            if (faturaKalan.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal tahsis = kalan.min(faturaKalan);

            HareketDTO olusan = hareketService.hareketOlustur(HareketDTO.builder()
                            .cariHesapId(cariId)
                            .tur("TAHSILAT")
                            .tutar(tahsis)
                            .hareketTarihi(hareketTarihi != null ? hareketTarihi : LocalDate.now())
                            .aciklama(aciklama != null && !aciklama.isBlank()
                                    ? aciklama : "Tahsilat: " + f.getFaturaNumarasi())
                            .odemeYontemi(odemeYontemi)
                            .taksitKurum(taksitKurum)
                            .taksitTutar(taksitTutar)
                            .posTerminaliId(posTerminaliId)
                            .posAd(posAd)
                            .komisyonTutar(komisyonTutar)
                            .valorTarihi(valorTarihi)
                            .faturaId(f.getId())
                            .tahsilatIslemId(tahsilatIslemId)
                            .build(), sirketId);

            if (olusan != null) {
                if (ilkHareketId == null) ilkHareketId = olusan.getId();
                // Kasa/banka girisi fatura bazli tahsisle birebir eslesir ve cari harekete
                // baglanir; tahsilat silinince yalnizca ilgili kasa/banka hareketi ters kaydedilir.
                if (kasaId != null) {
                    kasaGirisiIsle(kasaId, tahsis, cari, hareketTarihi, sirketId, olusan.getId());
                } else if (bankaYazilir) {
                    bankaGirisiIsle(bankaId, tahsis, cari, hareketTarihi, sirketId, olusan.getId());
                }
            }
            kalan = kalan.subtract(tahsis);
            uygulananFaturalar.add(f.getId());
        }

        if (kalan.compareTo(BigDecimal.ZERO) > 0) {
            // Fazla ödeme veya faturasız (avans) tahsilat: cariye alacak olarak kaydedilir.
            boolean faturasiz = uygulananFaturalar.isEmpty();
            HareketDTO olusanFazla = hareketService.hareketOlustur(HareketDTO.builder()
                    .cariHesapId(cariId)
                    .tur("TAHSILAT")
                    .tutar(kalan)
                    .hareketTarihi(hareketTarihi != null ? hareketTarihi : LocalDate.now())
                    .aciklama(faturasiz
                            ? (aciklama != null && !aciklama.isBlank() ? aciklama : "Avans tahsilat (faturasız)")
                            : "Fazla ödeme (alacak)")
                    .odemeYontemi(odemeYontemi)
                    .taksitKurum(taksitKurum)
                    .taksitTutar(taksitTutar)
                    .posTerminaliId(posTerminaliId)
                    .posAd(posAd)
                    .komisyonTutar(komisyonTutar)
                    .valorTarihi(valorTarihi)
                    .faturaId(null)
                    .tahsilatIslemId(tahsilatIslemId)
                    .build(), sirketId);
            if (olusanFazla != null) {
                if (ilkHareketId == null) ilkHareketId = olusanFazla.getId();
                if (kasaId != null) {
                    kasaGirisiIsle(kasaId, kalan, cari, hareketTarihi, sirketId, olusanFazla.getId());
                } else if (bankaYazilir) {
                    bankaGirisiIsle(bankaId, kalan, cari, hareketTarihi, sirketId, olusanFazla.getId());
                }
            }
        }

        // Muhasebe entegrasyonu (iskelet): cari tahsilatı için yevmiye fişi.
        // Kaynak: ilk cari hareket id'si; idempotenttir, hata akışı bloklamaz.
        if (ilkHareketId != null && sirketId != null) {
            otomatikMuhasebeService.tahsilatIsle(sirketId, ilkHareketId, tutar,
                    hareketTarihi != null ? hareketTarihi : LocalDate.now(),
                    kasaId, bankaId, cari.getAd());
        }

        log.info("Tahsilat kaydedildi -> Cari: {}, Tutar: {}, Yöntem: {}, Fatura sayısı: {}",
                cari.getAd(), tutar, odemeYontemi, uygulananFaturalar.size());

        Map<String, Object> sonuc = new java.util.LinkedHashMap<>();
        sonuc.put("cariId", cariId);
        sonuc.put("cariAd", cari.getAd());
        // Idempotency yanıtının kilitlenebilmesi için: anahtarı tamamlamak
        // hareket id'si gerektirir. Aynı zamanda istemci (mükerrer istek
        // doğrulaması) kaydı geçmişten teyit edebilir.
        sonuc.put("hareketId", ilkHareketId);
        sonuc.put("tahsilatIslemId", tahsilatIslemId);
        sonuc.put("tutar", tutar);
        sonuc.put("odemeYontemi", odemeYontemi);
        sonuc.put("uygulananFaturalar", uygulananFaturalar);
        sonuc.put("fazlaOdeme", kalan.compareTo(BigDecimal.ZERO) > 0 ? kalan : BigDecimal.ZERO);
        if (kasaId != null) sonuc.put("kasaId", kasaId);
        if (bankaId != null) sonuc.put("bankaId", bankaId);

        if (taksitId != null) {
            taksitService.ode(taksitId, com.raspel.erp.dto.finans.TaksitOdeDTO.builder()
                    .odemeTarihi(hareketTarihi != null ? hareketTarihi : LocalDate.now())
                    // REDTEAM/Faz1.2: Oluşturulan tahsilat hareketinin id'si taksite
                    // bağlanır. Bu alan daha önce HİÇ doldurulmuyordu; taksit ile
                    // parasal hareket arasındaki tek bağ `aciklama` metnine
                    // kalıyordu (denetim izi yoktu). Artık taksitten
                    // doğrudan harekete gidilebilir.
                    .hareketId(ilkHareketId)
                    .aciklama("Tahsilat ile odendi")
                    .build(), sirketId);
            sonuc.put("taksitId", taksitId);
        }
        return sonuc;
    }

    /**
     * Tahsilat geçmişi (eski imza): filtresiz, işlem bazlı sayfa.
     */
    @Transactional(readOnly = true)
    public Page<TahsilatIslemDTO> gecmis(Long sirketId, Pageable pageable) {
        return gecmisFiltreli(sirketId, null, null, null, null, null, null, null, null, pageable);
    }

    /**
     * İşlem bazlı tahsilat geçmişi. Filtreler sunucu tarafında uygulanır;
     * dağıtım satırları iptal edilmiş hareketler dahil döner (geçmiş görünürlüğü).
     */
    @Transactional(readOnly = true)
    public Page<TahsilatIslemDTO> gecmisFiltreli(Long sirketId, String q, Long cariId,
                                                 String odemeYontemi, String durum,
                                                 Long kasaId, Long bankaId,
                                                 LocalDate baslangic, LocalDate bitis,
                                                 Pageable pageable) {
        String yontem = (odemeYontemi != null && !odemeYontemi.isBlank())
                ? odemeYontemi.trim().toUpperCase(Locale.ROOT) : null;
        String durumFiltre = (durum != null && !durum.isBlank())
                ? durum.trim().toUpperCase(Locale.ROOT) : null;
        String arama = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : null;

        Specification<TahsilatIslem> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> kosullar = new ArrayList<>();
            kosullar.add(cb.equal(root.get("sirketId"), sirketId));
            if (cariId != null) kosullar.add(cb.equal(root.get("cariId"), cariId));
            if (yontem != null) kosullar.add(cb.equal(root.get("odemeYontemi"), yontem));
            if (durumFiltre != null) kosullar.add(cb.equal(root.get("durum"), durumFiltre));
            if (kasaId != null) kosullar.add(cb.equal(root.get("kasaId"), kasaId));
            if (bankaId != null) kosullar.add(cb.equal(root.get("bankaId"), bankaId));
            if (baslangic != null) kosullar.add(cb.greaterThanOrEqualTo(root.get("tarih"), baslangic));
            if (bitis != null) kosullar.add(cb.lessThanOrEqualTo(root.get("tarih"), bitis));
            if (arama != null) {
                var altSorgu = query.subquery(Long.class);
                var cariKok = altSorgu.from(CariHesap.class);
                altSorgu.select(cariKok.get("id")).where(
                        cb.equal(cariKok.get("id"), root.get("cariId")),
                        cb.or(
                                cb.like(cb.lower(cariKok.<String>get("ad")), arama),
                                cb.like(cariKok.<String>get("telefon"), arama)));
                kosullar.add(cb.exists(altSorgu));
            }
            return cb.and(kosullar.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<TahsilatIslem> sayfa = tahsilatIslemRepository.findAll(spec, pageable);
        if (sayfa.isEmpty()) {
            return sayfa.map(i -> islemDto(i, null, null, null, List.of()));
        }

        List<Long> islemIds = sayfa.getContent().stream().map(TahsilatIslem::getId).toList();
        List<Long> cariIds = sayfa.getContent().stream()
                .map(TahsilatIslem::getCariId).filter(Objects::nonNull).distinct().toList();
        List<Long> kasaIds = sayfa.getContent().stream()
                .map(TahsilatIslem::getKasaId).filter(Objects::nonNull).distinct().toList();
        List<Long> bankaIds = sayfa.getContent().stream()
                .map(TahsilatIslem::getBankaId).filter(Objects::nonNull).distinct().toList();

        Map<Long, String> cariAdlari = new HashMap<>();
        if (!cariIds.isEmpty()) {
            cariHesapRepository.findAllById(cariIds)
                    .forEach(c -> cariAdlari.put(c.getId(), c.getAd()));
        }
        Map<Long, String> kasaAdlari = new HashMap<>();
        if (!kasaIds.isEmpty()) {
            kasaRepository.findAllById(kasaIds).forEach(k -> kasaAdlari.put(k.getId(), k.getAd()));
        }
        Map<Long, String> bankaAdlari = new HashMap<>();
        if (!bankaIds.isEmpty()) {
            bankaRepository.findAllById(bankaIds).forEach(b -> bankaAdlari.put(b.getId(), b.getAd()));
        }
        Map<Long, List<TahsilatIslemDTO.Dagilim>> dagilimlar = dagilimlariGetir(islemIds);

        return sayfa.map(i -> islemDto(i,
                cariAdlari.get(i.getCariId()),
                i.getKasaId() != null ? kasaAdlari.get(i.getKasaId()) : null,
                i.getBankaId() != null ? bankaAdlari.get(i.getBankaId()) : null,
                dagilimlar.getOrDefault(i.getId(), List.of())));
    }

    private TahsilatIslemDTO islemDto(TahsilatIslem islem, String cariAd, String kasaAd,
                                      String bankaAd, List<TahsilatIslemDTO.Dagilim> dagilimlar) {
        List<TahsilatIslemDTO.Dagilim> liste = dagilimlar != null ? dagilimlar : List.of();
        return TahsilatIslemDTO.builder()
                .id(islem.getId())
                .tarih(islem.getTarih())
                .cariId(islem.getCariId())
                .cariAd(cariAd)
                .tutar(islem.getTutar())
                .odemeYontemi(islem.getOdemeYontemi())
                .kasaAd(kasaAd)
                .bankaAd(bankaAd)
                .durum(islem.getDurum())
                .iptalGerekce(islem.getIptalGerekce())
                .olusturanKullaniciId(islem.getOlusturanKullaniciId())
                .satirSayisi(liste.size())
                .dagilimlar(liste)
                .build();
    }

    /**
     * İşlemlerin dağıtım satırlarını tek sorguda getirir. Native sorgu,
     * {@code @SQLRestriction("iptal = false")} nedeniyle gizlenen iptalli
     * hareketleri de döndürür.
     */
    private Map<Long, List<TahsilatIslemDTO.Dagilim>> dagilimlariGetir(List<Long> islemIds) {
        if (islemIds == null || islemIds.isEmpty()) return Map.of();
        Map<Long, List<TahsilatIslemDTO.Dagilim>> sonuc = new LinkedHashMap<>();
        for (Object[] r : hareketRepository.tahsilatDagilimlariniGetir(islemIds)) {
            Long islemId = r[0] != null ? ((Number) r[0]).longValue() : null;
            if (islemId == null) continue;
            Long hareketId = r[1] != null ? ((Number) r[1]).longValue() : null;
            Long faturaId = r[2] != null ? ((Number) r[2]).longValue() : null;
            String faturaNo = r[3] != null ? r[3].toString() : null;
            BigDecimal tutar = r[4] != null ? new BigDecimal(r[4].toString()) : BigDecimal.ZERO;
            String aciklama = r[5] != null ? r[5].toString() : null;
            boolean iptal = r[6] instanceof Boolean b ? b
                    : (r[6] instanceof Number n && n.intValue() != 0);
            sonuc.computeIfAbsent(islemId, k -> new ArrayList<>()).add(
                    TahsilatIslemDTO.Dagilim.builder()
                            .hareketId(hareketId)
                            .faturaId(faturaId)
                            .faturaNumarasi(faturaNo)
                            .tutar(tutar)
                            .aciklama(aciklama)
                            .iptal(iptal)
                            .build());
        }
        return sonuc;
    }

    private TahsilatIslemDTO tekIslemDto(TahsilatIslem islem) {
        String cariAd = cariHesapRepository.findById(islem.getCariId())
                .map(CariHesap::getAd).orElse(null);
        String kasaAd = islem.getKasaId() != null
                ? kasaRepository.findById(islem.getKasaId()).map(k -> k.getAd()).orElse(null) : null;
        String bankaAd = islem.getBankaId() != null
                ? bankaRepository.findById(islem.getBankaId()).map(b -> b.getAd()).orElse(null) : null;
        List<TahsilatIslemDTO.Dagilim> dagilimlar =
                dagilimlariGetir(List.of(islem.getId())).getOrDefault(islem.getId(), List.of());
        return islemDto(islem, cariAd, kasaAd, bankaAd, dagilimlar);
    }

    /**
     * Tahsilat işlemini gerekçeli olarak bütün halinde geri alır. İşlemdeki her
     * hareket {@code hareketIptal} ile ters kaydedilir (cari bakiye, fatura
     * ödenen/kalan, kasa/banka bakiyesi). Kısmi iptal yoktur; tek transaction.
     */
    @Transactional
    public TahsilatIslemDTO iptalEt(Long islemId, String gerekce, Long kullaniciId) {
        TahsilatIslem islem = tahsilatIslemRepository.findByIdForUpdate(islemId)
                .orElseThrow(() -> new ResourceNotFoundException("Tahsilat İşlemi", islemId));
        tenantChecker.check(islem.getSirketId(), "Tahsilat İşlemi");

        if (!islem.aktifMi()) {
            throw new BusinessException("Bu tahsilat işlemi zaten iptal edilmiş");
        }
        if (gerekce == null || gerekce.trim().length() < 3) {
            throw new BusinessException("İptal gerekçesi en az 3 karakter olmalıdır");
        }
        donemService.kilitKontrol(islem.getSirketId(), islem.getTarih(), "tahsilat iptali");

        List<Long> hareketIds = hareketRepository.findIdByTahsilatIslemId(islemId);
        for (Long hareketId : hareketIds) {
            hareketService.hareketIptal(hareketId);
        }

        islem.setDurum(TahsilatIslem.DURUM_IPTAL);
        islem.setIptalGerekce(gerekce.trim());
        islem.setIptalEdenKullaniciId(kullaniciId);
        islem.setIptalTarihi(LocalDateTime.now());
        TahsilatIslem kaydedilen = tahsilatIslemRepository.save(islem);

        log.info("Tahsilat işlemi iptal edildi -> id: {}, hareket sayısı: {}, gerekçe: {}",
                islemId, hareketIds.size(), gerekce);
        return tekIslemDto(kaydedilen);
    }

    /**
     * Aylık tahsilat hedefi upsert. Aynı (şirket, yıl, ay) için mevcut kayıt
     * güncellenir; para birimi TRY.
     */
    @Transactional
    public TahsilatHedefDTO hedefKaydet(Long sirketId, Integer yil, Integer ay,
                                        BigDecimal tutar, Long kullaniciId) {
        if (yil == null || yil < 2000 || yil > 2100) {
            throw new BusinessException("Yıl 2000 ile 2100 arasında olmalıdır");
        }
        if (ay == null || ay < 1 || ay > 12) {
            throw new BusinessException("Ay 1 ile 12 arasında olmalıdır");
        }
        if (tutar == null || tutar.signum() < 0) {
            throw new BusinessException("Hedef tutarı negatif olamaz");
        }
        tenantChecker.check(sirketId, "Tahsilat Hedefi");

        TahsilatHedef hedef = tahsilatHedefRepository.findBySirketIdAndYilAndAy(sirketId, yil, ay)
                .orElseGet(() -> TahsilatHedef.builder()
                        .sirketId(sirketId)
                        .yil(yil)
                        .ay(ay)
                        .olusturanKullaniciId(kullaniciId)
                        .build());
        hedef.setTutar(tutar);
        if (hedef.getId() != null) {
            hedef.setGuncellemeTarihi(LocalDateTime.now());
        }
        TahsilatHedef kaydedilen = tahsilatHedefRepository.save(hedef);

        return TahsilatHedefDTO.builder()
                .yil(kaydedilen.getYil())
                .ay(kaydedilen.getAy())
                .tutar(kaydedilen.getTutar())
                .build();
    }

    /**
     * Hedef–performans: seçili ayın hedefi, iptal edilmemiş tahsilat toplamı,
     * gerçekleşme yüzdesi, işlem sayısı, vadesi geçmiş toplam ve son 6 ay trendi.
     */
    @Transactional(readOnly = true)
    public TahsilatPerformansDTO performans(Long sirketId, Integer yil, Integer ay) {
        LocalDate bugun = LocalDate.now();
        int secilenYil = yil != null ? yil : bugun.getYear();
        int secilenAy = ay != null ? ay : bugun.getMonthValue();
        if (secilenYil < 2000 || secilenYil > 2100) {
            throw new BusinessException("Yıl 2000 ile 2100 arasında olmalıdır");
        }
        if (secilenAy < 1 || secilenAy > 12) {
            throw new BusinessException("Ay 1 ile 12 arasında olmalıdır");
        }
        YearMonth seciliAy = YearMonth.of(secilenYil, secilenAy);

        BigDecimal hedef = tahsilatHedefRepository
                .findBySirketIdAndYilAndAy(sirketId, secilenYil, secilenAy)
                .map(TahsilatHedef::getTutar).orElse(null);
        BigDecimal tahsilEdilen = tahsilatIslemRepository.aktifTahsilatToplami(
                sirketId, seciliAy.atDay(1), seciliAy.atEndOfMonth());
        long tahsilatSayisi = tahsilatIslemRepository.aktifTahsilatSayisi(
                sirketId, seciliAy.atDay(1), seciliAy.atEndOfMonth());

        BigDecimal gerceklesme = (hedef == null || hedef.signum() <= 0)
                ? null
                : tahsilEdilen.multiply(BigDecimal.valueOf(100))
                        .divide(hedef, 2, RoundingMode.HALF_UP);

        List<TahsilatPerformansDTO.Trend> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth t = seciliAy.minusMonths(i);
            BigDecimal tTahsil = tahsilatIslemRepository.aktifTahsilatToplami(
                    sirketId, t.atDay(1), t.atEndOfMonth());
            BigDecimal tHedef = tahsilatHedefRepository
                    .findBySirketIdAndYilAndAy(sirketId, t.getYear(), t.getMonthValue())
                    .map(TahsilatHedef::getTutar).orElse(null);
            trend.add(TahsilatPerformansDTO.Trend.builder()
                    .ay(t.toString())
                    .tahsilat(tTahsil)
                    .hedef(tHedef)
                    .build());
        }

        return TahsilatPerformansDTO.builder()
                .hedef(hedef)
                .tahsilEdilen(tahsilEdilen)
                .gerceklesmeYuzde(gerceklesme)
                .tahsilatSayisi(tahsilatSayisi)
                .vadesiGecmisToplam(vadesiGecmisToplam(sirketId))
                .trend(trend)
                .build();
    }

    private BigDecimal vadesiGecmisToplam(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        return faturaRepository.findTahsilatEdilecek(
                        sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI)
                .stream()
                .filter(f -> gecikmeGunu(vade(f), bugun) > 0)
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Seçili carilere toplu ödeme hatırlatma e-postası. E-postası olmayan veya
     * açık faturası bulunmayan cariler atlanır; gönderim hataları kısmi sonuçta
     * raporlanır (tek hata tüm işlemi düşürmez).
     */
    @Transactional(readOnly = true)
    public TopluHatirlatmaSonuc topluHatirlat(Long sirketId, List<Long> cariIds) {
        if (cariIds == null || cariIds.isEmpty()) {
            throw new BusinessException("En az bir cari seçilmelidir");
        }
        List<Long> temizIds = cariIds.stream().filter(Objects::nonNull).distinct().toList();
        if (temizIds.isEmpty()) {
            throw new BusinessException("En az bir cari seçilmelidir");
        }

        List<CariHesap> cariler = cariHesapRepository.findBySirketIdAndIdIn(sirketId, temizIds);
        int gonderilen = 0;
        int atlanan = 0;
        List<String> hatalar = new ArrayList<>();

        for (CariHesap cari : cariler) {
            if (cari.getEmail() == null || cari.getEmail().isBlank()) {
                atlanan++;
                continue;
            }
            List<Fatura> cariFaturalari = faturaRepository.findTahsilatEdilecekByCari(
                    sirketId, cari.getId(), Fatura.FaturaTur.SATIS,
                    Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI);
            if (cariFaturalari.isEmpty()) {
                atlanan++;
                continue;
            }
            boolean gonderildi = false;
            for (Fatura f : cariFaturalari) {
                LocalDate vade = vade(f);
                boolean ok = emailService.odemeHatimlaticiGonder(
                        cari.getEmail(),
                        f.getFaturaNumarasi(),
                        f.getGenelToplam() != null ? f.getGenelToplam().toString() : "0.00",
                        f.getKalanTutar() != null ? f.getKalanTutar().toString() : "0.00",
                        vade != null ? vade.format(VADE_FORMAT) : "-",
                        cari.getAd());
                if (ok) gonderildi = true;
            }
            if (gonderildi) {
                gonderilen++;
            } else {
                atlanan++;
                hatalar.add(cari.getAd() + " (" + cari.getEmail()
                        + "): e-posta gönderilemedi (SMTP yapılandırılmamış veya hata)");
            }
        }
        // Seçilen ancak bu şirkette bulunamayan id'ler de atlanan sayılır.
        long bulunamayan = temizIds.size() - cariler.size();
        if (bulunamayan > 0) atlanan += (int) bulunamayan;

        log.info("Toplu tahsilat hatırlatması -> gönderilen: {}, atlanan: {}, hata: {}",
                gonderilen, atlanan, hatalar.size());
        return TopluHatirlatmaSonuc.builder()
                .gonderilen(gonderilen)
                .atlanan(atlanan)
                .hatalar(hatalar)
                .build();
    }

    @Transactional(readOnly = true)
    public int hatirlat(Long cariId, Long sirketId) {
        List<Fatura> cariFaturalari = faturaRepository.findTahsilatEdilecekByCari(
                sirketId, cariId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, ODENDI_DURUMLARI);

        if (cariFaturalari.isEmpty()) {
            throw new ResourceNotFoundException("Cari için ödenmemiş fatura bulunamadı", cariId);
        }

        CariHesap cari = cariFaturalari.get(0).getCariHesap();
        if (cari.getEmail() == null || cari.getEmail().isBlank()) {
            throw new BusinessException("Bu cari hesap için e-posta adresi tanımlı değil");
        }

        int gonderilen = 0;
        for (Fatura f : cariFaturalari) {
            LocalDate vade = vade(f);
            boolean gonderildi = emailService.odemeHatimlaticiGonder(
                    cari.getEmail(),
                    f.getFaturaNumarasi(),
                    f.getGenelToplam() != null ? f.getGenelToplam().toString() : "0.00",
                    f.getKalanTutar() != null ? f.getKalanTutar().toString() : "0.00",
                    vade != null ? vade.format(VADE_FORMAT) : "-",
                    cari.getAd());
            if (gonderildi) {
                gonderilen++;
            }
        }
        if (gonderilen == 0) {
            throw new BusinessException("Ödeme hatırlatma e-postası gönderilemedi: SMTP yapılandırılmamış veya gönderim hatası");
        }
        log.info("Tahsilat hatırlatması gönderildi -> Cari: {}, Fatura sayısı: {}", cari.getAd(), gonderilen);
        return gonderilen;
    }

    private TahsilatDTO.CariOzet cariOzetiOlustur(List<Fatura> faturalar, LocalDate bugun) {
        CariHesap cari = faturalar.get(0).getCariHesap();

        BigDecimal toplam = faturalar.stream()
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gecikmis = faturalar.stream()
                .filter(f -> gecikmeGunu(vade(f), bugun) > 0)
                .map(Fatura::getKalanTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int maxGecikme = faturalar.stream()
                .mapToInt(f -> gecikmeGunu(vade(f), bugun))
                .max().orElse(0);

        List<TahsilatDTO.FaturaOzet> faturaOzetleri = faturalar.stream()
                .sorted(Comparator.comparing(f -> vade(f), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(f -> TahsilatDTO.FaturaOzet.builder()
                        .faturaId(f.getId())
                        .faturaNumarasi(f.getFaturaNumarasi())
                        .vadeTarihi(vade(f))
                        .kalanTutar(f.getKalanTutar())
                        .gecikmeGunu(gecikmeGunu(vade(f), bugun))
                        .build())
                .collect(Collectors.toList());

        return TahsilatDTO.CariOzet.builder()
                .cariId(cari.getId())
                .cariAd(cari.getAd())
                .telefon(cari.getTelefon())
                .email(cari.getEmail())
                .toplamAlacak(toplam)
                .gecikmisAlacak(gecikmis)
                .faturaSayisi(faturalar.size())
                .maxGecikmeGunu(maxGecikme)
                .aralik(aralik(maxGecikme))
                .faturalar(faturaOzetleri)
                .build();
    }

    private LocalDate vade(Fatura f) {
        if (f.getVadeTarihi() != null) return f.getVadeTarihi();
        CariHesap cari = f.getCariHesap();
        if (cari != null && cari.getOdemeVadesi() != null) {
            return f.getTarih().plusDays(cari.getOdemeVadesi());
        }
        return f.getTarih();
    }

    private int gecikmeGunu(LocalDate vade, LocalDate bugun) {
        if (vade == null) return 0;
        return (int) ChronoUnit.DAYS.between(vade, bugun);
    }

    private String aralik(int gun) {
        if (gun <= 0) return "Vadesi Gelmemiş";
        if (gun <= 30) return "0-30 Gün";
        if (gun <= 60) return "31-60 Gün";
        if (gun <= 90) return "61-90 Gün";
        return "90+ Gün";
    }

    /** Tahsilat tutarini secili kasaya GELIR hareketi olarak isler; cari harekete baglar. */
    private void kasaGirisiIsle(Long kasaId, BigDecimal tutar, CariHesap cari, LocalDate tarih, Long sirketId, Long kaynakHareketId) {
        com.raspel.erp.entity.finans.Kasa kasa = kasaRepository.findByIdForUpdate(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setBakiye(kasa.getBakiye() != null ? kasa.getBakiye().add(tutar) : tutar);
        kasaRepository.save(kasa);
        kasaHareketRepository.save(com.raspel.erp.entity.finans.KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(tutar)
                .hareketTarihi(tarih != null ? tarih : LocalDate.now())
                .aciklama("Tahsilat: " + (cari != null ? cari.getAd() : ""))
                .kaynakTip("TAHSILAT")
                .kaynakId(kaynakHareketId)
                .build());
    }

    /** Tahsilat tutarini secili banka hesabina alacak hareketi olarak isler; cari harekete baglar. */
    private void bankaGirisiIsle(Long bankaId, BigDecimal tutar, CariHesap cari, LocalDate tarih, Long sirketId, Long kaynakHareketId) {
        com.raspel.erp.entity.finans.Banka banka = bankaRepository.findByIdForUpdate(bankaId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", bankaId));
        tenantChecker.check(banka.getSirketId(), "Banka");
        banka.setBakiye(banka.getBakiye() != null ? banka.getBakiye().add(tutar) : tutar);
        bankaRepository.save(banka);
        bankaHareketiRepository.save(com.raspel.erp.entity.finans.BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(tarih != null ? tarih : LocalDate.now())
                .aciklama("Tahsilat: " + (cari != null ? cari.getAd() : ""))
                .borc(BigDecimal.ZERO)
                .alacak(tutar)
                .kaynakTip("TAHSILAT")
                .kaynakId(kaynakHareketId)
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(sirketId)
                .build());
    }
}
