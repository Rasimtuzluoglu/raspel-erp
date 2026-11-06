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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SirketService {

    private final SirketRepository sirketRepository;
    private final KullaniciRepository kullaniciRepository;
    private final com.raspel.erp.repository.envanter.StokRepository stokRepository;
    private final com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    private final com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
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
                .kod(kodHazirla(dto.getKod(), dto.getAd(), null))
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
        if (dto.getKod() != null && !dto.getKod().isBlank()) {
            s.setKod(kodHazirla(dto.getKod(), s.getAd(), s.getId()));
        } else if (s.getKod() == null || s.getKod().isBlank()) {
            s.setKod(kodHazirla(null, s.getAd(), s.getId()));
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
        java.math.BigDecimal toplamCiro = java.math.BigDecimal.ZERO;

        List<com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO> sirketOzetleri = new java.util.ArrayList<>();

        for (Sirket s : tumGrup) {
            // Stok degeri ve cari bakiyeler tek SQL toplaminda hesaplanir; tum kayitlarin
            // entity olarak yuklenmesi (Pageable.unpaged) bellek riski olusturuyordu.
            java.math.BigDecimal sirketStokDeger = stokRepository.toplamStokDegeriBySirketId(s.getId());
            if (sirketStokDeger == null) sirketStokDeger = java.math.BigDecimal.ZERO;

            java.math.BigDecimal sirketAlacak = cariHesapRepository.toplamPozitifBakiyeBySirketId(s.getId());
            java.math.BigDecimal sirketBorc = cariHesapRepository.toplamNegatifBakiyeBySirketId(s.getId());
            if (sirketAlacak == null) sirketAlacak = java.math.BigDecimal.ZERO;
            if (sirketBorc == null) sirketBorc = java.math.BigDecimal.ZERO;

            java.math.BigDecimal sirketCiro = faturaRepository.sumKesilmisSatisCiro(s.getId());

            toplamStokDegeri = toplamStokDegeri.add(sirketStokDeger);
            toplamCiro = toplamCiro.add(sirketCiro != null ? sirketCiro : java.math.BigDecimal.ZERO);
            toplamAlacak = toplamAlacak.add(sirketAlacak);
            toplamBorc = toplamBorc.add(sirketBorc.abs());

            sirketOzetleri.add(com.raspel.erp.dto.sistem.KonsolideOzetDTO.SirketOzetDTO.builder()
                    .sirketId(s.getId())
                    .sirketAdi(s.getAd())
                    .tur(s.getTur())
                    .yil(s.getYil())
                    .stokDegeri(sirketStokDeger)
                    .bakiye(sirketAlacak.subtract(sirketBorc.abs()))
                    .stokSayisi(stokRepository.countBySirketId(s.getId()))
                    .cariSayisi(cariHesapRepository.countBySirketId(s.getId()))
                    .build());
        }

        return com.raspel.erp.dto.sistem.KonsolideOzetDTO.builder()
                .anaSirketId(ana.getId())
                .anaSirketAdi(ana.getAd())
                .altSirketSayisi(tumGrup.size() - 1)
                .toplamStokDegeri(toplamStokDegeri)
                .toplamAlacakBakiye(toplamAlacak)
                .toplamBorcBakiye(toplamBorc)
                .toplamCiro(toplamCiro)
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

    private static final Set<String> REZERVE_KODLAR = Set.of(
            "api", "giris", "admin", "app", "www", "durum", "qr-onay", "sistem", "destek");
    private static final Pattern KOD_DESENI = Pattern.compile("^[a-z0-9][a-z0-9-]{1,58}[a-z0-9]$");

    /**
     * Sirket kodunu normalize eder, dogrular ve benzersizligini garanti eder.
     * Bos istekte addan slug uretilir; cakismada otomatik son ek eklenir.
     */
    private String kodHazirla(String istenen, String ad, Long id) {
        boolean otomatik = istenen == null || istenen.isBlank();
        String kod = otomatik ? slug(ad) : istenen.trim().toLowerCase(Locale.ROOT);
        kod = kod.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
        if (kod.length() < 2) {
            kod = "sirket" + (id != null ? "-" + id : "");
        }
        if (kod.length() > 60) {
            kod = kod.substring(0, 60).replaceAll("-+$", "");
        }
        if (REZERVE_KODLAR.contains(kod)) {
            throw new BusinessException("Bu şirket kodu kullanılamaz");
        }
        if (!KOD_DESENI.matcher(kod).matches()) {
            throw new BusinessException(
                    "Şirket kodu yalnızca küçük harf, rakam ve tire içerebilir (2-60 karakter)");
        }
        boolean var = id == null
                ? sirketRepository.existsByKodIgnoreCase(kod)
                : sirketRepository.existsByKodIgnoreCaseAndIdNot(kod, id);
        if (var) {
            if (!otomatik) {
                throw new BusinessException("Bu şirket kodu zaten kullanılıyor");
            }
            String temel = kod.length() > 50 ? kod.substring(0, 50) : kod;
            for (int i = 2; i <= 99; i++) {
                String aday = temel + "-" + i;
                if (!sirketRepository.existsByKodIgnoreCase(aday)) {
                    return aday;
                }
            }
            throw new BusinessException("Şirket kodu üretilemedi, lütfen elle giriniz");
        }
        return kod;
    }

    private String slug(String ad) {
        String ham = ad == null ? "" : ad.trim().toLowerCase(Locale.ROOT);
        ham = ham.replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
                .replace('ö', 'o').replace('ş', 's').replace('ü', 'u');
        return ham.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private SirketDTO entityToDTO(Sirket s) {
        return SirketDTO.builder()
                .id(s.getId()).ad(s.getAd()).kod(s.getKod())
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
