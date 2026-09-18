package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.IskontoKuraliDTO;
import com.raspel.erp.entity.ticaret.IskontoKurali;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.ticaret.IskontoKuraliRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Gelişmiş fiyat/iskonto motoru. Kademeli kuralları yönetir ve bir satır için
 * (stok, cari, kategori, adet, tarih) en uygun iskonto oranını hesaplar.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IskontoMotoruService {

    private final IskontoKuraliRepository iskontoKuraliRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public Page<IskontoKuraliDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        return iskontoKuraliRepository.findBySirketIdOrderByOncelikAscIdDesc(sirketId, pageable)
                .map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public IskontoKuraliDTO getir(Long id) {
        IskontoKurali k = iskontoKuraliRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İskonto Kuralı", id));
        tenantChecker.check(k.getSirketId(), "İskonto Kuralı");
        return entityToDTO(k);
    }

    public IskontoKuraliDTO olustur(IskontoKuraliDTO dto) {
        if (dto.getSirketId() == null) dto.setSirketId(tenantChecker.getCurrentSirketId());
        if (dto.getSirketId() == null) throw new BusinessException("Şirket bilgisi zorunludur");
        tenantChecker.checkSirketId(dto.getSirketId(), "İskonto Kuralı");
        kuralDogrula(dto);
        IskontoKurali k = IskontoKurali.builder()
                .sirketId(dto.getSirketId())
                .ad(dto.getAd())
                .stokId(dto.getStokId())
                .cariHesapId(dto.getCariHesapId())
                .kategori(dto.getKategori())
                .minAdet(dto.getMinAdet())
                .maxAdet(dto.getMaxAdet())
                .iskontoOrani(dto.getIskontoOrani())
                .oncelik(dto.getOncelik() != null ? dto.getOncelik() : 100)
                .gecerliBaslangic(dto.getGecerliBaslangic())
                .gecerliBitis(dto.getGecerliBitis())
                .aktif(dto.getAktif() == null || dto.getAktif())
                .aciklama(dto.getAciklama())
                .build();
        return entityToDTO(iskontoKuraliRepository.save(k));
    }

    public IskontoKuraliDTO guncelle(Long id, IskontoKuraliDTO dto) {
        IskontoKurali k = iskontoKuraliRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İskonto Kuralı", id));
        tenantChecker.check(k.getSirketId(), "İskonto Kuralı");
        if (dto.getAd() != null) k.setAd(dto.getAd());
        if (dto.getStokId() != null) k.setStokId(dto.getStokId());
        if (dto.getCariHesapId() != null) k.setCariHesapId(dto.getCariHesapId());
        if (dto.getKategori() != null) k.setKategori(dto.getKategori());
        if (dto.getMinAdet() != null) k.setMinAdet(dto.getMinAdet());
        if (dto.getMaxAdet() != null) k.setMaxAdet(dto.getMaxAdet());
        if (dto.getIskontoOrani() != null) k.setIskontoOrani(dto.getIskontoOrani());
        if (dto.getOncelik() != null) k.setOncelik(dto.getOncelik());
        if (dto.getGecerliBaslangic() != null) k.setGecerliBaslangic(dto.getGecerliBaslangic());
        if (dto.getGecerliBitis() != null) k.setGecerliBitis(dto.getGecerliBitis());
        if (dto.getAktif() != null) k.setAktif(dto.getAktif());
        if (dto.getAciklama() != null) k.setAciklama(dto.getAciklama());
        kuralDogrula(entityToDTO(k));
        return entityToDTO(iskontoKuraliRepository.save(k));
    }

    public void sil(Long id) {
        IskontoKurali k = iskontoKuraliRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İskonto Kuralı", id));
        tenantChecker.check(k.getSirketId(), "İskonto Kuralı");
        iskontoKuraliRepository.delete(k);
    }

    private void kuralDogrula(IskontoKuraliDTO dto) {
        if (dto.getIskontoOrani() != null
                && (dto.getIskontoOrani().compareTo(BigDecimal.ZERO) < 0
                    || dto.getIskontoOrani().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException("İskonto oranı 0-100 arasında olmalıdır");
        }
        if (dto.getMinAdet() != null && dto.getMaxAdet() != null
                && dto.getMinAdet().compareTo(dto.getMaxAdet()) > 0) {
            throw new BusinessException("Minimum adet maksimum adetten büyük olamaz");
        }
    }

    /**
     * Verilen satır için uygulanacak iskonto oranını hesaplar. En yüksek öncelik
     * (küçük sayı) ve en yüksek oran kazanır. Eşleşen kural yoksa sıfır döner.
     */
    @Transactional(readOnly = true)
    public BigDecimal iskontoHesapla(Long sirketId, Long stokId, Long cariHesapId,
                                     String kategori, BigDecimal adet, LocalDate tarih) {
        if (sirketId == null) return BigDecimal.ZERO;
        LocalDate gun = tarih != null ? tarih : LocalDate.now();
        List<IskontoKurali> kurallar = iskontoKuraliRepository.findBySirketIdAndAktifTrue(sirketId);

        Optional<IskontoKurali> enIyi = kurallar.stream()
                .filter(k -> kapsamUygun(k, stokId, cariHesapId, kategori))
                .filter(k -> miktarUygun(k, adet))
                .filter(k -> tarihUygun(k, gun))
                .max(Comparator
                        .comparingInt((IskontoKurali k) -> -(k.getOncelik() != null ? k.getOncelik() : 100))
                        .thenComparing(k -> k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO));

        return enIyi.map(k -> k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
    }

    private boolean kapsamUygun(IskontoKurali k, Long stokId, Long cariHesapId, String kategori) {
        // Tanımlı her kapsam alanı eşleşmek zorundadır; boş alanlar joker karakterdir.
        if (k.getStokId() != null && !k.getStokId().equals(stokId)) return false;
        if (k.getCariHesapId() != null && !k.getCariHesapId().equals(cariHesapId)) return false;
        if (k.getKategori() != null && !k.getKategori().isBlank()
                && (kategori == null || !k.getKategori().equalsIgnoreCase(kategori))) return false;
        return true;
    }

    private boolean miktarUygun(IskontoKurali k, BigDecimal adet) {
        BigDecimal miktar = adet != null ? adet : BigDecimal.ZERO;
        if (k.getMinAdet() != null && miktar.compareTo(k.getMinAdet()) < 0) return false;
        if (k.getMaxAdet() != null && miktar.compareTo(k.getMaxAdet()) > 0) return false;
        return true;
    }

    private boolean tarihUygun(IskontoKurali k, LocalDate gun) {
        if (k.getGecerliBaslangic() != null && gun.isBefore(k.getGecerliBaslangic())) return false;
        if (k.getGecerliBitis() != null && gun.isAfter(k.getGecerliBitis())) return false;
        return true;
    }

    private IskontoKuraliDTO entityToDTO(IskontoKurali k) {
        return IskontoKuraliDTO.builder()
                .id(k.getId()).sirketId(k.getSirketId()).ad(k.getAd())
                .stokId(k.getStokId()).cariHesapId(k.getCariHesapId()).kategori(k.getKategori())
                .minAdet(k.getMinAdet()).maxAdet(k.getMaxAdet())
                .iskontoOrani(k.getIskontoOrani()).oncelik(k.getOncelik())
                .gecerliBaslangic(k.getGecerliBaslangic()).gecerliBitis(k.getGecerliBitis())
                .aktif(k.getAktif()).aciklama(k.getAciklama())
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .build();
    }
}
