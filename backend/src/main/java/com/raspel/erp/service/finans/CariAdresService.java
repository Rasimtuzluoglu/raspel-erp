package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CariAdresDTO;
import com.raspel.erp.entity.finans.CariAdres;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariAdresRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Faz 2.5: Cariye ait çoklu adres/iletişim kayıtlarının yönetimi.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CariAdresService {

    private final CariAdresRepository cariAdresRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public List<CariAdresDTO> listele(Long cariHesapId, Long sirketId) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("CariHesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        return cariAdresRepository
                .findBySirketIdAndCariHesapIdOrderByVarsayilanDescIdAsc(cari.getSirketId(), cariHesapId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CariAdresDTO ekle(Long cariHesapId, CariAdresDTO dto, Long sirketId) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("CariHesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "CariHesap");
        Long hedefSirketId = cari.getSirketId() != null ? cari.getSirketId() : sirketId;
        boolean ilkKayit = cariAdresRepository.countBySirketIdAndCariHesapId(hedefSirketId, cariHesapId) == 0;
        CariAdres adres = CariAdres.builder()
                .cariHesapId(cariHesapId)
                .sirketId(hedefSirketId)
                .baslik(dto.getBaslik())
                .adres(dto.getAdres())
                .il(dto.getIl())
                .ilce(dto.getIlce())
                .yetkiliKisi(dto.getYetkiliKisi())
                .telefon(dto.getTelefon())
                // İlk kayıt otomatik varsayılan olur.
                .varsayilan(Boolean.TRUE.equals(dto.getVarsayilan()) || ilkKayit)
                .build();
        CariAdres kaydedilen = cariAdresRepository.save(adres);
        if (Boolean.TRUE.equals(kaydedilen.getVarsayilan())) {
            varsayilanYap(hedefSirketId, cariHesapId, kaydedilen.getId());
        }
        return toDTO(kaydedilen);
    }

    public CariAdresDTO guncelle(Long id, CariAdresDTO dto, Long sirketId) {
        CariAdres adres = cariAdresRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CariAdres", id));
        tenantChecker.check(adres.getSirketId(), "CariAdres");
        if (dto.getBaslik() != null) adres.setBaslik(dto.getBaslik());
        if (dto.getAdres() != null) adres.setAdres(dto.getAdres());
        if (dto.getIl() != null) adres.setIl(dto.getIl());
        if (dto.getIlce() != null) adres.setIlce(dto.getIlce());
        if (dto.getYetkiliKisi() != null) adres.setYetkiliKisi(dto.getYetkiliKisi());
        if (dto.getTelefon() != null) adres.setTelefon(dto.getTelefon());
        if (dto.getVarsayilan() != null) adres.setVarsayilan(dto.getVarsayilan());
        CariAdres kaydedilen = cariAdresRepository.save(adres);
        if (Boolean.TRUE.equals(kaydedilen.getVarsayilan())) {
            varsayilanYap(kaydedilen.getSirketId(), kaydedilen.getCariHesapId(), kaydedilen.getId());
        }
        return toDTO(kaydedilen);
    }

    public void sil(Long id, Long sirketId) {
        CariAdres adres = cariAdresRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CariAdres", id));
        tenantChecker.check(adres.getSirketId(), "CariAdres");
        cariAdresRepository.deleteById(id);
    }

    /** Verilen adres dışındaki tüm adreslerin varsayılan bayrağını kaldırır. */
    private void varsayilanYap(Long sirketId, Long cariHesapId, Long adresId) {
        List<CariAdres> liste = cariAdresRepository
                .findBySirketIdAndCariHesapIdOrderByVarsayilanDescIdAsc(sirketId, cariHesapId);
        List<CariAdres> degisen = liste.stream().filter(a -> {
            boolean hedefVarsayilan = Objects.equals(a.getId(), adresId);
            return !Objects.equals(a.getVarsayilan(), hedefVarsayilan);
        }).peek(a -> a.setVarsayilan(Objects.equals(a.getId(), adresId))).collect(Collectors.toList());
        if (!degisen.isEmpty()) cariAdresRepository.saveAll(degisen);
    }

    private CariAdresDTO toDTO(CariAdres a) {
        return CariAdresDTO.builder()
                .id(a.getId()).cariHesapId(a.getCariHesapId()).sirketId(a.getSirketId())
                .baslik(a.getBaslik()).adres(a.getAdres()).il(a.getIl()).ilce(a.getIlce())
                .yetkiliKisi(a.getYetkiliKisi()).telefon(a.getTelefon())
                .varsayilan(a.getVarsayilan()).olusturmaTarihi(a.getOlusturmaTarihi())
                .build();
    }
}
