package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.dto.sube.DepoHareketDTO;
import com.raspel.erp.dto.sube.DepoDetayOzetDTO;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import com.raspel.erp.repository.sube.SubeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.entity.envanter.Stok;

@Service
@Transactional
@RequiredArgsConstructor
public class DepoService {

    private final DepoRepository depoRepository;
    private final DepoStokRepository depoStokRepository;
    private final SubeRepository subeRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.envanter.StokService stokService;
    private final com.raspel.erp.repository.sube.DepoTransferRepository depoTransferRepository;
    private final StokHareketRepository stokHareketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final DepoYetki depoYetki;

    /**
     * Depolar ekranı KPI özeti: depo sayıları, stok kalemi/sıfır stok ve
     * bekleyen transfer talebi sayısı (şirket kapsamlı).
     */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sube.DepoOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.sube.DepoOzetDTO.builder().build();
        }
        long toplam = depoRepository.countBySirketId(sirketId);
        long aktif = depoRepository.countBySirketIdAndAktifTrue(sirketId);
        long bekleyen = depoTransferRepository.countBySirketIdAndDurum(sirketId, "BEKLIYOR");
        long toplamKalem = 0;
        long sifirKalem = 0;
        Object[] stokOzet = depoStokRepository.ozet(sirketId);
        if (stokOzet != null && stokOzet.length >= 2) {
            if (stokOzet[0] != null) toplamKalem = ((Number) stokOzet[0]).longValue();
            if (stokOzet[1] != null) sifirKalem = ((Number) stokOzet[1]).longValue();
        }
        return com.raspel.erp.dto.sube.DepoOzetDTO.builder()
                .toplamDepo(toplam)
                .aktifDepo(aktif)
                .pasifDepo(toplam - aktif)
                .stokKalemiSayisi(toplamKalem)
                .sifirStokluKalem(sifirKalem)
                .bekleyenTalep(bekleyen)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<DepoDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return tumunuGetir(sirketId, null, null, null, false, pageable);
    }

    /**
     * Operasyon listesi: arama + sube/aktif/kritik filtresi + sayfalama.
     * Sayfa basina sabit sorgu: depo sayfasi, gruplu istatistik, kullanici adlari.
     */
    @Transactional(readOnly = true)
    public Page<DepoDTO> tumunuGetir(Long sirketId, String q, Long subeId,
                                     Boolean aktif, Boolean kritik, Pageable pageable) {
        String arama = (q == null || q.isBlank())
                ? null
                : "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        Map<Long, String> subeHaritasi = subeRepository.findBySirketIdOrderByAdAsc(sirketId, Pageable.unpaged()).stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getAd()));
        Page<Depo> sayfa = depoRepository.filtreli(sirketId, arama, subeId, aktif,
                Boolean.TRUE.equals(kritik), pageable);
        List<Long> idler = sayfa.getContent().stream().map(Depo::getId).collect(Collectors.toList());
        Map<Long, Object[]> istatistikler = idler.isEmpty() ? Map.of()
                : depoStokRepository.depoIstatistikleri(idler).stream()
                        .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> r));
        Map<Long, String> sorumlular = kullaniciAdlari(sayfa.getContent().stream()
                .map(Depo::getSorumluKullaniciId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        return sayfa.map(d -> entityToDTO(d, subeHaritasi.get(d.getSubeId()),
                istatistikler.get(d.getId()),
                d.getSorumluKullaniciId() != null ? sorumlular.get(d.getSorumluKullaniciId()) : null));
    }

    /** Tek depo icin operasyon ozeti: KPI'lar + son 5 hareket. */
    @Transactional(readOnly = true)
    public DepoDetayOzetDTO detayOzet(Long depoId) {
        Depo d = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(d.getSirketId(), "Depo");
        Object[] st = depoStokRepository.depoIstatistikleri(List.of(depoId)).stream()
                .findFirst().orElse(null);
        List<StokHareket> son = stokHareketRepository.findTop5ByDepoIdOrderByOlusturmaTarihiDesc(depoId);
        Map<Long, String> adlar = kullaniciAdlari(son.stream()
                .map(StokHareket::getKullaniciId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        List<DepoHareketDTO> hareketler = son.stream().map(h -> DepoHareketDTO.builder()
                .id(h.getId())
                .tarih(h.getOlusturmaTarihi())
                .hareketTarihi(h.getHareketTarihi())
                .tur(h.getTur())
                .kaynakTip(h.getKaynakTip())
                .stokId(h.getStok() != null ? h.getStok().getId() : null)
                .stokKod(h.getStok() != null ? h.getStok().getStokKodu() : null)
                .stokAd(h.getStok() != null ? h.getStok().getAd() : null)
                .miktar(h.getMiktar())
                .kullaniciAd(h.getKullaniciId() != null ? adlar.get(h.getKullaniciId()) : null)
                .aciklama(h.getAciklama())
                .build()).collect(Collectors.toList());
        long kalem = sayi(st, 1);
        return DepoDetayOzetDTO.builder()
                .depoId(depoId)
                .kalemSayisi(kalem)
                .sifirStokSayisi(sayi(st, 2))
                .kritikStokSayisi(sayi(st, 3))
                .stokDegeri(para(st, 4))
                .dolulukYuzde(doluluk(kalem, d.getKapasiteKalem()))
                .sonHareketler(hareketler)
                .build();
    }

    @Transactional(readOnly = true)
    public DepoDTO getir(Long id) {
        Depo d = depoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", id));
        tenantChecker.check(d.getSirketId(), "Depo");
        String subeAd = subeRepository.findById(d.getSubeId())
                .map(s -> s.getAd()).orElse(null);
        Object[] st = depoStokRepository.depoIstatistikleri(List.of(id)).stream()
                .findFirst().orElse(null);
        String sorumluAd = d.getSorumluKullaniciId() != null
                ? kullaniciAdlari(java.util.Set.of(d.getSorumluKullaniciId())).get(d.getSorumluKullaniciId())
                : null;
        return entityToDTO(d, subeAd, st, sorumluAd);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DepoDTO olustur(DepoDTO dto) {
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("Depo adı boş olamaz");
        }
        if (dto.getSubeId() == null) {
            throw new BusinessException("Bağlı şube seçilmelidir");
        }
        if (depoRepository.existsBySirketIdAndAdIgnoreCase(dto.getSirketId(), dto.getAd().trim())) {
            throw new BusinessException("Bu isimde bir depo zaten mevcut: " + dto.getAd());
        }
        if (dto.getKapasiteKalem() != null && dto.getKapasiteKalem() < 0) {
            throw new BusinessException("Kapasite negatif olamaz");
        }
        Depo d = Depo.builder()
                .ad(dto.getAd().trim()).adres(dto.getAdres())
                .yetkili(dto.getYetkili()).subeId(dto.getSubeId())
                .sirketId(dto.getSirketId())
                .kapasiteKalem(dto.getKapasiteKalem())
                .sorumluKullaniciId(dto.getSorumluKullaniciId())
                .build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Depo");
        return entityToDTO(depoRepository.save(d), null);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DepoDTO guncelle(Long id, DepoDTO dto) {
        Depo d = depoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", id));
        tenantChecker.check(d.getSirketId(), "Depo");
        if (dto.getAd() != null) d.setAd(dto.getAd());
        if (dto.getAdres() != null) d.setAdres(dto.getAdres());
        if (dto.getYetkili() != null) d.setYetkili(dto.getYetkili());
        if (dto.getSubeId() != null) d.setSubeId(dto.getSubeId());
        if (dto.getAktif() != null) d.setAktif(dto.getAktif());
        if (dto.getKapasiteKalem() != null) {
            if (dto.getKapasiteKalem() < 0) throw new BusinessException("Kapasite negatif olamaz");
            d.setKapasiteKalem(dto.getKapasiteKalem());
        }
        if (dto.getSorumluKullaniciId() != null) d.setSorumluKullaniciId(dto.getSorumluKullaniciId());
        return entityToDTO(depoRepository.save(d), null);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Depo d = depoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", id));
        tenantChecker.check(d.getSirketId(), "Depo");
        depoRepository.deleteById(id);
    }

    /**
 * Depodaki stok kırılımı.
 *
 * <p>Tenant izolasyonu: depoyu yükleyip {@code tenantChecker.check} çağırmak
 * zorunlu. Bu kontrol olmadan başka bir şirketin depo id'si bilindiğinde o
 * depodaki stok adları, kodları, birimleri ve miktarları okunabiliyordu
 * (cross-tenant IDOR). Yazma yolları ({@link #stokEkle}, {@link #stokCikar})
 * kontrol ediyordu; okuma yolu atlıyordu.
 */
@Transactional(readOnly = true)
    public List<DepoStokDTO> depoStoklari(Long depoId) {
        Depo depo = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(depo.getSirketId(), "Depo");
        List<com.raspel.erp.entity.envanter.Stok> tumStoklar = stokRepository.findBySirketIdOrderByAd(depo.getSirketId());
        Map<Long, String> stokHaritasi = tumStoklar.stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getAd(), (a, b) -> a));
        Map<Long, String> stokKodHaritasi = tumStoklar.stream()
                .filter(s -> s.getStokKodu() != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getStokKodu(), (a, b) -> a));
        Map<Long, String> birimHaritasi = tumStoklar.stream()
                .filter(s -> s.getBirim() != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getBirim(), (a, b) -> a));

        return depoStokRepository.findByDepoId(depoId).stream()
                .map(ds -> DepoStokDTO.builder()
                        .id(ds.getId()).depoId(ds.getDepoId())
                        .stokId(ds.getStokId())
                        .stokAd(stokHaritasi.get(ds.getStokId()))
                        .stokKodu(stokKodHaritasi.get(ds.getStokId()))
                        .birim(birimHaritasi.get(ds.getStokId()))
                        .miktar(ds.getMiktar())
                        .olusturmaTarihi(ds.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());
    }

    /**
     * Şirketin tüm depolarındaki stok kırılımı (stok x depo). Stoklar ekranındaki
     * "Depo Dağılımı" görünümü tek çağrıda beslenir; depo başına ayrı istek atılmaz.
     */
    @Transactional(readOnly = true)
    public List<DepoStokDTO> stokDagilimi(Long sirketId) {
        List<Depo> depolar = depoRepository.findBySirketIdOrderByAdAsc(sirketId, Pageable.unpaged()).getContent();
        if (depolar.isEmpty()) return List.of();
        Map<Long, String> depoAdlari = depolar.stream()
                .collect(Collectors.toMap(Depo::getId, Depo::getAd));
        return depoStokRepository.findByDepoIdIn(depolar.stream().map(Depo::getId).collect(Collectors.toList()))
                .stream()
                .map(ds -> DepoStokDTO.builder()
                        .id(ds.getId()).depoId(ds.getDepoId())
                        .depoAdi(depoAdlari.get(ds.getDepoId()))
                        .stokId(ds.getStokId()).miktar(ds.getMiktar())
                        .build())
                .collect(Collectors.toList());
    }

    /** Depo stok paneli: arama + kritik/sifir filtresi + sayfalama. */
    @Transactional(readOnly = true)
    public Page<DepoStokDTO> stokSayfali(Long depoId, String q, Boolean kritik, Boolean sifir, Pageable pageable) {
        Depo depo = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(depo.getSirketId(), "Depo");
        String arama = (q == null || q.isBlank())
                ? null : "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        Page<DepoStok> sayfa = depoStokRepository.sayfali(depoId, arama,
                Boolean.TRUE.equals(kritik), Boolean.TRUE.equals(sifir), pageable);
        List<Long> stokIdler = sayfa.getContent().stream().map(DepoStok::getStokId).collect(Collectors.toList());
        Map<Long, Stok> stoklar = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s, (a, b) -> a));
        return sayfa.map(ds -> {
            Stok s = stoklar.get(ds.getStokId());
            BigDecimal miktar = ds.getMiktar() != null ? ds.getMiktar() : BigDecimal.ZERO;
            BigDecimal maliyet = s != null
                    ? (s.getOrtalamaMaliyet() != null ? s.getOrtalamaMaliyet() : s.getFiyat())
                    : null;
            BigDecimal deger = maliyet != null ? miktar.multiply(maliyet) : BigDecimal.ZERO;
            boolean kritikMi = s != null && s.getMinMiktar() != null && miktar.compareTo(s.getMinMiktar()) <= 0;
            return DepoStokDTO.builder()
                    .id(ds.getId()).depoId(ds.getDepoId()).depoAdi(depo.getAd())
                    .stokId(ds.getStokId())
                    .stokAd(s != null ? s.getAd() : null)
                    .stokKodu(s != null ? s.getStokKodu() : null)
                    .birim(s != null ? s.getBirim() : null)
                    .miktar(miktar)
                    .minMiktar(s != null ? s.getMinMiktar() : null)
                    .ortalamaMaliyet(maliyet)
                    .deger(deger)
                    .kritikMi(kritikMi)
                    .olusturmaTarihi(ds.getOlusturmaTarihi())
                    .build();
        });
    }

    /** Depo hareket gecmisi (tip filtresi + sayfalama). */
    @Transactional(readOnly = true)
    public Page<DepoHareketDTO> hareketler(Long depoId, String tip, Pageable pageable) {
        Depo depo = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(depo.getSirketId(), "Depo");
        Page<StokHareket> sayfa = (tip == null || tip.isBlank())
                ? stokHareketRepository.findByDepoIdOrderByOlusturmaTarihiDesc(depoId, pageable)
                : stokHareketRepository.findByDepoIdAndTurOrderByOlusturmaTarihiDesc(depoId, tip, pageable);
        Map<Long, String> adlar = kullaniciAdlari(sayfa.getContent().stream()
                .map(StokHareket::getKullaniciId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        return sayfa.map(h -> DepoHareketDTO.builder()
                .id(h.getId())
                .tarih(h.getOlusturmaTarihi())
                .hareketTarihi(h.getHareketTarihi())
                .tur(h.getTur())
                .kaynakTip(h.getKaynakTip())
                .stokId(h.getStok() != null ? h.getStok().getId() : null)
                .stokKod(h.getStok() != null ? h.getStok().getStokKodu() : null)
                .stokAd(h.getStok() != null ? h.getStok().getAd() : null)
                .miktar(h.getMiktar())
                .kullaniciAd(h.getKullaniciId() != null ? adlar.get(h.getKullaniciId()) : null)
                .aciklama(h.getAciklama())
                .build());
    }

    public DepoStokDTO stokEkle(Long depoId, Long stokId, BigDecimal miktar) {
        return stokEkle(depoId, stokId, miktar, null);
    }

    public DepoStokDTO stokEkle(Long depoId, Long stokId, BigDecimal miktar, Long kullaniciId) {
        Depo d = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(d.getSirketId(), "Depo");
        pozitifMiktar(miktar);
        if (kullaniciId != null && !depoYetki.islemYapabilir(d, kullaniciId)) {
            throw new BusinessException("Bu depoda stok işlemi yapma yetkiniz yok");
        }
        // Global stok ve StokHareket tek bir noktadan (StokService) güncellenir; aksi halde
        // depo kırılımı ile ana stok miktarı birbirinden kopar.
        depoHareketiIsle(stokId, depoId, miktar, "GIRIS", kullaniciId);
        return depoStoklari(depoId).stream()
                .filter(s -> s.getStokId().equals(stokId))
                .findFirst().orElse(null);
    }

    public DepoStokDTO stokCikar(Long depoId, Long stokId, BigDecimal miktar) {
        return stokCikar(depoId, stokId, miktar, null);
    }

    public DepoStokDTO stokCikar(Long depoId, Long stokId, BigDecimal miktar, Long kullaniciId) {
        Depo d = depoRepository.findById(depoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", depoId));
        tenantChecker.check(d.getSirketId(), "Depo");
        pozitifMiktar(miktar);
        if (kullaniciId != null && !depoYetki.islemYapabilir(d, kullaniciId)) {
            throw new BusinessException("Bu depoda stok işlemi yapma yetkiniz yok");
        }
        DepoStok ds = depoStokRepository.findByDepoIdAndStokIdForUpdate(depoId, stokId)
                .orElseThrow(() -> new BusinessException("Bu depoda stok bulunamadı"));
        if (ds.getMiktar().compareTo(miktar) < 0)
            throw new BusinessException("Yetersiz stok! Mevcut: " + ds.getMiktar() + ", Çıkış: " + miktar);
        depoHareketiIsle(stokId, depoId, miktar, "CIKIS", kullaniciId);
        return depoStoklari(depoId).stream()
                .filter(s -> s.getStokId().equals(stokId))
                .findFirst().orElse(null);
    }

    public void stokTransfer(Long kaynakDepoId, Long hedefDepoId, Long stokId, BigDecimal miktar) {
        if (kaynakDepoId == null || kaynakDepoId.equals(hedefDepoId)) {
            throw new BusinessException("Kaynak ve hedef depo aynı olamaz");
        }
        // Transfer global toplamı değiştirmez: kaynak çıkışı + hedef girişi net sıfırdır.
        stokCikar(kaynakDepoId, stokId, miktar);
        stokEkle(hedefDepoId, stokId, miktar);
    }

    private static void pozitifMiktar(BigDecimal miktar) {
        if (miktar == null || miktar.signum() <= 0) {
            throw new BusinessException("Miktar 0'dan büyük olmalı");
        }
    }

    private void depoHareketiIsle(Long stokId, Long depoId, BigDecimal miktar, String tur, Long kullaniciId) {
        stokService.hareketEkle(com.raspel.erp.dto.envanter.StokHareketDTO.builder()
                .stokId(stokId).depoId(depoId).tur(tur).miktar(miktar)
                .kullaniciId(kullaniciId)
                .hareketTarihi(java.time.LocalDate.now())
                .aciklama("GIRIS".equals(tur) ? "Depo stok girişi" : "Depo stok çıkışı")
                .build());
    }

    private DepoDTO entityToDTO(Depo d, String subeAd) {
        return entityToDTO(d, subeAd, null, null);
    }

    private DepoDTO entityToDTO(Depo d, String subeAd, Object[] istatistik, String sorumluAd) {
        long kalem = sayi(istatistik, 1);
        long kritik = sayi(istatistik, 3);
        return DepoDTO.builder()
                .id(d.getId()).ad(d.getAd()).adres(d.getAdres())
                .yetkili(d.getYetkili()).subeId(d.getSubeId())
                .subeAdi(subeAd).sirketId(d.getSirketId())
                .aktif(d.getAktif()).olusturmaTarihi(d.getOlusturmaTarihi())
                .kapasiteKalem(d.getKapasiteKalem())
                .sorumluKullaniciId(d.getSorumluKullaniciId())
                .sorumluKullaniciAd(sorumluAd)
                .kalemSayisi(kalem)
                .sifirStokSayisi(sayi(istatistik, 2))
                .kritikStokSayisi(kritik)
                .stokDegeri(para(istatistik, 4))
                .dolulukYuzde(doluluk(kalem, d.getKapasiteKalem()))
                .kritikMi(kritik > 0)
                .build();
    }

    private static long sayi(Object[] satir, int idx) {
        if (satir == null || satir.length <= idx || satir[idx] == null) return 0L;
        return ((Number) satir[idx]).longValue();
    }

    private static BigDecimal para(Object[] satir, int idx) {
        if (satir == null || satir.length <= idx || satir[idx] == null) return BigDecimal.ZERO;
        return (BigDecimal) satir[idx];
    }

    private static Integer doluluk(long kalem, Integer kapasite) {
        if (kapasite == null || kapasite <= 0) return null;
        return (int) Math.round(kalem * 100.0 / kapasite);
    }

    private Map<Long, String> kullaniciAdlari(java.util.Collection<Long> idler) {
        if (idler == null || idler.isEmpty()) return Map.of();
        return kullaniciRepository.findAllById(idler).stream()
                .collect(Collectors.toMap(k -> k.getId(), k -> k.getDisplayName(), (a, b) -> a));
    }
}