package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.SatinalmaTalepDTO;
import com.raspel.erp.dto.ticaret.SatinalmaTalepKalemDTO;
import com.raspel.erp.entity.ticaret.SatinalmaTalep;
import com.raspel.erp.entity.ticaret.SatinalmaTalepKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.ticaret.SatinalmaTalepKalemRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SatinalmaTalepService {

    private final SatinalmaTalepRepository talepRepository;
    private final SatinalmaTalepKalemRepository kalemRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.sistem.OnayAyariService onayAyariService;

    @Transactional(readOnly = true)
    public Page<SatinalmaTalepDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return talepRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public SatinalmaTalepDTO getir(Long id) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        tenantChecker.check(t.getSirketId(), "Talep");
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO olustur(SatinalmaTalepDTO dto) {
        return olustur(dto, null);
    }

    public SatinalmaTalepDTO olustur(SatinalmaTalepDTO dto, Long sirketId) {
        // Tenant baglami request'ten gelir; DTO'da yoksa/null ise buradan set edilir.
        if (sirketId != null) {
            dto.setSirketId(sirketId);
        }
        SatinalmaTalep t = SatinalmaTalep.builder()
                .talepNo(dto.getTalepNo())
                .tarih(dto.getTarih())
                .talepEden(dto.getTalepEden())
                .departman(dto.getDepartman())
                .durum("TASLAK")
                .aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId())
                .build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Satınalma Talebi");
        t = talepRepository.save(t);

        if (dto.getKalemler() != null) {
            for (SatinalmaTalepKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaTalepKalem.builder()
                        .talepId(t.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .build());
            }
        }

        // Otomatik onay kuralı: eşiğin altındaki talepler doğrudan onaylanır.
        try {
            BigDecimal toplam = BigDecimal.ZERO;
            if (dto.getKalemler() != null) {
                for (SatinalmaTalepKalemDTO k : dto.getKalemler()) {
                    BigDecimal miktar = k.getMiktar() != null ? k.getMiktar() : BigDecimal.ZERO;
                    BigDecimal fiyat = k.getTahminiBirimFiyat() != null ? k.getTahminiBirimFiyat() : BigDecimal.ZERO;
                    toplam = toplam.add(miktar.multiply(fiyat));
                }
            }
            if (toplam.signum() > 0
                    && onayAyariService.otomatikOnayGecerli(t.getSirketId(), "SATINALMA", toplam)) {
                t.setDurum("ONAYLANDI");
                t = talepRepository.save(t);
                log.info("Satınalma talebi otomatik onaylandı - Talep #{} ({} ₺)", t.getId(), toplam);
            }
        } catch (Exception e) {
            log.warn("Otomatik onay kontrolü yapılamadı (talep #{}): {}", t.getId(), e.getMessage());
        }
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO guncelle(Long id, SatinalmaTalepDTO dto) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        tenantChecker.check(t.getSirketId(), "Talep");
        t.setTalepNo(dto.getTalepNo());
        t.setTarih(dto.getTarih());
        t.setTalepEden(dto.getTalepEden());
        t.setDepartman(dto.getDepartman());
        if (dto.getDurum() != null) t.setDurum(dto.getDurum());
        t.setAciklama(dto.getAciklama());
        t = talepRepository.save(t);
        if (dto.getKalemler() != null) {
            kalemRepository.deleteByTalepId(t.getId());
            for (SatinalmaTalepKalemDTO k : dto.getKalemler()) {
                kalemRepository.save(SatinalmaTalepKalem.builder()
                        .talepId(t.getId()).stokId(k.getStokId())
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .build());
            }
        }
        return entityToDTO(t);
    }

    public SatinalmaTalepDTO durumGuncelle(Long id, String durum) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        tenantChecker.check(t.getSirketId(), "Talep");
        if (!GECERLI_DURUMLAR.contains(durum)) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz talep durumu: " + durum);
        }
        // Faturası/siparişi oluşmuş talep geri alınamaz.
        if ("SIPARISE_DONUSTU".equals(t.getDurum()) && !"SIPARISE_DONUSTU".equals(durum)) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Siparişe dönüştürülmüş talep başka duruma alınamaz.");
        }
        t.setDurum(durum);
        return entityToDTO(talepRepository.save(t));
    }

    private static final java.util.Set<String> GECERLI_DURUMLAR =
            java.util.Set.of("TASLAK", "ONAYLANDI", "REDDEDILDI", "SIPARISE_DONUSTU");

    public void sil(Long id) {
        SatinalmaTalep t = talepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talep", id));
        tenantChecker.check(t.getSirketId(), "Talep");
        // Onaylanmis/donusturulmus talep silinemez; denetim izi ve siparis bagi kopmasin.
        if ("SIPARISE_DONUSTU".equals(t.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Siparişe dönüştürülmüş talep silinemez.");
        }
        if ("ONAYLANDI".equals(t.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Onaylanmış talep silinemez; önce durumunu TASLAK'a alın.");
        }
        kalemRepository.deleteByTalepId(id);
        talepRepository.deleteById(id);
    }

    private SatinalmaTalepDTO entityToDTO(SatinalmaTalep t) {
        List<SatinalmaTalepKalemDTO> kalemler = kalemRepository.findByTalepId(t.getId()).stream()
                .map(k -> SatinalmaTalepKalemDTO.builder()
                        .id(k.getId()).talepId(k.getTalepId()).stokId(k.getStokId())
                        .stokAdi(k.getStokId() != null ? stokRepository.findById(k.getStokId()).map(s -> s.getAd()).orElse(null) : null)
                        .aciklama(k.getAciklama()).miktar(k.getMiktar())
                        .birim(k.getBirim()).tahminiBirimFiyat(k.getTahminiBirimFiyat())
                        .olusturmaTarihi(k.getOlusturmaTarihi()).build())
                .collect(Collectors.toList());

        return SatinalmaTalepDTO.builder()
                .id(t.getId()).talepNo(t.getTalepNo()).tarih(t.getTarih())
                .talepEden(t.getTalepEden()).departman(t.getDepartman())
                .durum(t.getDurum()).aciklama(t.getAciklama())
                .sirketId(t.getSirketId()).olusturmaTarihi(t.getOlusturmaTarihi())
                .kalemler(kalemler).build();
    }
}
