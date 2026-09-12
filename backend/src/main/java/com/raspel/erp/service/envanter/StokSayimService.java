package com.raspel.erp.service.envanter;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.envanter.StokSayim;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokSayimRepository;
import com.raspel.erp.service.sistem.BildirimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.raspel.erp.exception.BusinessException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StokSayimService {

    private final StokSayimRepository stokSayimRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;
    private final BildirimService bildirimService;

    @Transactional(readOnly = true)
    public Page<StokSayimDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return stokSayimRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public StokSayimDTO getir(Long id) {
        StokSayim s = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(s.getSirketId(), "StokSayim");
        return entityToDTO(s);
    }

    public StokSayimDTO olustur(StokSayimDTO dto, Long sirketId) {
        Stok stok = stokRepository.findById(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        StokSayim sayim = StokSayim.builder()
                .tarih(dto.getTarih())
                .stok(stok)
                .beklenenMiktar(dto.getBeklenenMiktar() != null ? dto.getBeklenenMiktar() : BigDecimal.ZERO)
                .sayilanMiktar(dto.getSayilanMiktar() != null ? dto.getSayilanMiktar() : BigDecimal.ZERO)
                .fark(dto.getFark())
                .durum(dto.getDurum() != null ? dto.getDurum() : "TASLAK")
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    public StokSayimDTO guncelle(Long id, StokSayimDTO dto) {
        StokSayim sayim = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(sayim.getSirketId(), "StokSayim");
        if (dto.getTarih() != null) sayim.setTarih(dto.getTarih());
        if (dto.getBeklenenMiktar() != null) sayim.setBeklenenMiktar(dto.getBeklenenMiktar());
        if (dto.getSayilanMiktar() != null) sayim.setSayilanMiktar(dto.getSayilanMiktar());
        if (dto.getFark() != null) sayim.setFark(dto.getFark());
        if (dto.getDurum() != null) sayim.setDurum(dto.getDurum());
        if (dto.getAciklama() != null) sayim.setAciklama(dto.getAciklama());
        if (dto.getStokId() != null) {
            Stok stok = stokRepository.findById(dto.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
            sayim.setStok(stok);
        }
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    public void sil(Long id) {
        StokSayim s = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(s.getSirketId(), "StokSayim");
        stokSayimRepository.deleteById(id);
    }

    public StokSayimDTO durumGuncelle(Long id, String yeniDurum) {
        StokSayim sayim = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(sayim.getSirketId(), "StokSayim");
        if (yeniDurum == null || !java.util.List.of("TASLAK", "TAMAMLANDI", "IPTAL").contains(yeniDurum)) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz durum: " + yeniDurum);
        }
        if ("TAMAMLANDI".equals(yeniDurum) && sayim.getStok() != null) {
            BigDecimal fark = (sayim.getSayilanMiktar() != null ? sayim.getSayilanMiktar() : BigDecimal.ZERO)
                    .subtract(sayim.getBeklenenMiktar() != null ? sayim.getBeklenenMiktar() : BigDecimal.ZERO);
            sayim.setFark(fark);
            if (fark.compareTo(BigDecimal.ZERO) != 0) {
                Stok stok = stokRepository.findByIdForUpdate(sayim.getStok().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stok", sayim.getStok().getId()));
                stok.setMiktar(stok.getMiktar().add(fark));
                stokRepository.save(stok);
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur(fark.compareTo(BigDecimal.ZERO) > 0 ? "GIRIS" : "CIKIS")
                        .miktar(fark.abs())
                        .hareketTarihi(java.time.LocalDate.now())
                        .aciklama("Stok sayımı #" + sayim.getId() + " farkı")
                        .build());
                kritikStokBildirimiGonder(stok);
                cacheYardimci.temizle("stoklar", "dashboard");
            }
        }
        sayim.setDurum(yeniDurum);
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    /**
     * Barkod/stok kodu taraması: aynı gün için TASLAK bir sayım varsa sayılan miktarı artırır,
     * yoksa beklenen miktar stok mevcudu olacak şekilde yeni bir TASLAK sayım başlatır.
     */
    public StokSayimDTO tara(String barkod, BigDecimal adet, Long sirketId) {
        if (barkod == null || barkod.isBlank()) {
            throw new BusinessException("Barkod boş olamaz");
        }
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi eksik");
        }
        Stok stok = stokRepository.findBySirketIdAndBarkod(sirketId, barkod).stream().findFirst()
                .orElseGet(() -> stokRepository.findBySirketIdAndStokKodu(sirketId, barkod).orElse(null));
        if (stok == null) {
            throw new BusinessException("Barkod eşleşen stok bulunamadı: " + barkod);
        }
        BigDecimal miktar = (adet != null && adet.compareTo(BigDecimal.ZERO) > 0) ? adet : BigDecimal.ONE;
        StokSayim sayim = stokSayimRepository
                .findFirstBySirketIdAndStokIdAndDurumOrderByOlusturmaTarihiDesc(sirketId, stok.getId(), "TASLAK")
                .orElseGet(() -> {
                    StokSayim yeni = StokSayim.builder()
                            .tarih(java.time.LocalDate.now())
                            .stok(stok)
                            .beklenenMiktar(stok.getMiktar() != null ? stok.getMiktar() : BigDecimal.ZERO)
                            .sayilanMiktar(BigDecimal.ZERO)
                            .durum("TASLAK")
                            .sirketId(sirketId)
                            .aciklama("Barkod taraması ile oluşturuldu")
                            .build();
                    return stokSayimRepository.saveAndFlush(yeni);
                });
        sayim.setSayilanMiktar(sayim.getSayilanMiktar() != null ? sayim.getSayilanMiktar() : BigDecimal.ZERO);
        sayim.setSayilanMiktar(sayim.getSayilanMiktar().add(miktar));
        sayim.setFark(sayim.getSayilanMiktar()
                .subtract(sayim.getBeklenenMiktar() != null ? sayim.getBeklenenMiktar() : BigDecimal.ZERO));
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    private void kritikStokBildirimiGonder(Stok stok) {
        try {
            if (stok.getMinMiktar() != null && stok.getMiktar().compareTo(stok.getMinMiktar()) <= 0 && stok.getSirketId() != null) {
                bildirimService.bildirimGonder(stok.getSirketId(), "STOK",
                        "Kritik Stok: " + stok.getAd(),
                        "Stok miktarı (" + stok.getMiktar() + ") kritik seviyeye (" + stok.getMinMiktar() + ") düştü.");
            }
        } catch (Exception e) {
            log.warn("Kritik stok bildirimi gönderilemedi: {}", e.getMessage());
        }
    }

    private StokSayimDTO entityToDTO(StokSayim s) {
        return StokSayimDTO.builder()
                .id(s.getId())
                .tarih(s.getTarih())
                .stokId(s.getStok() != null ? s.getStok().getId() : null)
                .stokAdi(s.getStok() != null ? s.getStok().getAd() : null)
                .beklenenMiktar(s.getBeklenenMiktar())
                .sayilanMiktar(s.getSayilanMiktar())
                .fark(s.getFark())
                .durum(s.getDurum())
                .sirketId(s.getSirketId())
                .aciklama(s.getAciklama())
                .olusturmaTarihi(s.getOlusturmaTarihi())
                .build();
    }
}