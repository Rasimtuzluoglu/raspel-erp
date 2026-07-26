package com.raspel.erp.service;

import com.raspel.erp.dto.BankaDTO;
import com.raspel.erp.entity.Banka;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.BankaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BankaService {

    private final BankaRepository bankaRepository;

    @Transactional(readOnly = true)
    public List<BankaDTO> tumBankalariGetir(Long sirketId) {
        return bankaRepository.findBySirketId(sirketId).stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankaDTO bankaGetir(Long id) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        return entityDTOyeCevir(banka);
    }

    public BankaDTO bankaOlustur(BankaDTO dto, Long sirketId) {
        log.info("Yeni banka hesabı oluşturuluyor: {}, sirketId: {}", dto.getAd(), sirketId);
        Banka banka = Banka.builder()
                .ad(dto.getAd())
                .hesapNo(dto.getHesapNo())
                .iban(dto.getIban())
                .bakiye(dto.getBakiye() != null ? dto.getBakiye() : BigDecimal.ZERO)
                .sirketId(sirketId)
                .build();
        Banka kaydedilen = bankaRepository.save(banka);
        return entityDTOyeCevir(kaydedilen);
    }

    public BankaDTO bankaGuncelle(Long id, BankaDTO dto) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        if (dto.getAd() != null) banka.setAd(dto.getAd());
        if (dto.getHesapNo() != null) banka.setHesapNo(dto.getHesapNo());
        if (dto.getIban() != null) banka.setIban(dto.getIban());
        Banka guncellenen = bankaRepository.save(banka);
        return entityDTOyeCevir(guncellenen);
    }

    public void bankaSil(Long id) {
        if (!bankaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banka", id);
        }
        bankaRepository.deleteById(id);
    }

    private BankaDTO entityDTOyeCevir(Banka banka) {
        return BankaDTO.builder()
                .id(banka.getId())
                .ad(banka.getAd())
                .hesapNo(banka.getHesapNo())
                .iban(banka.getIban())
                .bakiye(banka.getBakiye())
                .olusturmaTarihi(banka.getOlusturmaTarihi())
                .build();
    }
}
