package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.DonemDTO;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.DonemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DonemService {

    private final DonemRepository donemRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<DonemDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        return donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'donemSirket:' + #sirketId")
    @Transactional(readOnly = true)
    public List<DonemDTO> sirketeGoreGetir(Long sirketId) {
        return donemRepository.findBySirketIdOrderByBaslangicDesc(sirketId, Pageable.unpaged()).map(this::entityToDTO).getContent();
    }

    @Cacheable(value = "lookup", key = "'donemAktif:' + #sirketId")
    @Transactional(readOnly = true)
    public List<DonemDTO> aktifDonemler(Long sirketId) {
        return donemRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "lookup", key = "'donemId:' + #id")
    @Transactional(readOnly = true)
    public DonemDTO getir(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        return entityToDTO(d);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO olustur(DonemDTO dto) {
        Donem d = Donem.builder()
                .sirketId(dto.getSirketId())
                .ad(dto.getAd())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Dönem");
        return entityToDTO(donemRepository.save(d));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public DonemDTO guncelle(Long id, DonemDTO dto) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        if (dto.getAd() != null) d.setAd(dto.getAd());
        if (dto.getBaslangic() != null) d.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) d.setBitis(dto.getBitis());
        if (dto.getAktif() != null) d.setAktif(dto.getAktif());
        return entityToDTO(donemRepository.save(d));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Donem d = donemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dönem", id));
        tenantChecker.check(d.getSirketId(), "Dönem");
        donemRepository.deleteById(id);
    }

    private DonemDTO entityToDTO(Donem d) {
        return DonemDTO.builder()
                .id(d.getId()).sirketId(d.getSirketId())
                .ad(d.getAd()).baslangic(d.getBaslangic())
                .bitis(d.getBitis()).aktif(d.getAktif())
                .olusturmaTarihi(d.getOlusturmaTarihi())
                .build();
    }
}
