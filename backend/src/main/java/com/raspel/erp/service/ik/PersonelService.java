package com.raspel.erp.service.ik;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonelService {

    private final PersonelRepository personelRepository;
    private final com.raspel.erp.repository.ik.PersonelIzinRepository personelIzinRepository;
    private final com.raspel.erp.repository.ik.PersonelPuantajRepository personelPuantajRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;

    public Page<PersonelDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return personelRepository.findBySirketIdOrderByAdAsc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    /** Personel KPI özeti: toplam/aktif/pasif + şirket geneli bekleyen izin sayısı. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.ik.PersonelOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.ik.PersonelOzetDTO.builder().build();
        }
        return com.raspel.erp.dto.ik.PersonelOzetDTO.builder()
                .toplam(personelRepository.countBySirketId(sirketId))
                .aktif(personelRepository.countByAktifTrueAndSirketId(sirketId))
                .pasif(personelRepository.countBySirketIdAndAktifFalse(sirketId))
                .bekleyenIzin(personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId))
                .build();
    }

    /**
     * Personel listesi: arama (ad/soyad/departman/pozisyon/telefon/e-posta),
     * departman, rol ve durum filtreleri + sayfalama.
     */
    @Transactional(readOnly = true)
    public Page<PersonelDTO> filtreli(Long sirketId, String q, String departman, String rol, String durum, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        return personelRepository.findAll(filtreSpec(sirketId, q, departman, rol, durum), pageable)
                .map(this::entityToDTO);
    }

    /** Filtre seçenekleri: şirketteki farklı departmanlar. */
    @Transactional(readOnly = true)
    public List<String> departmanlar(Long sirketId) {
        if (sirketId == null) {
            return List.of();
        }
        return personelRepository.findDistinctDepartmanBySirketId(sirketId);
    }

    private Specification<Personel> filtreSpec(Long sirketId, String q, String departman, String rol, String durum) {
        return (root, query, cb) -> {
            List<Predicate> kosullar = new ArrayList<>();
            kosullar.add(cb.equal(root.get("sirketId"), sirketId));

            if (q != null && !q.isBlank()) {
                String desen = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Expression<String> adSoyad = cb.lower(cb.concat(cb.concat(root.<String>get("ad"), " "), root.<String>get("soyad")));
                kosullar.add(cb.or(
                        cb.like(adSoyad, desen),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("departman"), "")), desen),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("pozisyon"), "")), desen),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("telefon"), "")), desen),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("email"), "")), desen)
                ));
            }
            if (departman != null && !departman.isBlank()) {
                kosullar.add(cb.equal(root.get("departman"), departman.trim()));
            }
            if (rol != null && !rol.isBlank()) {
                kosullar.add(cb.equal(root.get("rol"), rol.trim().toUpperCase(Locale.ROOT)));
            }
            if ("AKTIF".equalsIgnoreCase(durum)) {
                kosullar.add(cb.isTrue(root.get("aktif")));
            } else if ("PASIF".equalsIgnoreCase(durum)) {
                kosullar.add(cb.isFalse(root.get("aktif")));
            }
            return cb.and(kosullar.toArray(new Predicate[0]));
        };
    }

    public PersonelDTO getir(Long id) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        return entityToDTO(p);
    }

    /** Personel detayı: özlük + son izinler + son 30 gün puantaj özeti. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.ik.PersonelDetayDTO detay(Long id) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");

        LocalDate bugun = LocalDate.now();
        var tumIzinler = personelIzinRepository.findByPersonelIdOrderByBaslangicDesc(id);
        List<com.raspel.erp.dto.ik.PersonelIzinDTO> izinler = tumIzinler.stream()
                .limit(10)
                .map(izin -> com.raspel.erp.dto.ik.PersonelIzinDTO.builder()
                        .id(izin.getId()).personelId(izin.getPersonelId())
                        .personelAdi(p.getAd() + " " + p.getSoyad())
                        .izinTuru(izin.getIzinTuru()).baslangic(izin.getBaslangic())
                        .bitis(izin.getBitis()).gunSayisi(izin.getGunSayisi())
                        .durum(izin.getDurum()).aciklama(izin.getAciklama())
                        .onaylayan(izin.getOnaylayan()).olusturmaTarihi(izin.getOlusturmaTarihi())
                        .build())
                .collect(Collectors.toList());

        boolean bugunIzinli = tumIzinler.stream()
                .anyMatch(i -> "ONAYLANDI".equals(i.getDurum())
                        && i.getBaslangic() != null && i.getBitis() != null
                        && !bugun.isBefore(i.getBaslangic()) && !bugun.isAfter(i.getBitis()));

        long bekleyenIzin = tumIzinler.stream()
                .filter(i -> "BEKLEMEDE".equals(i.getDurum()))
                .count();

        var puantajlar = p.getSirketId() == null ? List.<com.raspel.erp.entity.ik.PersonelPuantaj>of()
                : personelPuantajRepository.findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(
                        p.getSirketId(), id, bugun.minusDays(30), bugun);

        return com.raspel.erp.dto.ik.PersonelDetayDTO.builder()
                .personel(entityToDTO(p))
                .sonIzinler(izinler)
                .bugunIzinli(bugunIzinli)
                .bekleyenIzin(bekleyenIzin)
                .puantajKayitSayisi(puantajlar.size())
                .geldi(sayDurum(puantajlar, "GELDI"))
                .gelmedi(sayDurum(puantajlar, "GELMEDI"))
                .izinli(sayDurum(puantajlar, "IZINLI"))
                .mazeretli(sayDurum(puantajlar, "MAZERETLI"))
                .build();
    }

    private long sayDurum(List<com.raspel.erp.entity.ik.PersonelPuantaj> liste, String durum) {
        return liste.stream().filter(p -> durum.equals(p.getDurum())).count();
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public PersonelDTO olustur(PersonelDTO dto) {
        Long sirketId = dto.getSirketId() != null ? dto.getSirketId() : tenantChecker.getCurrentSirketId();
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi zorunludur");
        }
        tenantChecker.checkSirketId(sirketId, "Personel");

        String ad = adDogrula(dto.getAd(), "Ad");
        String soyad = adDogrula(dto.getSoyad(), "Soyad");
        String tc = tcDogrula(dto.getTcKimlik(), null);
        tarihDogrula(dto.getIseGirisTarihi(), dto.getCikisTarihi());

        Personel p = Personel.builder()
                .ad(ad).soyad(soyad)
                .tcKimlik(tc).dogumTarihi(dto.getDogumTarihi())
                .iseGirisTarihi(dto.getIseGirisTarihi()).cikisTarihi(dto.getCikisTarihi())
                .departman(dto.getDepartman()).pozisyon(dto.getPozisyon())
                .rol(dto.getRol()).kullaniciId(dto.getKullaniciId())
                .maas(maasDogrula(dto.getMaas())).telefon(telefonDogrula(dto.getTelefon()))
                .email(emailDogrula(dto.getEmail())).adres(dto.getAdres())
                .aktif(dto.getAktif() != null ? dto.getAktif() : dto.getCikisTarihi() == null)
                .sirketId(sirketId)
                .build();
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(personelRepository.save(p));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public PersonelDTO guncelle(Long id, PersonelDTO dto) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        if (dto.getAd() != null) p.setAd(adDogrula(dto.getAd(), "Ad"));
        if (dto.getSoyad() != null) p.setSoyad(adDogrula(dto.getSoyad(), "Soyad"));
        if (dto.getTcKimlik() != null) p.setTcKimlik(tcDogrula(dto.getTcKimlik(), id));
        if (dto.getDogumTarihi() != null) p.setDogumTarihi(dto.getDogumTarihi());
        if (dto.getIseGirisTarihi() != null) p.setIseGirisTarihi(dto.getIseGirisTarihi());
        if (dto.getDepartman() != null) p.setDepartman(dto.getDepartman());
        if (dto.getPozisyon() != null) p.setPozisyon(dto.getPozisyon());
        if (dto.getRol() != null) p.setRol(dto.getRol());
        if (dto.getKullaniciId() != null) p.setKullaniciId(dto.getKullaniciId());
        if (dto.getMaas() != null) p.setMaas(maasDogrula(dto.getMaas()));
        if (dto.getTelefon() != null) p.setTelefon(telefonDogrula(dto.getTelefon()));
        if (dto.getEmail() != null) p.setEmail(emailDogrula(dto.getEmail()));
        if (dto.getAdres() != null) p.setAdres(dto.getAdres());
        if (dto.getAktif() != null) p.setAktif(dto.getAktif());
        if (dto.getCikisTarihi() != null) p.setCikisTarihi(dto.getCikisTarihi());
        tarihDogrula(p.getIseGirisTarihi(), p.getCikisTarihi());
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(personelRepository.save(p));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        try {
            personelRepository.deleteById(id);
            // FK kisitlari (izin/puantaj/maas vb.) hemen tetiklensin ki anlamli hata donelim.
            personelRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Bu personele bagli kayitlar (izin, puantaj, maas vb.) oldugu icin silinemez. "
                            + "Once bagli kayitlari kaldirin.");
        }
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    @Transactional(readOnly = true)
    public List<PersonelDTO> roleGoreGetir(Long sirketId, String rol) {
        if (sirketId == null || rol == null || rol.isBlank()) return List.of();
        return personelRepository.findBySirketIdAndRolOrderByAdAsc(sirketId, rol.trim().toUpperCase())
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    /** Telefon: rakam, boşluk ve + - ( ) karakterlerinden oluşan 7-20 karakter. */
    private static final Pattern TELEFON_DESENI = Pattern.compile("^[0-9+()\\s-]{7,20}$");
    private static final Pattern EMAIL_DESENI = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String adDogrula(String deger, String alan) {
        String temiz = deger == null ? null : deger.trim();
        if (temiz == null || temiz.isEmpty()) {
            throw new BusinessException(alan + " zorunludur");
        }
        return temiz;
    }

    /** TC: boş olabilir; doluysa 11 hane + algoritma + tekillik. */
    private String tcDogrula(String tc, Long haricId) {
        String temiz = tc == null ? null : tc.replaceAll("\\s", "");
        if (temiz == null || temiz.isEmpty()) {
            return null;
        }
        if (!temiz.matches("\\d{11}") || temiz.charAt(0) == '0') {
            throw new BusinessException("TC kimlik numarası 11 haneli olmalıdır");
        }
        int[] d = new int[11];
        for (int i = 0; i < 11; i++) {
            d[i] = temiz.charAt(i) - '0';
        }
        int tek = d[0] + d[2] + d[4] + d[6] + d[8];
        int cift = d[1] + d[3] + d[5] + d[7];
        if (Math.floorMod(tek * 7 - cift, 10) != d[9]) {
            throw new BusinessException("TC kimlik numarası geçersiz");
        }
        int ilkOn = d[0] + d[1] + d[2] + d[3] + d[4] + d[5] + d[6] + d[7] + d[8] + d[9];
        if (ilkOn % 10 != d[10]) {
            throw new BusinessException("TC kimlik numarası geçersiz");
        }
        boolean mevcut = haricId == null
                ? personelRepository.existsByTcKimlik(temiz)
                : personelRepository.existsByTcKimlikAndIdNot(temiz, haricId);
        if (mevcut) {
            throw new BusinessException("Bu TC kimlik numarası zaten kayıtlı");
        }
        return temiz;
    }

    private String telefonDogrula(String telefon) {
        String temiz = telefon == null ? null : telefon.trim();
        if (temiz == null || temiz.isEmpty()) {
            return null;
        }
        if (!TELEFON_DESENI.matcher(temiz).matches()) {
            throw new BusinessException("Telefon numarası geçersiz (7-20 karakter; rakam, boşluk, + - ( ) )");
        }
        return temiz;
    }

    private String emailDogrula(String email) {
        String temiz = email == null ? null : email.trim();
        if (temiz == null || temiz.isEmpty()) {
            return null;
        }
        if (!EMAIL_DESENI.matcher(temiz).matches()) {
            throw new BusinessException("E-posta adresi geçersiz");
        }
        return temiz;
    }

    private BigDecimal maasDogrula(BigDecimal maas) {
        if (maas != null && maas.signum() < 0) {
            throw new BusinessException("Maaş negatif olamaz");
        }
        return maas;
    }

    private void tarihDogrula(LocalDate iseGiris, LocalDate cikis) {
        if (iseGiris != null && cikis != null && cikis.isBefore(iseGiris)) {
            throw new BusinessException("Çıkış tarihi işe giriş tarihinden önce olamaz");
        }
    }

    /** Maaş yalnızca ADMIN ve MUHASEBE rollerine gösterilir. */
    private boolean maasGorebilir() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(r -> "ROLE_ADMIN".equals(r) || "ROLE_MUHASEBE".equals(r));
    }

    private PersonelDTO entityToDTO(Personel p) {
        return PersonelDTO.builder()
                .id(p.getId()).ad(p.getAd()).soyad(p.getSoyad())
                .tcKimlik(p.getTcKimlik()).dogumTarihi(p.getDogumTarihi())
                .iseGirisTarihi(p.getIseGirisTarihi()).cikisTarihi(p.getCikisTarihi())
                .departman(p.getDepartman()).pozisyon(p.getPozisyon())
                .rol(p.getRol()).kullaniciId(p.getKullaniciId())
                .maas(maasGorebilir() ? p.getMaas() : null).telefon(p.getTelefon())
                .email(p.getEmail()).adres(p.getAdres())
                .aktif(p.getAktif()).sirketId(p.getSirketId())
                .olusturmaTarihi(p.getOlusturmaTarihi())
                .build();
    }
}
