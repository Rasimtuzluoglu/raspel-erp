package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.PersonelPuantajDTO;
import com.raspel.erp.entity.ik.PersonelPuantaj;
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

    @Transactional(readOnly = true)
    public List<PersonelPuantajDTO> personelPuantajlari(Long personelId, LocalDate baslangic, LocalDate bitis) {
        Map<Long, String> personelHaritasi = personelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getAd() + " " + p.getSoyad()
                ));
        return puantajRepository.findByPersonelIdAndTarihBetweenOrderByTarihAsc(personelId, baslangic, bitis)
                .stream().map(p -> entityToDTO(p, personelHaritasi)).collect(Collectors.toList());
    }

    public PersonelPuantajDTO olustur(PersonelPuantajDTO dto) {
        PersonelPuantaj p = PersonelPuantaj.builder()
                .personelId(dto.getPersonelId())
                .tarih(dto.getTarih())
                .durum(dto.getDurum())
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(puantajRepository.save(p));
    }

    public PersonelPuantajDTO guncelle(Long id, PersonelPuantajDTO dto) {
        PersonelPuantaj p = puantajRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puantaj kaydi", id));
        p.setPersonelId(dto.getPersonelId());
        p.setTarih(dto.getTarih());
        p.setDurum(dto.getDurum());
        p.setAciklama(dto.getAciklama());
        return entityToDTO(puantajRepository.save(p));
    }

    public void sil(Long id) {
        if (!puantajRepository.existsById(id))
            throw new ResourceNotFoundException("Puantaj kaydi", id);
        puantajRepository.deleteById(id);
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
                .tarih(p.getTarih()).durum(p.getDurum())
                .aciklama(p.getAciklama()).olusturmaTarihi(p.getOlusturmaTarihi())
                .build();
    }
}
