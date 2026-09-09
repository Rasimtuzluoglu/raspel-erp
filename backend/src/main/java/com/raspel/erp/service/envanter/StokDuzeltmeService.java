package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.StokDuzeltmeDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokDuzeltme;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokDuzeltmeRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
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
public class StokDuzeltmeService {

    private final StokDuzeltmeRepository duzeltmeRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;

    @Transactional
    public StokDuzeltmeDTO duzelt(StokDuzeltmeDTO dto, Long sirketId, Long kullaniciId) {
        if (dto.getStokId() == null) {
            throw new BusinessException("Stok seçilmelidir");
        }
        if (dto.getYeniMiktar() == null || dto.getYeniMiktar().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Yeni miktar negatif olamaz");
        }
        Stok stok = stokRepository.findByIdForUpdate(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        BigDecimal eski = stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO;
        stok.setMiktar(dto.getYeniMiktar());
        stokRepository.save(stok);

        BigDecimal fark = dto.getYeniMiktar().subtract(eski);
        if (fark.compareTo(BigDecimal.ZERO) != 0) {
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok)
                    .tur(fark.compareTo(BigDecimal.ZERO) > 0 ? "GIRIS" : "CIKIS")
                    .miktar(fark.abs())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("Stok düzeltme: " + (dto.getNeden() != null ? dto.getNeden() : "-"))
                    .build());
        }

        StokDuzeltme d = duzeltmeRepository.save(StokDuzeltme.builder()
                .sirketId(sirketId)
                .stokId(stok.getId())
                .stokAd(stok.getAd())
                .eskiMiktar(eski)
                .yeniMiktar(dto.getYeniMiktar())
                .neden(dto.getNeden())
                .kullaniciId(kullaniciId)
                .build());
        log.info("Stok düzeltildi - Stok: {}, {} -> {}", stok.getAd(), eski, dto.getYeniMiktar());
        return toDTO(d);
    }

    @Transactional(readOnly = true)
    public List<StokDuzeltmeDTO> gecmis(Long sirketId) {
        return duzeltmeRepository.findTop100BySirketIdOrderByOlusturmaTarihiDesc(sirketId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    private StokDuzeltmeDTO toDTO(StokDuzeltme d) {
        return StokDuzeltmeDTO.builder()
                .id(d.getId()).sirketId(d.getSirketId()).stokId(d.getStokId())
                .stokAd(d.getStokAd()).eskiMiktar(d.getEskiMiktar()).yeniMiktar(d.getYeniMiktar())
                .neden(d.getNeden()).kullaniciId(d.getKullaniciId()).olusturmaTarihi(d.getOlusturmaTarihi())
                .build();
    }
}
