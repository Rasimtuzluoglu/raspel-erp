package com.raspel.erp.service.envanter;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.envanter.StokSeri;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokSeriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.entity.finans.Hareket;

@Service
@Transactional
@RequiredArgsConstructor
public class StokSeriService {

    private final StokSeriRepository stokSeriRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<StokSeriDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        return stokSeriRepository.findByStokSirketId(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public List<StokSeriDTO> stokIcinGetir(Long stokId) {
        return stokSeriRepository.findByStokId(stokId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StokSeriDTO getir(Long id) {
        StokSeri seri = stokSeriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSeri", id));
        tenantChecker.check(seri.getStok().getSirketId(), "StokSeri");
        return entityToDTO(seri);
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
        tenantChecker.check(seri.getStok().getSirketId(), "StokSeri");
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
        StokSeri seri = stokSeriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSeri", id));
        tenantChecker.check(seri.getStok().getSirketId(), "StokSeri");
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