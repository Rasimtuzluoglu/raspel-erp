package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.PosOzetDTO;
import com.raspel.erp.dto.finans.PosTerminaliDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.PosTerminali;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.PosTerminaliRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosTerminaliService {

    private final PosTerminaliRepository posRepository;
    private final BankaRepository bankaRepository;
    private final HareketRepository hareketRepository;

    @Transactional(readOnly = true)
    public List<PosTerminaliDTO> liste(Long sirketId) {
        return posRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PosTerminaliDTO> aktif(Long sirketId) {
        return posRepository.findBySirketIdAndAktifTrueOrderByAd(sirketId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PosTerminaliDTO olustur(PosTerminaliDTO dto, Long sirketId) {
        if (dto.getAd() == null || dto.getAd().isBlank()) {
            throw new BusinessException("POS terminali adı boş olamaz");
        }
        PosTerminali p = PosTerminali.builder()
                .sirketId(sirketId)
                .ad(dto.getAd().trim())
                .bankaId(dto.getBankaId())
                .komisyonOrani(dto.getKomisyonOrani() != null ? dto.getKomisyonOrani() : BigDecimal.ZERO)
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        return toDTO(posRepository.save(p));
    }

    @Transactional
    public PosTerminaliDTO guncelle(Long id, PosTerminaliDTO dto, Long sirketId) {
        PosTerminali p = posRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POS Terminali", id));
        if (dto.getAd() != null && !dto.getAd().isBlank()) p.setAd(dto.getAd().trim());
        if (dto.getBankaId() != null) p.setBankaId(dto.getBankaId());
        if (dto.getKomisyonOrani() != null) p.setKomisyonOrani(dto.getKomisyonOrani());
        if (dto.getAktif() != null) p.setAktif(dto.getAktif());
        return toDTO(posRepository.save(p));
    }

    @Transactional
    public void sil(Long id, Long sirketId) {
        PosTerminali p = posRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POS Terminali", id));
        posRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public List<PosOzetDTO> ozet(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        return posRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(p -> {
                    List<Hareket> hareketler = hareketRepository
                            .findBySirketIdAndPosTerminaliIdOrderByHareketTarihiDesc(sirketId, p.getId());
                    BigDecimal bugunTutar = hareketler.stream()
                            .filter(h -> bugun.equals(h.getHareketTarihi()))
                            .map(h -> h.getTutar() != null ? h.getTutar() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal bugunKomisyon = hareketler.stream()
                            .filter(h -> bugun.equals(h.getHareketTarihi()))
                            .map(h -> h.getKomisyonTutar() != null ? h.getKomisyonTutar() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal toplamTutar = hareketler.stream()
                            .map(h -> h.getTutar() != null ? h.getTutar() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal toplamKomisyon = hareketler.stream()
                            .map(h -> h.getKomisyonTutar() != null ? h.getKomisyonTutar() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return PosOzetDTO.builder()
                            .posId(p.getId())
                            .posAd(p.getAd())
                            .bankaAd(bankaAd(p.getBankaId()))
                            .bugunTutar(bugunTutar)
                            .bugunKomisyon(bugunKomisyon)
                            .toplamTutar(toplamTutar)
                            .toplamKomisyon(toplamKomisyon)
                            .build();
                }).collect(Collectors.toList());
    }

    private String bankaAd(Long bankaId) {
        if (bankaId == null) return null;
        return bankaRepository.findById(bankaId).map(Banka::getAd).orElse(null);
    }

    private PosTerminaliDTO toDTO(PosTerminali p) {
        return PosTerminaliDTO.builder()
                .id(p.getId())
                .sirketId(p.getSirketId())
                .ad(p.getAd())
                .bankaId(p.getBankaId())
                .bankaAd(bankaAd(p.getBankaId()))
                .komisyonOrani(p.getKomisyonOrani())
                .aktif(p.getAktif())
                .build();
    }
}
