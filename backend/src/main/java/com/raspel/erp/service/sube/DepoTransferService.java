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
    private final com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;

    @org.springframework.beans.factory.annotation.Value("${app.depo.transfer-sla-saat:24}")
    private long slaSaat;

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

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DepoTransferDTO> sayfali(
            Long sirketId, Long depoId, String durum,
            java.time.LocalDate baslangic, java.time.LocalDate bitis,
            org.springframework.data.domain.Pageable pageable) {
        // PostgreSQL parametre tipini `? IS NULL` uzerinden cozemedigi icin
        // (SQLState 42P18) tarih araligi daima dolu gonderilir.
        java.time.LocalDateTime bas = baslangic != null
                ? baslangic.atStartOfDay()
                : java.time.LocalDateTime.of(1970, 1, 1, 0, 0);
        java.time.LocalDateTime bit = bitis != null
                ? bitis.plusDays(1).atStartOfDay()
                : java.time.LocalDateTime.of(9999, 12, 31, 0, 0);
        String durumFiltre = (durum == null || durum.isBlank() || "TUMU".equals(durum)) ? null : durum;
        var sayfa = transferRepository.filtreli(sirketId, depoId, durumFiltre, bas, bit, pageable);
        var depolar = depoRepository.findBySirketIdOrderByAdAsc(sirketId,
                        org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getAd()));
        var stoklar = sayfa.getContent().stream().map(DepoTransfer::getStokId).distinct()
                .map(id -> stokRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getAd()));
        java.util.Set<Long> kullaniciIdleri = new java.util.HashSet<>();
        sayfa.getContent().forEach(t -> {
            if (t.getOlusturanKullaniciId() != null) kullaniciIdleri.add(t.getOlusturanKullaniciId());
            if (t.getOnaylayanKullaniciId() != null) kullaniciIdleri.add(t.getOnaylayanKullaniciId());
        });
        var adlar = kullaniciIdleri.isEmpty() ? java.util.Map.<Long, String>of()
                : kullaniciRepository.findAllById(kullaniciIdleri).stream()
                        .collect(Collectors.toMap(k -> k.getId(), k -> k.getDisplayName(), (a, b) -> a));
        return sayfa.map(t -> entityToDTO(t, depolar, stoklar, adlar));
    }

    @Transactional
    public DepoTransferDTO onayla(Long id) {
        return onayla(id, null);
    }

    @Transactional
    public DepoTransferDTO onayla(Long id, Long kullaniciId) {
        DepoTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
        tenantChecker.check(t.getSirketId(), "Transfer");
        yoneticiKontrol(kullaniciId);
        if (!"BEKLIYOR".equals(t.getDurum())) {
            throw new BusinessException("Bu transfer zaten işlendi. Durum: " + t.getDurum());
        }
        depoService.stokTransfer(t.getKaynakDepoId(), t.getHedefDepoId(), t.getStokId(), t.getMiktar());
        t.setDurum("ONAYLANDI");
        t.setOnayTarihi(LocalDateTime.now());
        if (kullaniciId != null) t.setOnaylayanKullaniciId(kullaniciId);
        log.info("Depo transferi onaylandı - ID: {}", id);
        return entityToDTO(transferRepository.save(t));
    }

    @Transactional
    public DepoTransferDTO reddet(Long id) {
        return reddet(id, null, "Belirtilmedi");
    }

    @Transactional
    public DepoTransferDTO reddet(Long id, Long kullaniciId, String sebep) {
        DepoTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
        tenantChecker.check(t.getSirketId(), "Transfer");
        yoneticiKontrol(kullaniciId);
        if (!"BEKLIYOR".equals(t.getDurum())) {
            throw new BusinessException("Bu transfer zaten işlendi. Durum: " + t.getDurum());
        }
        if (sebep == null || sebep.trim().length() < 3) {
            throw new BusinessException("Red gerekçesi en az 3 karakter olmalıdır");
        }
        t.setDurum("REDDEDILDI");
        t.setRedTarihi(LocalDateTime.now());
        t.setRedSebebi(sebep.trim());
        if (kullaniciId != null) t.setOnaylayanKullaniciId(kullaniciId);
        return entityToDTO(transferRepository.save(t));
    }

    /** SLA kovasi: NORMAL <4s, YAKLASIYOR 4..slaSaat, GECIKTI > slaSaat. */
    public String slaDurumu(LocalDateTime olusturma) {
        if (olusturma == null) return "NORMAL";
        long saat = java.time.Duration.between(olusturma, LocalDateTime.now()).toHours();
        if (saat < 4) return "NORMAL";
        return saat < slaSaat ? "YAKLASIYOR" : "GECIKTI";
    }

    private void yoneticiKontrol(Long kullaniciId) {
        if (kullaniciId == null) return;
        boolean admin = kullaniciRepository.findById(kullaniciId)
                .map(k -> "ADMIN".equalsIgnoreCase(k.getRole())).orElse(false);
        if (!admin) throw new BusinessException("Transferi yalnızca ADMIN onaylayabilir");
    }

    private DepoTransferDTO entityToDTO(DepoTransfer t) {
        return entityToDTO(t, java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
    }

    private DepoTransferDTO entityToDTO(DepoTransfer t, java.util.Map<Long, String> depoAdlari,
                                        java.util.Map<Long, String> stokAdlari,
                                        java.util.Map<Long, String> kullaniciAdlari) {
        String kaynakAd = depoAdlari.containsKey(t.getKaynakDepoId()) ? depoAdlari.get(t.getKaynakDepoId())
                : depoRepository.findById(t.getKaynakDepoId()).map(d -> d.getAd()).orElse(null);
        String hedefAd = depoAdlari.containsKey(t.getHedefDepoId()) ? depoAdlari.get(t.getHedefDepoId())
                : depoRepository.findById(t.getHedefDepoId()).map(d -> d.getAd()).orElse(null);
        String stokAd = stokAdlari.containsKey(t.getStokId()) ? stokAdlari.get(t.getStokId())
                : stokRepository.findById(t.getStokId()).map(s -> s.getAd()).orElse(null);
        long yasSaat = t.getOlusturmaTarihi() == null ? 0
                : Math.max(0, java.time.Duration.between(t.getOlusturmaTarihi(), LocalDateTime.now()).toHours());
        return DepoTransferDTO.builder()
                .id(t.getId()).sirketId(t.getSirketId())
                .kaynakDepoId(t.getKaynakDepoId()).kaynakDepoAd(kaynakAd)
                .hedefDepoId(t.getHedefDepoId()).hedefDepoAd(hedefAd)
                .stokId(t.getStokId()).stokAd(stokAd)
                .miktar(t.getMiktar()).durum(t.getDurum()).aciklama(t.getAciklama())
                .olusturanKullaniciId(t.getOlusturanKullaniciId())
                .olusturanKullaniciAd(t.getOlusturanKullaniciId() != null
                        ? kullaniciAdlari.get(t.getOlusturanKullaniciId()) : null)
                .onaylayanKullaniciId(t.getOnaylayanKullaniciId())
                .onaylayanKullaniciAd(t.getOnaylayanKullaniciId() != null
                        ? kullaniciAdlari.get(t.getOnaylayanKullaniciId()) : null)
                .olusturmaTarihi(t.getOlusturmaTarihi()).onayTarihi(t.getOnayTarihi())
                .redTarihi(t.getRedTarihi()).redSebebi(t.getRedSebebi())
                .yasSaat(yasSaat).slaDurumu(slaDurumu(t.getOlusturmaTarihi()))
                .build();
    }
}
