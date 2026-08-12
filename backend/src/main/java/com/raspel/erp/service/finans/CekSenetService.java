package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.entity.finans.CekSenet;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.entity.sube.Sube;

@Service
@Transactional
@RequiredArgsConstructor
public class CekSenetService {

    private final CekSenetRepository cekSenetRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<CekSenetDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Map<Long, String> cariHaritasi = cariHesapRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getAd(), (a, b) -> a));
        return cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(sirketId, pageable)
                .map(cs -> entityToDTO(cs, cariHaritasi));
    }

    @Transactional(readOnly = true)
    public CekSenetDTO getir(Long id) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        return entityToDTO(cs);
    }

    public CekSenetDTO olustur(CekSenetDTO dto) {
        CekSenet cs = CekSenet.builder()
                .tur(dto.getTur()).cariHesapId(dto.getCariHesapId())
                .bankaAdi(dto.getBankaAdi()).sube(dto.getSube())
                .cekNo(dto.getCekNo()).hesapNo(dto.getHesapNo())
                .vadeTarihi(dto.getVadeTarihi()).tutar(dto.getTutar())
                .durum("PORTFOY").aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId()).build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Çek/Senet");
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public CekSenetDTO guncelle(Long id, CekSenetDTO dto) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        cs.setTur(dto.getTur());
        cs.setCariHesapId(dto.getCariHesapId());
        cs.setBankaAdi(dto.getBankaAdi());
        cs.setSube(dto.getSube());
        cs.setCekNo(dto.getCekNo());
        cs.setHesapNo(dto.getHesapNo());
        cs.setVadeTarihi(dto.getVadeTarihi());
        cs.setKesinmeTarihi(dto.getKesinmeTarihi());
        cs.setTutar(dto.getTutar());
        if (dto.getDurum() != null) cs.setDurum(dto.getDurum());
        cs.setAciklama(dto.getAciklama());
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public CekSenetDTO durumGuncelle(Long id, String durum) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        cs.setDurum(durum);
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public void sil(Long id) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        cekSenetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs) {
        return entityToDTO(cs, null);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs, Map<Long, String> cariHaritasi) {
        String cariAdi = null;
        if (cs.getCariHesapId() != null) {
            if (cariHaritasi != null) {
                cariAdi = cariHaritasi.get(cs.getCariHesapId());
            }
            if (cariAdi == null) {
                cariAdi = cariHesapRepository.findById(cs.getCariHesapId())
                        .map(c -> c.getAd()).orElse(null);
            }
        }
        return CekSenetDTO.builder()
                .id(cs.getId()).tur(cs.getTur())
                .cariHesapId(cs.getCariHesapId())
                .cariHesapAdi(cariAdi)
                .bankaAdi(cs.getBankaAdi()).sube(cs.getSube())
                .cekNo(cs.getCekNo()).hesapNo(cs.getHesapNo())
                .vadeTarihi(cs.getVadeTarihi()).kesinmeTarihi(cs.getKesinmeTarihi())
                .tutar(cs.getTutar()).durum(cs.getDurum())
                .aciklama(cs.getAciklama()).sirketId(cs.getSirketId())
                .olusturmaTarihi(cs.getOlusturmaTarihi()).build();
    }
}