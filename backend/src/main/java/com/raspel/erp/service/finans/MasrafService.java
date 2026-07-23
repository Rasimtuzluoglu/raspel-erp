package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.MasrafRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MasrafService {

    private final MasrafRepository masrafRepository;

    @Transactional(readOnly = true)
    public List<MasrafDTO> tumunuGetir(Long sirketId) {
        return masrafRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MasrafDTO getir(Long id) {
        return entityToDTO(masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id)));
    }

    public MasrafDTO olustur(MasrafDTO dto, Long sirketId) {
        Masraf masraf = Masraf.builder()
                .tarih(dto.getTarih())
                .tutar(dto.getTutar())
                .aciklama(dto.getAciklama())
                .kategori(dto.getKategori())
                .cariHesapId(dto.getCariHesapId())
                .belgeNo(dto.getBelgeNo())
                .sirketId(sirketId)
                .build();
        return entityToDTO(masrafRepository.save(masraf));
    }

    public MasrafDTO guncelle(Long id, MasrafDTO dto) {
        Masraf masraf = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        if (dto.getTarih() != null) masraf.setTarih(dto.getTarih());
        if (dto.getTutar() != null) masraf.setTutar(dto.getTutar());
        if (dto.getAciklama() != null) masraf.setAciklama(dto.getAciklama());
        if (dto.getKategori() != null) masraf.setKategori(dto.getKategori());
        if (dto.getCariHesapId() != null) masraf.setCariHesapId(dto.getCariHesapId());
        if (dto.getBelgeNo() != null) masraf.setBelgeNo(dto.getBelgeNo());
        return entityToDTO(masrafRepository.save(masraf));
    }

    public void sil(Long id) {
        if (!masrafRepository.existsById(id))
            throw new ResourceNotFoundException("Masraf", id);
        masrafRepository.deleteById(id);
    }

    private MasrafDTO entityToDTO(Masraf m) {
        return MasrafDTO.builder()
                .id(m.getId()).tarih(m.getTarih()).tutar(m.getTutar())
                .aciklama(m.getAciklama()).kategori(m.getKategori())
                .cariHesapId(m.getCariHesapId()).belgeNo(m.getBelgeNo())
                .sirketId(m.getSirketId()).olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
