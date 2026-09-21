package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.SirketDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SirketService {

    private final SirketRepository sirketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final com.raspel.erp.repository.envanter.StokRepository stokRepository;
    private final com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    private final DosyaDepolamaService dosyaDepolamaService;

    public Page<SirketDTO> tumunuGetir(Pageable pageable) {
        return sirketRepository.findAll(pageable).map(this::entityToDTO);
    }

    /**
     * Oturum açmış kullanıcının erişebildiği aktif şirketleri döndürür.
     * ADMIN tüm aktif şirketleri görür; USER/MUHASEBE yalnızca üye olduğu şirketleri.
     * Req konteksti yoksa (test/dahili çağrı) geriye dönük: tüm aktif şirketler.
     */
    public List<SirketDTO> aktifOlanlariGetir() {
        Long kullaniciId = mevcutKullaniciId();
        if (kullaniciId != null) {
            Kullanici k = kullaniciRepository.findById(kullaniciId).orElse(null);
            if (k != null) {
                return kullanicininSirketleri(k);
            }
        }
        return sirketRepository.findByAktifTrue().stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    public SirketDTO getir(Long id) {
        Sirket s = sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id));
        erisimKontrol(s);
        return entityToDTO(s);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO olustur(SirketDTO dto) {
        Sirket s = Sirket.builder()
                .ad(dto.getAd())
                .vergiNo(dto.getVergiNo())
                .vergiDairesi(dto.getVergiDairesi())
                .adres(dto.getAdres())
                .telefon(dto.getTelefon())
                .email(dto.getEmail())
                .webSite(dto.getWebSite())
                .logoUrl(dto.getLogoUrl())
                .parentId(dto.getParentId())
                .tur(dto.getTur() != null ? dto.getTur() : "DIGER")
                .yil(dto.getYil())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .negatifStokIzni(dto.getNegatifStokIzni() != null && dto.getNegatifStokIzni())
                .build();
        return entityToDTO(sirketRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO guncelle(Long id, SirketDTO dto) {
        Sirket s = sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id));
        boolean adDegisiyor = dto.getAd() != null && !dto.getAd().equals(s.getAd());
        if (adDegisiyor) {
            if (s.getSonAdGuncellemeTarihi() != null &&
                s.getSonAdGuncellemeTarihi().plusDays(30).isAfter(LocalDateTime.now())) {
                long kalanGun = 30 - java.time.temporal.ChronoUnit.DAYS.between(s.getSonAdGuncellemeTarihi(), LocalDateTime.now());
                throw new BusinessException("Şirket adı " + kalanGun + " gün içinde tekrar değiştirilemez. Son değişiklik: " +
                        s.getSonAdGuncellemeTarihi().toLocalDate());
            }
            s.setAd(dto.getAd());
            s.setSonAdGuncellemeTarihi(LocalDateTime.now());
        }
        if (dto.getVergiNo() != null) s.setVergiNo(dto.getVergiNo());
        if (dto.getVergiDairesi() != null) s.setVergiDairesi(dto.getVergiDairesi());
        if (dto.getAdres() != null) s.setAdres(dto.getAdres());
        if (dto.getTelefon() != null) s.setTelefon(dto.getTelefon());
        if (dto.getEmail() != null) s.setEmail(dto.getEmail());
        if (dto.getWebSite() != null) s.setWebSite(dto.getWebSite());
        if (dto.getLogoUrl() != null) {
            // Yalnizca kendi yukleme yolumuz kabul edilir; dis URL reddedilir.
            String url = dto.getLogoUrl().trim();
            if (!url.isEmpty() && !url.startsWith("/api/uploads/sirket-logos/")) {
                throw new BusinessException("Geçersiz logo adresi");
            }
            s.setLogoUrl(url.isEmpty() ? null : url);
        }
        if (dto.getParentId() != null) s.setParentId(dto.getParentId());
        if (dto.getTur() != null) s.setTur(dto.getTur());
        if (dto.getYil() != null) s.setYil(dto.getYil());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        if (dto.getNegatifStokIzni() != null) s.setNegatifStokIzni(dto.getNegatifStokIzni());
        return entityToDTO(sirketRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        if (!sirketRepository.existsById(id)) throw new ResourceNotFoundException("Şirket", id);
        sirketRepository.deleteById(id);
    }

    /**
     * Yüklenmiş logo adresini şirkete bağlar (ADMIN/USER/MUHASEBE, kendi şirketi).
     * Şirket güncelleme (ADMIN-only) yetkisi olmayan kullanıcılar için ayrı uç.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO logoGuncelle(Long id, String logoUrl) {
        Sirket s = sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id));
        erisimKontrol(s);
        String url = logoUrl == null ? "" : logoUrl.trim();
        if (!url.isEmpty() && !url.startsWith("/api/uploads/sirket-logos/")) {
            throw new BusinessException("Geçersiz logo adresi");
        }
        s.setLogoUrl(url.isEmpty() ? null : url);
        return entityToDTO(sirketRepository.save(s));
    }

    /**
     * Şirket logosunu kaldırır: logoUrl temizlenir ve yüklenen dosya silinir.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public SirketDTO logoSil(Long id) {
        Sirket s = sirketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", id));
        erisimKontrol(s);
        String eski = s.getLogoUrl();
        s.setLogoUrl(null);
        Sirket kaydedilen = sirketRepository.save(s);
        if (eski != null && !eski.isBlank()) {
            try {
                String filename = eski.substring(eski.lastIndexOf('/') + 1);
                dosyaDepolamaService.sil("sirket-logos/s" + id, filename);
            } catch (Exception ignored) {
                // Dosya zaten yoksa/erisilemezse logo referansi yine temizlendi.
            }
        }
        return entityToDTO(kaydedilen);
    }

    @Transactional(readOnly = true)
    public String faturaSablonuGetir(Long sirketId) {
        Sirket s = sirketRepository.findById(sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", sirketId));
        erisimKontrol(s);
        return s.getFaturaSablonu();
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public String faturaSablonuKaydet(Long sirketId, String sablonJson) {
        Sirket s = sirketRepository.findById(sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", sirketId));
        erisimKontrol(s);
        s.setFaturaSablonu(sablonJson);
        sirketRepository.save(s);
        return s.getFaturaSablonu();
    }

    @Transactional(readOnly = true)
    public String posFisAyarlariGetir(Long sirketId) {
        Sirket s = sirketRepository.findById(sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", sirketId));
        erisimKontrol(s);
        return s.getPosFisAyarlari();
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public String posFisAyarlariKaydet(Long sirketId, String ayarJson) {
        Sirket s = sirketRepository.findById(sirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", sirketId));
        erisimKontrol(s);
        s.setPosFisAyarlari(ayarJson);
        sirketRepository.save(s);
        return s.getPosFisAyarlari();
    }

    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sistem.KonsolideOzetDTO konsolideOzet(Long anaSirketId) {
        Sirket ana = sirketRepository.findById(anaSirketId)
                .orElseThrow(() -> new ResourceNotFoundException("Şirket", anaSirketId));

        List<Sirket> tumGrup = sirketRepository.findAll().stream()
                .filter(s -> s.getId().equals(anaSirketId) || (s.getParentId() != null && s.getParentId().equals(anaSirketId)))
                .collect(Collectors.toList());

        java.math.BigDecimal toplamStokDegeri = java.math.BigDecimal.ZERO;
        java.math.BigDecimal toplamAlacak = java.math.BigDecimal.ZERO;
        java.math.BigDecimal toplamBorc = java.math.BigDecimal.ZERO;

        List<com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO> sirketOzetleri = new java.util.ArrayList<>();

        for (Sirket s : tumGrup) {
            List<com.raspel.erp.entity.envanter.Stok> stoklar = stokRepository.findBySirketIdOrderByAd(s.getId(), Pageable.unpaged()).getContent();
            java.math.BigDecimal sirketStokDeger = stoklar.stream()
                    .map(stok -> {
                        java.math.BigDecimal miktar = stok.getMiktar() != null ? stok.getMiktar() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal fiyat = stok.getFiyat() != null ? stok.getFiyat() : java.math.BigDecimal.ZERO;
                        return miktar.multiply(fiyat);
                    })
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            List<com.raspel.erp.entity.finans.CariHesap> cariler = cariHesapRepository.findBySirketId(s.getId(), Pageable.unpaged()).getContent();
            java.math.BigDecimal sirketBakiye = cariler.stream()
                    .map(c -> c.getBakiye() != null ? c.getBakiye() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            toplamStokDegeri = toplamStokDegeri.add(sirketStokDeger);
            if (sirketBakiye.compareTo(java.math.BigDecimal.ZERO) >= 0) {
                toplamAlacak = toplamAlacak.add(sirketBakiye);
            } else {
                toplamBorc = toplamBorc.add(sirketBakiye.abs());
            }

            sirketOzetleri.add(com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO.builder()
                    .sirketId(s.getId())
                    .sirketAdi(s.getAd())
                    .tur(s.getTur())
                    .yil(s.getYil())
                    .stokDegeri(sirketStokDeger)
                    .bakiye(sirketBakiye)
                    .stokSayisi(stoklar.size())
                    .cariSayisi(cariler.size())
                    .build());
        }

        return com.raspel.erp.dto.sistem.KonsolideOzetDTO.builder()
                .anaSirketId(ana.getId())
                .anaSirketAdi(ana.getAd())
                .altSirketSayisi(tumGrup.size() - 1)
                .toplamStokDegeri(toplamStokDegeri)
                .toplamAlacakBakiye(toplamAlacak)
                .toplamBorcBakiye(toplamBorc)
                .toplamCiro(java.math.BigDecimal.ZERO)
                .sirketler(sirketOzetleri)
                .build();
    }

    private List<SirketDTO> kullanicininSirketleri(Kullanici k) {
        if ("ADMIN".equals(k.getRole())) {
            return sirketRepository.findByAktifTrue().stream().map(this::entityToDTO).collect(Collectors.toList());
        }
        Set<Sirket> uye = new LinkedHashSet<>(k.getSirketler() != null ? k.getSirketler() : Set.of());
        if (k.getSirketId() != null) {
            sirketRepository.findById(k.getSirketId()).ifPresent(uye::add);
        }
        return uye.stream()
                .filter(s -> Boolean.TRUE.equals(s.getAktif()))
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    private void erisimKontrol(Sirket s) {
        Long kullaniciId = mevcutKullaniciId();
        if (kullaniciId == null) return; // dahili çağrı / test, request konteksti yok
        Kullanici k = kullaniciRepository.findById(kullaniciId).orElse(null);
        if (k == null || "ADMIN".equals(k.getRole())) return;
        boolean uye = k.getSirketler() != null && k.getSirketler().stream().anyMatch(x -> x.getId().equals(s.getId()));
        if (uye) return;
        if (k.getSirketId() != null && k.getSirketId().equals(s.getId())) return;
        throw new ResourceNotFoundException("Şirket", s.getId());
    }

    private Long mevcutKullaniciId() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return (Long) req.getAttribute("kullaniciId");
        } catch (Exception e) {
            return null;
        }
    }

    private SirketDTO entityToDTO(Sirket s) {
        return SirketDTO.builder()
                .id(s.getId()).ad(s.getAd())
                .vergiNo(s.getVergiNo()).vergiDairesi(s.getVergiDairesi())
                .adres(s.getAdres()).telefon(s.getTelefon())
                .email(s.getEmail()).webSite(s.getWebSite())
                .logoUrl(s.getLogoUrl())
                .parentId(s.getParentId())
                .tur(s.getTur())
                .yil(s.getYil())
                .aktif(s.getAktif())
                .negatifStokIzni(s.getNegatifStokIzni())
                .olusturmaTarihi(s.getOlusturmaTarihi())
                .sonAdGuncellemeTarihi(s.getSonAdGuncellemeTarihi())
                .build();
    }
}
