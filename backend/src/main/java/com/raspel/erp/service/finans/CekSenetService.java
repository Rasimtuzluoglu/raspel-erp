package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.entity.finans.CekSenet;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.CekSenetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.stream.Collectors;
import com.raspel.erp.entity.sube.Sube;

@Service
@Transactional
@RequiredArgsConstructor
public class CekSenetService {

    private final CekSenetRepository cekSenetRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;
    private final HareketService hareketService;
    private final com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    private final com.raspel.erp.repository.finans.KasaHareketRepository kasaHareketRepository;
    private final com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    private final com.raspel.erp.repository.finans.BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;

    @Transactional(readOnly = true)
    public Page<CekSenetDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        Map<Long, String> cariHaritasi = cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getAd(), (a, b) -> a));
        return cekSenetRepository.findBySirketIdOrderByVadeTarihiAsc(sirketId, pageable)
                .map(cs -> entityToDTO(cs, cariHaritasi));
    }

    @Transactional(readOnly = true)
    public CekSenetDTO getir(Long id) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        return entityToDTO(cs);
    }

    public CekSenetDTO olustur(CekSenetDTO dto) {
        CekSenet cs = CekSenet.builder()
                .tur(dto.getTur()).cariHesapId(dto.getCariHesapId())
                .bankaAdi(dto.getBankaAdi()).sube(dto.getSube())
                .cekNo(dto.getCekNo()).hesapNo(dto.getHesapNo())
                .vadeTarihi(dto.getVadeTarihi()).tutar(dto.getTutar())
                .durum("PORTFOY").aciklama(dto.getAciklama())
                .sirketId(dto.getSirketId()).build();
        tenantChecker.checkSirketId(dto.getSirketId(), "Çek/Senet");
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public CekSenetDTO guncelle(Long id, CekSenetDTO dto) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        donemService.kilitKontrol(cs.getSirketId(),
                cs.getVadeTarihi() != null ? cs.getVadeTarihi() : java.time.LocalDate.now(), "çek/senet düzenleme");
        if ("TAHSIL_EDILDI".equals(cs.getDurum()) || "ODENDI".equals(cs.getDurum())) {
            throw new BusinessException("Tahsil edilmiş veya ödenmiş çek/senet kaydı doğrudan düzenlenemez");
        }
        cs.setTur(dto.getTur());
        cs.setCariHesapId(dto.getCariHesapId());
        cs.setBankaAdi(dto.getBankaAdi());
        cs.setSube(dto.getSube());
        cs.setCekNo(dto.getCekNo());
        cs.setHesapNo(dto.getHesapNo());
        cs.setVadeTarihi(dto.getVadeTarihi());
        cs.setKesinmeTarihi(dto.getKesinmeTarihi());
        cs.setTutar(dto.getTutar());
        if (dto.getDurum() != null) cs.setDurum(dto.getDurum());
        cs.setAciklama(dto.getAciklama());
        return entityToDTO(cekSenetRepository.save(cs));
    }

    public CekSenetDTO durumGuncelle(Long id, String durum) {
        return durumGuncelle(id, durum, null, null);
    }

    /**
     * Durum günceller. {@code TAHSIL_EDILDI} durumuna geçişte cari hesaba TAHSILAT hareketi
     * yazılır ve (seçildiyse) kasa/banka hesabına giriş işlenir; böylece tahsilat cari/kasa/
     * hareket kayıtlarına yansır. Tahsil edilmiş kayıt geri alınamaz.
     */
    public CekSenetDTO durumGuncelle(Long id, String durum, Long kasaId, Long bankaId) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        donemService.kilitKontrol(cs.getSirketId(),
                cs.getVadeTarihi() != null ? cs.getVadeTarihi() : java.time.LocalDate.now(), "çek/senet durum güncelleme");
        String eskiDurum = cs.getDurum();
        if ("TAHSIL_EDILDI".equals(eskiDurum) && !"TAHSIL_EDILDI".equals(durum)) {
            throw new BusinessException("Tahsil edilmiş çek/senet geri alınamaz");
        }
        boolean yeniTahsil = "TAHSIL_EDILDI".equals(durum) && !"TAHSIL_EDILDI".equals(eskiDurum);
        cs.setDurum(durum);
        CekSenet kaydedilen = cekSenetRepository.save(cs);
        if (yeniTahsil) {
            tahsilatiIsle(kaydedilen, kasaId, bankaId);
        }
        return entityToDTO(kaydedilen);
    }

    private void tahsilatiIsle(CekSenet cs, Long kasaId, Long bankaId) {
        if (cs.getTutar() == null || cs.getTutar().signum() <= 0) return;
        if (kasaId != null && bankaId != null) {
            throw new BusinessException("Aynı tahsilat hem kasaya hem bankaya işlenemez; tek hesap seçin");
        }
        if (cs.getCariHesapId() != null) {
            hareketService.hareketOlustur(com.raspel.erp.dto.finans.HareketDTO.builder()
                    .cariHesapId(cs.getCariHesapId())
                    .tur("TAHSILAT")
                    .tutar(cs.getTutar())
                    .hareketTarihi(java.time.LocalDate.now())
                    .odemeSekli("SENET".equals(cs.getTur()) ? "SENET" : "CEK")
                    .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                    .build(), cs.getSirketId());
        }
        if (kasaId != null) {
            kasaGirisi(kasaId, cs);
        } else if (bankaId != null) {
            bankaGirisi(bankaId, cs);
        }
    }

    private void kasaGirisi(Long kasaId, CekSenet cs) {
        com.raspel.erp.entity.finans.Kasa kasa = kasaRepository.findByIdForUpdate(kasaId)
                .orElseThrow(() -> new ResourceNotFoundException("Kasa", kasaId));
        tenantChecker.check(kasa.getSirketId(), "Kasa");
        kasa.setBakiye(kasa.getBakiye() != null ? kasa.getBakiye().add(cs.getTutar()) : cs.getTutar());
        kasaRepository.save(kasa);
        kasaHareketRepository.save(com.raspel.erp.entity.finans.KasaHareket.builder()
                .kasa(kasa).tur("GELIR").tutar(cs.getTutar())
                .hareketTarihi(java.time.LocalDate.now())
                .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                .build());
    }

    private void bankaGirisi(Long bankaId, CekSenet cs) {
        com.raspel.erp.entity.finans.Banka banka = bankaRepository.findByIdForUpdate(bankaId)
                .orElseThrow(() -> new ResourceNotFoundException("Banka", bankaId));
        tenantChecker.check(banka.getSirketId(), "Banka");
        banka.setBakiye(banka.getBakiye() != null ? banka.getBakiye().add(cs.getTutar()) : cs.getTutar());
        bankaRepository.save(banka);
        bankaHareketiRepository.save(com.raspel.erp.entity.finans.BankaHareketi.builder()
                .bankaId(banka.getId())
                .tarih(java.time.LocalDate.now())
                .aciklama("Çek/Senet tahsili" + (cs.getCekNo() != null ? " #" + cs.getCekNo() : ""))
                .borc(java.math.BigDecimal.ZERO)
                .alacak(cs.getTutar())
                .bakiye(banka.getBakiye())
                .eslestirildi(false)
                .sirketId(cs.getSirketId())
                .build());
    }

    public void sil(Long id) {
        CekSenet cs = cekSenetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cek/Senet", id));
        tenantChecker.check(cs.getSirketId(), "Cek/Senet");
        donemService.kilitKontrol(cs.getSirketId(),
                cs.getVadeTarihi() != null ? cs.getVadeTarihi() : java.time.LocalDate.now(), "çek/senet silme");
        if ("TAHSIL_EDILDI".equals(cs.getDurum()) || "ODENDI".equals(cs.getDurum())) {
            throw new BusinessException("Tahsil edilmiş veya ödenmiş çek/senet kaydı silinemez");
        }
        cekSenetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs) {
        return entityToDTO(cs, null);
    }

    @Transactional(readOnly = true)
    public CekSenetDTO entityToDTO(CekSenet cs, Map<Long, String> cariHaritasi) {
        String cariAdi = null;
        if (cs.getCariHesapId() != null) {
            if (cariHaritasi != null) {
                cariAdi = cariHaritasi.get(cs.getCariHesapId());
            }
            if (cariAdi == null) {
                cariAdi = cariHesapRepository.findById(cs.getCariHesapId())
                        .map(c -> c.getAd()).orElse(null);
            }
        }
        return CekSenetDTO.builder()
                .id(cs.getId()).tur(cs.getTur())
                .cariHesapId(cs.getCariHesapId())
                .cariHesapAdi(cariAdi)
                .bankaAdi(cs.getBankaAdi()).sube(cs.getSube())
                .cekNo(cs.getCekNo()).hesapNo(cs.getHesapNo())
                .vadeTarihi(cs.getVadeTarihi()).kesinmeTarihi(cs.getKesinmeTarihi())
                .tutar(cs.getTutar()).durum(cs.getDurum())
                .aciklama(cs.getAciklama()).sirketId(cs.getSirketId())
                .olusturmaTarihi(cs.getOlusturmaTarihi()).build();
    }
}