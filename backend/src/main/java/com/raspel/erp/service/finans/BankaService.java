package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.BankaDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BankaService {

    private final BankaRepository bankaRepository;
    private final BankaHareketiRepository bankaHareketiRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;

    @Transactional(readOnly = true)
    public Page<BankaDTO> tumBankalariGetir(Long sirketId, Pageable pageable) {
        return bankaRepository.findBySirketId(sirketId, pageable).map(this::entityDTOyeCevir);
    }

    @Transactional(readOnly = true)
    public BankaDTO bankaGetir(Long id) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        return entityDTOyeCevir(banka);
    }

    /**
     * Bankalar ekranı KPI özeti: hesap sayısı, toplam/pozitif bakiye ve
     * eşleşmemiş (mutabakat bekleyen) hareket sayısı.
     */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.finans.BankaOzetDTO ozet(Long sirketId) {
        if (sirketId == null) {
            return com.raspel.erp.dto.finans.BankaOzetDTO.builder()
                    .toplamBakiye(BigDecimal.ZERO).pozitifBakiye(BigDecimal.ZERO).build();
        }
        BigDecimal toplam = bankaRepository.sumBakiyeBySirketId(sirketId);
        BigDecimal pozitif = bankaRepository.sumPozitifBakiyeBySirketId(sirketId);
        return com.raspel.erp.dto.finans.BankaOzetDTO.builder()
                .toplamBanka(bankaRepository.countBySirketId(sirketId))
                .toplamBakiye(toplam != null ? toplam : BigDecimal.ZERO)
                .pozitifBakiye(pozitif != null ? pozitif : BigDecimal.ZERO)
                .eslesmemisHareket(bankaHareketiRepository.countBySirketIdAndEslestirildiFalse(sirketId))
                .build();
    }

    /** IBAN'i karsilastirma icin normalize eder (bosluk yok, buyuk harf). */
    private String normalizeIban(String iban) {
        if (iban == null) return null;
        String s = iban.replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
        return s.isBlank() ? null : s;
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public BankaDTO bankaOlustur(BankaDTO dto, Long sirketId) {
        log.info("Yeni banka hesabı oluşturuluyor: {}, sirketId: {}", dto.getAd(), sirketId);
        String iban = normalizeIban(dto.getIban());
        String hesapNo = dto.getHesapNo() != null && !dto.getHesapNo().isBlank() ? dto.getHesapNo().trim() : null;
        // Mükerrer kontrolü: aynı şirkette aynı IBAN/hesapNo iki kez tanımlanamaz.
        if (sirketId != null && iban != null && bankaRepository.existsBySirketIdAndIbanIgnoreCase(sirketId, iban)) {
            throw new BusinessException("Bu IBAN ile kayıtlı bir banka hesabı zaten var.");
        }
        if (sirketId != null && hesapNo != null && bankaRepository.existsBySirketIdAndHesapNoIgnoreCase(sirketId, hesapNo)) {
            throw new BusinessException("Bu hesap numarası ile kayıtlı bir banka hesabı zaten var.");
        }
        Banka banka = Banka.builder()
                .ad(dto.getAd())
                .hesapNo(hesapNo)
                .iban(iban)
                .bakiye(dto.getBakiye() != null ? dto.getBakiye() : BigDecimal.ZERO)
                .sirketId(sirketId)
                .build();
        Banka kaydedilen = bankaRepository.save(banka);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityDTOyeCevir(kaydedilen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public BankaDTO bankaGuncelle(Long id, BankaDTO dto) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        Long sirketId = banka.getSirketId();
        if (dto.getIban() != null) {
            String iban = normalizeIban(dto.getIban());
            if (sirketId != null && iban != null
                    && bankaRepository.existsBySirketIdAndIbanIgnoreCaseAndIdNot(sirketId, iban, id)) {
                throw new BusinessException("Bu IBAN ile kayıtlı bir banka hesabı zaten var.");
            }
            banka.setIban(iban);
        }
        if (dto.getHesapNo() != null) {
            String hesapNo = dto.getHesapNo().isBlank() ? null : dto.getHesapNo().trim();
            if (sirketId != null && hesapNo != null
                    && bankaRepository.existsBySirketIdAndHesapNoIgnoreCaseAndIdNot(sirketId, hesapNo, id)) {
                throw new BusinessException("Bu hesap numarası ile kayıtlı bir banka hesabı zaten var.");
            }
            banka.setHesapNo(hesapNo);
        }
        if (dto.getAd() != null) banka.setAd(dto.getAd());
        Banka guncellenen = bankaRepository.save(banka);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void bankaSil(Long id) {
        Banka banka = bankaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", id));
        tenantChecker.check(banka.getSirketId(), "Banka");
        if (banka.getBakiye() != null && banka.getBakiye().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Bakiyesi sıfır olmayan banka hesabı silinemez. Mevcut bakiye: " + banka.getBakiye() + " ₺");
        }
        long hareketSayisi = bankaHareketiRepository.countByBankaId(id);
        if (hareketSayisi > 0) {
            throw new BusinessException("Bu banka hesabına ait " + hareketSayisi + " adet hareket kaydı bulunmaktadır. Önce hareketleri temizleyiniz.");
        }
        bankaRepository.deleteById(id);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    private BankaDTO entityDTOyeCevir(Banka banka) {
        return BankaDTO.builder()
                .id(banka.getId())
                .ad(banka.getAd())
                .hesapNo(banka.getHesapNo())
                .iban(banka.getIban())
                .bakiye(banka.getBakiye())
                .olusturmaTarihi(banka.getOlusturmaTarihi())
                .build();
    }
}
