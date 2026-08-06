package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.MasrafRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MasrafService {

    private final MasrafRepository masrafRepository;
    private final TenantChecker tenantChecker;

    @Cacheable(value = "lookup", key = "'masraf:sirket:' + #sirketId")
    @Transactional(readOnly = true)
    public Page<MasrafDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return masrafRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public MasrafDTO getir(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
        return entityToDTO(m);
    }

    @CacheEvict(value = "lookup", allEntries = true)
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

    @CacheEvict(value = "lookup", allEntries = true)
    public MasrafDTO guncelle(Long id, MasrafDTO dto) {
        Masraf masraf = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(masraf.getSirketId(), "Masraf");
        if (dto.getTarih() != null) masraf.setTarih(dto.getTarih());
        if (dto.getTutar() != null) masraf.setTutar(dto.getTutar());
        if (dto.getAciklama() != null) masraf.setAciklama(dto.getAciklama());
        if (dto.getKategori() != null) masraf.setKategori(dto.getKategori());
        if (dto.getCariHesapId() != null) masraf.setCariHesapId(dto.getCariHesapId());
        if (dto.getBelgeNo() != null) masraf.setBelgeNo(dto.getBelgeNo());
        return entityToDTO(masrafRepository.save(masraf));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
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
