package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.dto.ticaret.IadeKalemDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IadeService {

    private final IadeRepository iadeRepository;
    private final IadeKalemRepository iadeKalemRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;

    @org.springframework.beans.factory.annotation.Value("${app.kdv.varsayilan-oran:20}")
    private BigDecimal varsayilanKdvOrani;

    @Transactional(readOnly = true)
    public Page<IadeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return iadeRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public IadeDTO getir(Long id) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        return entityToDTO(iade);
    }

    public IadeDTO olustur(IadeDTO dto, Long sirketId) {
        BigDecimal toplamTutar = BigDecimal.ZERO;
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            for (IadeKalemDTO k : dto.getKalemler()) {
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal kalemTutar = netTutar.add(kdvTutari);
                toplamTutar = toplamTutar.add(kalemTutar);
            }
        } else if (dto.getTutar() != null) {
            toplamTutar = dto.getTutar();
        }

        Iade iade = Iade.builder()
                .faturaId(dto.getFaturaId())
                .tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .tarih(dto.getTarih())
                .tutar(toplamTutar)
                .aciklama(dto.getAciklama())
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .sirketId(sirketId)
                .build();
        iade = iadeRepository.save(iade);

        if (dto.getKalemler() != null) {
            for (IadeKalemDTO k : dto.getKalemler()) {
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                iadeKalemRepository.save(IadeKalem.builder()
                        .iadeId(iade.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(kdvOrani).tutar(netTutar.add(kdvTutari))
                        .build());
            }
        }

        if ("TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriIsle(iade);
        }

        return entityToDTO(iade);
    }

    public IadeDTO guncelle(Long id, IadeDTO dto) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        if (dto.getFaturaId() != null) iade.setFaturaId(dto.getFaturaId());
        if (dto.getTarih() != null) iade.setTarih(dto.getTarih());
        if (dto.getAciklama() != null) iade.setAciklama(dto.getAciklama());
        if (dto.getDurum() != null) iade.setDurum(dto.getDurum());

        BigDecimal toplamTutar = BigDecimal.ZERO;
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            iadeKalemRepository.deleteByIadeId(iade.getId());
            for (IadeKalemDTO k : dto.getKalemler()) {
                BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : varsayilanKdvOrani;
                BigDecimal netTutar = k.getBirimFiyat().multiply(k.getMiktar());
                BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal kalemTutar = netTutar.add(kdvTutari);
                toplamTutar = toplamTutar.add(kalemTutar);
                iadeKalemRepository.save(IadeKalem.builder()
                        .iadeId(iade.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                        .kdvOrani(kdvOrani).tutar(kalemTutar)
                        .build());
            }
            iade.setTutar(toplamTutar);
        } else if (dto.getTutar() != null) {
            iade.setTutar(dto.getTutar());
        }

        boolean yeniTamamlandi = "TAMAMLANDI".equals(iade.getDurum())
                && !"TAMAMLANDI".equals(dto.getDurum() != null ? null : iade.getDurum());
        if (yeniTamamlandi) {
            stokHareketleriIsle(iade);
        } else if ("IPTAL".equals(iade.getDurum()) && "TAMAMLANDI".equals(dto.getDurum() != null ? dto.getDurum() : null)) {
            stokHareketleriniTersineCevir(iade);
        }

        return entityToDTO(iadeRepository.save(iade));
    }

    public void sil(Long id) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        iadeKalemRepository.deleteByIadeId(id);
        iadeRepository.deleteById(id);
    }

    public IadeDTO durumGuncelle(Long id, String yeniDurum) {
        Iade iade = iadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Iade", id));
        tenantChecker.check(iade.getSirketId(), "Iade");
        if (yeniDurum == null || !List.of("TASLAK", "TAMAMLANDI", "IPTAL").contains(yeniDurum)) {
            throw new BusinessException("Geçersiz durum: " + yeniDurum);
        }
        if ("TAMAMLANDI".equals(yeniDurum) && !"TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriIsle(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        } else if ("IPTAL".equals(yeniDurum) && "TAMAMLANDI".equals(iade.getDurum())) {
            stokHareketleriniTersineCevir(iade);
            cacheYardimci.temizle("stoklar", "dashboard");
        }
        iade.setDurum(yeniDurum);
        return entityToDTO(iadeRepository.save(iade));
    }

    private void stokHareketleriIsle(Iade iade) {
        boolean alisIadesi = "ALIS".equals(iade.getTur());
        List<IadeKalem> kalemler = iadeKalemRepository.findByIadeId(iade.getId());
        for (IadeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            if (alisIadesi) {
                if (stok.getMiktar().compareTo(k.getMiktar()) < 0)
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd() + ", Mevcut: " + stok.getMiktar());
                stok.setMiktar(stok.getMiktar().subtract(k.getMiktar()));
            } else {
                stok.setMiktar(stok.getMiktar().add(k.getMiktar()));
            }
            stokRepository.save(stok);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok).tur(alisIadesi ? "CIKIS" : "GIRIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("İade #" + iade.getId())
                    .build());
        }
    }

    private void stokHareketleriniTersineCevir(Iade iade) {
        boolean alisIadesi = "ALIS".equals(iade.getTur());
        List<IadeKalem> kalemler = iadeKalemRepository.findByIadeId(iade.getId());
        for (IadeKalem k : kalemler) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            if (alisIadesi) {
                stok.setMiktar(stok.getMiktar().add(k.getMiktar()));
            } else {
                stok.setMiktar(stok.getMiktar().subtract(k.getMiktar()));
            }
            stokRepository.save(stok);
            stokHareketRepository.save(StokHareket.builder()
                    .stok(stok).tur(alisIadesi ? "GIRIS" : "CIKIS")
                    .miktar(k.getMiktar())
                    .hareketTarihi(LocalDate.now())
                    .aciklama("İade iptal #" + iade.getId())
                    .build());
        }
    }

    private IadeDTO entityToDTO(Iade i) {
        List<IadeKalem> kalemEntities = iadeKalemRepository.findByIadeId(i.getId());

        List<Long> stokIdler = kalemEntities.stream()
                .map(IadeKalem::getStokId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Stok> stokHaritasi = stokIdler.isEmpty()
                ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, s -> s));

        List<IadeKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> {
                    String stokAd = null;
                    String stokKodu = null;
                    if (k.getStokId() != null) {
                        Stok stok = stokHaritasi.get(k.getStokId());
                        if (stok != null) {
                            stokAd = stok.getAd();
                            stokKodu = stok.getStokKodu();
                        }
                    }
                    return IadeKalemDTO.builder()
                            .id(k.getId()).stokId(k.getStokId())
                            .stokAd(stokAd).stokKodu(stokKodu)
                            .aciklama(k.getAciklama()).miktar(k.getMiktar())
                            .birim(k.getBirim()).birimFiyat(k.getBirimFiyat())
                            .kdvOrani(k.getKdvOrani()).tutar(k.getTutar())
                            .build();
                }).collect(Collectors.toList());

        return IadeDTO.builder()
                .id(i.getId()).faturaId(i.getFaturaId()).tur(i.getTur()).tarih(i.getTarih())
                .tutar(i.getTutar()).aciklama(i.getAciklama()).durum(i.getDurum())
                .sirketId(i.getSirketId()).olusturmaTarihi(i.getOlusturmaTarihi())
                .kalemler(kalemler)
                .build();
    }
}
