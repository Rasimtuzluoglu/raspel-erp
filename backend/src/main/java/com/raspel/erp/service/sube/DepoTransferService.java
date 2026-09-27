package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.DepoTransferDTO;
import com.raspel.erp.entity.sube.DepoTransfer;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoTransferRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Depolar arası stok transferi onay akışı. Transfer önce BEKLIYOR durumunda
 * oluşturulur; admin onayladığında gerçek stok hareketi gerçekleşir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepoTransferService {

    private final DepoTransferRepository transferRepository;
    private final DepoService depoService;
    private final DepoRepository depoRepository;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.repository.sube.DepoStokRepository depoStokRepository;

    @Transactional(readOnly = true)
    public List<DepoTransferDTO> listele(Long sirketId) {
        return transferRepository.findBySirketIdOrderByIdDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepoTransferDTO> bekleyenler(Long sirketId) {
        return transferRepository.findBySirketIdAndDurumOrderByIdAsc(sirketId, "BEKLIYOR").stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional
    public DepoTransferDTO talepOlustur(DepoTransferDTO dto, Long sirketId, Long kullaniciId) {
        if (dto.getKaynakDepoId() == null || dto.getHedefDepoId() == null) {
            throw new BusinessException("Kaynak ve hedef depo seçilmelidir");
        }
        if (dto.getKaynakDepoId().equals(dto.getHedefDepoId())) {
            throw new BusinessException("Kaynak ve hedef depo aynı olamaz");
        }
        if (dto.getStokId() == null) {
            throw new BusinessException("Transfer edilecek ürün seçilmelidir");
        }
        if (dto.getMiktar() == null || dto.getMiktar().signum() <= 0) {
            throw new BusinessException("Transfer miktarı 0'dan büyük olmalıdır");
        }
        // Talep aninda dogrulama: depolar ve stok bu sirkete ait olmali; kaynak depoda
        // yeterli miktar bulunmali. Aksi halde hata yalnizca onay aninda goruluyordu.
        var kaynak = depoRepository.findById(dto.getKaynakDepoId())
                .orElseThrow(() -> new ResourceNotFoundException("Depo", dto.getKaynakDepoId()));
        var hedef = depoRepository.findById(dto.getHedefDepoId())
                .orElseThrow(() -> new ResourceNotFoundException("Depo", dto.getHedefDepoId()));
        tenantChecker.check(kaynak.getSirketId(), "Depo");
        tenantChecker.check(hedef.getSirketId(), "Depo");
        var stok = stokRepository.findById(dto.getStokId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", dto.getStokId()));
        tenantChecker.check(stok.getSirketId(), "Stok");
        if (sirketId != null) {
            if (kaynak.getSirketId() != null && !sirketId.equals(kaynak.getSirketId())) {
                throw new ResourceNotFoundException("Depo bu sirkete ait degil");
            }
            if (hedef.getSirketId() != null && !sirketId.equals(hedef.getSirketId())) {
                throw new ResourceNotFoundException("Depo bu sirkete ait degil");
            }
            if (stok.getSirketId() != null && !sirketId.equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
        }
        java.math.BigDecimal kaynakMiktar = depoStokRepository
                .findByDepoIdAndStokIdForUpdate(dto.getKaynakDepoId(), dto.getStokId())
                .map(ds -> ds.getMiktar() != null ? ds.getMiktar() : java.math.BigDecimal.ZERO)
                .orElse(java.math.BigDecimal.ZERO);
        if (kaynakMiktar.compareTo(dto.getMiktar()) < 0) {
            throw new BusinessException("Kaynak depoda yeterli stok yok. Mevcut: " + kaynakMiktar
                    + ", istenen: " + dto.getMiktar());
        }
        DepoTransfer t = DepoTransfer.builder()
                .sirketId(sirketId)
                .kaynakDepoId(dto.getKaynakDepoId())
                .hedefDepoId(dto.getHedefDepoId())
                .stokId(dto.getStokId())
                .miktar(dto.getMiktar())
                .durum("BEKLIYOR")
                .aciklama(dto.getAciklama())
                .olusturanKullaniciId(kullaniciId)
                .build();
        return entityToDTO(transferRepository.save(t));
    }

    @Transactional
    public DepoTransferDTO onayla(Long id) {
        DepoTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
        tenantChecker.check(t.getSirketId(), "Transfer");
        if (!"BEKLIYOR".equals(t.getDurum())) {
            throw new BusinessException("Bu transfer zaten işlendi. Durum: " + t.getDurum());
        }
        depoService.stokTransfer(t.getKaynakDepoId(), t.getHedefDepoId(), t.getStokId(), t.getMiktar());
        t.setDurum("ONAYLANDI");
        t.setOnayTarihi(LocalDateTime.now());
        log.info("Depo transferi onaylandı - ID: {}", id);
        return entityToDTO(transferRepository.save(t));
    }

    @Transactional
    public DepoTransferDTO reddet(Long id) {
        DepoTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
        tenantChecker.check(t.getSirketId(), "Transfer");
        if (!"BEKLIYOR".equals(t.getDurum())) {
            throw new BusinessException("Bu transfer zaten işlendi. Durum: " + t.getDurum());
        }
        t.setDurum("REDDEDILDI");
        return entityToDTO(transferRepository.save(t));
    }

    private DepoTransferDTO entityToDTO(DepoTransfer t) {
        String kaynakAd = depoRepository.findById(t.getKaynakDepoId()).map(d -> d.getAd()).orElse(null);
        String hedefAd = depoRepository.findById(t.getHedefDepoId()).map(d -> d.getAd()).orElse(null);
        String stokAd = stokRepository.findById(t.getStokId()).map(s -> s.getAd()).orElse(null);
        return DepoTransferDTO.builder()
                .id(t.getId()).sirketId(t.getSirketId())
                .kaynakDepoId(t.getKaynakDepoId()).kaynakDepoAd(kaynakAd)
                .hedefDepoId(t.getHedefDepoId()).hedefDepoAd(hedefAd)
                .stokId(t.getStokId()).stokAd(stokAd)
                .miktar(t.getMiktar()).durum(t.getDurum()).aciklama(t.getAciklama())
                .olusturanKullaniciId(t.getOlusturanKullaniciId())
                .olusturmaTarihi(t.getOlusturmaTarihi()).onayTarihi(t.getOnayTarihi())
                .build();
    }
}
