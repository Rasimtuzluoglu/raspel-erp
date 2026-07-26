package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.entity.Stok;
import com.raspel.erp.entity.envanter.StokSayim;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.StokRepository;
import com.raspel.erp.repository.envanter.StokSayimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class StokSayimService {

    private final StokSayimRepository stokSayimRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public Page<StokSayimDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return stokSayimRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public StokSayimDTO getir(Long id) {
        return entityToDTO(stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id)));
    }

    public StokSayimDTO olustur(StokSayimDTO dto, Long sirketId) {
        Stok stok = stokRepository.findById(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        StokSayim sayim = StokSayim.builder()
                .tarih(dto.getTarih())
                .stok(stok)
                .beklenenMiktar(dto.getBeklenenMiktar() != null ? dto.getBeklenenMiktar() : BigDecimal.ZERO)
                .sayilanMiktar(dto.getSayilanMiktar() != null ? dto.getSayilanMiktar() : BigDecimal.ZERO)
                .fark(dto.getFark())
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    public StokSayimDTO guncelle(Long id, StokSayimDTO dto) {
        StokSayim sayim = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        if (dto.getTarih() != null) sayim.setTarih(dto.getTarih());
        if (dto.getBeklenenMiktar() != null) sayim.setBeklenenMiktar(dto.getBeklenenMiktar());
        if (dto.getSayilanMiktar() != null) sayim.setSayilanMiktar(dto.getSayilanMiktar());
        if (dto.getFark() != null) sayim.setFark(dto.getFark());
        if (dto.getDurum() != null) sayim.setDurum(dto.getDurum());
        if (dto.getAciklama() != null) sayim.setAciklama(dto.getAciklama());
        if (dto.getStokId() != null) {
            Stok stok = stokRepository.findById(dto.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
            sayim.setStok(stok);
        }
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    public void sil(Long id) {
        if (!stokSayimRepository.existsById(id))
            throw new ResourceNotFoundException("StokSayim", id);
        stokSayimRepository.deleteById(id);
    }

    private StokSayimDTO entityToDTO(StokSayim s) {
        return StokSayimDTO.builder()
                .id(s.getId())
                .tarih(s.getTarih())
                .stokId(s.getStok() != null ? s.getStok().getId() : null)
                .stokAdi(s.getStok() != null ? s.getStok().getAd() : null)
                .beklenenMiktar(s.getBeklenenMiktar())
                .sayilanMiktar(s.getSayilanMiktar())
                .fark(s.getFark())
                .durum(s.getDurum())
                .sirketId(s.getSirketId())
                .aciklama(s.getAciklama())
                .olusturmaTarihi(s.getOlusturmaTarihi())
                .build();
    }
}
