package com.raspel.erp.service;

import com.raspel.erp.dto.IrsaliyeDTO;
import com.raspel.erp.dto.IrsaliyeKalemDTO;
import com.raspel.erp.entity.Irsaliye;
import com.raspel.erp.entity.IrsaliyeKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class IrsaliyeService {

    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public List<IrsaliyeDTO> tumunuGetir(Long sirketId) {
        return irsaliyeRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IrsaliyeDTO getir(Long id) {
        return entityToDTO(irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id)));
    }

    public IrsaliyeDTO olustur(IrsaliyeDTO dto) {
        Irsaliye i = Irsaliye.builder()
                .irsaliyeNo(dto.getIrsaliyeNo()).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).faturaId(dto.getFaturaId())
                .durum("TASLAK").tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .aciklama(dto.getAciklama()).sirketId(dto.getSirketId()).build();
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    public IrsaliyeDTO durumGuncelle(Long id, String durum) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        i.setDurum(durum);
        return entityToDTO(irsaliyeRepository.save(i));
    }

    public void sil(Long id) {
        if (!irsaliyeRepository.existsById(id)) {
            throw new ResourceNotFoundException("İrsaliye", id);
        }
        kalemRepository.deleteByIrsaliyeId(id);
        irsaliyeRepository.deleteById(id);
    }

    private IrsaliyeDTO entityToDTO(Irsaliye i) {
        List<IrsaliyeKalemDTO> kalemler = kalemRepository.findByIrsaliyeId(i.getId()).stream()
                .map(k -> IrsaliyeKalemDTO.builder().id(k.getId())
                        .stokId(k.getStokId()).stokAdi(k.getStokId() != null ? stokRepository.findById(k.getStokId()).map(s -> s.getAd()).orElse(null) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim()).build())
                .collect(Collectors.toList());
        return IrsaliyeDTO.builder().id(i.getId()).irsaliyeNo(i.getIrsaliyeNo()).tarih(i.getTarih())
                .cariHesapId(i.getCariHesapId())
                .cariHesapAdi(i.getCariHesapId() != null ? cariHesapRepository.findById(i.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null)
                .faturaId(i.getFaturaId()).durum(i.getDurum()).tur(i.getTur())
                .aciklama(i.getAciklama()).sirketId(i.getSirketId())
                .olusturmaTarihi(i.getOlusturmaTarihi()).kalemler(kalemler).build();
    }
}
