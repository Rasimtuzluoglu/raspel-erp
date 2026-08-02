package com.raspel.erp.service;

import com.raspel.erp.dto.KategoriDTO;
import com.raspel.erp.entity.GelirGiderKategori;
import com.raspel.erp.repository.KategoriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class KategoriService {

    private final KategoriRepository kategoriRepository;

    @Cacheable(value = "lookup", key = "'kategoriler:' + #sirketId + ':' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<KategoriDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return kategoriRepository.findBySirketId(sirketId, pageable).map(this::entityToDTO);
    }

    @Cacheable(value = "lookup", key = "'kategoriTur:' + #tur")
    @Transactional(readOnly = true)
    public List<KategoriDTO> turuGetir(String tur) {
        return kategoriRepository.findByTurOrderByAd(tur).stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO olustur(KategoriDTO dto, Long sirketId) {
        GelirGiderKategori k = GelirGiderKategori.builder().ad(dto.getAd()).tur(dto.getTur()).sirketId(sirketId).build();
        return entityToDTO(kategoriRepository.save(k));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO guncelle(Long id, KategoriDTO dto) {
        GelirGiderKategori k = kategoriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        k.setAd(dto.getAd());
        k.setTur(dto.getTur());
        return entityToDTO(kategoriRepository.save(k));
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        kategoriRepository.deleteById(id);
    }

    private KategoriDTO entityToDTO(GelirGiderKategori k) {
        return KategoriDTO.builder().id(k.getId()).ad(k.getAd()).tur(k.getTur()).olusturmaTarihi(k.getOlusturmaTarihi()).build();
    }
}
