package com.raspel.erp.service.ik;

import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.ik.Vardiya;
import com.raspel.erp.exception.BusinessException;
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

    /** Vardiya KPI özeti: toplam/bugün/bu hafta + vardiyası olan personel sayısı. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.ik.VardiyaOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.ik.VardiyaOzetDTO.builder().build();
        }
        java.time.LocalDate bugun = java.time.LocalDate.now();
        java.time.LocalDate haftaBaslangic = bugun.minusDays(6);
        return com.raspel.erp.dto.ik.VardiyaOzetDTO.builder()
                .toplam(vardiyaRepository.countBySirketId(sirketId))
                .bugun(vardiyaRepository.countBySirketIdAndTarih(sirketId, bugun))
                .buHafta(vardiyaRepository.countBySirketIdAndTarihBetween(sirketId, haftaBaslangic, bugun))
                .personelSayisi(vardiyaRepository.countDistinctPersonelBySirketId(sirketId))
                .build();
    }

    @Transactional(readOnly = true)
    public VardiyaDTO getir(Long id) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");
        return entityToDTO(vardiya);
    }

    /**
     * Vardiya listesi: personel, tarih aralığı, tür ve personel adı araması + sayfalama.
     */
    @Transactional(readOnly = true)
    public Page<VardiyaDTO> filtreli(Long sirketId, Long personelId, java.time.LocalDate baslangic,
                                     java.time.LocalDate bitis, String tur, String q, Pageable pageable) {
        if (sirketId == null) {
            return Page.empty(pageable);
        }
        String turFiltre = (tur == null || tur.isBlank() || "TUMU".equalsIgnoreCase(tur))
                ? null : tur.trim().toUpperCase(java.util.Locale.ROOT);
        String arama = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(java.util.Locale.ROOT);
        return vardiyaRepository.filtrele(sirketId, personelId, baslangic, bitis, turFiltre, arama, pageable)
                .map(this::entityToDTO);
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
        vardiyaDogrula(dto.getTarih(), dto.getBaslangic(), dto.getBitis(), dto.getTur());
        if (vardiyaRepository.existsByPersonelIdAndTarih(dto.getPersonelId(), dto.getTarih())) {
            throw new BusinessException("Bu personel için seçilen tarihte vardiya kaydı zaten mevcut");
        }
        Vardiya vardiya = Vardiya.builder()
                .personel(personel)
                .tarih(dto.getTarih())
                .baslangic(dto.getBaslangic())
                .bitis(dto.getBitis())
                .tur(dto.getTur().trim().toUpperCase(java.util.Locale.ROOT))
                .sirketId(sirketId)
                .build();
        return entityToDTO(vardiyaRepository.save(vardiya));
    }

    public VardiyaDTO guncelle(Long id, VardiyaDTO dto) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");

        Long personelId = dto.getPersonelId() != null ? dto.getPersonelId() : vardiya.getPersonel().getId();
        java.time.LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : vardiya.getTarih();
        java.time.LocalTime baslangic = dto.getBaslangic() != null ? dto.getBaslangic() : vardiya.getBaslangic();
        java.time.LocalTime bitis = dto.getBitis() != null ? dto.getBitis() : vardiya.getBitis();
        String tur = dto.getTur() != null ? dto.getTur() : vardiya.getTur();
        vardiyaDogrula(tarih, baslangic, bitis, tur);

        if (!personelId.equals(vardiya.getPersonel().getId())) {
            Personel personel = personelRepository.findById(personelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Personel", personelId));
            tenantChecker.check(personel.getSirketId(), "Personel");
            vardiya.setPersonel(personel);
        }
        if (vardiyaRepository.existsByPersonelIdAndTarihAndIdNot(personelId, tarih, id)) {
            throw new BusinessException("Bu personel için seçilen tarihte vardiya kaydı zaten mevcut");
        }
        vardiya.setTarih(tarih);
        vardiya.setBaslangic(baslangic);
        vardiya.setBitis(bitis);
        vardiya.setTur(tur.trim().toUpperCase(java.util.Locale.ROOT));
        return entityToDTO(vardiyaRepository.save(vardiya));
    }

    public void sil(Long id) {
        Vardiya vardiya = vardiyaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vardiya", id));
        tenantChecker.check(vardiya.getSirketId(), "Vardiya");
        vardiyaRepository.delete(vardiya);
    }

    private static final List<String> GECERLI_TURLER = List.of("SABAH", "AKSAM", "GECE");

    private void vardiyaDogrula(java.time.LocalDate tarih, java.time.LocalTime baslangic,
                                java.time.LocalTime bitis, String tur) {
        if (tarih == null) {
            throw new BusinessException("Vardiya tarihi zorunludur");
        }
        if (baslangic == null || bitis == null) {
            throw new BusinessException("Vardiya başlangıç ve bitiş saatleri zorunludur");
        }
        if (baslangic.equals(bitis)) {
            throw new BusinessException("Başlangıç ve bitiş saati aynı olamaz");
        }
        if (tur == null || tur.isBlank()) {
            throw new BusinessException("Vardiya türü zorunludur");
        }
        if (!GECERLI_TURLER.contains(tur.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new BusinessException("Geçersiz vardiya türü");
        }
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
