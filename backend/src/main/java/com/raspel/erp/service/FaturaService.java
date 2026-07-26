package com.raspel.erp.service;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.dto.FaturaKalemDTO;
import com.raspel.erp.entity.*;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.*;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final CariHesapRepository cariHesapRepository;
    private final StokRepository stokRepository;
    private final StokHareketRepository stokHareketRepository;

    @Transactional(readOnly = true)
    public Page<FaturaDTO> tumFaturalariGetir(Long sirketId, Pageable pageable) {
        return faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, pageable)
                .map(this::entityDTOyeCevir);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "faturalar", key = "#id")
    public FaturaDTO faturaGetir(Long id) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        return entityDTOyeCevir(fatura);
    }

    public FaturaDTO faturaOlustur(FaturaDTO dto, Long sirketId) {
        log.info("Fatura oluşturuluyor - Tür: {}, sirketId: {}", dto.getTur(), sirketId);

        CariHesap cariHesap = null;
        if (dto.getCariHesapId() != null) {
            cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", dto.getCariHesapId()));
        }

        Fatura.FaturaTur tur;
        try {
            tur = Fatura.FaturaTur.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz fatura türü: " + dto.getTur());
        }

        String faturaNo = "FTR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                + "-" + String.format("%03d", (int)(Math.random() * 900) + 100);

        List<FaturaKalem> kalemler = dto.getKalemler().stream().map(k -> {
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.valueOf(20);
            BigDecimal iskontoOrani = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
            BigDecimal brütTutar = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
            BigDecimal iskontoTutari = brütTutar.multiply(iskontoOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal netTutar = brütTutar.subtract(iskontoTutari);
            BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal kalemTutar = netTutar.add(kdvTutari);

            return FaturaKalem.builder()
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(kdvOrani)
                    .iskontoOrani(iskontoOrani)
                    .tutar(kalemTutar)
                    .stokId(k.getStokId())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal araToplam = kalemler.stream()
                .map(k -> {
                    BigDecimal brüt = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
                    BigDecimal iskonto = brüt.multiply(k.getIskontoOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return brüt.subtract(iskonto);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal kdv = kalemler.stream()
                .map(k -> {
                    BigDecimal brüt = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
                    BigDecimal iskonto = brüt.multiply(k.getIskontoOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal net = brüt.subtract(iskonto);
                    return net.multiply(k.getKdvOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal genelIskonto = dto.getGenelIskontoTutari() != null ? dto.getGenelIskontoTutari() : BigDecimal.ZERO;
        BigDecimal genelToplam = araToplam.add(kdv).subtract(genelIskonto);
        if (genelToplam.compareTo(BigDecimal.ZERO) < 0) genelToplam = BigDecimal.ZERO;

        BigDecimal odenenTutar = dto.getOdenenTutar() != null ? dto.getOdenenTutar() : BigDecimal.ZERO;
        BigDecimal kalanTutar = genelToplam.subtract(odenenTutar);
        String odemeDurumu = dto.getOdemeDurumu();
        if (odemeDurumu == null) {
            odemeDurumu = kalanTutar.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                    : odenenTutar.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI";
        }

        Fatura.FaturaDurum faturaDurum;
        try {
            faturaDurum = dto.getDurum() != null
                    ? Fatura.FaturaDurum.valueOf(dto.getDurum().toUpperCase())
                    : Fatura.FaturaDurum.TASLAK;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz durum: " + dto.getDurum());
        }

        Fatura fatura = Fatura.builder()
                .faturaNumarasi(faturaNo)
                .tarih(dto.getTarih() != null ? dto.getTarih() : LocalDate.now())
                .tur(tur)
                .durum(faturaDurum)
                .cariHesap(cariHesap)
                .aciklama(dto.getAciklama())
                .araToplam(araToplam)
                .kdv(kdv)
                .genelToplam(genelToplam)
                .genelIskontoTutari(genelIskonto)
                .odemeDurumu(odemeDurumu)
                .odenenTutar(odenenTutar)
                .kalanTutar(kalanTutar)
                .sirketId(sirketId)
                .build();

        kalemler.forEach(k -> k.setFatura(fatura));
        fatura.setKalemler(kalemler);

        Fatura kaydedilen = faturaRepository.save(fatura);

        if (faturaDurum == Fatura.FaturaDurum.KESILDI) {
            stokHareketleriIsle(fatura, "CIKIS", "Fatura #" + fatura.getFaturaNumarasi());
        }

        log.info("Fatura oluşturuldu - No: {}, ID: {}", faturaNo, kaydedilen.getId());
        return entityDTOyeCevir(kaydedilen);
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public FaturaDTO faturaDurumGuncelle(Long id, String yeniDurum) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));

        Fatura.FaturaDurum durum;
        try {
            durum = Fatura.FaturaDurum.valueOf(yeniDurum.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz durum: " + yeniDurum);
        }

        if (fatura.getDurum() == Fatura.FaturaDurum.IPTAL) {
            throw new BusinessException("İptal edilmiş fatura güncellenemez");
        }

        if (durum == Fatura.FaturaDurum.KESILDI && fatura.getDurum() != Fatura.FaturaDurum.KESILDI) {
            stokHareketleriIsle(fatura, "CIKIS", "Fatura #" + fatura.getFaturaNumarasi());
        } else if (durum == Fatura.FaturaDurum.IPTAL && fatura.getDurum() == Fatura.FaturaDurum.KESILDI) {
            stokHareketleriIsle(fatura, "GIRIS", "Fatura iptal #" + fatura.getFaturaNumarasi());
        }

        fatura.setDurum(durum);
        Fatura guncellenen = faturaRepository.save(fatura);
        log.info("Fatura durumu güncellendi - ID: {}, Durum: {}", id, durum);
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public FaturaDTO faturaGuncelle(Long id, FaturaDTO dto) {
        log.info("Fatura düzenleniyor - ID: {}", id);
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));

        if (fatura.getDurum() != Fatura.FaturaDurum.TASLAK) {
            throw new BusinessException("Yalnızca taslak faturalar düzenlenebilir. Güncel durum: " + fatura.getDurum());
        }

        CariHesap cariHesap = null;
        if (dto.getCariHesapId() != null) {
            cariHesap = cariHesapRepository.findById(dto.getCariHesapId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cari hesap", dto.getCariHesapId()));
        }

        Fatura.FaturaTur tur;
        try {
            tur = Fatura.FaturaTur.valueOf(dto.getTur().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz fatura türü: " + dto.getTur());
        }

        List<FaturaKalem> yeniKalemler = dto.getKalemler().stream().map(k -> {
            BigDecimal kdvOrani = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.valueOf(20);
            BigDecimal iskontoOrani = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
            BigDecimal brütTutar = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
            BigDecimal iskontoTutari = brütTutar.multiply(iskontoOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal netTutar = brütTutar.subtract(iskontoTutari);
            BigDecimal kdvTutari = netTutar.multiply(kdvOrani).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal kalemTutar = netTutar.add(kdvTutari);
            return FaturaKalem.builder()
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(kdvOrani)
                    .iskontoOrani(iskontoOrani)
                    .tutar(kalemTutar)
                    .stokId(k.getStokId())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal araToplam = yeniKalemler.stream()
                .map(k -> {
                    BigDecimal brüt = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
                    BigDecimal iskonto = brüt.multiply(k.getIskontoOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return brüt.subtract(iskonto);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal kdv = yeniKalemler.stream()
                .map(k -> {
                    BigDecimal brüt = k.getBirimFiyat().multiply(BigDecimal.valueOf(k.getAdet()));
                    BigDecimal iskonto = brüt.multiply(k.getIskontoOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal net = brüt.subtract(iskonto);
                    return net.multiply(k.getKdvOrani()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal genelIskonto = dto.getGenelIskontoTutari() != null ? dto.getGenelIskontoTutari() : BigDecimal.ZERO;
        BigDecimal genelToplam = araToplam.add(kdv).subtract(genelIskonto);
        if (genelToplam.compareTo(BigDecimal.ZERO) < 0) genelToplam = BigDecimal.ZERO;

        BigDecimal odenenTutar = dto.getOdenenTutar() != null ? dto.getOdenenTutar() : BigDecimal.ZERO;
        BigDecimal kalanTutar = genelToplam.subtract(odenenTutar);
        String odemeDurumu = dto.getOdemeDurumu();
        if (odemeDurumu == null) {
            odemeDurumu = kalanTutar.compareTo(BigDecimal.ZERO) <= 0 ? "ODENDI"
                    : odenenTutar.compareTo(BigDecimal.ZERO) > 0 ? "KISMI_ODENDI" : "ODENMEDI";
        }

        fatura.setCariHesap(cariHesap);
        fatura.setTur(tur);
        fatura.setTarih(dto.getTarih() != null ? dto.getTarih() : fatura.getTarih());
        fatura.setAciklama(dto.getAciklama());
        fatura.setAraToplam(araToplam);
        fatura.setKdv(kdv);
        fatura.setGenelToplam(genelToplam);
        fatura.setGenelIskontoTutari(genelIskonto);
        fatura.setOdemeDurumu(odemeDurumu);
        fatura.setOdenenTutar(odenenTutar);
        fatura.setKalanTutar(kalanTutar);

        fatura.getKalemler().clear();
        yeniKalemler.forEach(k -> k.setFatura(fatura));
        fatura.getKalemler().addAll(yeniKalemler);

        Fatura guncellenen = faturaRepository.save(fatura);
        log.info("Fatura düzenlendi - ID: {}, No: {}", id, guncellenen.getFaturaNumarasi());
        return entityDTOyeCevir(guncellenen);
    }

    @CacheEvict(value = "faturalar", allEntries = true)
    public void faturaSil(Long id) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", id));
        if (fatura.getDurum() == Fatura.FaturaDurum.KESILDI) {
            throw new BusinessException("Kesilmiş fatura silinemez");
        }
        faturaRepository.deleteById(id);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    private List<Long> stokHareketleriIsle(Fatura fatura, String tur, String aciklama) {
        List<Long> kritikStokIds = new ArrayList<>();
        for (FaturaKalem k : fatura.getKalemler()) {
            if (k.getStokId() == null) continue;
            Stok stok = stokRepository.findById(k.getStokId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stok", k.getStokId()));
            if ("CIKIS".equals(tur)) {
                BigDecimal adet = BigDecimal.valueOf(k.getAdet());
                if (stok.getMiktar().compareTo(adet) < 0)
                    throw new BusinessException("Yetersiz stok! Ürün: " + stok.getAd()
                            + ", Mevcut: " + stok.getMiktar() + ", İstenen: " + adet);
                stok.setMiktar(stok.getMiktar().subtract(adet));
            } else {
                stok.setMiktar(stok.getMiktar().add(BigDecimal.valueOf(k.getAdet())));
            }
            stokRepository.save(stok);

            if (stok.getMinMiktar() != null && stok.getMiktar().compareTo(stok.getMinMiktar()) < 0) {
                log.warn("Kritik stok seviyesi! {} - Mevcut: {}, Minimum: {}", stok.getAd(), stok.getMiktar(), stok.getMinMiktar());
                kritikStokIds.add(stok.getId());
            }

            StokHareket h = StokHareket.builder()
                    .stok(stok).tur(tur)
                    .miktar(BigDecimal.valueOf(k.getAdet()))
                    .hareketTarihi(LocalDate.now())
                    .aciklama(aciklama)
                    .cariHesap(fatura.getCariHesap())
                    .build();
            stokHareketRepository.save(h);
        }
        return kritikStokIds;
    }

    private FaturaDTO entityDTOyeCevir(Fatura fatura) {
        Map<Long, Stok> stokHaritasi = stokRepository.findAllById(
                fatura.getKalemler().stream()
                        .map(FaturaKalem::getStokId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Stok::getId, s -> s, (s1, s2) -> s1));

        List<FaturaKalemDTO> kalemDTO = fatura.getKalemler().stream().map(k -> {
            String stokAd = null;
            String stokKodu = null;
            if (k.getStokId() != null && stokHaritasi.containsKey(k.getStokId())) {
                Stok s = stokHaritasi.get(k.getStokId());
                stokAd = s.getAd();
                stokKodu = s.getStokKodu();
            }
            return FaturaKalemDTO.builder()
                    .id(k.getId())
                    .aciklama(k.getAciklama())
                    .adet(k.getAdet())
                    .birimFiyat(k.getBirimFiyat())
                    .kdvOrani(k.getKdvOrani())
                    .iskontoOrani(k.getIskontoOrani())
                    .tutar(k.getTutar())
                    .stokId(k.getStokId())
                    .stokAd(stokAd)
                    .stokKodu(stokKodu)
                    .build();
        }).collect(Collectors.toList());

        return FaturaDTO.builder()
                .id(fatura.getId())
                .faturaNumarasi(fatura.getFaturaNumarasi())
                .tarih(fatura.getTarih())
                .tur(fatura.getTur().name())
                .durum(fatura.getDurum().name())
                .cariHesapId(fatura.getCariHesap() != null ? fatura.getCariHesap().getId() : null)
                .cariHesapAd(fatura.getCariHesap() != null ? fatura.getCariHesap().getAd() : null)
                .aciklama(fatura.getAciklama())
                .araToplam(fatura.getAraToplam())
                .kdv(fatura.getKdv())
                .genelToplam(fatura.getGenelToplam())
                .genelIskontoTutari(fatura.getGenelIskontoTutari())
                .odemeDurumu(fatura.getOdemeDurumu())
                .odenenTutar(fatura.getOdenenTutar())
                .kalanTutar(fatura.getKalanTutar())
                .kalemler(kalemDTO)
                .olusturmaTarihi(fatura.getOlusturmaTarihi())
                .build();
    }
}
