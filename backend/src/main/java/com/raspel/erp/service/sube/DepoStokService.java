package com.raspel.erp.service.sube;

import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Depo bazlı stok (DepoStok) senkronizasyonu. Stok.miktar ana kaynaktır; bu servis
 * hareketlerin depo kırılımını tutarlı tutar. Tüm stok hareketleri aynı işlem
 * (transaction) içinde buradan geçirilmelidir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepoStokService {

    private final DepoStokRepository depoStokRepository;
    private final DepoRepository depoRepository;

    /**
     * Depo stoğunu delta kadar değiştirir (yoksa oluşturur). Kilitli okuma ile
     * eşzamanlı hareketlerde tutarlılık sağlar.
     */
    @Transactional
    public void guncelle(Long depoId, Long stokId, BigDecimal delta) {
        if (depoId == null || stokId == null || delta == null || delta.signum() == 0) return;
        DepoStok ds = depoStokRepository.findByDepoIdAndStokIdForUpdate(depoId, stokId)
                .orElse(DepoStok.builder().depoId(depoId).stokId(stokId).miktar(BigDecimal.ZERO).build());
        BigDecimal mevcut = ds.getMiktar() != null ? ds.getMiktar() : BigDecimal.ZERO;
        BigDecimal yeni = mevcut.add(delta);
        if (yeni.signum() < 0) {
            log.warn("Depo stoğu negatife düştü (depo: {}, stok: {}, mevcut: {}, delta: {}) - 0'a çekildi",
                    depoId, stokId, mevcut, delta);
            yeni = BigDecimal.ZERO;
        }
        ds.setMiktar(yeni);
        depoStokRepository.save(ds);
    }

    /**
     * Depo seçilmediyse kullanılacak varsayılan (ilk aktif) depo. Yoksa null döner
     * ve depo bazlı düşüm yapılmaz.
     */
    @Transactional(readOnly = true)
    public Long varsayilanDepoId(Long sirketId) {
        if (sirketId == null) return null;
        return depoRepository.findBySirketIdAndAktifTrue(sirketId).stream()
                .min(java.util.Comparator.comparing(Depo::getId))
                .map(Depo::getId)
                .orElse(null);
    }

    /**
     * Verilen depo; boşsa varsayılan aktif depoya düşer. Hiç depo yoksa null.
     * Verilen depo başka bir şirkete aitse erişim reddedilir (tenant izolasyonu).
     */
    @Transactional(readOnly = true)
    public Long coz(Long depoId, Long sirketId) {
        if (depoId != null) {
            Depo depo = depoRepository.findById(depoId)
                    .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Depo", depoId));
            if (sirketId != null && !sirketId.equals(depo.getSirketId()))
                throw new com.raspel.erp.exception.ResourceNotFoundException("Depo", depoId);
            return depoId;
        }
        return varsayilanDepoId(sirketId);
    }
}
