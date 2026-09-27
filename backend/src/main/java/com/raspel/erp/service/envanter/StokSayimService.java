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
        private final com.raspel.erp.service.sube.DepoStokService depoStokService;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;

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
        tenantChecker.check(stok.getSirketId(), "Stok");
        if (sirketId != null && stok.getSirketId() != null && !sirketId.equals(stok.getSirketId())) {
            throw new ResourceNotFoundException("Stok bu sirkete ait degil");
        }
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
            tenantChecker.check(stok.getSirketId(), "Stok");
            if (sayim.getSirketId() != null && stok.getSirketId() != null && !sayim.getSirketId().equals(stok.getSirketId())) {
                throw new ResourceNotFoundException("Stok bu sirkete ait degil");
            }
            sayim.setStok(stok);
        }
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    public void sil(Long id) {
        StokSayim s = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(s.getSirketId(), "StokSayim");
        if ("TAMAMLANDI".equals(s.getDurum())) {
            throw new BusinessException("Tamamlanmış sayım doğrudan silinemez; önce iptal edin (stok geri alınır).");
        }
        stokSayimRepository.deleteById(id);
    }

    public StokSayimDTO durumGuncelle(Long id, String yeniDurum) {
        StokSayim sayim = stokSayimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StokSayim", id));
        tenantChecker.check(sayim.getSirketId(), "StokSayim");
        if (yeniDurum == null || !java.util.List.of("TASLAK", "TAMAMLANDI", "IPTAL").contains(yeniDurum)) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz durum: " + yeniDurum);
        }
        String eskiDurum = sayim.getDurum();

        // Idempotency: tamamlanmis sayim tekrar tamamlanamaz; aksi halde fark stoga
        // ikinci kez uygulanir (API'nin iki kez cagrilmasi durumu).
        if ("TAMAMLANDI".equals(eskiDurum) && "TAMAMLANDI".equals(yeniDurum)) {
            throw new BusinessException("Bu sayım zaten tamamlanmış.");
        }

        if ("TAMAMLANDI".equals(yeniDurum) && !"TAMAMLANDI".equals(eskiDurum) && sayim.getStok() != null) {
            Stok stok = sayimStoguKilitle(sayim);
            BigDecimal sayilan = nz(sayim.getSayilanMiktar());
            BigDecimal guncel = nz(stok.getMiktar());
            // Sayim, stogu SAYILAN degere esitler; arada yapilan satislar korunur.
            // Uygulanan fark (guncel -> sayilan) hareket olarak kaydedilir.
            BigDecimal uygulananFark = sayilan.subtract(guncel);
            stok.setMiktar(sayilan);
            stokRepository.save(stok);
            sayim.setFark(uygulananFark);
            if (uygulananFark.signum() != 0) {
                if (uygulananFark.signum() > 0) {
                    maliyetService.girisIsle(stok, guncel, uygulananFark, null, sayim.getSirketId(), "SAYIM", sayim.getId());
                } else {
                    maliyetService.cikisIsle(stok, uygulananFark.abs(), sayilan, sayim.getSirketId(), "SAYIM", sayim.getId());
                }
                Long depoId = depoStokService.coz(null, sayim.getSirketId());
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur(uygulananFark.signum() > 0 ? "GIRIS" : "CIKIS")
                        .miktar(uygulananFark.abs())
                        .hareketTarihi(java.time.LocalDate.now())
                        .aciklama("Stok sayımı #" + sayim.getId() + " farkı")
                        .depoId(depoId)
                        .kaynakTip("SAYIM")
                        .kaynakId(sayim.getId())
                        .build());
                depoStokService.guncelle(depoId, stok.getId(), uygulananFark);
                kritikStokBildirimiGonder(stok);
                cacheYardimci.temizle("stoklar", "dashboard");
            }
        } else if ("TAMAMLANDI".equals(eskiDurum) && !"TAMAMLANDI".equals(yeniDurum) && sayim.getStok() != null) {
            // Tamamlanmis sayimdan geri donus (IPTAL/TASLAK): uygulanan fark tersine cevrilir.
            Stok stok = sayimStoguKilitle(sayim);
            BigDecimal uygulananFark = nz(sayim.getFark());
            if (uygulananFark.signum() != 0) {
                BigDecimal guncel = nz(stok.getMiktar());
                BigDecimal yeniMiktar = guncel.subtract(uygulananFark);
                stok.setMiktar(yeniMiktar);
                stokRepository.save(stok);
                if (uygulananFark.signum() > 0) {
                    maliyetService.cikisIsle(stok, uygulananFark, yeniMiktar, sayim.getSirketId(), "SAYIM_IPTAL", sayim.getId());
                } else {
                    maliyetService.girisIsle(stok, guncel, uygulananFark.abs(), null, sayim.getSirketId(), "SAYIM_IPTAL", sayim.getId());
                }
                Long depoId = depoStokService.coz(null, sayim.getSirketId());
                stokHareketRepository.save(StokHareket.builder()
                        .stok(stok)
                        .tur(uygulananFark.signum() > 0 ? "CIKIS" : "GIRIS")
                        .miktar(uygulananFark.abs())
                        .hareketTarihi(java.time.LocalDate.now())
                        .aciklama("Stok sayımı #" + sayim.getId() + " geri alındı")
                        .depoId(depoId)
                        .kaynakTip("SAYIM_IPTAL")
                        .kaynakId(sayim.getId())
                        .build());
                depoStokService.guncelle(depoId, stok.getId(), uygulananFark.negate());
                kritikStokBildirimiGonder(stok);
                cacheYardimci.temizle("stoklar", "dashboard");
            }
            // Ters kayit uygulandi; tekrar uygulanmamasi icin fark sifirlanir.
            sayim.setFark(BigDecimal.ZERO);
        }
        sayim.setDurum(yeniDurum);
        return entityToDTO(stokSayimRepository.save(sayim));
    }

    /** Sayim stogunu kilitler ve tenant dogrulamasini yapar. */
    private Stok sayimStoguKilitle(StokSayim sayim) {
        Stok stok = stokRepository.findByIdForUpdate(sayim.getStok().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stok", sayim.getStok().getId()));
        tenantChecker.check(stok.getSirketId(), "Stok");
        if (sayim.getSirketId() != null && stok.getSirketId() != null && !sayim.getSirketId().equals(stok.getSirketId())) {
            throw new ResourceNotFoundException("Stok bu sirkete ait degil");
        }
        return stok;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
                Long sirketId = stok.getSirketId();
                String ad = stok.getAd();
                java.math.BigDecimal miktar = stok.getMiktar();
                java.math.BigDecimal minMiktar = stok.getMinMiktar();
                com.raspel.erp.support.AfterCommitExecutor.calistir(() -> bildirimService.bildirimGonder(sirketId, "STOK",
                        "Kritik Stok: " + ad,
                        "Stok miktarı (" + miktar + ") kritik seviyeye (" + minMiktar + ") düştü."));
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