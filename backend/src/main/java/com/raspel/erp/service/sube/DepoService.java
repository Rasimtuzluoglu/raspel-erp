package com.raspel.erp.service.sube;

import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import com.raspel.erp.repository.sube.SubeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.entity.envanter.Stok;

@Service
@Transactional
@RequiredArgsConstructor
public class DepoService {

    private final DepoRepository depoRepository;
    private final DepoStokRepository depoStokRepository;
    private final SubeRepository subeRepository;
    private final StokRepository stokRepository;

    @Transactional(readOnly = true)
    public Page<DepoDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Map<Long, String> subeHaritasi = subeRepository.findBySirketIdOrderByAdAsc(sirketId, Pageable.unpaged()).stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getAd()));
        return depoRepository.findBySirketIdOrderByAdAsc(sirketId, pageable)
                .map(d -> entityToDTO(d, subeHaritasi.get(d.getSubeId())));
    }

    @Transactional(readOnly = true)
    public DepoDTO getir(Long id) {
        Depo d = depoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", id));
        String subeAd = subeRepository.findById(d.getSubeId())
                .map(s -> s.getAd()).orElse(null);
        return entityToDTO(d, subeAd);
    }

    public DepoDTO olustur(DepoDTO dto) {
        Depo d = Depo.builder()
                .ad(dto.getAd()).adres(dto.getAdres())
                .yetkili(dto.getYetkili()).subeId(dto.getSubeId())
                .sirketId(dto.getSirketId()).build();
        return entityToDTO(depoRepository.save(d), null);
    }

    public DepoDTO guncelle(Long id, DepoDTO dto) {
        Depo d = depoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depo", id));
        if (dto.getAd() != null) d.setAd(dto.getAd());
        if (dto.getAdres() != null) d.setAdres(dto.getAdres());
        if (dto.getYetkili() != null) d.setYetkili(dto.getYetkili());
        if (dto.getSubeId() != null) d.setSubeId(dto.getSubeId());
        if (dto.getAktif() != null) d.setAktif(dto.getAktif());
        return entityToDTO(depoRepository.save(d), null);
    }

    public void sil(Long id) {
        if (!depoRepository.existsById(id))
            throw new ResourceNotFoundException("Depo", id);
        depoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DepoStokDTO> depoStoklari(Long depoId) {
        List<com.raspel.erp.entity.envanter.Stok> tumStoklar = stokRepository.findAll();
        Map<Long, String> stokHaritasi = tumStoklar.stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getAd()));
        Map<Long, String> stokKodHaritasi = tumStoklar.stream()
                .filter(s -> s.getStokKodu() != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getStokKodu()));
        Map<Long, String> birimHaritasi = tumStoklar.stream()
                .filter(s -> s.getBirim() != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getBirim()));

        return depoStokRepository.findByDepoId(depoId).stream()
                .map(ds -> DepoStokDTO.builder()
                        .id(ds.getId()).depoId(ds.getDepoId())
                        .stokId(ds.getStokId())
                        .stokAd(stokHaritasi.get(ds.getStokId()))
                        .stokKodu(stokKodHaritasi.get(ds.getStokId()))
                        .birim(birimHaritasi.get(ds.getStokId()))
                        .miktar(ds.getMiktar())
                        .olusturmaTarihi(ds.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());
    }

    public DepoStokDTO stokEkle(Long depoId, Long stokId, BigDecimal miktar) {
        DepoStok ds = depoStokRepository.findByDepoIdAndStokId(depoId, stokId)
                .orElse(DepoStok.builder().depoId(depoId).stokId(stokId).miktar(BigDecimal.ZERO).build());
        ds.setMiktar(ds.getMiktar().add(miktar));
        depoStokRepository.save(ds);
        return depoStoklari(depoId).stream()
                .filter(s -> s.getStokId().equals(stokId))
                .findFirst().orElse(null);
    }

    public DepoStokDTO stokCikar(Long depoId, Long stokId, BigDecimal miktar) {
        DepoStok ds = depoStokRepository.findByDepoIdAndStokId(depoId, stokId)
                .orElseThrow(() -> new BusinessException("Bu depoda stok bulunamadı"));
        if (ds.getMiktar().compareTo(miktar) < 0)
            throw new BusinessException("Yetersiz stok! Mevcut: " + ds.getMiktar() + ", Çıkış: " + miktar);
        ds.setMiktar(ds.getMiktar().subtract(miktar));
        depoStokRepository.save(ds);
        return depoStoklari(depoId).stream()
                .filter(s -> s.getStokId().equals(stokId))
                .findFirst().orElse(null);
    }

    public void stokTransfer(Long kaynakDepoId, Long hedefDepoId, Long stokId, BigDecimal miktar) {
        stokCikar(kaynakDepoId, stokId, miktar);
        stokEkle(hedefDepoId, stokId, miktar);
    }

    private DepoDTO entityToDTO(Depo d, String subeAd) {
        return DepoDTO.builder()
                .id(d.getId()).ad(d.getAd()).adres(d.getAdres())
                .yetkili(d.getYetkili()).subeId(d.getSubeId())
                .subeAdi(subeAd).sirketId(d.getSirketId())
                .aktif(d.getAktif()).olusturmaTarihi(d.getOlusturmaTarihi()).build();
    }
}