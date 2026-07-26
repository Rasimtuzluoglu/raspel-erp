package com.raspel.erp.service;

import com.raspel.erp.dto.SatinalmaTalepDTO;
import com.raspel.erp.dto.SatinalmaTalepKalemDTO;
import com.raspel.erp.entity.SatinalmaTalep;
import com.raspel.erp.entity.SatinalmaTalepKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.SatinalmaTalepKalemRepository;
import com.raspel.erp.repository.SatinalmaTalepRepository;
import com.raspel.erp.repository.StokRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SatinalmaTalepService {

    private final SatinalmaTalepRepository talepRepository;
    private final SatinalmaTalepKalemRepository kalemRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public List<SatinalmaTalepDTO> tumunuGetir(Long sirketId) {
        return talepRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SatinalmaTalepDTO getir(Long id) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO olustur(SatinalmaTalepDTO dto) {
        SatinalmaTalep t = SatinalmaTalep.builder()
                .talepNo(dto.getTalepNo())
                .tarih(dto.getTarih())
                .talepEden(dto.getTalepEden())
                .departman(dto.getDepartman())
                .durum("TASLAK")
                .aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId())
                .build();
        t = talepRepository.save(t);

        if (dto.getKalemler() != null) {
            for (SatinalmaTalepKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaTalepKalem.builder()
                        .talepId(t.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .build());
            }
        }
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO guncelle(Long id, SatinalmaTalepDTO dto) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        t.setTalepNo(dto.getTalepNo());
        t.setTarih(dto.getTarih());
        t.setTalepEden(dto.getTalepEden());
        t.setDepartman(dto.getDepartman());
        if (dto.getDurum() != null) t.setDurum(dto.getDurum());
        t.setAciklama(dto.getAciklama());
        t = talepRepository.save(t);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteByTalepId(t.getId());
            for (SatinalmaTalepKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaTalepKalem.builder()
                        .talepId(t.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .build());
            }
        }
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO durumGuncelle(Long id, String durum) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        t.setDurum(durum);
        return entityToDTO(talepRepository.save(t));
    }

    public void sil(Long id) {
        if (!talepRepository.existsById(id)) {
            throw new ResourceNotFoundException("Talep", id);
        }
        kalemRepository.deleteByTalepId(id);
        talepRepository.deleteById(id);
    }

    private SatinalmaTalepDTO entityToDTO(SatinalmaTalep t) {
        List<SatinalmaTalepKalemDTO> kalemler = kalemRepository.findByTalepId(t.getId()).stream()
                .map(k -> SatinalmaTalepKalemDTO.builder()
                        .id(k.getId()).talepId(k.getTalepId()).stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokRepository.findById(k.getStokId()).map(s -> s.getAd()).orElse(null) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .olusturmaTarihi(k.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());

        return SatinalmaTalepDTO.builder()
                .id(t.getId()).talepNo(t.getTalepNo()).tarih(t.getTarih())
                .talepEden(t.getTalepEden()).departman(t.getDepartman())
                .durum(t.getDurum()).aciklama(t.getAciklama())
                .sirketId(t.getSirketId()).olusturmaTarihi(t.getOlusturmaTarihi())
                .kalemler(kalemler).build();
    }
}
