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

import java.time.LocalDate;
import java.util.List;
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

    public PersonelPuantajDTO olustur(PersonelPuantajDTO dto) {
        Long sirketId = personelSirketiniDogrula(dto.getPersonelId());
        PersonelPuantaj p = PersonelPuantaj.builder()
                .personelId(dto.getPersonelId())
                .sirketId(sirketId)
                .tarih(dto.getTarih())
                .durum(dto.getDurum())
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(puantajRepository.save(p));
    }

    public PersonelPuantajDTO guncelle(Long id, PersonelPuantajDTO dto) {
        PersonelPuantaj p = puantajRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puantaj kaydi", id));
        tenantChecker.check(p.getSirketId(), "Puantaj kaydi");
        if (dto.getPersonelId() != null && !dto.getPersonelId().equals(p.getPersonelId())) {
            Long yeniSirketId = personelSirketiniDogrula(dto.getPersonelId());
            if (!yeniSirketId.equals(p.getSirketId())) {
                throw new BusinessException("Puantaj kaydı farklı şirkete ait personele taşınamaz");
            }
            p.setPersonelId(dto.getPersonelId());
        }
        if (dto.getTarih() != null) p.setTarih(dto.getTarih());
        if (dto.getDurum() != null) p.setDurum(dto.getDurum());
        if (dto.getAciklama() != null) p.setAciklama(dto.getAciklama());
        return entityToDTO(puantajRepository.save(p));
    }

    public void sil(Long id) {
        PersonelPuantaj p = puantajRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puantaj kaydi", id));
        tenantChecker.check(p.getSirketId(), "Puantaj kaydi");
        puantajRepository.deleteById(id);
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
