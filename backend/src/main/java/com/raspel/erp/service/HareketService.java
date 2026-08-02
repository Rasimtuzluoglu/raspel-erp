package com.raspel.erp.service;

import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.entity.CariHesap;
import com.raspel.erp.entity.Hareket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.CariHesapRepository;
import com.raspel.erp.repository.HareketRepository;
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
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     */
    public List<HareketDTO> cariHesapHareketleriGetir(Long cariHesapId) {
        log.debug("Cari hesap hareketleri getiriliyor - ID: {}", cariHesapId);
        
        // Cari hesabın var olduğunu kontrol et
        if (!cariHesapRepository.existsById(cariHesapId)) {
            throw new ResourceNotFoundException("Cari Hesap", cariHesapId);
        }
        
        return hareketRepository.findByCariHesapIdOrderByHareketTarihiDesc(cariHesapId)
                .stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }
    
    /**
     * Son n hareketi getir (Dashboard için)
     */
    public List<HareketDTO> sonHareketleriGetir(int limit) {
        log.debug("Son {} hareket getiriliyor", limit);
        Pageable pageable = PageRequest.of(0, limit);
        
        return hareketRepository.findAllByOrderByHareketTarihiDescOlusturmaTarihiDesc(pageable)
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
        
        // Cari hesabın var olduğunu kontrol et
        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));
        
        // Hareket türünü valide et
        Hareket.HareketTuru hareketTuru;
        try {
            hareketTuru = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }
        
        // Bakiye güncelleme tutarını hesapla (Tahsilat +, Ödeme -)
        BigDecimal bakiyeGuncellemeTutari = hareketTuru == Hareket.HareketTuru.TAHSILAT 
                ? dto.getTutar() 
                : dto.getTutar().negate();
        
        // Hareket oluştur
        Hareket hareket = Hareket.builder()
                .cariHesap(cariHesap)
                .tur(hareketTuru)
                .tutar(dto.getTutar())
                .hareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now())
                .aciklama(dto.getAciklama())
                .odemeSekli(dto.getOdemeSekli())
                .sirketId(sirketId)
                .build();
        
        Hareket kaydedilenHareket = hareketRepository.save(hareket);
        
        // Cari hesabın bakiyesini güncelle
        cariHesapService.bakiyeGuncelle(dto.getCariHesapId(), bakiyeGuncellemeTutari);
        
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
     * Tarih aralığına göre hareketleri filtrele
     */
    @Transactional(readOnly = true)
    public List<HareketDTO> hareketleriFiltrele(Long cariHesapId, LocalDate baslangic, LocalDate bitis) {
        log.debug("Hareketler filtreleniyor - Cari: {}, Tarih: {} - {}", cariHesapId, baslangic, bitis);

        if (baslangic == null) baslangic = LocalDate.of(2000, 1, 1);
        if (bitis == null) bitis = LocalDate.now().plusDays(1);

        List<Hareket> sonuc;
        if (cariHesapId != null) {
            sonuc = hareketRepository.findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(cariHesapId, baslangic, bitis);
        } else {
            sonuc = hareketRepository.findByHareketTarihiBetweenOrderByHareketTarihiDesc(baslangic, bitis);
        }

        return sonuc.stream()
                .map(this::entityDTOyeCevir)
                .collect(Collectors.toList());
    }

    /**
     * Hareket güncelle
     */
    public HareketDTO hareketGuncelle(Long id, HareketDTO dto) {
        log.info("Hareket güncelleniyor - ID: {}", id);

        Hareket hareket = hareketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket", id));

        CariHesap cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", dto.getCariHesapId()));

        Hareket.HareketTuru yeniTur;
        try {
            yeniTur = Hareket.HareketTuru.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz hareket türü: " + dto.getTur());
        }

        BigDecimal eskiBakiyeEtkisi = hareket.getTur() == Hareket.HareketTuru.TAHSILAT
                ? hareket.getTutar() : hareket.getTutar().negate();

        BigDecimal yeniBakiyeEtkisi = yeniTur == Hareket.HareketTuru.TAHSILAT
                ? dto.getTutar() : dto.getTutar().negate();

        hareket.setCariHesap(cariHesap);
        hareket.setTur(yeniTur);
        hareket.setTutar(dto.getTutar());
        hareket.setHareketTarihi(dto.getHareketTarihi() != null ? dto.getHareketTarihi() : LocalDate.now());
        hareket.setAciklama(dto.getAciklama());
        if (dto.getOdemeSekli() != null) hareket.setOdemeSekli(dto.getOdemeSekli());

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
        
        // Bakiye güncellemeyi ters işlemle yap
        BigDecimal bakiyeGuncellemeTutari = hareket.getTur() == Hareket.HareketTuru.TAHSILAT 
                ? hareket.getTutar().negate() 
                : hareket.getTutar();
        
        cariHesapService.bakiyeGuncelle(hareket.getCariHesap().getId(), bakiyeGuncellemeTutari);
        
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
                .olusturmaTarihi(hareket.getOlusturmaTarihi())
                .build();
    }
}
