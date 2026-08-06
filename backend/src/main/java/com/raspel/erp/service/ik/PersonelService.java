package com.raspel.erp.service.ik;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonelService {

    private final PersonelRepository personelRepository;
    private final TenantChecker tenantChecker;

    @Cacheable(value = "lookup", key = "'personel:sirket:' + #sirketId")
    public Page<PersonelDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return personelRepository.findBySirketIdOrderByAdAsc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    public PersonelDTO getir(Long id) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        return entityToDTO(p);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public PersonelDTO olustur(PersonelDTO dto) {
        Personel p = Personel.builder()
                .ad(dto.getAd()).soyad(dto.getSoyad())
                .tcKimlik(dto.getTcKimlik()).dogumTarihi(dto.getDogumTarihi())
                .iseGirisTarihi(dto.getIseGirisTarihi())
                .departman(dto.getDepartman()).pozisyon(dto.getPozisyon())
                .maas(dto.getMaas()).telefon(dto.getTelefon())
                .email(dto.getEmail()).adres(dto.getAdres())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .sirketId(dto.getSirketId())
                .build();
        return entityToDTO(personelRepository.save(p));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public PersonelDTO guncelle(Long id, PersonelDTO dto) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        if (dto.getAd() != null) p.setAd(dto.getAd());
        if (dto.getSoyad() != null) p.setSoyad(dto.getSoyad());
        if (dto.getTcKimlik() != null) p.setTcKimlik(dto.getTcKimlik());
        if (dto.getDepartman() != null) p.setDepartman(dto.getDepartman());
        if (dto.getPozisyon() != null) p.setPozisyon(dto.getPozisyon());
        if (dto.getMaas() != null) p.setMaas(dto.getMaas());
        if (dto.getTelefon() != null) p.setTelefon(dto.getTelefon());
        if (dto.getEmail() != null) p.setEmail(dto.getEmail());
        if (dto.getAdres() != null) p.setAdres(dto.getAdres());
        if (dto.getAktif() != null) p.setAktif(dto.getAktif());
        if (dto.getCikisTarihi() != null) p.setCikisTarihi(dto.getCikisTarihi());
        return entityToDTO(personelRepository.save(p));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Personel p = personelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", id));
        tenantChecker.check(p.getSirketId(), "Personel");
        personelRepository.deleteById(id);
    }

    private PersonelDTO entityToDTO(Personel p) {
        return PersonelDTO.builder()
                .id(p.getId()).ad(p.getAd()).soyad(p.getSoyad())
                .tcKimlik(p.getTcKimlik()).dogumTarihi(p.getDogumTarihi())
                .iseGirisTarihi(p.getIseGirisTarihi()).cikisTarihi(p.getCikisTarihi())
                .departman(p.getDepartman()).pozisyon(p.getPozisyon())
                .maas(p.getMaas()).telefon(p.getTelefon())
                .email(p.getEmail()).adres(p.getAdres())
                .aktif(p.getAktif()).sirketId(p.getSirketId())
                .olusturmaTarihi(p.getOlusturmaTarihi())
                .build();
    }
}
