package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisDTO;
import com.raspel.erp.dto.ticaret.SatinalmaSiparisKalemDTO;
import com.raspel.erp.entity.ticaret.SatinalmaSiparis;
import com.raspel.erp.entity.ticaret.SatinalmaSiparisKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaSiparisRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.repository.ticaret.SiparisRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class SatinalmaSiparisService {

    private final SatinalmaSiparisRepository siparisRepository;
    private final SatinalmaSiparisKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<SatinalmaSiparisDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return siparisRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public SatinalmaSiparisDTO getir(Long id) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO olustur(SatinalmaSiparisDTO dto) {
        SatinalmaSiparis s = SatinalmaSiparis.builder()
                .siparisNo(dto.getSiparisNo())
                .tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId())
                .talepId(dto.getTalepId())
                .durum("TASLAK")
                .aciklama(dto.getAciklama())
                .araToplam(dto.getAraToplam())
                .kdv(dto.getKdv())
                .genelToplam(dto.getGenelToplam())
                .sirketId(dto.getSirketId())
                .build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Satınalma Siparişi");
        s = siparisRepository.save(s);

        if (dto.getKalemler() != null) {
            for (SatinalmaSiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaSiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                        .build());
            }
        }
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO guncelle(Long id, SatinalmaSiparisDTO dto) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        s.setSiparisNo(dto.getSiparisNo());
        s.setTarih(dto.getTarih());
        s.setCariHesapId(dto.getCariHesapId());
        s.setTalepId(dto.getTalepId());
        if (dto.getDurum() != null) s.setDurum(dto.getDurum());
        s.setAciklama(dto.getAciklama());
        s.setAraToplam(dto.getAraToplam());
        s.setKdv(dto.getKdv());
        s.setGenelToplam(dto.getGenelToplam());
        s = siparisRepository.save(s);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteBySiparisId(s.getId());
            for (SatinalmaSiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaSiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                        .build());
            }
        }
        return entityToDTO(s);
    }

    public SatinalmaSiparisDTO durumGuncelle(Long id, String durum) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        s.setDurum(durum);
        return entityToDTO(siparisRepository.save(s));
    }

    public void sil(Long id) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        tenantChecker.check(s.getSirketId(), "Sipariş");
        kalemRepository.deleteBySiparisId(id);
        siparisRepository.deleteById(id);
    }

    private SatinalmaSiparisDTO entityToDTO(SatinalmaSiparis s) {
        List<SatinalmaSiparisKalemDTO> kalemler = kalemRepository.findBySiparisId(s.getId()).stream()
                .map(k -> SatinalmaSiparisKalemDTO.builder()
                        .id(k.getId()).siparisId(k.getSiparisId()).stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokRepository.findById(k.getStokId()).map(st -> st.getAd()).orElse(null) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                        .olusturmaTarihi(k.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());

        return SatinalmaSiparisDTO.builder()
                .id(s.getId()).siparisNo(s.getSiparisNo()).tarih(s.getTarih())
                .cariHesapId(s.getCariHesapId())
                .cariHesapAdi(s.getCariHesapId() != null ? cariHesapRepository.findById(s.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null)
                .talepId(s.getTalepId()).durum(s.getDurum())
                .araToplam(s.getAraToplam()).kdv(s.getKdv()).genelToplam(s.getGenelToplam())
                .aciklama(s.getAciklama()).sirketId(s.getSirketId())
                .olusturmaTarihi(s.getOlusturmaTarihi()).kalemler(kalemler).build();
    }
}