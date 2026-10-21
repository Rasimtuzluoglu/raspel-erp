package com.raspel.erp.service.ik;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelIzinDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonelIzinService {

    private final PersonelIzinRepository izinRepository;
    private final PersonelRepository personelRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;

    @Transactional(readOnly = true)
    public Page<PersonelIzinDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Map<Long, String> personelHaritasi = personelRepository.findBySirketIdOrderByAdAsc(sirketId, Pageable.unpaged()).stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getAd() + " " + p.getSoyad()
                ));
        List<Long> personelIds = new java.util.ArrayList<>(personelHaritasi.keySet());
        if (personelIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return izinRepository.findByPersonelIdIn(personelIds, pageable)
                .map(i -> entityToDTO(i, personelHaritasi));
    }

    /** İzin KPI özeti: bekleyen/onaylanan/reddedilen + bugün izinli personel. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.ik.IzinOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.ik.IzinOzetDTO.builder().build();
        }
        return com.raspel.erp.dto.ik.IzinOzetDTO.builder()
                .bekleyen(izinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId))
                .onaylandi(izinRepository.countByDurumAndSirketId("ONAYLANDI", sirketId))
                .reddedildi(izinRepository.countByDurumAndSirketId("REDDEDILDI", sirketId))
                .bugunIzinli(izinRepository.countBugunIzinliAndSirketId(java.time.LocalDate.now(), sirketId))
                .build();
    }

    /**
 * Bir personelin izin kayıtları.
 *
 * <p>Tenant izolasyonu: personeli yükleyip {@code tenantChecker.check}
 * çağırmak zorunlu. Kardeş metotlarda ({@link #getir}, {@link #guncelle},
 * {@link #durumGuncelle}, {@link #sil}) bu kontrol vardı; bu metot atlıyordu ve
 * başka şirketin personel izin kayıtları okunabiliyordu (cross-tenant IDOR).
 */
