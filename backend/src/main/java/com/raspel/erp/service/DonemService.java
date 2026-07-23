package com.raspel.erp.service;

import com.raspel.erp.dto.DonemDTO;
import com.raspel.erp.entity.Donem;
import com.raspel.erp.repository.DonemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DonemService {

    private final DonemRepository donemRepository;

    public List<DonemDTO> tumunuGetir() {
        return donemRepository.findAll().stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    public List<DonemDTO> sirketeGoreGetir(Long sirketId) {
        return donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    public List<DonemDTO> aktifDonemler(Long sirketId) {
        return donemRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    public DonemDTO getir(Long id) {
        return entityToDTO(donemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dönem bulunamadı: " + id)));
    }

    public DonemDTO olustur(DonemDTO dto) {
        Donem d = Donem.builder()
                .sirketId(dto.getSirketId())
                .ad(dto.getAd())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        return entityToDTO(donemRepository.save(d));
    }

    public DonemDTO guncelle(Long id, DonemDTO dto) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dönem bulunamadı: " + id));
        if (dto.getAd() != null) d.setAd(dto.getAd());
        if (dto.getBaslangic() != null) d.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) d.setBitis(dto.getBitis());
        if (dto.getAktif() != null) d.setAktif(dto.getAktif());
        return entityToDTO(donemRepository.save(d));
    }

    public void sil(Long id) {
        if (!donemRepository.existsById(id)) throw new RuntimeException("Dönem bulunamadı: " + id);
        donemRepository.deleteById(id);
    }

    private DonemDTO entityToDTO(Donem d) {
        return DonemDTO.builder()
                .id(d.getId()).sirketId(d.getSirketId())
                .ad(d.getAd()).baslangic(d.getBaslangic())
                .bitis(d.getBitis()).aktif(d.getAktif())
                .olusturmaTarihi(d.getOlusturmaTarihi())
                .build();
    }
}
