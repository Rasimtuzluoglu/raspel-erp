package com.raspel.erp.service;

import com.raspel.erp.dto.NotDTO;
import com.raspel.erp.entity.Not;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.NotRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public Page<NotDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return notRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public List<NotDTO> kullaniciNotlari(Long sirketId, Long kullaniciId) {
        return notRepository.findBySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(sirketId, kullaniciId)
                .stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotDTO idyeGoreGetir(Long id) {
        Not not = notRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not", id));
        return entityToDTO(not);
    }

    public NotDTO olustur(NotDTO dto, Long sirketId, Long kullaniciId) {
        Not not = Not.builder()
                .baslik(dto.getBaslik())
                .icerik(dto.getIcerik())
                .onemDerecesi(dto.getOnemDerecesi() != null ? dto.getOnemDerecesi() : "NORMAL")
                .kullaniciId(kullaniciId)
                .sirketId(sirketId)
                .build();
        return entityToDTO(notRepository.save(not));
    }

    public NotDTO guncelle(Long id, NotDTO dto) {
        Not not = notRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not", id));
        not.setBaslik(dto.getBaslik());
        not.setIcerik(dto.getIcerik());
        not.setOnemDerecesi(dto.getOnemDerecesi() != null ? dto.getOnemDerecesi() : "NORMAL");
        return entityToDTO(notRepository.save(not));
    }

    public void sil(Long id) {
        notRepository.deleteById(id);
    }

    private NotDTO entityToDTO(Not not) {
        return NotDTO.builder()
                .id(not.getId())
                .baslik(not.getBaslik())
                .icerik(not.getIcerik())
                .onemDerecesi(not.getOnemDerecesi())
                .kullaniciId(not.getKullaniciId())
                .olusturmaTarihi(not.getOlusturmaTarihi())
                .guncellemeTarihi(not.getGuncellemeTarihi())
                .build();
    }
}
