package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.KasaDTO;
import com.raspel.erp.dto.finans.KasaHareketDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.entity.sistem.GelirGiderKategori;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.sistem.KategoriRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KasaService {

    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final KategoriRepository kategoriRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<KasaDTO> tumKasalarGetir(Long sirketId, Pageable pageable) {
        return kasaRepository.findBySirketId(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public KasaDTO kasaGetir(Long id) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        return entityToDTO(kasa);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KasaDTO kasaOlustur(KasaDTO dto, Long sirketId) {
        Kasa kasa = Kasa.builder().ad(dto.getAd()).bakiye(dto.getBakiye() != null ? dto.getBakiye() : BigDecimal.ZERO).sirketId(sirketId).build();
        return entityToDTO(kasaRepository.save(kasa));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KasaDTO kasaGuncelle(Long id, KasaDTO dto) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setAd(dto.getAd());
        return entityToDTO(kasaRepository.save(kasa));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void kasaSil(Long id) {
        Kasa kasa = kasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", id));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        if (kasa.getBakiye() != null && kasa.getBakiye().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Bakiyesi sıfır olmayan kasa silinemez. Mevcut bakiye: " + kasa.getBakiye() + " ₺");
        }
        if (kasaHareketRepository.countByKasaId(id) > 0)
            throw new BusinessException("Bu kasaya ait hareketler var, önce hareketleri silin");
        kasaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<KasaHareketDTO> kasaHareketleriGetir(Long kasaId) {
        return kasaHareketRepository.findByKasaIdOrderByHareketTarihiDesc(kasaId)
                .stream().map(this::hareketToDTO).collect(Collectors.toList());
    }

    public KasaHareketDTO hareketEkle(KasaHareketDTO dto) {
        Kasa kasa = kasaRepository.findByIdForUpdate(dto.getKasaId())
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", dto.getKasaId()));
        tenantChecker.check(kasa.getSirketId(), "Kasa");

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
        tenantChecker.check(kasa.getSirketId(), "Kasa");
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