package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.BankaDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.BankaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BankaService {

    private final BankaRepository bankaRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<BankaDTO> tumBankalariGetir(Long sirketId, Pageable pageable) {
        return bankaRepository.findBySirketId(sirketId, pageable).map(this::entityDTOyeCevir);
    }

    @Transactional(readOnly = true)
    public BankaDTO bankaGetir(Long id) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        return entityDTOyeCevir(banka);
    }

    @CacheEvict(value = "lookup", allEntries = true)
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

    @CacheEvict(value = "lookup", allEntries = true)
    public BankaDTO bankaGuncelle(Long id, BankaDTO dto) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        if (dto.getAd() != null) banka.setAd(dto.getAd());
        if (dto.getHesapNo() != null) banka.setHesapNo(dto.getHesapNo());
        if (dto.getIban() != null) banka.setIban(dto.getIban());
        Banka guncellenen = bankaRepository.save(banka);
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void bankaSil(Long id) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        if (banka.getBakiye() != null && banka.getBakiye().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Bakiyesi sıfır olmayan banka hesabı silinemez. Mevcut bakiye: " + banka.getBakiye() + " ₺");
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
