package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.entity.ik.PersonelMasrafTalep;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.OnayKarar;
import com.raspel.erp.entity.ticaret.SatinalmaTalep;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.OnayKararRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.service.ik.PersonelIzinService;
import com.raspel.erp.service.ik.PersonelMasrafTalepService;
import com.raspel.erp.service.ticaret.SatinalmaTalepService;
import com.raspel.erp.service.ticaret.SiparisService;
import com.raspel.erp.util.AramaTemizleyici;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Birleşik onay servisi.
 *
 * <p>İzin, masraf/avans, satınalma ve sipariş onaylarını tek noktadan
 * listeler ve karara bağlar. Modül davranışları değiştirilmez: karar,
 * ilgili modülün mevcut servis metodu çağrılarak uygulanır; ardından
 * {@code sistem.onay_karar} tablosuna denetim kaydı yazılır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnayService {

    /** "Tümü" görünümünde tür başına çekilen en fazla kayıt (dokümante sınır). */
    private static final int TUMU_TUR_LIMIT = 100;

    /** Excel çıktısı için tür başına üst sınır (ExcelExportController ile uyumlu). */
    private static final int EXCEL_LIMIT = 10000;

    /** API sayfa boyutu üst sınırı (tek istekte aşırı veri çekilmesini engeller). */
    private static final int MAKS_SAYFA_BOYUTU = 200;

    private static final String IZIN_BEKLEYEN = "BEKLEMEDE";
    private static final String MASRAF_BEKLEYEN = "BEKLEMEDE";
    private static final String SATINALMA_BEKLEYEN = "TASLAK";
    // Normal sipariş akışı "TEKLIF", saha portalı "BEKLIYOR" gönderir.
    private static final List<String> SIPARIS_BEKLEYEN = List.of("TEKLIF", "BEKLIYOR");

    /** Tümü görünümünde birleşik sıralama: en yeni oluşturulan önce. */
    private static final Comparator<OnayBekleyenDTO> BEKLEYEN_SIRALAMA =
            Comparator.comparing(OnayBekleyenDTO::getOlusturmaTarihi,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(OnayBekleyenDTO::getTarih,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(OnayBekleyenDTO::getTur,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(OnayBekleyenDTO::getKaynakId,
                            Comparator.nullsLast(Comparator.reverseOrder()));

    private final OnayKararRepository onayKararRepository;
    private final PersonelIzinRepository personelIzinRepository;
    private final PersonelRepository personelRepository;
    private final PersonelMasrafTalepRepository personelMasrafTalepRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SatinalmaTalepRepository satinalmaTalepRepository;
    private final SatinalmaTalepKalemRepository satinalmaTalepKalemRepository;
    private final SiparisRepository siparisRepository;
    private final CariHesapRepository cariHesapRepository;

    private final PersonelIzinService personelIzinService;
    private final PersonelMasrafTalepService personelMasrafTalepService;
    private final SatinalmaTalepService satinalmaTalepService;
    private final SiparisService siparisService;

    // ------------------------------------------------------------------
    // Özet
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OnayOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return OnayOzetDTO.builder().build();
        }
        long izin = personelIzinRepository.countByDurumAndSirketId(IZIN_BEKLEYEN, sirketId);
        long masraf = personelMasrafTalepRepository.countBySirketIdAndDurum(sirketId, MASRAF_BEKLEYEN);
        long satinalma = satinalmaTalepRepository.countBySirketIdAndDurum(sirketId, SATINALMA_BEKLEYEN);
        long siparis = siparisRepository.countBySirketIdAndDurumIn(sirketId, SIPARIS_BEKLEYEN);
        return OnayOzetDTO.builder()
                .izin(izin).masraf(masraf).satinalma(satinalma).siparis(siparis)
                .toplam(izin + masraf + satinalma + siparis)
                .build();
    }

    // ------------------------------------------------------------------
    // Bekleyenler
    // ------------------------------------------------------------------

    /**
     * Bekleyen onay kayıtları.
     *
     * <p>{@code tur} boş ise her türden en fazla {@link #TUMU_TUR_LIMIT} kayıt
     * birleştirilir ve oluşturma tarihine göre yeniden eskiye sıralanır
     * (birleşik sayfalama). {@code tur} dolu ise yalnızca o tür sunucu
     * tarafında sayfalanır.
     */
    @Transactional(readOnly = true)
    public Page<OnayBekleyenDTO> bekleyen(Long sirketId, String tur, String q, Pageable pageable) {
        Pageable sayfa = (pageable != null && pageable.isPaged())
                ? pageable : PageRequest.of(0, 20);
        if (sirketId == null) {
            return Page.empty(sayfa);
        }
        OnayTur turFiltre = turCozOpsiyonel(tur);
        String arama = AramaTemizleyici.like(q);

        if (turFiltre != null) {
            return tekTurBekleyen(sirketId, turFiltre, arama, sayfa);
        }

        List<OnayBekleyenDTO> tumu = new ArrayList<>();
        for (OnayTur t : OnayTur.values()) {
            tumu.addAll(turBekleyenListesi(sirketId, t, arama, TUMU_TUR_LIMIT));
        }
        tumu.sort(BEKLEYEN_SIRALAMA);
        return sayfala(tumu, sayfa);
    }

    /** Excel çıktısı için bekleyenler (tür başına en fazla {@link #EXCEL_LIMIT}). */
    @Transactional(readOnly = true)
    public List<OnayBekleyenDTO> excel(Long sirketId, String tur, String q) {
        if (sirketId == null) {
            return List.of();
        }
        OnayTur turFiltre = turCozOpsiyonel(tur);
        String arama = AramaTemizleyici.like(q);
        if (turFiltre != null) {
            return turBekleyenListesi(sirketId, turFiltre, arama, EXCEL_LIMIT);
        }
        List<OnayBekleyenDTO> tumu = new ArrayList<>();
        for (OnayTur t : OnayTur.values()) {
            tumu.addAll(turBekleyenListesi(sirketId, t, arama, TUMU_TUR_LIMIT));
        }
        tumu.sort(BEKLEYEN_SIRALAMA);
        return tumu;
    }

    private Page<OnayBekleyenDTO> tekTurBekleyen(Long sirketId, OnayTur tur, String q, Pageable pageable) {
        Pageable sayfa = sayfaliPageable(pageable, turSiralamasi(tur));
        return switch (tur) {
            case IZIN -> {
                Page<PersonelIzin> sayfaVerisi = personelIzinRepository
                        .onayBekleyenler(sirketId, IZIN_BEKLEYEN, q, sayfa);
                yield new PageImpl<>(izinleriEsle(sayfaVerisi.getContent()),
                        sayfaVerisi.getPageable(), sayfaVerisi.getTotalElements());
            }
            case MASRAF -> {
                Page<PersonelMasrafTalep> sayfaVerisi = personelMasrafTalepRepository
                        .onayBekleyenler(sirketId, MASRAF_BEKLEYEN, q, sayfa);
                yield new PageImpl<>(masraflariEsle(sayfaVerisi.getContent()),
                        sayfaVerisi.getPageable(), sayfaVerisi.getTotalElements());
            }
            case SATINALMA -> {
                Page<SatinalmaTalep> sayfaVerisi = satinalmaTalepRepository
                        .onayBekleyenler(sirketId, SATINALMA_BEKLEYEN, q, sayfa);
                yield new PageImpl<>(satinalmalariEsle(sayfaVerisi.getContent()),
                        sayfaVerisi.getPageable(), sayfaVerisi.getTotalElements());
            }
            case SIPARIS -> {
                Page<Siparis> sayfaVerisi = siparisRepository
                        .onayBekleyenler(sirketId, SIPARIS_BEKLEYEN, q, sayfa);
                yield new PageImpl<>(siparisleriEsle(sayfaVerisi.getContent()),
                        sayfaVerisi.getPageable(), sayfaVerisi.getTotalElements());
            }
        };
    }

    private List<OnayBekleyenDTO> turBekleyenListesi(Long sirketId, OnayTur tur, String q, int limit) {
        return tekTurBekleyen(sirketId, tur, q, PageRequest.of(0, limit, turSiralamasi(tur)))
                .getContent();
    }

    private Sort turSiralamasi(OnayTur tur) {
        // PersonelIzin'de "tarih" alanı yok; başlangıç tarihi kullanılır.
        String alan = tur == OnayTur.IZIN ? "baslangic" : "tarih";
        return Sort.by(Sort.Direction.DESC, alan).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private Pageable sayfaliPageable(Pageable pageable, Sort sort) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, MAKS_SAYFA_BOYUTU, sort);
        }
        int sayfa = Math.max(pageable.getPageNumber(), 0);
        int boyut = pageable.getPageSize();
        if (boyut <= 0) boyut = 20;
        if (boyut > MAKS_SAYFA_BOYUTU) boyut = MAKS_SAYFA_BOYUTU;
        return PageRequest.of(sayfa, boyut, sort);
    }

    /**
     * "Tümü" görünümü: birleşik liste tek sayfa olarak döner (tür başına
     * {@link #TUMU_TUR_LIMIT} sınırı dokümante edilmiştir); toplam sayı
     * birleşik listenin boyutudur.
     */
    private Page<OnayBekleyenDTO> sayfala(List<OnayBekleyenDTO> liste, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(liste);
        }
        return new PageImpl<>(liste, pageable, liste.size());
    }

    // ------------------------------------------------------------------
    // Tür eşlemeleri (DTO)
    // ------------------------------------------------------------------

    private List<OnayBekleyenDTO> izinleriEsle(List<PersonelIzin> liste) {
        if (liste == null || liste.isEmpty()) {
            return List.of();
        }
        Map<Long, String> adlar = personelAdlari(
                liste.stream().map(PersonelIzin::getPersonelId).toList());
        List<OnayBekleyenDTO> sonuc = new ArrayList<>(liste.size());
        for (PersonelIzin i : liste) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("bitisTarihi", i.getBitis());
            meta.put("gunSayisi", i.getGunSayisi());
            sonuc.add(OnayBekleyenDTO.builder()
                    .tur(OnayTur.IZIN.name())
                    .kaynakId(i.getId())
                    .baslik(adlar.get(i.getPersonelId()))
                    .altBaslik(i.getIzinTuru())
                    .tarih(i.getBaslangic())
                    .olusturmaTarihi(i.getOlusturmaTarihi())
                    .durum(i.getDurum())
                    .aciklama(i.getAciklama())
                    .meta(meta)
                    .build());
        }
        return sonuc;
    }

    private List<OnayBekleyenDTO> masraflariEsle(List<PersonelMasrafTalep> liste) {
        if (liste == null || liste.isEmpty()) {
            return List.of();
        }
        Map<Long, String> personelAdlari = personelAdlari(
                liste.stream().map(PersonelMasrafTalep::getPersonelId).toList());
        Map<Long, String> kullaniciAdlari = kullaniciAdlari(
                liste.stream().map(PersonelMasrafTalep::getKullaniciId).toList());
        List<OnayBekleyenDTO> sonuc = new ArrayList<>(liste.size());
        for (PersonelMasrafTalep t : liste) {
            // Personel bağlı değilse saha portalı kullanıcısının adı gösterilir.
            String baslik = t.getPersonelId() != null ? personelAdlari.get(t.getPersonelId()) : null;
            if (baslik == null && t.getKullaniciId() != null) {
                baslik = kullaniciAdlari.get(t.getKullaniciId());
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("avansMi", "AVANS".equalsIgnoreCase(t.getTur()));
            meta.put("kategori", t.getKategori());
            sonuc.add(OnayBekleyenDTO.builder()
                    .tur(OnayTur.MASRAF.name())
                    .kaynakId(t.getId())
                    .baslik(baslik)
                    .altBaslik(t.getTur())
                    .tutar(t.getTutar())
                    .tarih(t.getTarih())
                    .olusturmaTarihi(t.getOlusturmaTarihi())
                    .durum(t.getDurum())
                    .aciklama(t.getAciklama())
                    .meta(meta)
                    .build());
        }
        return sonuc;
    }

    private List<OnayBekleyenDTO> satinalmalariEsle(List<SatinalmaTalep> liste) {
        if (liste == null || liste.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> toplamlar = satinalmaToplamlari(
                liste.stream().map(SatinalmaTalep::getId).toList());
        List<OnayBekleyenDTO> sonuc = new ArrayList<>(liste.size());
        for (SatinalmaTalep t : liste) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("talepTarihi", t.getTarih());
            meta.put("talepEden", t.getTalepEden());
            sonuc.add(OnayBekleyenDTO.builder()
                    .tur(OnayTur.SATINALMA.name())
                    .kaynakId(t.getId())
                    .baslik(t.getTalepNo() != null && !t.getTalepNo().isBlank()
                            ? t.getTalepNo() : t.getAciklama())
                    .altBaslik(t.getDepartman() != null && !t.getDepartman().isBlank()
                            ? t.getDepartman() : t.getTalepEden())
                    .tutar(toplamlar.get(t.getId()))
                    .tarih(t.getTarih())
                    .olusturmaTarihi(t.getOlusturmaTarihi())
                    .durum(t.getDurum())
                    .aciklama(t.getAciklama())
                    .meta(meta)
                    .build());
        }
        return sonuc;
    }

    private List<OnayBekleyenDTO> siparisleriEsle(List<Siparis> liste) {
        if (liste == null || liste.isEmpty()) {
            return List.of();
        }
        Map<Long, String> cariAdlari = cariAdlari(
                liste.stream().map(Siparis::getCariHesapId).toList());
        List<OnayBekleyenDTO> sonuc = new ArrayList<>(liste.size());
        for (Siparis s : liste) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("siparisTarihi", s.getTarih());
            meta.put("cariHesapId", s.getCariHesapId());
            sonuc.add(OnayBekleyenDTO.builder()
                    .tur(OnayTur.SIPARIS.name())
                    .kaynakId(s.getId())
                    .baslik(s.getSiparisNo())
                    .altBaslik(s.getCariHesapId() != null ? cariAdlari.get(s.getCariHesapId()) : null)
                    .tutar(s.getGenelToplam())
                    .tarih(s.getTarih())
                    .olusturmaTarihi(s.getOlusturmaTarihi())
                    .durum(s.getDurum())
                    .aciklama(s.getAciklama())
                    .meta(meta)
                    .build());
        }
        return sonuc;
    }

    private Map<Long, String> personelAdlari(List<Long> idler) {
        List<Long> temiz = temizIdler(idler);
        if (temiz.isEmpty()) {
            return Map.of();
        }
        return personelRepository.findAllById(temiz).stream()
                .collect(Collectors.toMap(Personel::getId, OnayService::tamAd, (a, b) -> a));
    }

    private Map<Long, String> kullaniciAdlari(List<Long> idler) {
        List<Long> temiz = temizIdler(idler);
        if (temiz.isEmpty()) {
            return Map.of();
        }
        return kullaniciRepository.findAllById(temiz).stream()
                .collect(Collectors.toMap(Kullanici::getId,
                        k -> k.getDisplayName() != null && !k.getDisplayName().isBlank()
                                ? k.getDisplayName() : k.getUsername(),
                        (a, b) -> a));
    }

    private Map<Long, String> cariAdlari(List<Long> idler) {
        List<Long> temiz = temizIdler(idler);
        if (temiz.isEmpty()) {
            return Map.of();
        }
        return cariHesapRepository.findAllById(temiz).stream()
                .collect(Collectors.toMap(CariHesap::getId, CariHesap::getAd, (a, b) -> a));
    }

    private List<Long> temizIdler(List<Long> idler) {
        if (idler == null) return List.of();
        return idler.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static String tamAd(Personel p) {
        if (p == null) return null;
        String ad = p.getAd() != null ? p.getAd().trim() : "";
        String soyad = p.getSoyad() != null ? p.getSoyad().trim() : "";
        String tam = (ad + " " + soyad).trim();
        return tam.isEmpty() ? null : tam;
    }

    /** Satınalma taleplerinin tahmini toplamı (kalemlerden, tek sorgu). */
    private Map<Long, BigDecimal> satinalmaToplamlari(List<Long> talepIdleri) {
        List<Long> temiz = temizIdler(talepIdleri);
        if (temiz.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> harita = new HashMap<>();
        for (Object[] satir : satinalmaTalepKalemRepository.toplamTutarByTalepIds(temiz)) {
            if (satir == null || satir.length < 2 || satir[0] == null) continue;
            Long talepId = ((Number) satir[0]).longValue();
            BigDecimal tutar = satir[1] instanceof BigDecimal b
                    ? b : (satir[1] != null ? new BigDecimal(satir[1].toString()) : null);
            if (tutar != null) harita.put(talepId, tutar);
        }
        return harita;
    }

    // ------------------------------------------------------------------
    // Karar
    // ------------------------------------------------------------------

    /**
     * Tek kayıt için onay/red kararı.
     *
     * <p>Önce ilgili modülün mevcut servis metodu çağrılır; başarılıysa karar
     * kaydı yazılır. Modül hatası olduğu gibi (404/400) dışarı bırakılır ve
     * karar kaydı oluşmaz.
     */
    @Transactional
    public OnayKararDTO karar(Long sirketId, Long kullaniciId, String kullaniciAd, OnayKararRequest req) {
        if (req == null) {
            throw new BusinessException("Karar isteği boş olamaz");
        }
        OnayTur tur = turCoz(req.getTur());
        String karar = kararCoz(req.getKarar());
        String gerekce = gerekceDogrula(karar, req.getGerekce());
        OnayKarar kayit = tekKarar(sirketId, kullaniciId, kullaniciAd,
                tur, req.getKaynakId(), karar, gerekce);
        return entityToDTO(kayit);
    }

    /**
     * Toplu onay/red.
     *
     * <p>Bilinçli olarak sınıf/metot seviyesinde transaction AÇILMAZ: her kayıt
     * için modül servisi kendi transaction'ında çalışır. Böylece bir kaydın
     * hatası diğerlerini geri almaz (kayıt bazında bağımsızlık).
     */
    public OnayTopluSonucDTO toplu(Long sirketId, Long kullaniciId, String kullaniciAd,
                                   OnayTopluRequest req) {
        if (req == null) {
            throw new BusinessException("Toplu karar isteği boş olamaz");
        }
        OnayTur tur = turCoz(req.getTur());
        String karar = kararCoz(req.getKarar());
        String gerekce = gerekceDogrula(karar, req.getGerekce());
        if (req.getKaynakIds() == null || req.getKaynakIds().isEmpty()) {
            throw new BusinessException("En az bir kayıt seçilmelidir");
        }

        int basarili = 0;
        List<OnayTopluSonucDTO.Atlanan> atlanan = new ArrayList<>();
        for (Long kaynakId : req.getKaynakIds()) {
            try {
                tekKarar(sirketId, kullaniciId, kullaniciAd, tur, kaynakId, karar, gerekce);
                basarili++;
            } catch (Exception e) {
                log.warn("Toplu onay kararı uygulanamadı - tür={}, kaynakId={}: {}",
                        tur, kaynakId, e.getMessage());
                atlanan.add(OnayTopluSonucDTO.Atlanan.builder()
                        .kaynakId(kaynakId)
                        .mesaj(kullaniciMesaji(e))
                        .build());
            }
        }
        return OnayTopluSonucDTO.builder().basarili(basarili).atlanan(atlanan).build();
    }

    private OnayKarar tekKarar(Long sirketId, Long kullaniciId, String kullaniciAd,
                               OnayTur tur, Long kaynakId, String karar, String gerekce) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }
        if (kaynakId == null) {
            throw new BusinessException("Kaynak kayıt seçilmelidir");
        }
        String oncekiDurum = oncekiDurum(tur, kaynakId);
        String yeniDurum = hedefDurum(tur, karar);

        // Önce modül işlemi; başarısızsa aşağıya geçilmez (karar kaydı yok).
        modulKarari(tur, kaynakId, karar, kullaniciAd, gerekce);

        return onayKararRepository.save(OnayKarar.builder()
                .sirketId(sirketId)
                .tur(tur.name())
                .kaynakId(kaynakId)
                .karar(karar)
                .gerekce(gerekce)
                .oncekiDurum(oncekiDurum)
                .yeniDurum(yeniDurum)
                .kararVerenId(kullaniciId)
                .kararVerenAd(kullaniciAd)
                .olusturmaTarihi(LocalDateTime.now())
                .build());
    }

    /** Modül çağrısı öncesi mevcut durum (kayıt yoksa null; modül çağrısı hata verir). */
    private String oncekiDurum(OnayTur tur, Long kaynakId) {
        return switch (tur) {
            case IZIN -> personelIzinRepository.findById(kaynakId)
                    .map(PersonelIzin::getDurum).orElse(null);
            case MASRAF -> personelMasrafTalepRepository.findById(kaynakId)
                    .map(PersonelMasrafTalep::getDurum).orElse(null);
            case SATINALMA -> satinalmaTalepRepository.findById(kaynakId)
                    .map(SatinalmaTalep::getDurum).orElse(null);
            case SIPARIS -> siparisRepository.findById(kaynakId)
                    .map(Siparis::getDurum).orElse(null);
        };
    }

    private String hedefDurum(OnayTur tur, String karar) {
        boolean onay = "ONAY".equals(karar);
        return switch (tur) {
            case IZIN, MASRAF, SATINALMA -> onay ? "ONAYLANDI" : "REDDEDILDI";
            case SIPARIS -> onay ? "HAZIRLANIYOR" : "IPTAL";
        };
    }

    private void modulKarari(OnayTur tur, Long kaynakId, String karar,
                             String kullaniciAd, String gerekce) {
        boolean onay = "ONAY".equals(karar);
        switch (tur) {
            case IZIN -> personelIzinService.durumGuncelle(
                    kaynakId, hedefDurum(tur, karar), kullaniciAd);
            case MASRAF -> {
                if (onay) {
                    personelMasrafTalepService.onayla(kaynakId, kullaniciAd, gerekce);
                } else {
                    personelMasrafTalepService.reddet(kaynakId, kullaniciAd, gerekce);
                }
            }
            case SATINALMA -> satinalmaTalepService.durumGuncelle(kaynakId, hedefDurum(tur, karar));
            case SIPARIS -> siparisService.durumGuncelle(kaynakId, hedefDurum(tur, karar));
        }
    }

    // ------------------------------------------------------------------
    // Geçmiş
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<OnayKararDTO> gecmis(Long sirketId, String tur, String karar, String q,
                                     LocalDate baslangic, LocalDate bitis, Pageable pageable) {
        Pageable sayfa = (pageable != null && pageable.isPaged())
                ? pageable : PageRequest.of(0, 20);
        if (sirketId == null) {
            return Page.empty(sayfa);
        }
        OnayTur turFiltre = turCozOpsiyonel(tur);
        String kararFiltre = (karar == null || karar.isBlank() || "TUMU".equalsIgnoreCase(karar.trim()))
                ? null : kararCoz(karar);
        String arama = AramaTemizleyici.like(q);
        LocalDateTime bas = baslangic != null ? baslangic.atStartOfDay() : null;
        // Bitiş günü dahil: ertesi günün başlangıcına kadar.
        LocalDateTime son = bitis != null ? bitis.plusDays(1).atStartOfDay() : null;

        Pageable siraliSayfa = PageRequest.of(
                Math.max(sayfa.getPageNumber(), 0),
                Math.min(Math.max(sayfa.getPageSize(), 1), MAKS_SAYFA_BOYUTU),
                Sort.by(Sort.Direction.DESC, "olusturmaTarihi")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        return onayKararRepository.filtrele(sirketId,
                        turFiltre != null ? turFiltre.name() : null,
                        kararFiltre, arama, bas, son, siraliSayfa)
                .map(this::entityToDTO);
    }

    // ------------------------------------------------------------------
    // Doğrulama / dönüşüm
    // ------------------------------------------------------------------

    private OnayTur turCozOpsiyonel(String tur) {
        if (tur == null || tur.isBlank() || "TUMU".equalsIgnoreCase(tur.trim())) {
            return null;
        }
        return turCoz(tur);
    }

    private OnayTur turCoz(String tur) {
        if (tur == null || tur.isBlank()) {
            throw new BusinessException("Onay türü zorunludur");
        }
        String temiz = tur.trim().toUpperCase(Locale.ROOT);
        try {
            return OnayTur.valueOf(temiz);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz onay türü: " + tur);
        }
    }

    private String kararCoz(String karar) {
        if (karar == null || karar.isBlank()) {
            throw new BusinessException("Karar zorunludur (ONAY veya RED)");
        }
        String temiz = karar.trim().toUpperCase(Locale.ROOT);
        if (!"ONAY".equals(temiz) && !"RED".equals(temiz)) {
            throw new BusinessException("Geçersiz karar: " + karar + " (ONAY veya RED olmalıdır)");
        }
        return temiz;
    }

    /** RED kararında gerekçe en az 3 karakter olmalıdır. */
    private String gerekceDogrula(String karar, String gerekce) {
        String temiz = gerekce != null ? gerekce.trim() : null;
        if (temiz != null && temiz.isEmpty()) {
            temiz = null;
        }
        if ("RED".equals(karar) && (temiz == null || temiz.length() < 3)) {
            throw new BusinessException("Red için gerekçe en az 3 karakter olmalıdır");
        }
        return temiz;
    }

    /** Toplu işlemde kullanıcıya gösterilecek mesaj; teknik detay sızdırılmaz. */
    private String kullaniciMesaji(Exception e) {
        if (e instanceof BusinessException || e instanceof ResourceNotFoundException) {
            return e.getMessage() != null ? e.getMessage() : "Karar uygulanamadı";
        }
        return "Karar uygulanamadı";
    }

    private OnayKararDTO entityToDTO(OnayKarar k) {
        return OnayKararDTO.builder()
                .id(k.getId())
                .tur(k.getTur())
                .kaynakId(k.getKaynakId())
                .karar(k.getKarar())
                .gerekce(k.getGerekce())
                .oncekiDurum(k.getOncekiDurum())
                .yeniDurum(k.getYeniDurum())
                .kararVerenId(k.getKararVerenId())
                .kararVerenAd(k.getKararVerenAd())
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .build();
    }
}
