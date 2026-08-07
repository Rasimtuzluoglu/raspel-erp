package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.entity.sube.Sube;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sube.SubeRepository;
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
public class SubeService {

    private final SubeRepository subeRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<SubeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return subeRepository.findBySirketIdOrderByAdAsc(sirketId, pageable).map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'sube:aktif:sirket:' + #sirketId")
    @Transactional(readOnly = true)
    public List<SubeDTO> aktifSubeler(Long sirketId) {
        return subeRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubeDTO getir(Long id) {
        Sube s = subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id));
        tenantChecker.check(s.getSirketId(), "Sube");
        return entityToDTO(s);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SubeDTO olustur(SubeDTO dto) {
        Sube s = Sube.builder()
                .ad(dto.getAd()).adres(dto.getAdres())
                .telefon(dto.getTelefon()).yetkili(dto.getYetkili())
                .sirketId(dto.getSirketId()).build();
        return entityToDTO(subeRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public SubeDTO guncelle(Long id, SubeDTO dto) {
        Sube s = subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id));
        tenantChecker.check(s.getSirketId(), "Sube");
        if (dto.getAd() != null) s.setAd(dto.getAd());
        if (dto.getAdres() != null) s.setAdres(dto.getAdres());
        if (dto.getTelefon() != null) s.setTelefon(dto.getTelefon());
        if (dto.getYetkili() != null) s.setYetkili(dto.getYetkili());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        return entityToDTO(subeRepository.save(s));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Sube s = subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id));
        tenantChecker.check(s.getSirketId(), "Sube");
        subeRepository.deleteById(id);
    }

    private SubeDTO entityToDTO(Sube s) {
        return SubeDTO.builder()
                .id(s.getId()).ad(s.getAd()).adres(s.getAdres())
                .telefon(s.getTelefon()).yetkili(s.getYetkili())
                .sirketId(s.getSirketId()).aktif(s.getAktif())
                .olusturmaTarihi(s.getOlusturmaTarihi()).build();
    }
}
