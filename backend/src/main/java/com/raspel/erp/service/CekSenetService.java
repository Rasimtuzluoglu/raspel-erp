package com.raspel.erp.service;

import com.raspel.erp.dto.CekSenetDTO;
import com.raspel.erp.entity.CekSenet;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.CekSenetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CekSenetService {

    private final CekSenetRepository cekSenetRepository;
    private final CariHesapRepository cariHesapRepository;

    @Transactional(readOnly = true)
    public List<CekSenetDTO> tumunuGetir(Long sirketId) {
        return cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CekSenetDTO getir(Long id) {
        return entityToDTO(cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id)));
    }

    public CekSenetDTO olustur(CekSenetDTO dto) {
        CekSenet cs = CekSenet.builder()
                .tur(dto.getTur()).cariHesapId(dto.getCariHesapId())
                .bankaAdi(dto.getBankaAdi()).sube(dto.getSube())
                .cekNo(dto.getCekNo()).hesapNo(dto.getHesapNo())
                .vadeTarihi(dto.getVadeTarihi()).tutar(dto.getTutar())
                .durum("PORTFOY").aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId()).build();
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public CekSenetDTO durumGuncelle(Long id, String durum) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        cs.setDurum(durum);
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public void sil(Long id) {
        if (!cekSenetRepository.existsById(id))
            throw new ResourceNotFoundException("Cek/Senet", id);
        cekSenetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs) {
        return CekSenetDTO.builder()
                .id(cs.getId()).tur(cs.getTur())
                .cariHesapId(cs.getCariHesapId())
                .cariHesapAdi(cs.getCariHesapId() != null ?
                        cariHesapRepository.findById(cs.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null)
                .bankaAdi(cs.getBankaAdi()).sube(cs.getSube())
                .cekNo(cs.getCekNo()).hesapNo(cs.getHesapNo())
                .vadeTarihi(cs.getVadeTarihi()).kesinmeTarihi(cs.getKesinmeTarihi())
                .tutar(cs.getTutar()).durum(cs.getDurum())
                .aciklama(cs.getAciklama()).sirketId(cs.getSirketId())
                .olusturmaTarihi(cs.getOlusturmaTarihi()).build();
    }
}
