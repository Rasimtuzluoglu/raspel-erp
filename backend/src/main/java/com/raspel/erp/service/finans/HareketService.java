package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.service.sistem.BildirimService;

/**
 * Hareket Service
 * Hareket işlemlerinin business logic'ini yönetir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HareketService {
    
    private final HareketRepository hareketRepository;
    private final CariHesapRepository cariHesapRepository;
    private final CariHesapService cariHesapService;
    private final BildirimService bildirimService;
    private final FaturaRepository faturaRepository;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final TenantChecker tenantChecker;
    private final CacheYardimci cacheYardimci;

    /**
     * Faturanın ödenen tutarını ve ödeme durumunu günceller.
     * delta: TAHSILAT/ODEME hareketi için pozitif, silme/güncelleme tersi için negatif.
     */
    private void faturaOdemeUygula(Long faturaId, BigDecimal delta, String aciklama) {
        if (faturaId == null || delta == null || delta.compareTo(BigDecimal.ZERO) == 0) return;
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + faturaId));
        tenantChecker.check(fatura.getSirketId(), "Fatura");
        BigDecimal yeniOdenen = (fatura.getOdenenTutar() != null ? fatura.getOdenenTutar() : BigDecimal.ZERO).add(delta);
        if (yeniOdenen.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Fatura ödenen tutarı negatif olamaz. Ödenen: "
                    + fatura.getOdenenTutar() + ", İşlem: " + delta + " (" + aciklama + ")");
        }
        BigDecimal toplam = fatura.getGenelToplam() != null ? fatura.getGenelToplam() : BigDecimal.ZERO;
        BigDecimal kalan = toplam.subtract(yeniOdenen);
        fatura.setOdenenTutar(yeniOdenen);
        fatura.setKalanTutar(kalan.max(BigDecimal.ZERO));
        fatura.setOdemeDurumu(kalan.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                : yeniOdenen.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI");
        faturaRepository.save(fatura);
        cacheYardimci.temizle("faturalar", "dashboard");
    }
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     */
    @Transactional(readOnly = true)
    public List<HareketDTO> cariHesapHareketleriGetir(Long cariHesapId) {
        log.debug("Cari hesap hareketleri getiriliyor - ID: {}", cariHesapId);
        
        // Cari hesabın var olduğunu ve geçerli firmaya ait olduğunu kontrol et
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        
        return hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(cariHesapId)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }
    
    /**
     * Son n hareketi getir (tenant filtreli).
     */
    @Transactional(readOnly = true)
    public List<HareketDTO> sonHareketleriGetir(int limit, Long sirketId) {
        Pageable pageable = PageRequest.of(0, limit);
        return hareketRepository.findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(sirketId, pageable)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni hareket oluştur ve cari hesabın bakiyesini güncelle
     */
    public HareketDTO hareketOlustur(HareketDTO dto, Long sirketId) {
        log.info("Yeni hareket oluşturuluyor - Cari ID: {}, Tür: {}, Tutar: {}, sirketId: {}", 
                dto.getCariHesapId(), dto.getTur(), dto.getTutar(), sirketId);
        
        // Cari hesabın var olduğunu ve geçerli firmaya ait olduğunu kontrol et
        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");

        // Bağlı fatura varsa önceden doğrula (fatura şirketi ile eşleşmeli)
        if (dto.getFaturaId() != null) {
            Fatura fatura = faturaRepository.findById(dto.getFaturaId())
                    .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + dto.getFaturaId()));
            tenantChecker.check(fatura.getSirketId(), "Fatura");
            if (fatura.getCariHesap() != null && !fatura.getCariHesap().getId().equals(dto.getCariHesapId())) {
                throw new BusinessException("Fatura bu cari hesaba ait değil");
            }
        }
        
        // Hareket türünü valide et
        Hareket.HareketTuru hareketTuru;
        try {
            hareketTuru = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }
        
        // Bakiye güncelleme tutarını hesapla (Alacak +, Borç -: Tahsilat alacağı azaltır, Ödeme borcu azaltır)
        BigDecimal bakiyeGuncellemeTutari = hareketTuru == Hareket.HareketTuru.TAHSILAT 
                ? dto.getTutar().negate() 
                : dto.getTutar();
        
        // Hareket oluştur
        Hareket hareket = Hareket.builder()
                .cariHesap(cariHesap)
                .tur(hareketTuru)
                .tutar(dto.getTutar())
                .hareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now())
                .aciklama(dto.getAciklama())
                .odemeSekli(dto.getOdemeSekli())
                .faturaId(dto.getFaturaId())
                .sirketId(sirketId)
                .build();
        
        Hareket kaydedilenHareket = hareketRepository.save(hareket);
        
        // Cari hesabın bakiyesini güncelle
        cariHesapService.bakiyeGuncelle(dto.getCariHesapId(), bakiyeGuncellemeTutari);

        // Faturaya işle (varsa): ödenen tutar artar
        if (dto.getFaturaId() != null) {
            faturaOdemeUygula(dto.getFaturaId(), dto.getTutar(), "Hareket #" + kaydedilenHareket.getId());
        }
        
        try {
            if (sirketId != null) {
                bildirimService.bildirimGonder(sirketId, hareketTuru == Hareket.HareketTuru.TAHSILAT ? "TAKSILAT" : "ODEME",
                        (hareketTuru == Hareket.HareketTuru.TAHSILAT ? "Tahsilat: " : "Ödeme: ") + dto.getTutar() + " ₺",
                        cariHesap.getAd() + (dto.getAciklama() != null ? " - " + dto.getAciklama() : ""));
            }
        } catch (Exception e) {
            log.warn("Hareket bildirimi gönderilemedi: {}", e.getMessage());
        }
        
        log.info("Hareket başarıyla oluşturuldu - ID: {}", kaydedilenHareket.getId());
        
        return entityDTOyeCevir(kaydedilenHareket);
    }
    
    @Transactional(readOnly = true)
    public Page<HareketDTO> tumHareketleriGetir(Long sirketId, Pageable pageable) {
        log.debug("Tüm hareketler getiriliyor, sirketId: {}", sirketId);
        return hareketRepository.findBySirketIdOrderByHareketTarihiDesc(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    /**
     * Tarih aralığına göre hareketleri filtrele (tenant filtreli)
     */
    @Transactional(readOnly = true)
    public Page<HareketDTO> hareketleriFiltrele(Long cariHesapId, LocalDate baslangic, LocalDate bitis, Pageable pageable, Long sirketId) {
        log.debug("Hareketler filtreleniyor - Cari: {}, Tarih: {} - {}, sirketId: {}", cariHesapId, baslangic, bitis, sirketId);

        if (baslangic == null) baslangic = LocalDate.of(2000, 1, 1);
        if (bitis == null) bitis = LocalDate.now().plusDays(1);

        Page<Hareket> sonuc;
        if (cariHesapId != null) {
            CariHesap cari = cariHesapRepository.findById(cariHesapId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariHesapId));
            tenantChecker.check(cari.getSirketId(), "Cari Hesap");
            sonuc = hareketRepository.findBySirketIdAndCariHesapIdAndHareketTarihiBetween(sirketId, cariHesapId, baslangic, bitis, pageable);
        } else {
            sonuc = hareketRepository.findBySirketIdAndHareketTarihiBetween(sirketId, baslangic, bitis, pageable);
        }

        return sonuc.map(this::entityDTOyeCevir);
    }

    /**
     * Hareket güncelle
     */
    public HareketDTO hareketGuncelle(Long id, HareketDTO dto) {
        log.info("Hareket güncelleniyor - ID: {}", id);

        Hareket hareket = hareketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", id));
        tenantChecker.check(hareket.getSirketId(), "Hareket");

        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));
        tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        if (dto.getCariHesapId() != null && !dto.getCariHesapId().equals(hareket.getCariHesap().getId())) {
            tenantChecker.check(cariHesap.getSirketId(), "Cari Hesap");
        }

        if (dto.getFaturaId() != null) {
            Fatura yeniFatura = faturaRepository.findById(dto.getFaturaId())
                    .orElseThrow(() -> new BusinessException("Bağlı fatura bulunamadı: " + dto.getFaturaId()));
            tenantChecker.check(yeniFatura.getSirketId(), "Fatura");
        }

        Hareket.HareketTuru yeniTur;
        try {
            yeniTur = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }

        BigDecimal eskiBakiyeEtkisi = hareket.getTur() == Hareket.HareketTuru.TAHSILAT
                ? hareket.getTutar().negate() : hareket.getTutar();

        BigDecimal yeniBakiyeEtkisi = yeniTur == Hareket.HareketTuru.TAHSILAT
                ? dto.getTutar().negate() : dto.getTutar();

        // Eski bağlı fatura etkisini geri al, yeni faturaya uygula
        Long eskiFaturaId = hareket.getFaturaId();
        if (eskiFaturaId != null) {
            faturaOdemeUygula(eskiFaturaId, hareket.getTutar().negate(), "Hareket #" + hareket.getId() + " güncellendi");
        }
        if (dto.getFaturaId() != null && !dto.getFaturaId().equals(eskiFaturaId)) {
            faturaOdemeUygula(dto.getFaturaId(), dto.getTutar(), "Hareket #" + hareket.getId() + " güncellendi");
        } else if (dto.getFaturaId() != null) {
            faturaOdemeUygula(dto.getFaturaId(), dto.getTutar(), "Hareket #" + hareket.getId() + " güncellendi");
        }

        hareket.setCariHesap(cariHesap);
        hareket.setTur(yeniTur);
        hareket.setTutar(dto.getTutar());
        hareket.setHareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now());
        hareket.setAciklama(dto.getAciklama());
        if (dto.getOdemeSekli() != null) hareket.setOdemeSekli(dto.getOdemeSekli());
        hareket.setFaturaId(dto.getFaturaId());

        Hareket guncellenen = hareketRepository.save(hareket);

        cariHesapService.bakiyeGuncelle(hareket.getCariHesap().getId(), yeniBakiyeEtkisi.subtract(eskiBakiyeEtkisi));

        log.info("Hareket başarıyla güncellendi - ID: {}", id);
        return entityDTOyeCevir(guncellenen);
    }

    /**
     * Hareket sil (ve bakiyeyi ters işlemle güncelle)
     */
    public void hareketSil(Long id) {
        log.info("Hareket siliniliyor - ID: {}", id);
        
        Hareket hareket = hareketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", id));
        tenantChecker.check(hareket.getSirketId(), "Hareket");
        
        // Bakiye güncellemeyi ters işlemle yap
        BigDecimal bakiyeGuncellemeTutari = hareket.getTur() == Hareket.HareketTuru.TAHSILAT 
                ? hareket.getTutar() 
                : hareket.getTutar().negate();
        
        cariHesapService.bakiyeGuncelle(hareket.getCariHesap().getId(), bakiyeGuncellemeTutari);

        // Bağlı fatura varsa ödenen tutarı geri al
        if (hareket.getFaturaId() != null) {
            faturaOdemeUygula(hareket.getFaturaId(), hareket.getTutar().negate(), "Hareket #" + hareket.getId() + " silindi");
        }
        
        auditLogService.finansalSilmeLog("Hareket", id,
                "Hareket silindi: " + hareket.getTur() + " " + hareket.getTutar() + " TL - Cari: "
                        + hareket.getCariHesap().getAd() + " (bakiye terslendi)"
                        + (hareket.getFaturaId() != null ? " - Fatura: " + hareket.getFaturaId() : ""));
        
        hareketRepository.deleteById(id);
        log.info("Hareket başarıyla silindi - ID: {}", id);
    }
    
    /**
     * Entity'yi DTO'ya çevir
     */
    public HareketDTO entityDTOyeCevir(Hareket hareket) {
        return HareketDTO.builder()
                .id(hareket.getId())
                .cariHesapId(hareket.getCariHesap().getId())
                .cariHesapAd(hareket.getCariHesap().getAd())
                .tur(hareket.getTur().name())
                .tutar(hareket.getTutar())
                .hareketTarihi(hareket.getHareketTarihi())
                .aciklama(hareket.getAciklama())
                .odemeSekli(hareket.getOdemeSekli())
                .faturaId(hareket.getFaturaId())
                .olusturmaTarihi(hareket.getOlusturmaTarihi())
                .build();
    }
}