package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.CariFirsatDTO;
import com.raspel.erp.entity.ticaret.CariFirsat;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.ticaret.CariFirsatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CrmService {

    private final CariFirsatRepository cariFirsatRepository;
    private final CariHesapRepository cariHesapRepository;

    @Transactional(readOnly = true)
    public List<CariFirsatDTO> firsatlariGetir(Long sirketId, String durum) {
        List<CariFirsat> list = durum != null && !durum.isBlank()
                ? cariFirsatRepository.findBySirketIdAndDurum(sirketId, durum)
                : cariFirsatRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId);
        return list.stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CariFirsatDTO firsatGetir(Long id) {
        return entityToDTO(cariFirsatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fırsat", id)));
    }

    public CariFirsatDTO firsatOlustur(CariFirsatDTO dto, Long sirketId) {
        CariFirsat firsat = CariFirsat.builder()
                .ad(dto.getAd())
                .cariHesapId(dto.getCariHesapId())
                .durum(dto.getDurum() != null ? dto.getDurum() : "YENI")
                .kaynak(dto.getKaynak())
                .deger(dto.getDeger())
                .tahminiKapanis(dto.getTahminiKapanis())
                .aciklama(dto.getAciklama())
                .kullaniciId(dto.getKullaniciId())
                .sirketId(sirketId)
                .build();
        CariFirsat saved = cariFirsatRepository.save(firsat);
        log.info("CRM fırsatı oluşturuldu: {}", saved.getAd());
        return entityToDTO(saved);
    }

    public CariFirsatDTO firsatGuncelle(Long id, CariFirsatDTO dto) {
        CariFirsat firsat = cariFirsatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fırsat", id));
        if (dto.getAd() != null) firsat.setAd(dto.getAd());
        if (dto.getCariHesapId() != null) firsat.setCariHesapId(dto.getCariHesapId());
        if (dto.getDurum() != null) firsat.setDurum(dto.getDurum());
        if (dto.getKaynak() != null) firsat.setKaynak(dto.getKaynak());
        if (dto.getDeger() != null) firsat.setDeger(dto.getDeger());
        if (dto.getTahminiKapanis() != null) firsat.setTahminiKapanis(dto.getTahminiKapanis());
        if (dto.getAciklama() != null) firsat.setAciklama(dto.getAciklama());
        return entityToDTO(cariFirsatRepository.save(firsat));
    }

    public void firsatSil(Long id) {
        if (!cariFirsatRepository.existsById(id)) throw new ResourceNotFoundException("Fırsat", id);
        cariFirsatRepository.deleteById(id);
    }

    private CariFirsatDTO entityToDTO(CariFirsat f) {
        String cariAd = f.getCariHesapId() != null
                ? cariHesapRepository.findById(f.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                : null;
        return CariFirsatDTO.builder()
                .id(f.getId()).ad(f.getAd()).cariHesapId(f.getCariHesapId()).cariHesapAd(cariAd)
                .durum(f.getDurum()).kaynak(f.getKaynak()).deger(f.getDeger())
                .tahminiKapanis(f.getTahminiKapanis()).aciklama(f.getAciklama())
                .kullaniciId(f.getKullaniciId()).sirketId(f.getSirketId())
                .olusturmaTarihi(f.getOlusturmaTarihi())
                .build();
    }
}
