package com.raspel.erp.service.finans;

import com.raspel.erp.dto.finans.DovizKuruDTO;
import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.repository.finans.DovizKuruRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DovizKuruService {

    private final DovizKuruRepository dovizKuruRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dovizKurlari", key = "#tarih.toString()")
    public List<DovizKuruDTO> gunlukKurlariGetir(LocalDate tarih) {
        LocalDate hedefTarih = tarih != null ? tarih : LocalDate.now();
        List<DovizKuru> kurlar = dovizKuruRepository.findByTarihOrderByDovizKoduAsc(hedefTarih);

        if (kurlar.isEmpty() && hedefTarih.equals(LocalDate.now())) {
            kurlar = varsayilanKurlariOlustur(hedefTarih);
        }

        return kurlar.stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = "dovizKurlari", allEntries = true)
    public DovizKuruDTO kurEkleVeyaGuncelle(DovizKuruDTO dto) {
        LocalDate tarih = dto.getTarih() != null ? dto.getTarih() : LocalDate.now();
        DovizKuru kur = dovizKuruRepository.findByDovizKoduAndTarih(dto.getDovizKodu().toUpperCase(), tarih)
                .orElseGet(() -> DovizKuru.builder()
                        .dovizKodu(dto.getDovizKodu().toUpperCase())
                        .dovizAdi(dto.getDovizAdi())
                        .tarih(tarih)
                        .build());

        kur.setAlisKuru(dto.getAlisKuru());
        kur.setSatisKuru(dto.getSatisKuru());
        kur.setEfektifAlis(dto.getEfektifAlis() != null ? dto.getEfektifAlis() : dto.getAlisKuru());
        kur.setEfektifSatis(dto.getEfektifSatis() != null ? dto.getEfektifSatis() : dto.getSatisKuru());

        DovizKuru saved = dovizKuruRepository.save(kur);
        log.info("Döviz kuru kaydedildi: {} - {} (Alış: {}, Satış: {})", kur.getDovizKodu(), tarih, kur.getAlisKuru(), kur.getSatisKuru());
        return entityToDTO(saved);
    }

    private List<DovizKuru> varsayilanKurlariOlustur(LocalDate tarih) {
        List<DovizKuru> varsayilanlar = new ArrayList<>();
        varsayilanlar.add(DovizKuru.builder()
                .dovizKodu("USD").dovizAdi("ABD Doları").tarih(tarih)
                .alisKuru(BigDecimal.valueOf(32.50)).satisKuru(BigDecimal.valueOf(32.65))
                .efektifAlis(BigDecimal.valueOf(32.48)).efektifSatis(BigDecimal.valueOf(32.68))
                .build());
        varsayilanlar.add(DovizKuru.builder()
                .dovizKodu("EUR").dovizAdi("Euro").tarih(tarih)
                .alisKuru(BigDecimal.valueOf(35.20)).satisKuru(BigDecimal.valueOf(35.40))
                .efektifAlis(BigDecimal.valueOf(35.18)).efektifSatis(BigDecimal.valueOf(35.45))
                .build());
        varsayilanlar.add(DovizKuru.builder()
                .dovizKodu("GBP").dovizAdi("İngiliz Sterlini").tarih(tarih)
                .alisKuru(BigDecimal.valueOf(41.30)).satisKuru(BigDecimal.valueOf(41.55))
                .efektifAlis(BigDecimal.valueOf(41.25)).efektifSatis(BigDecimal.valueOf(41.60))
                .build());

        return dovizKuruRepository.saveAll(varsayilanlar);
    }

    private DovizKuruDTO entityToDTO(DovizKuru k) {
        return DovizKuruDTO.builder()
                .id(k.getId())
                .dovizKodu(k.getDovizKodu())
                .dovizAdi(k.getDovizAdi())
                .tarih(k.getTarih())
                .alisKuru(k.getAlisKuru())
                .satisKuru(k.getSatisKuru())
                .efektifAlis(k.getEfektifAlis())
                .efektifSatis(k.getEfektifSatis())
                .build();
    }
}
