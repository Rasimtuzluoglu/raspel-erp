package com.raspel.erp.service.ik;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelPuantajDTO;
import com.raspel.erp.entity.ik.PersonelPuantaj;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelPuantajRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonelPuantajService {

    private final PersonelPuantajRepository puantajRepository;
    private final PersonelRepository personelRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public List<PersonelPuantajDTO> personelPuantajlari(Long personelId, LocalDate baslangic, LocalDate bitis) {
        Long sirketId = personelSirketiniDogrula(personelId);
        Map<Long, String> personelHaritasi = personelRepository.findById(personelId)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getAd() + " " + p.getSoyad()
                ));
        return puantajRepository.findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(sirketId, personelId, baslangic, bitis)
                .stream().map(p -> entityToDTO(p, personelHaritasi)).collect(Collectors.toList());
    }

    /**
     * Şirket geneli puantaj listesi: personel, tarih aralığı, durum ve
     * personel adı araması + sayfalama.
     */
    @Transactional(readOnly = true)
    public Page<PersonelPuantajDTO> filtreli(Long sirketId, Long personelId, LocalDate baslangic,
                                             LocalDate bitis, String durum, String q, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        String durumFiltre = (durum == null || durum.isBlank() || "TUMU".equalsIgnoreCase(durum))
                ? null : durum.trim().toUpperCase(Locale.ROOT);
        String arama = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);

        Page<PersonelPuantaj> sayfa = puantajRepository.filtrele(
                sirketId, personelId, baslangic, bitis, durumFiltre, arama, pageable);

        List<Long> personelIds = sayfa.getContent().stream()
                .map(PersonelPuantaj::getPersonelId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> adlar = personelRepository.findAllById(personelIds).stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getAd() + " " + p.getSoyad()
                ));
        return sayfa.map(p -> entityToDTO(p, adlar));
    }

    @Transactional(readOnly = true)
    public com.raspel.erp.dto.ik.PuantajOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.ik.PuantajOzetDTO.builder().build();
        }
        return com.raspel.erp.dto.ik.PuantajOzetDTO.builder()
                .toplam(puantajRepository.countBySirketId(sirketId))
                .geldi(puantajRepository.countBySirketIdAndDurum(sirketId, "GELDI"))
                .gelmedi(puantajRepository.countBySirketIdAndDurum(sirketId, "GELMEDI"))
                .izinli(puantajRepository.countBySirketIdAndDurum(sirketId, "IZINLI"))
                .mazeretli(puantajRepository.countBySirketIdAndDurum(sirketId, "MAZERETLI"))
                .build();
    }

    public PersonelPuantajDTO olustur(PersonelPuantajDTO dto) {
        Long sirketId = personelSirketiniDogrula(dto.getPersonelId());
        puantajDogrula(dto.getTarih(), dto.getDurum());
        if (puantajRepository.existsByPersonelIdAndTarih(dto.getPersonelId(), dto.getTarih())) {
            throw new BusinessException("Bu personel için seçilen tarihte puantaj kaydı zaten mevcut");
        }
        PersonelPuantaj p = PersonelPuantaj.builder()
                .personelId(dto.getPersonelId())
                .sirketId(sirketId)
                .tarih(dto.getTarih())
                .durum(dto.getDurum().trim().toUpperCase(Locale.ROOT))
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(puantajRepository.save(p));
    }

    public PersonelPuantajDTO guncelle(Long id, PersonelPuantajDTO dto) {
        PersonelPuantaj p = puantajRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puantaj kaydi", id));
        tenantChecker.check(p.getSirketId(), "Puantaj kaydi");

        Long personelId = dto.getPersonelId() != null ? dto.getPersonelId() : p.getPersonelId();
        LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : p.getTarih();
        String durum = dto.getDurum() != null ? dto.getDurum() : p.getDurum();
        puantajDogrula(tarih, durum);

        if (!personelId.equals(p.getPersonelId())) {
            Long yeniSirketId = personelSirketiniDogrula(personelId);
            if (!yeniSirketId.equals(p.getSirketId())) {
                throw new BusinessException("Puantaj kaydı farklı şirkete ait personele taşınamaz");
            }
        }
        if (puantajRepository.existsByPersonelIdAndTarihAndIdNot(personelId, tarih, id)) {
            throw new BusinessException("Bu personel için seçilen tarihte puantaj kaydı zaten mevcut");
        }

        p.setPersonelId(personelId);
        p.setTarih(tarih);
        p.setDurum(durum.trim().toUpperCase(Locale.ROOT));
        if (dto.getAciklama() != null) p.setAciklama(dto.getAciklama());
        return entityToDTO(puantajRepository.save(p));
    }

    public void sil(Long id) {
        PersonelPuantaj p = puantajRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puantaj kaydi", id));
        tenantChecker.check(p.getSirketId(), "Puantaj kaydi");
        puantajRepository.deleteById(id);
    }

    private static final List<String> GECERLI_DURUMLAR = List.of("GELDI", "GELMEDI", "IZINLI", "MAZERETLI");

    private void puantajDogrula(LocalDate tarih, String durum) {
        if (tarih == null) {
            throw new BusinessException("Puantaj tarihi zorunludur");
        }
        if (durum == null || durum.isBlank()) {
            throw new BusinessException("Puantaj durumu zorunludur");
        }
        if (!GECERLI_DURUMLAR.contains(durum.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Geçersiz puantaj durumu");
        }
    }

    private Long personelSirketiniDogrula(Long personelId) {
        if (personelId == null) throw new BusinessException("Personel seçilmelidir");
        return personelRepository.findById(personelId)
                .map(p -> {
                    if (p.getSirketId() == null) {
                        throw new BusinessException("Personelin şirketi tanımlı değil, puantaj işlemi yapılamaz");
                    }
                    tenantChecker.check(p.getSirketId(), "Personel");
                    return p.getSirketId();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Personel", personelId));
    }

    @Transactional(readOnly = true)
    public PersonelPuantajDTO entityToDTO(PersonelPuantaj p) {
        return entityToDTO(p, null);
    }

    private PersonelPuantajDTO entityToDTO(PersonelPuantaj p, Map<Long, String> personelHaritasi) {
        String personelAdi = null;
        if (p.getPersonelId() != null) {
            if (personelHaritasi != null) {
                personelAdi = personelHaritasi.get(p.getPersonelId());
            }
            if (personelAdi == null) {
                personelAdi = personelRepository.findById(p.getPersonelId())
                        .map(pr -> pr.getAd() + " " + pr.getSoyad()).orElse(null);
            }
        }
        return PersonelPuantajDTO.builder()
                .id(p.getId()).personelId(p.getPersonelId())
                .personelAdi(personelAdi)
                .sirketId(p.getSirketId())
                .tarih(p.getTarih()).durum(p.getDurum())
                .aciklama(p.getAciklama()).olusturmaTarihi(p.getOlusturmaTarihi())
                .build();
    }
}
