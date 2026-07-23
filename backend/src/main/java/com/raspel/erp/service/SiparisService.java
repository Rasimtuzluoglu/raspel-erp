package com.raspel.erp.service;

import com.raspel.erp.dto.SiparisDTO;
import com.raspel.erp.dto.SiparisKalemDTO;
import com.raspel.erp.entity.Siparis;
import com.raspel.erp.entity.SiparisKalem;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.SiparisKalemRepository;
import com.raspel.erp.repository.SiparisRepository;
import com.raspel.erp.repository.StokRepository;
import com.raspel.erp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SiparisService {

    private final SiparisRepository siparisRepository;
    private final SiparisKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public List<SiparisDTO> tumunuGetir(Long sirketId) {
        return siparisRepository.findBySirketIdOrderByTarihDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SiparisDTO getir(Long id) {
        return entityToDTO(siparisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id)));
    }

    public SiparisDTO olustur(SiparisDTO dto) {
        Siparis s = Siparis.builder()
                .siparisNo(dto.getSiparisNo()).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).tur("SATIS")
                .durum("TEKLIF").aciklama(dto.getAciklama())
                .araToplam(dto.getAraToplam()).kdv(dto.getKdv())
                .genelToplam(dto.getGenelToplam()).sirketId(dto.getSirketId())
                .build();
        s = siparisRepository.save(s);
        if (dto.getKalemler() != null) {
            for (SiparisKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SiparisKalem.builder()
                        .siparisId(s.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani()).tutar(k.getTutar()).build());
            }
        }
        return entityToDTO(s);
    }

    public SiparisDTO durumGuncelle(Long id, String durum) {
        Siparis s = siparisRepository.findById(id)
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

    private SiparisDTO entityToDTO(Siparis s) {
        List<SiparisKalemDTO> kalemler = kalemRepository.findBySiparisId(s.getId()).stream()
                .map(k -> SiparisKalemDTO.builder().id(k.getId()).siparisId(k.getSiparisId())
                        .stokId(k.getStokId()).stokAdi(k.getStokId() != null ? stokRepository.findById(k.getStokId()).map(st -> st.getAd()).orElse(null) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim())
                        .birimFiyat(k.getBirimFiyat()).kdvOrani(k.getKdvOrani()).tutar(k.getTutar()).build())
                .collect(Collectors.toList());
        return SiparisDTO.builder().id(s.getId()).siparisNo(s.getSiparisNo()).tarih(s.getTarih())
                .cariHesapId(s.getCariHesapId()).cariHesapAdi(s.getCariHesapId() != null ? cariHesapRepository.findById(s.getCariHesapId()).map(c -> c.getAd()).orElse(null) : null)
                .tur(s.getTur()).durum(s.getDurum()).aciklama(s.getAciklama())
                .araToplam(s.getAraToplam()).kdv(s.getKdv()).genelToplam(s.getGenelToplam())
                .sirketId(s.getSirketId()).olusturmaTarihi(s.getOlusturmaTarihi()).kalemler(kalemler).build();
    }
}
