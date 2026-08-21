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
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

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

    @Transactional(readOnly = true)
    public Page<StokDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Page<Stok> page = stokRepository.findBySirketIdOrderByAd(sirketId, pageable);
        Map<Long, String> tedarikciAdlari = tedarikciAdlari(page.getContent());
        return page.map(s -> entityToDTO(s, tedarikciAdlari));
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

    @Transactional(readOnly = true)
    public StokDTO barkodIleBul(String barkod, Long sirketId) {
        if (sirketId == null) return null;
        return stokRepository.findBySirketIdAndBarkod(sirketId, barkod).stream().findFirst()
                .map(s -> entityToDTO(s, tekTedarikciAdi(s))).orElse(null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "stoklar", key = "#id")
    public StokDTO getir(Long id) {
        Stok stok = stokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(stok.getSirketId(), "Stok");
        return entityToDTO(stok, tekTedarikciAdi(stok));
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public StokDTO olustur(StokDTO dto, Long sirketId) {
        Stok s = Stok.builder().stokKodu(dto.getStokKodu()).ad(dto.getAd())
                .birim(dto.getBirim()).fiyat(dto.getFiyat()).satisFiyati(dto.getSatisFiyati())
                .miktar(dto.getMiktar() != null ? dto.getMiktar() : BigDecimal.ZERO)
                .minMiktar(dto.getMinMiktar()).kdvOrani(dto.getKdvOrani()).stokGrubu(dto.getStokGrubu())
                .barkod(dto.getBarkod()).rafNo(dto.getRafNo()).marka(dto.getMarka())
                .agirlik(dto.getAgirlik()).kategori(dto.getKategori())
                .aciklama(dto.getAciklama()).fotoUrl(dto.getFotoUrl()).birim2(dto.getBirim2())
                .cevrimKatsayisi(dto.getCevrimKatsayisi()).tedarikciId(dto.getTedarikciId())
                .tedarikciStokKodu(dto.getTedarikciStokKodu()).tedarikciFiyat(dto.getTedarikciFiyat())
                .maliyetYontemi(dto.getMaliyetYontemi()).sirketId(sirketId).build();
        Stok kaydedilen = stokRepository.save(s);
        return entityToDTO(kaydedilen, tekTedarikciAdi(kaydedilen));
    }

    @CacheEvict(value = "stoklar", allEntries = true)
    public int topluOlustur(List<StokDTO> dtolar, Long sirketId) {
        List<Stok> stoklar = dtolar.stream()
                .map(dto -> Stok.builder().stokKodu(dto.getStokKodu()).ad(dto.getAd())
                        .birim(dto.getBirim()).fiyat(dto.getFiyat()).satisFiyati(dto.getSatisFiyati())
                        .miktar(dto.getMiktar() != null ? dto.getMiktar() : BigDecimal.ZERO)
                        .minMiktar(dto.getMinMiktar()).kdvOrani(dto.getKdvOrani()).stokGrubu(dto.getStokGrubu())
                        .barkod(dto.getBarkod()).rafNo(dto.getRafNo()).marka(dto.getMarka())
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
        Stok s = stokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", id));
        tenantChecker.check(s.getSirketId(), "Stok");
        s.setStokKodu(dto.getStokKodu()); s.setAd(dto.getAd()); s.setBirim(dto.getBirim());
        s.setFiyat(dto.getFiyat()); s.setSatisFiyati(dto.getSatisFiyati());
        s.setMiktar(dto.getMiktar() != null ? dto.getMiktar() : s.getMiktar());
        s.setMinMiktar(dto.getMinMiktar()); s.setKdvOrani(dto.getKdvOrani());
        s.setStokGrubu(dto.getStokGrubu()); s.setBarkod(dto.getBarkod());
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
                .stream().map(this::hareketToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StokHareketDTO> tumHareketler(Long sirketId) {
        return stokHareketRepository.findByStokSirketIdOrderByHareketTarihiDesc(sirketId)
                .stream().map(this::hareketToDTO).collect(Collectors.toList());
    }

    public StokHareketDTO hareketEkle(StokHareketDTO dto) {
        Stok stok = stokRepository.findByIdForUpdate(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        tenantChecker.check(stok.getSirketId(), "Stok");

        BigDecimal miktar = dto.getMiktar();
        if ("CIKIS".equals(dto.getTur())) {
            if (stok.getMiktar().compareTo(miktar) < 0)
                throw new BusinessException("Yetersiz stok! Mevcut: " + stok.getMiktar() + ", Çıkış: " + miktar);
            stok.setMiktar(stok.getMiktar().subtract(miktar));
        } else {
            stok.setMiktar(stok.getMiktar().add(miktar));
        }

        CariHesap cari = null;
        if (dto.getCariHesapId() != null)
            cari = cariHesapRepository.findById(dto.getCariHesapId()).orElse(null);

        StokHareket h = StokHareket.builder().stok(stok).tur(dto.getTur())
                .miktar(dto.getMiktar()).hareketTarihi(dto.getHareketTarihi())
                .aciklama(dto.getAciklama()).cariHesap(cari).build();

        stokRepository.save(stok);
        StokHareketDTO sonuc = hareketToDTO(stokHareketRepository.save(h));
        kritikStokBildirimiGonder(stok);
        cacheYardimci.temizle("stoklar", "dashboard");
        return sonuc;
    }

    private void kritikStokBildirimiGonder(Stok stok) {
        try {
            if (stok.getMinMiktar() != null && stok.getMiktar().compareTo(stok.getMinMiktar()) <= 0 && stok.getSirketId() != null) {
                bildirimService.bildirimGonder(stok.getSirketId(), "STOK",
                        "Kritik Stok: " + stok.getAd(),
                        "Stok miktarı (" + stok.getMiktar() + ") kritik seviyeye (" + stok.getMinMiktar() + ") düştü.");
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
        StokHareket h = stokHareketRepository.findById(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", hareketId));
        Stok stok = h.getStok();
        tenantChecker.check(stok.getSirketId(), "Stok");
        if ("CIKIS".equals(h.getTur())) {
            stok.setMiktar(stok.getMiktar().add(h.getMiktar()));
        } else {
            stok.setMiktar(stok.getMiktar().subtract(h.getMiktar()));
        }
        stokRepository.save(stok);
        stokHareketRepository.deleteById(hareketId);
        cacheYardimci.temizle("stoklar", "dashboard");
    }

    @Transactional(readOnly = true)
    public long toplamStokAdet(Long sirketId) { return stokRepository.countBySirketId(sirketId); }

    @Transactional(readOnly = true)
    public BigDecimal toplamStokMiktari() {
        BigDecimal toplam = stokRepository.toplamMiktar();
        return toplam != null ? toplam : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.envanter.TalepTahminiDTO> talepTahmini(Long sirketId) {
        List<Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        Map<Long, String> tedarikciler = tedarikciAdlari(stoklar);
        
        List<com.raspel.erp.dto.envanter.TalepTahminiDTO> tahminler = new ArrayList<>();
        
        for (Stok s : stoklar) {
            List<StokHareket> hareketler = stokHareketRepository.findByStokIdOrderByHareketTarihiDesc(s.getId());
            
            // Son 90 gün içindeki çıkış (tüketim) miktarı
            java.time.LocalDate ucAyOnce = java.time.LocalDate.now().minusDays(90);
            BigDecimal sonUcAylikCikis = hareketler.stream()
                    .filter(h -> "CIKIS".equals(h.getTur()) && h.getHareketTarihi() != null && !h.getHareketTarihi().isBefore(ucAyOnce))
                    .map(StokHareket::getMiktar)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
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
        return StokHareketDTO.builder().id(h.getId()).stokId(h.getStok().getId())
                .stokAd(h.getStok().getAd()).stokKodu(h.getStok().getStokKodu())
                .tur(h.getTur()).miktar(h.getMiktar()).hareketTarihi(h.getHareketTarihi())
                .aciklama(h.getAciklama())
                .cariHesapId(h.getCariHesap() != null ? h.getCariHesap().getId() : null)
                .cariHesapAd(h.getCariHesap() != null ? h.getCariHesap().getAd() : null)
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}