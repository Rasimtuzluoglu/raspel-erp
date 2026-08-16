package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.NotDTO;
import com.raspel.erp.entity.sistem.Not;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.NotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NotService {

    private final NotRepository notRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<NotDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return notRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'not:sirket:' + #sirketId + ':kullanici:' + #kullaniciId")
    @Transactional(readOnly = true)
    public List<NotDTO> kullaniciNotlari(Long sirketId, Long kullaniciId) {
        return notRepository.findBySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(sirketId, kullaniciId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotDTO idyeGoreGetir(Long id) {
        Not not = notRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not", id));
        tenantChecker.check(not.getSirketId(), "Not");
        return entityToDTO(not);
    }

    @Transactional(readOnly = true)
    public List<NotDTO> cariNotlari(Long cariHesapId) {
        return notRepository.findByCariHesapIdOrderByOlusturmaTarihiDesc(cariHesapId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public NotDTO olustur(NotDTO dto, Long sirketId, Long kullaniciId) {
        Not not = Not.builder()
                .baslik(dto.getBaslik())
                .icerik(dto.getIcerik())
                .onemDerecesi(dto.getOnemDerecesi() != null ? dto.getOnemDerecesi() : "NORMAL")
                .renk(dto.getRenk() != null ? dto.getRenk() : "MAVI")
                .cariHesapId(dto.getCariHesapId())
                .kullaniciId(kullaniciId)
                .sirketId(sirketId)
                .build();
        return entityToDTO(notRepository.save(not));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public NotDTO guncelle(Long id, NotDTO dto) {
        Not not = notRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not", id));
        tenantChecker.check(not.getSirketId(), "Not");
        not.setBaslik(dto.getBaslik());
        not.setIcerik(dto.getIcerik());
        not.setOnemDerecesi(dto.getOnemDerecesi() != null ? dto.getOnemDerecesi() : "NORMAL");
        not.setRenk(dto.getRenk() != null ? dto.getRenk() : "MAVI");
        return entityToDTO(notRepository.save(not));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Not not = notRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not", id));
        tenantChecker.check(not.getSirketId(), "Not");
        notRepository.deleteById(id);
    }

    private NotDTO entityToDTO(Not not) {
        return NotDTO.builder()
                .id(not.getId())
                .baslik(not.getBaslik())
                .icerik(not.getIcerik())
                .onemDerecesi(not.getOnemDerecesi())
                .renk(not.getRenk())
                .cariHesapId(not.getCariHesapId())
                .kullaniciId(not.getKullaniciId())
                .olusturmaTarihi(not.getOlusturmaTarihi())
                .guncellemeTarihi(not.getGuncellemeTarihi())
                .build();
    }
}
