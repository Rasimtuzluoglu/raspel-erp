package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.Vardiya;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ik.VardiyaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VardiyaService {

    private final VardiyaRepository vardiyaRepository;
    private final PersonelRepository personelRepository;
    private final com.raspel.erp.config.TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<VardiyaDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return vardiyaRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public VardiyaDTO getir(Long id) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");
        return entityToDTO(vardiya);
    }

    @Transactional(readOnly = true)
    public List<VardiyaDTO> personelVardiyalari(Long personelId) {
        Personel personel = personelRepository.findById(personelId)
                .orElseThrow(() -> new ResourceNotFoundException("Personel", personelId));
        tenantChecker.check(personel.getSirketId(), "Personel");
        return vardiyaRepository.findByPersonelIdOrderByTarihDesc(personelId).stream()
                .map(this::entityToDTO)
                .toList();
    }

    public VardiyaDTO olustur(VardiyaDTO dto, Long sirketId) {
        Personel personel = personelRepository.findById(dto.getPersonelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
        tenantChecker.check(personel.getSirketId(), "Personel");
        Vardiya vardiya = Vardiya.builder()
                .personel(personel)
                .tarih(dto.getTarih())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .tur(dto.getTur())
                .sirketId(sirketId)
                .build();
        return entityToDTO(vardiyaRepository.save(vardiya));
    }

    public VardiyaDTO guncelle(Long id, VardiyaDTO dto) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");
        if (dto.getTarih() != null) vardiya.setTarih(dto.getTarih());
        if (dto.getBaslangic() != null) vardiya.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) vardiya.setBitis(dto.getBitis());
        if (dto.getTur() != null) vardiya.setTur(dto.getTur());
        if (dto.getPersonelId() != null) {
            Personel personel = personelRepository.findById(dto.getPersonelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
            tenantChecker.check(personel.getSirketId(), "Personel");
            vardiya.setPersonel(personel);
        }
        return entityToDTO(vardiyaRepository.save(vardiya));
    }

    public void sil(Long id) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");
        vardiyaRepository.delete(vardiya);
    }

    private VardiyaDTO entityToDTO(Vardiya v) {
        return VardiyaDTO.builder()
                .id(v.getId())
                .personelId(v.getPersonel() != null ? v.getPersonel().getId() : null)
                .personelAdi(v.getPersonel() != null ? v.getPersonel().getAd() + " " + v.getPersonel().getSoyad() : null)
                .tarih(v.getTarih()).baslangic(v.getBaslangic()).bitis(v.getBitis())
                .tur(v.getTur()).sirketId(v.getSirketId())
                .olusturmaTarihi(v.getOlusturmaTarihi()).build();
    }
}
