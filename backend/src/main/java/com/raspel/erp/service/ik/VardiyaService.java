package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.entity.Personel;
import com.raspel.erp.entity.ik.Vardiya;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.PersonelRepository;
import com.raspel.erp.repository.ik.VardiyaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class VardiyaService {

    private final VardiyaRepository vardiyaRepository;
    private final PersonelRepository personelRepository;

    @Transactional(readOnly = true)
    public Page<VardiyaDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return vardiyaRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public VardiyaDTO getir(Long id) {
        return entityToDTO(vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id)));
    }

    public VardiyaDTO olustur(VardiyaDTO dto, Long sirketId) {
        Personel personel = personelRepository.findById(dto.getPersonelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
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
        if (dto.getTarih() != null) vardiya.setTarih(dto.getTarih());
        if (dto.getBaslangic() != null) vardiya.setBaslangic(dto.getBaslangic());
        if (dto.getBitis() != null) vardiya.setBitis(dto.getBitis());
        if (dto.getTur() != null) vardiya.setTur(dto.getTur());
        if (dto.getPersonelId() != null) {
            Personel personel = personelRepository.findById(dto.getPersonelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", dto.getPersonelId()));
            vardiya.setPersonel(personel);
        }
        return entityToDTO(vardiyaRepository.save(vardiya));
    }

    public void sil(Long id) {
        if (!vardiyaRepository.existsById(id))
            throw new ResourceNotFoundException("Vardiya", id);
        vardiyaRepository.deleteById(id);
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
