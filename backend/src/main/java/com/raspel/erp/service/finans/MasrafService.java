package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.finans.KasaHareket;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Masraf yönetimi. Tutar KDV DAHİL kabul edilir; matrah/KDV ayrıştırılır.
 * Ödeme kasa/banka seçilirse masraf anında ilgili hesaptan çıkış işlenir ve
 * muhasebe fişi (770 + 191 / kasa-banka veya satıcılar) otomatik üretilir.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MasrafService {

    private final MasrafRepository masrafRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final com.raspel.erp.config.CacheYardimci cacheYardimci;
    private final KasaRepository kasaRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final BankaRepository bankaRepository;
    private final BankaHareketiRepository bankaHareketiRepository;
    private final com.raspel.erp.service.sistem.DonemService donemService;
    private final com.raspel.erp.service.muhasebe.OtomatikMuhasebeService otomatikMuhasebeService;

    @Transactional(readOnly = true)
    public Page<MasrafDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return masrafRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public MasrafDTO getir(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
        return entityToDTO(m);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public MasrafDTO olustur(MasrafDTO dto, Long sirketId) {
        if (dto.getTutar() == null || dto.getTutar().signum() <= 0) {
            throw new BusinessException("Masraf tutarı sıfırdan büyük olmalıdır");
        }
        if (dto.getKasaId() != null && dto.getBankaId() != null) {
            throw new BusinessException("Ödeme hem kasadan hem bankadan yapılamaz; tek hesap seçin");
        }
        LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : LocalDate.now();
        donemService.kilitKontrol(sirketId, tarih, "masraf oluşturma");

        BigDecimal kdvOrani = dto.getKdvOrani() != null ? dto.getKdvOrani() : BigDecimal.ZERO;
        var satir = com.raspel.erp.util.FaturaTutar.satir(dto.getTutar(), BigDecimal.ONE, BigDecimal.ZERO, kdvOrani);

        Masraf masraf = Masraf.builder()
                .tarih(tarih)
                .tutar(dto.getTutar())
                .matrah(satir.net())
                .kdvOrani(kdvOrani)
                .kdvTutar(satir.kdv())
                .aciklama(dto.getAciklama())
                .kategori(dto.getKategori())
                .cariHesapId(dto.getCariHesapId())
                .belgeNo(dto.getBelgeNo())
                .odemeYontemi(dto.getOdemeYontemi())
                .kasaId(dto.getKasaId())
                .bankaId(dto.getBankaId())
                .sirketId(sirketId)
                .build();
        Masraf kaydedilen = masrafRepository.save(masraf);

        odemeIsle(kaydedilen);
        otomatikMuhasebeService.masrafIsle(kaydedilen);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kaydedilen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public MasrafDTO guncelle(Long id, MasrafDTO dto) {
        Masraf masraf = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(masraf.getSirketId(), "Masraf");
        if (dto.getTarih() != null) masraf.setTarih(dto.getTarih());
        if (dto.getTutar() != null) masraf.setTutar(dto.getTutar());
        if (dto.getAciklama() != null) masraf.setAciklama(dto.getAciklama());
        if (dto.getKategori() != null) masraf.setKategori(dto.getKategori());
        if (dto.getCariHesapId() != null) masraf.setCariHesapId(dto.getCariHesapId());
        if (dto.getBelgeNo() != null) masraf.setBelgeNo(dto.getBelgeNo());
        // KDV yeniden ayrıştırılır (tutar KDV dahil).
        BigDecimal kdvOrani = dto.getKdvOrani() != null ? dto.getKdvOrani() : masraf.getKdvOrani();
        if (kdvOrani == null) kdvOrani = BigDecimal.ZERO;
        var satir = com.raspel.erp.util.FaturaTutar.satir(masraf.getTutar(), BigDecimal.ONE, BigDecimal.ZERO, kdvOrani);
        masraf.setKdvOrani(kdvOrani);
        masraf.setMatrah(satir.net());
        masraf.setKdvTutar(satir.kdv());
        Masraf kaydedilen = masrafRepository.save(masraf);
        // Tutar/KDV değişmiş olabilir: eski fiş iptal edilip yenisi üretilir.
        otomatikMuhasebeService.kaynakFisIptal(kaydedilen.getSirketId(),
                com.raspel.erp.service.muhasebe.OtomatikMuhasebeService.KAYNAK_MASRAF, kaydedilen.getId());
        otomatikMuhasebeService.masrafIsle(kaydedilen);
        cacheYardimci.commitSonrasiTemizle("dashboard");
        return entityToDTO(kaydedilen);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Masraf m = masrafRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Masraf", id));
        tenantChecker.check(m.getSirketId(), "Masraf");
        auditLogService.finansalSilmeLog("Masraf", id,
                "Masraf silindi: " + m.getTutar() + " TL - " + (m.getAciklama() != null ? m.getAciklama() : "")
                        + (m.getTarih() != null ? " (" + m.getTarih() + ")" : ""));
        odemeTersineCevir(m);
        otomatikMuhasebeService.kaynakFisIptal(m.getSirketId(),
                com.raspel.erp.service.muhasebe.OtomatikMuhasebeService.KAYNAK_MASRAF, m.getId());
        masrafRepository.deleteById(id);
        cacheYardimci.commitSonrasiTemizle("dashboard");
    }

    /** Masraf ödemesi: seçilen kasa/banka hesabından çıkış işlenir. */
    private void odemeIsle(Masraf m) {
        BigDecimal tutar = m.getTutar();
        if (tutar == null || tutar.signum() <= 0) return;
        if (m.getKasaId() != null) {
            Kasa kasa = kasaRepository.findByIdForUpdate(m.getKasaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kasa", m.getKasaId()));
            tenantChecker.check(kasa.getSirketId(), "Kasa");
            BigDecimal bakiye = kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO;
            if (bakiye.compareTo(tutar) < 0) {
                throw new BusinessException("Kasada yetersiz bakiye. Mevcut: " + bakiye + " ₺");
            }
            kasa.setBakiye(bakiye.subtract(tutar));
            kasaRepository.save(kasa);
            kasaHareketRepository.save(KasaHareket.builder()
                    .kasa(kasa).tur("GIDER").tutar(tutar)
                    .hareketTarihi(m.getTarih() != null ? m.getTarih() : LocalDate.now())
                    .aciklama("Masraf" + (m.getBelgeNo() != null ? " #" + m.getBelgeNo() : "")
                            + (m.getAciklama() != null ? " - " + m.getAciklama() : ""))
                    .build());
        } else if (m.getBankaId() != null) {
            Banka banka = bankaRepository.findByIdForUpdate(m.getBankaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Banka", m.getBankaId()));
            tenantChecker.check(banka.getSirketId(), "Banka");
            BigDecimal bakiye = banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO;
            if (bakiye.compareTo(tutar) < 0) {
                throw new BusinessException("Bankada yetersiz bakiye. Mevcut: " + bakiye + " ₺");
            }
            banka.setBakiye(bakiye.subtract(tutar));
            bankaRepository.save(banka);
            bankaHareketiRepository.save(BankaHareketi.builder()
                    .bankaId(banka.getId())
                    .tarih(m.getTarih() != null ? m.getTarih() : LocalDate.now())
                    .aciklama("Masraf" + (m.getBelgeNo() != null ? " #" + m.getBelgeNo() : "")
                            + (m.getAciklama() != null ? " - " + m.getAciklama() : ""))
                    .borc(tutar)
                    .alacak(BigDecimal.ZERO)
                    .bakiye(banka.getBakiye())
                    .eslestirildi(false)
                    .sirketId(m.getSirketId())
                    .kaynakTip("MASRAF")
                    .build());
        }
    }

    /** Masraf silinince yapılan ödeme kasa/banka hesabına geri eklenir. */
    private void odemeTersineCevir(Masraf m) {
        BigDecimal tutar = m.getTutar();
        if (tutar == null || tutar.signum() <= 0) return;
        if (m.getKasaId() != null) {
            Kasa kasa = kasaRepository.findByIdForUpdate(m.getKasaId()).orElse(null);
            if (kasa == null) return;
            kasa.setBakiye((kasa.getBakiye() != null ? kasa.getBakiye() : BigDecimal.ZERO).add(tutar));
            kasaRepository.save(kasa);
            kasaHareketRepository.save(KasaHareket.builder()
                    .kasa(kasa).tur("GELIR").tutar(tutar)
                    .hareketTarihi(LocalDate.now())
                    .aciklama("Masraf silindi (geri alındı) #" + m.getId())
                    .build());
        } else if (m.getBankaId() != null) {
            Banka banka = bankaRepository.findByIdForUpdate(m.getBankaId()).orElse(null);
            if (banka == null) return;
            banka.setBakiye((banka.getBakiye() != null ? banka.getBakiye() : BigDecimal.ZERO).add(tutar));
            bankaRepository.save(banka);
            bankaHareketiRepository.save(BankaHareketi.builder()
                    .bankaId(banka.getId())
                    .tarih(LocalDate.now())
                    .aciklama("Masraf silindi (geri alındı) #" + m.getId())
                    .borc(BigDecimal.ZERO)
                    .alacak(tutar)
                    .bakiye(banka.getBakiye())
                    .eslestirildi(false)
                    .sirketId(m.getSirketId())
                    .kaynakTip("MASRAF_IPTAL")
                    .build());
        }
    }

    private MasrafDTO entityToDTO(Masraf m) {
        return MasrafDTO.builder()
                .id(m.getId()).tarih(m.getTarih()).tutar(m.getTutar())
                .aciklama(m.getAciklama()).kategori(m.getKategori())
                .cariHesapId(m.getCariHesapId()).belgeNo(m.getBelgeNo())
                .kdvOrani(m.getKdvOrani()).kdvTutar(m.getKdvTutar()).matrah(m.getMatrah())
                .odemeYontemi(m.getOdemeYontemi()).kasaId(m.getKasaId()).bankaId(m.getBankaId())
                .sirketId(m.getSirketId()).olusturmaTarihi(m.getOlusturmaTarihi())
                .build();
    }
}
