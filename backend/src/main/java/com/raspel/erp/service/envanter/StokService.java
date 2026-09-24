package com.raspel.erp.service.envanter;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.dto.envanter.KritikStokDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.envanter.StokFiyat;
import com.raspel.erp.dto.envanter.StokFiyatDTO;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokFiyatRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StokService {

    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final CariHesapRepository cariHesapRepository;
    private final BildirimService bildirimService;
        private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final StokFiyatRepository stokFiyatRepository;
    private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.repository.sube.DepoRepository depoRepository;
    private final com.raspel.erp.repository.envanter.StokSeriRepository stokSeriRepository;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;

    // ---------- ÇOKLU FİYAT ----------

    @Transactional(readOnly = true)
    public List<StokFiyatDTO> fiyatlariGetir(Long stokId) {
        Stok stok = stokRepository.findById(stokId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
        tenantChecker.check(stok.getSirketId(), "Stok");
        return stokFiyatRepository.findByStokIdOrderByFiyatAsc(stokId)
                .stream().map(this::fiyatEntityToDTO).collect(Collectors.toList());
    }

    /**
     * Birden fazla stok icin fiyat tanimlarini tek sorguda getirir.
     * Hizli Satis gibi N adet stok fiyati isteyen akislarda N HTTP/DB istegini
     * tek istege indirir. Yalnizca istege bagli sirkete ait stoklar doner.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<StokFiyatDTO>> fiyatlariTopluGetir(List<Long> stokIdler, Long sirketId) {
        Map<Long, List<StokFiyatDTO>> sonuc = new java.util.LinkedHashMap<>();
        if (stokIdler == null || stokIdler.isEmpty()) {
            return sonuc;
        }
        List<Long> izinli = stokRepository.findAllById(stokIdler).stream()
                .filter(s -> sirketId == null || sirketId.equals(s.getSirketId()))
                .map(Stok::getId)
                .collect(Collectors.toList());
        for (Long id : izinli) {
            sonuc.put(id, new ArrayList<>());
        }
        if (!izinli.isEmpty()) {
            stokFiyatRepository.findByStokIdInOrderByFiyatAsc(izinli).forEach(f ->
                    sonuc.computeIfAbsent(f.getStokId(), k -> new ArrayList<>()).add(fiyatEntityToDTO(f)));
        }
        return sonuc;
    }

    public StokFiyatDTO fiyatEkle(Long stokId, StokFiyatDTO dto, Long sirketId) {
        Stok stok = stokRepository.findById(stokId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
        tenantChecker.check(stok.getSirketId(), "Stok");
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("Fiyat adı boş olamaz");
        }
        StokFiyat fiyat = StokFiyat.builder()
                .stokId(stokId)
                .ad(dto.getAd())
                .fiyat(dto.getFiyat() != null ? dto.getFiyat() : BigDecimal.ZERO)
                .sirketId(sirketId)
                .build();
        return fiyatEntityToDTO(stokFiyatRepository.save(fiyat));
    }

    public StokFiyatDTO fiyatGuncelle(Long id, StokFiyatDTO dto) {
        StokFiyat fiyat = stokFiyatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokFiyat", id));
        Stok stok = stokRepository.findById(fiyat.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", fiyat.getStokId()));
        tenantChecker.check(stok.getSirketId(), "Stok");
        if (dto.getAd() != null && !dto.getAd().isBlank()) fiyat.setAd(dto.getAd());
        if (dto.getFiyat() != null) fiyat.setFiyat(dto.getFiyat());
        return fiyatEntityToDTO(stokFiyatRepository.save(fiyat));
    }

    public void fiyatSil(Long id) {
        StokFiyat fiyat = stokFiyatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokFiyat", id));
        Stok stok = stokRepository.findById(fiyat.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", fiyat.getStokId()));
        tenantChecker.check(stok.getSirketId(), "Stok");
        stokFiyatRepository.deleteById(id);
    }

    private StokFiyatDTO fiyatEntityToDTO(StokFiyat f) {
        return StokFiyatDTO.builder()
                .id(f.getId()).stokId(f.getStokId()).ad(f.getAd()).fiyat(f.getFiyat())
                .sirketId(f.getSirketId()).olusturmaTarihi(f.getOlusturmaTarihi())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<StokDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<Stok> page = stokRepository.findBySirketIdOrderByAd(sirketId, pageable);
        Map<Long, String> tedarikciAdlari = tedarikciAdlari(page.getContent());
        return page.map(s -> entityToDTO(s, tedarikciAdlari));
    }

    /**
     * Sunucu tarafında filtrelenmiş, aranmış ve sayfalanmış stok listesi.
     * 5000+ kayıt için tüm kayıtları çekmek yerine sorgu seviyesinde filtre uygular.
     */
    @Transactional(readOnly = true)
    public Page<StokDTO> filtreli(Long sirketId, String q, String kategori, String marka,
                                  String stokGrubu, BigDecimal minFiyat, BigDecimal maxFiyat, Long depoId, Pageable pageable) {
        Page<Stok> page = stokRepository.filtreli(sirketId, likeDeseni(q), bosIseNull(kategori),
                likeDeseni(marka), bosIseNull(stokGrubu), minFiyat, maxFiyat, depoId, pageable);
        Map<Long, String> tedarikciAdlari = tedarikciAdlari(page.getContent());
        return page.map(s -> entityToDTO(s, tedarikciAdlari));
    }

    private String bosIseNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String likeDeseni(String s) {
        if (s == null || s.isBlank()) return null;
        return "%" + s.toLowerCase() + "%";
    }

    @Transactional(readOnly = true)
    public List<StokDTO> ara(String q, Long sirketId) {
        if (sirketId == null) return List.of();
        List<Stok> sonuc = stokRepository.findBySirketIdAndBarkod(sirketId, q);
        if (sonuc.isEmpty()) {
            sonuc = stokRepository.findBySirketIdAndAdContainingIgnoreCase(sirketId, q);
        }
        Map<Long, String> tedarikciAdlari = tedarikciAdlari(sonuc);
        return sonuc.stream().map(s -> entityToDTO(s, tedarikciAdlari)).collect(Collectors.toList());
    }

    /**
     * En çok satan ürünleri (CIKIS hareket miktarına göre) tam StokDTO olarak döner.
     * POS ekranında hızlı erişim paneli için kullanılır.
     */
    @Transactional(readOnly = true)
    public List<StokDTO> enCokSatanlar(Long sirketId, int limit) {
        if (sirketId == null) return List.of();
        List<Map<String, Object>> satislar = stokHareketRepository.enCokSatanlarBySirket(sirketId);
        int cap = limit > 0 ? Math.min(limit, 50) : 12;
        return satislar.stream().limit(cap).map(m -> {
            Object kod = m.get("stokKodu");
            if (kod == null) return null;
            return stokRepository.findBySirketIdAndStokKodu(sirketId, kod.toString()).orElse(null);
        }).filter(s -> s != null)
                .map(s -> entityToDTO(s, tekTedarikciAdi(s)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StokDTO barkodIleBul(String barkod, Long sirketId) {
        if (sirketId == null) return null;
        List<Stok> eslesenler = stokRepository.findBySirketIdAndBarkod(sirketId, barkod);
        if (eslesenler.size() > 1) {
            log.warn("Barkod '{}' sirket '{}' icin {} stok ile eslesiyor; ilki secildi (V88 unique kısıt sonrası bu durum olmamalı)",
                    barkod, sirketId, eslesenler.size());
        }
        return eslesenler.stream().findFirst()
                .map(s -> entityToDTO(s, tekTedarikciAdi(s))).orElse(null);
    }

    /** Boş/null stok kodunu trim eder; zorunlu olduğundan eksikse STK-<8hex> üretir. */
    private String normalizeStokKodu(String kod) {
        String k = kod == null ? null : kod.trim();
        return (k == null || k.isEmpty())
                ? "STK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : k;
    }

    /** Boş/null barkodu NULL'a indirir (barkod opsiyonel). */
    private String normalizeBarkod(String barkod) {
        if (barkod == null) return null;
        String b = barkod.trim();
        return b.isEmpty() ? null : b;
    }

    /**
     * Tenant kontrolü yapılmış ham Stok entity'si döner (etiket/QR üretimi için).
     */
    @Transactional(readOnly = true)
    public Stok entityGetir(Long id) {
        Stok stok = stokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(stok.getSirketId(), "Stok");
        return stok;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "stoklar", key = "T(com.raspel.erp.config.TenantChecker).tenantKey(#id)")
    public StokDTO getir(Long id) {
        Stok stok = stokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(stok.getSirketId(), "Stok");
        return entityToDTO(stok, tekTedarikciAdi(stok));
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public StokDTO olustur(StokDTO dto, Long sirketId) {
        String stokKodu = normalizeStokKodu(dto.getStokKodu());
        String barkod = normalizeBarkod(dto.getBarkod());
        Stok s = Stok.builder().stokKodu(stokKodu).ad(dto.getAd())
                .birim(dto.getBirim()).fiyat(dto.getFiyat()).satisFiyati(dto.getSatisFiyati())
                .miktar(dto.getMiktar() != null ? dto.getMiktar() : BigDecimal.ZERO)
                .minMiktar(dto.getMinMiktar()).kdvOrani(dto.getKdvOrani()).stokGrubu(dto.getStokGrubu())
                .barkod(barkod).rafNo(dto.getRafNo()).marka(dto.getMarka())
                .agirlik(dto.getAgirlik()).kategori(dto.getKategori())
                .aciklama(dto.getAciklama()).fotoUrl(dto.getFotoUrl()).birim2(dto.getBirim2())
                .cevrimKatsayisi(dto.getCevrimKatsayisi()).tedarikciId(dto.getTedarikciId())
                .tedarikciStokKodu(dto.getTedarikciStokKodu()).tedarikciFiyat(dto.getTedarikciFiyat())
                .maliyetYontemi(dto.getMaliyetYontemi()).varsayilanDepoId(dto.getVarsayilanDepoId())
                .sirketId(sirketId).build();
        Stok kaydedilen = stokRepository.save(s);
        return entityToDTO(kaydedilen, tekTedarikciAdi(kaydedilen));
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public int topluOlustur(List<StokDTO> dtolar, Long sirketId) {
        List<Stok> stoklar = dtolar.stream()
                .map(dto -> Stok.builder().stokKodu(normalizeStokKodu(dto.getStokKodu())).ad(dto.getAd())
                        .birim(dto.getBirim()).fiyat(dto.getFiyat()).satisFiyati(dto.getSatisFiyati())
                        .miktar(dto.getMiktar() != null ? dto.getMiktar() : BigDecimal.ZERO)
                        .minMiktar(dto.getMinMiktar()).kdvOrani(dto.getKdvOrani()).stokGrubu(dto.getStokGrubu())
                        .barkod(normalizeBarkod(dto.getBarkod())).rafNo(dto.getRafNo()).marka(dto.getMarka())
                        .agirlik(dto.getAgirlik()).kategori(dto.getKategori())
                        .aciklama(dto.getAciklama()).birim2(dto.getBirim2())
                        .cevrimKatsayisi(dto.getCevrimKatsayisi()).tedarikciId(dto.getTedarikciId())
                        .tedarikciStokKodu(dto.getTedarikciStokKodu()).tedarikciFiyat(dto.getTedarikciFiyat())
                        .maliyetYontemi(dto.getMaliyetYontemi()).sirketId(sirketId).build())
                .collect(Collectors.toList());
        stokRepository.saveAll(stoklar);
        return stoklar.size();
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public StokDTO guncelle(Long id, StokDTO dto) {
        Stok s = stokRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(s.getSirketId(), "Stok");
        if (dto.getStokKodu() != null && !dto.getStokKodu().trim().isEmpty()) {
            s.setStokKodu(dto.getStokKodu().trim());
        }
        s.setAd(dto.getAd()); s.setBirim(dto.getBirim());
        s.setFiyat(dto.getFiyat()); s.setSatisFiyati(dto.getSatisFiyati());
        s.setMinMiktar(dto.getMinMiktar()); s.setKdvOrani(dto.getKdvOrani());
        s.setStokGrubu(dto.getStokGrubu()); s.setBarkod(normalizeBarkod(dto.getBarkod()));
        s.setRafNo(dto.getRafNo()); s.setMarka(dto.getMarka());
        s.setAgirlik(dto.getAgirlik()); s.setKategori(dto.getKategori());
        s.setAciklama(dto.getAciklama());
        if (dto.getFotoUrl() != null) s.setFotoUrl(dto.getFotoUrl());
        if (dto.getBirim2() != null) s.setBirim2(dto.getBirim2());
        if (dto.getCevrimKatsayisi() != null) s.setCevrimKatsayisi(dto.getCevrimKatsayisi());
        if (dto.getTedarikciId() != null) s.setTedarikciId(dto.getTedarikciId());
        if (dto.getTedarikciStokKodu() != null) s.setTedarikciStokKodu(dto.getTedarikciStokKodu());
        if (dto.getTedarikciFiyat() != null) s.setTedarikciFiyat(dto.getTedarikciFiyat());
        if (dto.getMaliyetYontemi() != null) s.setMaliyetYontemi(dto.getMaliyetYontemi());
        if (dto.getVarsayilanDepoId() != null) s.setVarsayilanDepoId(dto.getVarsayilanDepoId());

        // Miktar doğrudan ezilmez: stok hareket defterine "DUZELTME" kaydı düşülür
        BigDecimal yeniMiktar = dto.getMiktar() != null ? dto.getMiktar() : s.getMiktar();
        if (dto.getMiktar() != null && dto.getMiktar().compareTo(s.getMiktar()) != 0) {
            BigDecimal fark = dto.getMiktar().subtract(s.getMiktar());
            if (fark.compareTo(BigDecimal.ZERO) < 0 && s.getMiktar().add(fark).compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Stok miktarı negatif olamaz. Mevcut: " + s.getMiktar());
            }
            stokHareketRepository.save(StokHareket.builder()
                    .stok(s)
                    .tur("DUZELTME")
                    .miktar(fark.abs())
                    .hareketTarihi(java.time.LocalDate.now())
                    .aciklama("Manuel stok düzeltmesi (stok kartı): " + s.getMiktar() + " -> " + dto.getMiktar())
                    .kaynakTip("DUZELTME")
                    .build());
            BigDecimal eskiMiktar = s.getMiktar() != null ? s.getMiktar() : BigDecimal.ZERO;
            s.setMiktar(yeniMiktar);
            if (fark.signum() > 0) {
                maliyetService.girisIsle(s, eskiMiktar, fark, null, s.getSirketId(), "DUZELTME", null);
            } else {
                maliyetService.cikisIsle(s, fark.abs(), yeniMiktar, s.getSirketId(), "DUZELTME", null);
            }
            cacheYardimci.temizle("stoklar", "dashboard");
        }

        Stok kaydedilen = stokRepository.save(s);
        return entityToDTO(kaydedilen, tekTedarikciAdi(kaydedilen));
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public int topluFiyatGuncelle(com.raspel.erp.dto.envanter.TopluFiyatDTO dto, Long sirketId) {
        if (sirketId == null) throw new BusinessException("Şirket bilgisi eksik");
        List<Stok> hedef = stokRepository.findBySirketIdOrderByAd(sirketId, Pageable.unpaged()).getContent();
        hedef = hedef.stream()
                .filter(s -> dto.getKategori() == null || dto.getKategori().isBlank() || dto.getKategori().equals(s.getKategori()))
                .filter(s -> dto.getStokGrubu() == null || dto.getStokGrubu().isBlank() || dto.getStokGrubu().equals(s.getStokGrubu()))
                .filter(s -> dto.getMarka() == null || dto.getMarka().isBlank() || dto.getMarka().equals(s.getMarka()))
                .collect(Collectors.toList());

        double oran = dto.getOran() != null ? dto.getOran() : 0;
        double carpan = "AZALT".equalsIgnoreCase(dto.getYon()) ? (1 - oran / 100) : (1 + oran / 100);

        for (Stok s : hedef) {
            if (s.getFiyat() != null) {
                s.setFiyat(s.getFiyat().multiply(BigDecimal.valueOf(carpan))
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (s.getSatisFiyati() != null) {
                s.setSatisFiyati(s.getSatisFiyati().multiply(BigDecimal.valueOf(carpan))
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
        }
        stokRepository.saveAll(hedef);
        cacheYardimci.temizle("stoklar", "dashboard");
        return hedef.size();
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public void sil(Long id) {
        Stok s = stokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(s.getSirketId(), "Stok");
        if (s.getMiktar() != null && s.getMiktar().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Miktarı sıfırdan büyük olan stok kartı silinemez. Mevcut stok: " + s.getMiktar());
        }
        if (stokHareketRepository.countByStokId(id) > 0)
            throw new BusinessException("Bu stoğa ait hareketler var, önce hareketleri silin");
        stokRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StokHareketDTO> hareketler(Long stokId) {
        Stok stok = stokRepository.findById(stokId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
        tenantChecker.check(stok.getSirketId(), "Stok");
        return stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(stokId)
                .stream().map(h -> hareketToDTO(h, Map.of(), Map.of())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StokHareketDTO> tumHareketler(Long sirketId) {
        List<StokHareket> hareketler = stokHareketRepository.findByStokSirketIdOrderByHareketTarihiDesc(sirketId);
        // Depo ve seri adlarını tek sorguda toplu çöz (N+1 önlenir).
        Set<Long> depoIdler = hareketler.stream().map(StokHareket::getDepoId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> seriIdler = hareketler.stream().map(StokHareket::getSeriId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> depoAdlari = depoIdler.isEmpty() ? Map.of()
                : depoRepository.findAllById(depoIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.sube.Depo::getId, com.raspel.erp.entity.sube.Depo::getAd, (a, b) -> a));
        Map<Long, String> seriNolar = seriIdler.isEmpty() ? Map.of()
                : stokSeriRepository.findAllById(seriIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.envanter.StokSeri::getId, com.raspel.erp.entity.envanter.StokSeri::getSeriNo, (a, b) -> a));
        return hareketler.stream().map(h -> hareketToDTO(h, depoAdlari, seriNolar)).collect(Collectors.toList());
    }

    public StokHareketDTO hareketEkle(StokHareketDTO dto) {
        Stok stok = stokRepository.findByIdForUpdate(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        tenantChecker.check(stok.getSirketId(), "Stok");

        BigDecimal miktar = dto.getMiktar();
        BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
        if ("CIKIS".equals(dto.getTur())) {
            if (stok.getMiktar().compareTo(miktar) < 0)
                throw new BusinessException("Yetersiz stok! Mevcut: " + stok.getMiktar() + ", Çıkış: " + miktar);
            stok.setMiktar(stok.getMiktar().subtract(miktar));
            maliyetService.cikisIsle(stok, miktar, stok.getMiktar(), stok.getSirketId(), "MANUEL", null);
        } else {
            stok.setMiktar(stok.getMiktar().add(miktar));
            maliyetService.girisIsle(stok, eskiMiktar, miktar, null, stok.getSirketId(), "MANUEL", null);
        }

        CariHesap cari = null;
        if (dto.getCariHesapId() != null)
            cari = cariHesapRepository.findById(dto.getCariHesapId()).orElse(null);

        Long depoId = depoStokService.coz(dto.getDepoId(), stok.getSirketId());

        StokHareket h = StokHareket.builder().stok(stok).tur(dto.getTur())
                .miktar(dto.getMiktar()).hareketTarihi(dto.getHareketTarihi())
                .aciklama(dto.getAciklama()).cariHesap(cari)
                .depoId(depoId)
                .kaynakTip("MANUEL").build();

        stokRepository.save(stok);
        depoStokService.guncelle(depoId, stok.getId(),
                "CIKIS".equals(dto.getTur()) ? miktar.negate() : miktar);
        StokHareketDTO sonuc = hareketToDTO(stokHareketRepository.save(h));
        kritikStokBildirimiGonder(stok);
        cacheYardimci.temizle("stoklar", "dashboard");
        return sonuc;
    }

    private void kritikStokBildirimiGonder(Stok stok) {
        try {
            if (stok.getMinMiktar() != null && stok.getMiktar().compareTo(stok.getMinMiktar()) <= 0 && stok.getSirketId() != null) {
                Long sirketId = stok.getSirketId();
                String ad = stok.getAd();
                java.math.BigDecimal miktar = stok.getMiktar();
                java.math.BigDecimal minMiktar = stok.getMinMiktar();
                com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(sirketId, "STOK",
                        "Kritik Stok: " + ad,
                        "Stok miktarı (" + miktar + ") kritik seviyeye (" + minMiktar + ") düştü."));
            }
        } catch (Exception e) {
            log.warn("Kritik stok bildirimi gönderilemedi: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<KritikStokDTO> kritikStoklar(Long sirketId) {
        List<Stok> stoklar = stokRepository.kritikStoklar(sirketId);
        List<Long> tedarikciIdler = stoklar.stream()
                .map(Stok::getTedarikciId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> tedarikciAdlari = tedarikciIdler.isEmpty() ? Map.of()
                : cariHesapRepository.findAllById(tedarikciIdler).stream()
                        .collect(Collectors.toMap(CariHesap::getId, CariHesap::getAd));
        return stoklar.stream().map(s -> KritikStokDTO.builder()
                .id(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd()).birim(s.getBirim())
                .miktar(s.getMiktar()).minMiktar(s.getMinMiktar())
                .kategori(s.getKategori()).marka(s.getMarka())
                .onerilenSiparisMiktari(s.getMinMiktar().multiply(new BigDecimal("2")).subtract(s.getMiktar())
                        .max(BigDecimal.ZERO))
                .tedarikciAd(s.getTedarikciId() != null ? tedarikciAdlari.get(s.getTedarikciId()) : null)
                .build()).collect(Collectors.toList());
    }

    public void hareketSil(Long hareketId) {
        // Hareket satırını kilitle; aynı hareketin eşzamanlı iki kez silinmesi/terslenmesi önlenir.
        StokHareket h = stokHareketRepository.findByIdForUpdate(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", hareketId));
        // Stok satırını da kilitle: eşzamanlı hareketlerde miktar kaybını önler.
        Stok stok = stokRepository.findByIdForUpdate(h.getStok().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", h.getStok().getId()));
        tenantChecker.check(stok.getSirketId(), "Stok");
        if ("CIKIS".equals(h.getTur())) {
            BigDecimal eskiMiktar = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
            stok.setMiktar(stok.getMiktar().add(h.getMiktar()));
            maliyetService.girisIsle(stok, eskiMiktar, h.getMiktar(), null, stok.getSirketId(), "MANUEL", null);
        } else {
            stok.setMiktar(stok.getMiktar().subtract(h.getMiktar()));
            maliyetService.cikisIsle(stok, h.getMiktar(), stok.getMiktar(), stok.getSirketId(), "MANUEL", null);
        }
        stokRepository.save(stok);
        // Depo kırılımını da hareketin tersi yönünde düzelt (aksi halde depo stoğu kayar).
        if (h.getDepoId() != null) {
            depoStokService.guncelle(h.getDepoId(), stok.getId(),
                    "CIKIS".equals(h.getTur()) ? h.getMiktar() : h.getMiktar().negate());
        }
        stokHareketRepository.deleteById(hareketId);
        cacheYardimci.temizle("stoklar", "dashboard");
    }

    @Transactional(readOnly = true)
    public long toplamStokAdet(Long sirketId) { return stokRepository.countBySirketId(sirketId); }

    @Transactional(readOnly = true)
    public BigDecimal toplamStokMiktari(Long sirketId) {
        BigDecimal toplam = stokRepository.toplamMiktarBySirketId(sirketId);
        return toplam != null ? toplam : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.envanter.TalepTahminiDTO> talepTahmini(Long sirketId) {
        List<Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        Map<Long, String> tedarikciler = tedarikciAdlari(stoklar);

        // Son 90 günün çıkış toplamları tek sorguda (stok bazlı) alınır; her stok için
        // ayrı sorgu (N+1) ve tüm geçmişin belleğe yüklenmesi önlenir.
        java.time.LocalDate ucAyOnce = java.time.LocalDate.now().minusDays(90);
        Map<Long, BigDecimal> cikisHaritasi = new java.util.HashMap<>();
        for (Map<String, Object> satir : stokHareketRepository.sonCikisToplamlari(sirketId, ucAyOnce)) {
            Object id = satir.get("stokId");
            Object toplam = satir.get("toplam");
            if (id instanceof Number n) {
                cikisHaritasi.put(n.longValue(), toplam instanceof BigDecimal b ? b : BigDecimal.ZERO);
            }
        }

        List<com.raspel.erp.dto.envanter.TalepTahminiDTO> tahminler = new ArrayList<>();
        
        for (Stok s : stoklar) {
            // Son 90 gün içindeki çıkış (tüketim) miktarı (önceden hesaplanmış haritadan)
            BigDecimal sonUcAylikCikis = cikisHaritasi.getOrDefault(s.getId(), BigDecimal.ZERO);
            
            BigDecimal gunlukTuketim = sonUcAylikCikis.divide(BigDecimal.valueOf(90), 2, java.math.RoundingMode.HALF_UP);
            if (gunlukTuketim.compareTo(BigDecimal.ZERO) <= 0) {
                // Eğer son 90 günde çıkış yoksa varsayılan minimum tüketim tahmini (örnek 0.5 veya 0)
                gunlukTuketim = s.getMinMiktar() != null && s.getMinMiktar().compareTo(BigDecimal.ZERO) > 0 
                        ? s.getMinMiktar().divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP) 
                        : BigDecimal.valueOf(0.1);
            }
            
            int tahminiTukenmeGunu = 999;
            if (gunlukTuketim.compareTo(BigDecimal.ZERO) > 0 && s.getMiktar() != null) {
                tahminiTukenmeGunu = s.getMiktar().divide(gunlukTuketim, 0, java.math.RoundingMode.HALF_UP).intValue();
            }
            
            int tedarikSuresi = 5; // Standart ortalama tedarik süresi (gün)
            BigDecimal guvenlikStogu = s.getMinMiktar() != null ? s.getMinMiktar() : gunlukTuketim.multiply(BigDecimal.valueOf(7));
            BigDecimal onerilenSiparis = gunlukTuketim.multiply(BigDecimal.valueOf(30)).add(guvenlikStogu).subtract(s.getMiktar()).max(BigDecimal.ZERO);
            
            String durum = "GUVENLI";
            String oneri = "Stok seviyesi yeterli.";
            
            if (tahminiTukenmeGunu <= tedarikSuresi) {
                durum = "KRITIK";
                oneri = String.format("%s ürünü %d gün içinde tükenecek! Tedarik süresi %d gün olduğu için acil %s %s sipariş verilmelidir.",
                        s.getAd(), tahminiTukenmeGunu, tedarikSuresi, onerilenSiparis.setScale(0, java.math.RoundingMode.HALF_UP), s.getBirim() != null ? s.getBirim() : "Adet");
            } else if (tahminiTukenmeGunu <= 15) {
                durum = "DIKKAT";
                oneri = String.format("%s ürünü %d gün içinde kritik seviyeye düşecek. %s %s sipariş planlanmalı.",
                        s.getAd(), tahminiTukenmeGunu, onerilenSiparis.setScale(0, java.math.RoundingMode.HALF_UP), s.getBirim() != null ? s.getBirim() : "Adet");
            }
            
            tahminler.add(com.raspel.erp.dto.envanter.TalepTahminiDTO.builder()
                    .stokId(s.getId())
                    .stokKodu(s.getStokKodu())
                    .ad(s.getAd())
                    .birim(s.getBirim())
                    .mevcutMiktar(s.getMiktar())
                    .gunlukOrtalamaTuketim(gunlukTuketim)
                    .tahminiTukenmeGunu(tahminiTukenmeGunu)
                    .tedarikSuresiGun(tedarikSuresi)
                    .onerilenSiparisMiktari(onerilenSiparis.setScale(0, java.math.RoundingMode.HALF_UP))
                    .tedarikciAd(s.getTedarikciId() != null ? tedarikciler.get(s.getTedarikciId()) : null)
                    .durum(durum)
                    .proaktifOneri(oneri)
                    .build());
        }
        
        tahminler.sort(Comparator.comparing(com.raspel.erp.dto.envanter.TalepTahminiDTO::getTahminiTukenmeGunu));
        return tahminler;
    }

    private StokDTO entityToDTO(Stok s, Map<Long, String> tedarikciAdlari) {
        List<StokFiyatDTO> fiyatlar = null;
        try {
            fiyatlar = stokFiyatRepository.findByStokIdOrderByFiyatAsc(s.getId())
                    .stream().map(this::fiyatEntityToDTO).collect(Collectors.toList());
        } catch (Exception ignored) {
            // fiyat yüklenemezse boş bırakılır
        }
        return StokDTO.builder().id(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd())
                .birim(s.getBirim()).fiyat(s.getFiyat()).satisFiyati(s.getSatisFiyati())
                .miktar(s.getMiktar()).minMiktar(s.getMinMiktar()).kdvOrani(s.getKdvOrani())
                .stokGrubu(s.getStokGrubu()).barkod(s.getBarkod()).rafNo(s.getRafNo())
                .marka(s.getMarka()).agirlik(s.getAgirlik()).kategori(s.getKategori())
                .aciklama(s.getAciklama()).fotoUrl(s.getFotoUrl()).birim2(s.getBirim2())
                .cevrimKatsayisi(s.getCevrimKatsayisi()).tedarikciId(s.getTedarikciId())
                .tedarikciAd(s.getTedarikciId() != null ? tedarikciAdlari.get(s.getTedarikciId()) : null)
                .tedarikciStokKodu(s.getTedarikciStokKodu()).tedarikciFiyat(s.getTedarikciFiyat())
                .maliyetYontemi(s.getMaliyetYontemi())
                .varsayilanDepoId(s.getVarsayilanDepoId())
                .fiyatlar(fiyatlar)
                .olusturmaTarihi(s.getOlusturmaTarihi()).build();
    }

    private Map<Long, String> tedarikciAdlari(List<Stok> stoklar) {
        List<Long> idler = stoklar.stream()
                .map(Stok::getTedarikciId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (idler.isEmpty()) return Map.of();
        return cariHesapRepository.findAllById(idler).stream()
                .collect(Collectors.toMap(CariHesap::getId, CariHesap::getAd));
    }

    private Map<Long, String> tekTedarikciAdi(Stok s) {
        if (s.getTedarikciId() == null) return Map.of();
        return cariHesapRepository.findById(s.getTedarikciId())
                .map(c -> Map.<Long, String>of(c.getId(), c.getAd()))
                .orElse(Map.of());
    }

    private StokHareketDTO hareketToDTO(StokHareket h) {
        return hareketToDTO(h, Map.of(), Map.of());
    }

    private StokHareketDTO hareketToDTO(StokHareket h, Map<Long, String> depoAdlari, Map<Long, String> seriNolar) {
        BigDecimal agirlik = (h.getStok().getAgirlik() != null && h.getMiktar() != null)
                ? h.getStok().getAgirlik().multiply(h.getMiktar()) : null;
        String depoAd = h.getDepoId() != null
                ? (depoAdlari.containsKey(h.getDepoId()) ? depoAdlari.get(h.getDepoId())
                        : depoRepository.findById(h.getDepoId()).map(d -> d.getAd()).orElse(null))
                : null;
        String seriNo = h.getSeriId() != null
                ? (seriNolar.containsKey(h.getSeriId()) ? seriNolar.get(h.getSeriId())
                        : stokSeriRepository.findById(h.getSeriId()).map(s -> s.getSeriNo()).orElse(null))
                : null;
        return StokHareketDTO.builder().id(h.getId()).stokId(h.getStok().getId())
                .stokAd(h.getStok().getAd()).stokKodu(h.getStok().getStokKodu())
                .tur(h.getTur()).miktar(h.getMiktar()).hareketTarihi(h.getHareketTarihi())
                .aciklama(h.getAciklama())
                .cariHesapId(h.getCariHesap() != null ? h.getCariHesap().getId() : null)
                .cariHesapAd(h.getCariHesap() != null ? h.getCariHesap().getAd() : null)
                .depoId(h.getDepoId())
                .depoAd(depoAd)
                .seriId(h.getSeriId())
                .seriNo(seriNo)
                .kaynakTip(h.getKaynakTip())
                .kaynakId(h.getKaynakId())
                .agirlik(agirlik)
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}