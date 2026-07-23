package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.IadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class IadeService {

    private final IadeRepository iadeRepository;

    @Transactional(readOnly = true)
    public List<IadeDTO> tumunuGetir(Long sirketId) {
        return iadeRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IadeDTO getir(Long id) {
        return entityToDTO(iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id)));
    }

    public IadeDTO olustur(IadeDTO dto, Long sirketId) {
        Iade iade = Iade.builder()
                .faturaId(dto.getFaturaId())
                .tarih(dto.getTarih())
                .tutar(dto.getTutar())
                .aciklama(dto.getAciklama())
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .sirketId(sirketId)
                .build();
        return entityToDTO(iadeRepository.save(iade));
    }

    public IadeDTO guncelle(Long id, IadeDTO dto) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        if (dto.getFaturaId() != null) iade.setFaturaId(dto.getFaturaId());
        if (dto.getTarih() != null) iade.setTarih(dto.getTarih());
        if (dto.getTutar() != null) iade.setTutar(dto.getTutar());
        if (dto.getAciklama() != null) iade.setAciklama(dto.getAciklama());
        if (dto.getDurum() != null) iade.setDurum(dto.getDurum());
        return entityToDTO(iadeRepository.save(iade));
    }

    public void sil(Long id) {
        if (!iadeRepository.existsById(id))
            throw new ResourceNotFoundException("Iade", id);
        iadeRepository.deleteById(id);
    }

    private IadeDTO entityToDTO(Iade i) {
        return IadeDTO.builder()
                .id(i.getId()).faturaId(i.getFaturaId()).tarih(i.getTarih())
                .tutar(i.getTutar()).aciklama(i.getAciklama()).durum(i.getDurum())
                .sirketId(i.getSirketId()).olusturmaTarihi(i.getOlusturmaTarihi())
                .build();
    }
}
