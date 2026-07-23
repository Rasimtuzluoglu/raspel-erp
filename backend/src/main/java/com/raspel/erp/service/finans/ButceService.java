package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.ButceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ButceService {

    private final ButceRepository butceRepository;

    @Transactional(readOnly = true)
    public List<ButceDTO> tumunuGetir(Long sirketId) {
        return butceRepository.findBySirketIdOrderByYilDescAyDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ButceDTO getir(Long id) {
        return entityToDTO(butceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Butce", id)));
    }

    public ButceDTO olustur(ButceDTO dto, Long sirketId) {
        Butce butce = Butce.builder()
                .ad(dto.getAd())
                .yil(dto.getYil())
                .ay(dto.getAy())
                .tutar(dto.getTutar())
                .tur(dto.getTur())
                .kategori(dto.getKategori())
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(butceRepository.save(butce));
    }

    public ButceDTO guncelle(Long id, ButceDTO dto) {
        Butce butce = butceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Butce", id));
        if (dto.getAd() != null) butce.setAd(dto.getAd());
        if (dto.getYil() != null) butce.setYil(dto.getYil());
        if (dto.getAy() != null) butce.setAy(dto.getAy());
        if (dto.getTutar() != null) butce.setTutar(dto.getTutar());
        if (dto.getTur() != null) butce.setTur(dto.getTur());
        if (dto.getKategori() != null) butce.setKategori(dto.getKategori());
        if (dto.getAciklama() != null) butce.setAciklama(dto.getAciklama());
        return entityToDTO(butceRepository.save(butce));
    }

    public void sil(Long id) {
        if (!butceRepository.existsById(id))
            throw new ResourceNotFoundException("Butce", id);
        butceRepository.deleteById(id);
    }

    private ButceDTO entityToDTO(Butce b) {
        return ButceDTO.builder()
                .id(b.getId()).ad(b.getAd()).yil(b.getYil()).ay(b.getAy())
                .tutar(b.getTutar()).tur(b.getTur()).kategori(b.getKategori())
                .sirketId(b.getSirketId()).aciklama(b.getAciklama())
                .olusturmaTarihi(b.getOlusturmaTarihi()).build();
    }
}
