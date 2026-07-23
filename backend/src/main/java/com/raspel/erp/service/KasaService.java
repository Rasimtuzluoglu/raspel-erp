package com.raspel.erp.service;

import com.raspel.erp.dto.KasaDTO;
import com.raspel.erp.dto.KasaHareketDTO;
import com.raspel.erp.entity.*;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.*;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KasaService {

    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final KategoriRepository kategoriRepository;

    @Transactional(readOnly = true)
    public List<KasaDTO> tumKasalarGetir(Long sirketId) {
        return kasaRepository.findBySirketId(sirketId).stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KasaDTO kasaGetir(Long id) {
        return entityToDTO(kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id)));
    }

    public KasaDTO kasaOlustur(KasaDTO dto, Long sirketId) {
        Kasa kasa = Kasa.builder().ad(dto.getAd()).bakiye(dto.getBakiye() != null ? dto.getBakiye() : BigDecimal.ZERO).sirketId(sirketId).build();
        return entityToDTO(kasaRepository.save(kasa));
    }

    public KasaDTO kasaGuncelle(Long id, KasaDTO dto) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        kasa.setAd(dto.getAd());
        return entityToDTO(kasaRepository.save(kasa));
    }

    public void kasaSil(Long id) {
        if (kasaHareketRepository.countByKasaId(id) > 0)
            throw new BusinessException("Bu kasaya ait hareketler var, önce hareketleri silin");
        kasaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<KasaHareketDTO> kasaHareketleriGetir(Long kasaId) {
        return kasaHareketRepository.findByKasaIdOrderByHareketTarihiDesc(kasaId)
                .stream().map(this::hareketToDTO).collect(Collectors.toList());
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public KasaHareketDTO hareketEkle(KasaHareketDTO dto) {
        Kasa kasa = kasaRepository.findById(dto.getKasaId())
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", dto.getKasaId()));

        BigDecimal tutar = dto.getTutar();
        if ("GIDER".equals(dto.getTur())) tutar = tutar.negate();
        kasa.setBakiye(kasa.getBakiye().add(tutar));

        GelirGiderKategori kategori = null;
        if (dto.getKategoriId() != null) {
            kategori = kategoriRepository.findById(dto.getKategoriId()).orElse(null);
        }

        KasaHareket hareket = KasaHareket.builder()
                .kasa(kasa).tur(dto.getTur()).tutar(dto.getTutar())
                .hareketTarihi(dto.getHareketTarihi()).aciklama(dto.getAciklama())
                .kategori(kategori).build();

        kasaRepository.save(kasa);
        return hareketToDTO(kasaHareketRepository.save(hareket));
    }

    public void hareketSil(Long hareketId) {
        KasaHareket hareket = kasaHareketRepository.findById(hareketId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", hareketId));
        Kasa kasa = hareket.getKasa();
        BigDecimal tutar = hareket.getTutar();
        if ("GIDER".equals(hareket.getTur())) tutar = tutar.negate();
        kasa.setBakiye(kasa.getBakiye().subtract(tutar));
        kasaRepository.save(kasa);
        kasaHareketRepository.deleteById(hareketId);
    }

    private KasaDTO entityToDTO(Kasa k) {
        return KasaDTO.builder().id(k.getId()).ad(k.getAd()).bakiye(k.getBakiye())
                .olusturmaTarihi(k.getOlusturmaTarihi()).build();
    }

    private KasaHareketDTO hareketToDTO(KasaHareket h) {
        return KasaHareketDTO.builder()
                .id(h.getId()).kasaId(h.getKasa().getId()).kasaAd(h.getKasa().getAd())
                .tur(h.getTur()).tutar(h.getTutar()).hareketTarihi(h.getHareketTarihi())
                .aciklama(h.getAciklama())
                .kategoriId(h.getKategori() != null ? h.getKategori().getId() : null)
                .kategoriAd(h.getKategori() != null ? h.getKategori().getAd() : null)
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}
