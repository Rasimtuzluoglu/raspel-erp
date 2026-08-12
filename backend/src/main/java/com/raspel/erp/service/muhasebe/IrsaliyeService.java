package com.raspel.erp.service.muhasebe;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.dto.muhasebe.IrsaliyeKalemDTO;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.muhasebe.IrsaliyeKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IrsaliyeService {

    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeKalemRepository kalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;

    @Transactional(readOnly = true)
    public Page<IrsaliyeDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return irsaliyeRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public IrsaliyeDTO getir(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        return entityToDTO(i);
    }

    public IrsaliyeDTO olustur(IrsaliyeDTO dto, Long sirketId) {
        Irsaliye i = Irsaliye.builder()
                .irsaliyeNo(dto.getIrsaliyeNo()).tarih(dto.getTarih())
                .cariHesapId(dto.getCariHesapId()).faturaId(dto.getFaturaId())
                .durum("TASLAK").tur(dto.getTur() != null ? dto.getTur() : "SATIS")
                .aciklama(dto.getAciklama()).sirketId(sirketId).build();
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    public IrsaliyeDTO guncelle(Long id, IrsaliyeDTO dto) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        i.setIrsaliyeNo(dto.getIrsaliyeNo());
        i.setTarih(dto.getTarih());
        i.setCariHesapId(dto.getCariHesapId());
        i.setFaturaId(dto.getFaturaId());
        if (dto.getDurum() != null) i.setDurum(dto.getDurum());
        if (dto.getTur() != null) i.setTur(dto.getTur());
        i.setAciklama(dto.getAciklama());
        i = irsaliyeRepository.save(i);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteByIrsaliyeId(i.getId());
            for (IrsaliyeKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(IrsaliyeKalem.builder()
                        .irsaliyeId(i.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).build());
            }
        }
        return entityToDTO(i);
    }

    public IrsaliyeDTO durumGuncelle(Long id, String durum) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");

        if ("KESILDI".equals(durum) && !"KESILDI".equals(i.getDurum())) {
            List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
            for (IrsaliyeKalem k : kalemler) {
                if (k.getStokId() == null) continue;
                Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
                BigDecimal adet = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
                if ("SATIS".equals(i.getTur()) && stok.getMiktar().compareTo(adet) < 0) {
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                            + ", Mevcut: " + stok.getMiktar() + ", İstenen: " + adet);
                }
                if ("SATIS".equals(i.getTur())) {
                    stok.setMiktar(stok.getMiktar().subtract(adet));
                } else {
                    stok.setMiktar(stok.getMiktar().add(adet));
                }
                stokRepository.save(stok);
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur("SATIS".equals(i.getTur()) ? "CIKIS" : "GIRIS")
                        .miktar(adet)
                        .hareketTarihi(LocalDate.now())
                        .aciklama("İrsaliye #" + i.getIrsaliyeNo())
                        .build());
            }
        } else if ("IPTAL".equals(durum) && "KESILDI".equals(i.getDurum())) {
            List<IrsaliyeKalem> kalemler = kalemRepository.findByIrsaliyeId(i.getId());
            for (IrsaliyeKalem k : kalemler) {
                if (k.getStokId() == null) continue;
                Stok stok = stokRepository.findByIdForUpdate(k.getStokId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
                BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
                if ("SATIS".equals(i.getTur())) {
                    stok.setMiktar(stok.getMiktar().add(miktar));
                } else {
                    stok.setMiktar(stok.getMiktar().subtract(miktar));
                }
                stokRepository.save(stok);
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur("SATIS".equals(i.getTur()) ? "GIRIS" : "CIKIS")
                        .miktar(miktar)
                        .hareketTarihi(LocalDate.now())
                        .aciklama("İrsaliye iptal #" + i.getIrsaliyeNo())
                        .build());
            }
        }

        if ("KESILDI".equals(durum) || "IPTAL".equals(durum)) {
            cacheYardimci.temizle("stoklar", "dashboard");
        }

        i.setDurum(durum);
        return entityToDTO(irsaliyeRepository.save(i));
    }

    public void sil(Long id) {
        Irsaliye i = irsaliyeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İrsaliye", id));
        tenantChecker.check(i.getSirketId(), "İrsaliye");
        kalemRepository.deleteByIrsaliyeId(id);
        irsaliyeRepository.deleteById(id);
    }

    private IrsaliyeDTO entityToDTO(Irsaliye i) {
        List<IrsaliyeKalem> kalemEntities = kalemRepository.findByIrsaliyeId(i.getId());

        List<Long> stokIdler = kalemEntities.stream()
                .map(IrsaliyeKalem::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> stokAdlari = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(Stok::getId, Stok::getAd));

        String cariAdi = i.getCariHesapId() != null
                ? cariHesapRepository.findById(i.getCariHesapId()).map(c -> c.getAd()).orElse(null)
                : null;

        List<IrsaliyeKalemDTO> kalemler = kalemEntities.stream()
                .map(k -> IrsaliyeKalemDTO.builder().id(k.getId())
                        .stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokAdlari.get(k.getStokId()) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar()).birim(k.getBirim()).build())
                .collect(Collectors.toList());
        return IrsaliyeDTO.builder().id(i.getId()).irsaliyeNo(i.getIrsaliyeNo()).tarih(i.getTarih())
                .cariHesapId(i.getCariHesapId())
                .cariHesapAdi(cariAdi)
                .faturaId(i.getFaturaId()).durum(i.getDurum()).tur(i.getTur())
                .aciklama(i.getAciklama()).sirketId(i.getSirketId())
                .olusturmaTarihi(i.getOlusturmaTarihi()).kalemler(kalemler).build();
    }
}