package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.entity.Stok;
import com.raspel.erp.entity.StokHareket;
import com.raspel.erp.entity.envanter.StokSeri;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.StokRepository;
import com.raspel.erp.repository.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokSeriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StokSeriService {

    private final StokSeriRepository stokSeriRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;

    @Transactional(readOnly = true)
    public List<StokSeriDTO> tumunuGetir(Long sirketId) {
        return stokSeriRepository.findAll().stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StokSeriDTO> stokIcinGetir(Long stokId) {
        return stokSeriRepository.findByStokId(stokId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StokSeriDTO getir(Long id) {
        return entityToDTO(stokSeriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSeri", id)));
    }

    public StokSeriDTO olustur(StokSeriDTO dto) {
        Stok stok = stokRepository.findById(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        StokSeri seri = StokSeri.builder()
                .stok(stok)
                .seriNo(dto.getSeriNo())
                .lotNo(dto.getLotNo())
                .sonKullanmaTarihi(dto.getSonKullanmaTarihi())
                .build();
        if (dto.getStokHareketId() != null) {
            StokHareket hareket = stokHareketRepository.findById(dto.getStokHareketId())
                    .orElseThrow(() -> new ResourceNotFoundException("StokHareket", dto.getStokHareketId()));
            seri.setStokHareket(hareket);
        }
        return entityToDTO(stokSeriRepository.save(seri));
    }

    public StokSeriDTO guncelle(Long id, StokSeriDTO dto) {
        StokSeri seri = stokSeriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSeri", id));
        if (dto.getSeriNo() != null) seri.setSeriNo(dto.getSeriNo());
        if (dto.getLotNo() != null) seri.setLotNo(dto.getLotNo());
        if (dto.getSonKullanmaTarihi() != null) seri.setSonKullanmaTarihi(dto.getSonKullanmaTarihi());
        if (dto.getStokId() != null) {
            Stok stok = stokRepository.findById(dto.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
            seri.setStok(stok);
        }
        if (dto.getStokHareketId() != null) {
            StokHareket hareket = stokHareketRepository.findById(dto.getStokHareketId())
                    .orElseThrow(() -> new ResourceNotFoundException("StokHareket", dto.getStokHareketId()));
            seri.setStokHareket(hareket);
        }
        return entityToDTO(stokSeriRepository.save(seri));
    }

    public void sil(Long id) {
        if (!stokSeriRepository.existsById(id))
            throw new ResourceNotFoundException("StokSeri", id);
        stokSeriRepository.deleteById(id);
    }

    private StokSeriDTO entityToDTO(StokSeri s) {
        return StokSeriDTO.builder()
                .id(s.getId())
                .stokId(s.getStok() != null ? s.getStok().getId() : null)
                .stokAdi(s.getStok() != null ? s.getStok().getAd() : null)
                .seriNo(s.getSeriNo())
                .lotNo(s.getLotNo())
                .sonKullanmaTarihi(s.getSonKullanmaTarihi())
                .stokHareketId(s.getStokHareket() != null ? s.getStokHareket().getId() : null)
                .olusturmaTarihi(s.getOlusturmaTarihi())
                .build();
    }
}