@Transactional(readOnly = true)
    public List<PersonelIzinDTO> personelIzınleri(Long personelId) {
        Personel personel = personelRepository.findById(personelId)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", personelId));
        tenantChecker.check(personel.getSirketId(), "Personel Izin");
        return izinRepository.findByPersonelIdOrderByBaslangicDesc(personelId).stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    /**
     * İzin listesi: durum, personel, tarih aralığı ve serbest metin araması
     * (personel adı, izin türü, açıklama) + sayfalama.
     */
    @Transactional(readOnly = true)
    public Page<PersonelIzinDTO> filtreli(Long sirketId, String durum, Long personelId,
                                          java.time.LocalDate baslangic, java.time.LocalDate bitis,
                                          String q, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        String durumFiltre = (durum == null || durum.isBlank() || "TUMU".equalsIgnoreCase(durum))
                ? null : durum.trim().toUpperCase(java.util.Locale.ROOT);
        String arama = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(java.util.Locale.ROOT);

        Page<PersonelIzin> sayfa = izinRepository.filtrele(
                sirketId, durumFiltre, personelId, baslangic, bitis, arama, pageable);

        List<Long> personelIds = sayfa.getContent().stream()
                .map(PersonelIzin::getPersonelId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> adlar = personelRepository.findAllById(personelIds).stream()
                .collect(Collectors.toMap(
                        Personel::getId,
                        p -> p.getAd() + " " + p.getSoyad()
                ));
        return sayfa.map(i -> entityToDTO(i, adlar));
    }

    @Transactional(readOnly = true)
    public PersonelIzinDTO getir(Long id) {
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        Personel personel = personelRepository.findById(izin.getPersonelId())
                .orElse(null);
        if (personel != null) {
            tenantChecker.check(personel.getSirketId(), "Personel Izin");
        }
        return entityToDTO(izin);
    }

    public PersonelIzinDTO olustur(PersonelIzinDTO dto) {
        izinDogrula(dto.getPersonelId(), dto.getIzinTuru(), dto.getBaslangic(), dto.getBitis());
        Personel personel = personelRepository.findById(dto.getPersonelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
        tenantChecker.check(personel.getSirketId(), "Personel Izin");
        PersonelIzin izin = PersonelIzin.builder()
                .personelId(dto.getPersonelId())
                .izinTuru(dto.getIzinTuru().trim())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .gunSayisi(gunHesapla(dto.getBaslangic(), dto.getBitis()))
                .durum("BEKLEMEDE")
                .aciklama(dto.getAciklama())
                .build();
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(izinRepository.save(izin));
    }

    public PersonelIzinDTO guncelle(Long id, PersonelIzinDTO dto) {
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        Personel personel = personelRepository.findById(izin.getPersonelId())
                .orElse(null);
        if (personel != null) {
            tenantChecker.check(personel.getSirketId(), "Personel Izin");
        }

        Long personelId = dto.getPersonelId() != null ? dto.getPersonelId() : izin.getPersonelId();
        String izinTuru = dto.getIzinTuru() != null ? dto.getIzinTuru() : izin.getIzinTuru();
        java.time.LocalDate baslangic = dto.getBaslangic() != null ? dto.getBaslangic() : izin.getBaslangic();
        java.time.LocalDate bitis = dto.getBitis() != null ? dto.getBitis() : izin.getBitis();
        izinDogrula(personelId, izinTuru, baslangic, bitis);

        if (!personelId.equals(izin.getPersonelId())) {
            Personel yeni = personelRepository.findById(personelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", personelId));
            tenantChecker.check(yeni.getSirketId(), "Personel Izin");
        }

        izin.setPersonelId(personelId);
        izin.setIzinTuru(izinTuru.trim());
        izin.setBaslangic(baslangic);
        izin.setBitis(bitis);
        izin.setGunSayisi(gunHesapla(baslangic, bitis));
        if (dto.getAciklama() != null) izin.setAciklama(dto.getAciklama());
        // Durum ve onaylayan istemciden yazılmaz; yalnızca yetkili onay akışında
        // (durumGuncelle) sunucu tarafından atanır.
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(izinRepository.save(izin));
    }

    public PersonelIzinDTO durumGuncelle(Long id, String durum, String onaylayan) {
        if (durum == null || !List.of("BEKLEMEDE", "ONAYLANDI", "REDDEDILDI").contains(durum)) {
            throw new BusinessException("Geçersiz izin durumu");
        }
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        Personel personel = personelRepository.findById(izin.getPersonelId())
                .orElse(null);
        if (personel != null) {
            tenantChecker.check(personel.getSirketId(), "Personel Izin");
        }
        izin.setDurum(durum);
        if (onaylayan != null) izin.setOnaylayan(onaylayan);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(izinRepository.save(izin));
    }

    public void sil(Long id) {
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        Personel personel = personelRepository.findById(izin.getPersonelId())
                .orElse(null);
        if (personel != null) {
            tenantChecker.check(personel.getSirketId(), "Personel Izin");
        }
        izinRepository.deleteById(id);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    private void izinDogrula(Long personelId, String izinTuru, java.time.LocalDate baslangic, java.time.LocalDate bitis) {
        if (personelId == null) {
            throw new BusinessException("Personel seçilmelidir");
        }
        if (izinTuru == null || izinTuru.isBlank()) {
            throw new BusinessException("İzin türü zorunludur");
        }
        if (baslangic == null || bitis == null) {
            throw new BusinessException("İzin başlangıç ve bitiş tarihleri zorunludur");
        }
        if (bitis.isBefore(baslangic)) {
            throw new BusinessException("Bitiş tarihi başlangıç tarihinden önce olamaz");
        }
    }

    /** Gün sayısı: başlangıç ve bitiş dahil. */
    private int gunHesapla(java.time.LocalDate baslangic, java.time.LocalDate bitis) {
        return (int) (java.time.temporal.ChronoUnit.DAYS.between(baslangic, bitis) + 1);
    }

    @Transactional(readOnly = true)
    public PersonelIzinDTO entityToDTO(PersonelIzin i) {
        return entityToDTO(i, null);
    }

    private PersonelIzinDTO entityToDTO(PersonelIzin i, Map<Long, String> personelHaritasi) {
        String personelAdi = null;
        if (i.getPersonelId() != null) {
            if (personelHaritasi != null) {
                personelAdi = personelHaritasi.get(i.getPersonelId());
            }
            if (personelAdi == null) {
                personelAdi = personelRepository.findById(i.getPersonelId())
                        .map(p -> p.getAd() + " " + p.getSoyad()).orElse(null);
            }
        }
        return PersonelIzinDTO.builder()
                .id(i.getId()).personelId(i.getPersonelId())
                .personelAdi(personelAdi)
                .izinTuru(i.getIzinTuru()).baslangic(i.getBaslangic())
                .bitis(i.getBitis()).gunSayisi(i.getGunSayisi())
                .durum(i.getDurum()).aciklama(i.getAciklama())
                .onaylayan(i.getOnaylayan()).olusturmaTarihi(i.getOlusturmaTarihi())
                .build();
    }
}
