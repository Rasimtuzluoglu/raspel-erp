package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.FiyatListesiDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.FiyatListesi;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FiyatListesiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FiyatListesiService {

    private final FiyatListesiRepository fiyatListesiRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<FiyatListesiDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return fiyatListesiRepository.findBySirketId(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public FiyatListesiDTO getir(Long id) {
        FiyatListesi fl = fiyatListesiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FiyatListesi", id));
        tenantChecker.check(fl.getSirketId(), "FiyatListesi");
        return entityToDTO(fl);
    }

    public FiyatListesiDTO olustur(FiyatListesiDTO dto, Long sirketId) {
        Stok stok = stokRepository.findById(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        FiyatListesi fl = FiyatListesi.builder()
                .stok(stok)
                .alisFiyat(dto.getAlisFiyat())
                .satisFiyat(dto.getSatisFiyat())
                .gecerliBaslangic(dto.getGecerliBaslangic())
                .gecerliBitis(dto.getGecerliBitis())
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(fiyatListesiRepository.save(fl));
    }

    public FiyatListesiDTO guncelle(Long id, FiyatListesiDTO dto) {
        FiyatListesi fl = fiyatListesiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FiyatListesi", id));
        tenantChecker.check(fl.getSirketId(), "FiyatListesi");
        if (dto.getAlisFiyat() != null) fl.setAlisFiyat(dto.getAlisFiyat());
        if (dto.getSatisFiyat() != null) fl.setSatisFiyat(dto.getSatisFiyat());
        if (dto.getGecerliBaslangic() != null) fl.setGecerliBaslangic(dto.getGecerliBaslangic());
        if (dto.getGecerliBitis() != null) fl.setGecerliBitis(dto.getGecerliBitis());
        if (dto.getAciklama() != null) fl.setAciklama(dto.getAciklama());
        if (dto.getStokId() != null) {
            Stok stok = stokRepository.findById(dto.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
            fl.setStok(stok);
        }
        return entityToDTO(fiyatListesiRepository.save(fl));
    }

    public void sil(Long id) {
        FiyatListesi fl = fiyatListesiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FiyatListesi", id));
        tenantChecker.check(fl.getSirketId(), "FiyatListesi");
        fiyatListesiRepository.deleteById(id);
    }

    private FiyatListesiDTO entityToDTO(FiyatListesi f) {
        return FiyatListesiDTO.builder()
                .id(f.getId())
                .stokId(f.getStok() != null ? f.getStok().getId() : null)
                .stokAdi(f.getStok() != null ? f.getStok().getAd() : null)
                .alisFiyat(f.getAlisFiyat())
                .satisFiyat(f.getSatisFiyat())
                .gecerliBaslangic(f.getGecerliBaslangic())
                .gecerliBitis(f.getGecerliBitis())
                .sirketId(f.getSirketId())
                .aciklama(f.getAciklama())
                .olusturmaTarihi(f.getOlusturmaTarihi())
                .build();
    }
}
