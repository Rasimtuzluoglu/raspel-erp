package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.PersonelIzinDTO;
import com.raspel.erp.entity.ik.PersonelIzin;
import com.raspel.erp.exception.ResourceNotFoundException;
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

    @Transactional(readOnly = true)
    public List<PersonelIzinDTO> personelIzınleri(Long personelId) {
        return izinRepository.findByPersonelIdOrderByBaslangicDesc(personelId).stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonelIzinDTO getir(Long id) {
        return entityToDTO(izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id)));
    }

    public PersonelIzinDTO olustur(PersonelIzinDTO dto) {
        PersonelIzin izin = PersonelIzin.builder()
                .personelId(dto.getPersonelId())
                .izinTuru(dto.getIzinTuru())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .gunSayisi(dto.getGunSayisi())
                .durum("BEKLEMEDE")
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(izinRepository.save(izin));
    }

    public PersonelIzinDTO guncelle(Long id, PersonelIzinDTO dto) {
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        izin.setPersonelId(dto.getPersonelId());
        izin.setIzinTuru(dto.getIzinTuru());
        izin.setBaslangic(dto.getBaslangic());
        izin.setBitis(dto.getBitis());
        izin.setGunSayisi(dto.getGunSayisi());
        if (dto.getDurum() != null) izin.setDurum(dto.getDurum());
        izin.setAciklama(dto.getAciklama());
        izin.setOnaylayan(dto.getOnaylayan());
        return entityToDTO(izinRepository.save(izin));
    }

    public PersonelIzinDTO durumGuncelle(Long id, String durum, String onaylayan) {
        PersonelIzin izin = izinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Izin kaydi", id));
        izin.setDurum(durum);
        if (onaylayan != null) izin.setOnaylayan(onaylayan);
        return entityToDTO(izinRepository.save(izin));
    }

    public void sil(Long id) {
        if (!izinRepository.existsById(id))
            throw new ResourceNotFoundException("Izin kaydi", id);
        izinRepository.deleteById(id);
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
