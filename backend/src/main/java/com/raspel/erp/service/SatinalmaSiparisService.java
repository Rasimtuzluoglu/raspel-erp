package com.raspel.erp.service;

import com.raspel.erp.dto.SatinalmaSiparisDTO;
import com.raspel.erp.dto.SatinalmaSiparisKalemDTO;
import com.raspel.erp.entity.SatinalmaSiparis;
import com.raspel.erp.entity.SatinalmaSiparisKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.SatinalmaSiparisKalemRepository;
import com.raspel.erp.repository.SatinalmaSiparisRepository;
import com.raspel.erp.repository.StokRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SatinalmaSiparisService {

    private final SatinalmaSiparisRepository siparisRepository;
    private final SatinalmaSiparisKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public List<SatinalmaSiparisDTO> tumunuGetir(Long sirketId) {
        return siparisRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SatinalmaSiparisDTO getir(Long id) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
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

    public SatinalmaSiparisDTO durumGuncelle(Long id, String durum) {
        SatinalmaSiparis s = siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        s.setDurum(durum);
        return entityToDTO(siparisRepository.save(s));
    }

    public void sil(Long id) {
        if (!siparisRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sipariş", id);
        }
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
