package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.entity.sube.Sube;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sube.SubeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SubeService {

    private final SubeRepository subeRepository;

    @Transactional(readOnly = true)
    public List<SubeDTO> tumunuGetir(Long sirketId) {
        return subeRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubeDTO> aktifSubeler(Long sirketId) {
        return subeRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubeDTO getir(Long id) {
        return entityToDTO(subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id)));
    }

    public SubeDTO olustur(SubeDTO dto) {
        Sube s = Sube.builder()
                .ad(dto.getAd()).adres(dto.getAdres())
                .telefon(dto.getTelefon()).yetkili(dto.getYetkili())
                .sirketId(dto.getSirketId()).build();
        return entityToDTO(subeRepository.save(s));
    }

    public SubeDTO guncelle(Long id, SubeDTO dto) {
        Sube s = subeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sube", id));
        if (dto.getAd() != null) s.setAd(dto.getAd());
        if (dto.getAdres() != null) s.setAdres(dto.getAdres());
        if (dto.getTelefon() != null) s.setTelefon(dto.getTelefon());
        if (dto.getYetkili() != null) s.setYetkili(dto.getYetkili());
        if (dto.getAktif() != null) s.setAktif(dto.getAktif());
        return entityToDTO(subeRepository.save(s));
    }

    public void sil(Long id) {
        if (!subeRepository.existsById(id))
            throw new ResourceNotFoundException("Sube", id);
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
