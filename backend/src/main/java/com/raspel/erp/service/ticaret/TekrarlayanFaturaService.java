package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.FaturaKalemDTO;
import com.raspel.erp.dto.ticaret.TekrarlayanFaturaDTO;
import com.raspel.erp.entity.ticaret.TekrarlayanFatura;
import com.raspel.erp.entity.ticaret.TekrarlayanFaturaKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.TekrarlayanFaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tekrarlayan (periyodik) fatura yönetimi. Tanımlanan periyoda göre otomatik
 * fatura keser. Günlük 05:00'te vadesi gelen tanımlar işlenir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TekrarlayanFaturaService {

    private final TekrarlayanFaturaRepository repository;
    private final FaturaService faturaService;
    private final CariHesapRepository cariHesapRepository;

    @Lazy
    @Autowired
    private TekrarlayanFaturaService self;

    // ---------- CRUD ----------

    @Transactional(readOnly = true)
    public List<TekrarlayanFaturaDTO> listele(Long sirketId) {
        return repository.findBySirketIdOrderByIdDesc(sirketId).stream()
                .map(this::entityToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TekrarlayanFaturaDTO getir(Long id) {
        return entityToDTO(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tekrarlayan fatura", id)));
    }

    @Transactional
    public TekrarlayanFaturaDTO olustur(TekrarlayanFaturaDTO dto, Long sirketId) {
        TekrarlayanFatura tf = TekrarlayanFatura.builder()
                .sirketId(sirketId)
                .cariHesapId(dto.getCariHesapId())
                .tur(dto.getTur() != null ? dto.getTur().toUpperCase() : "SATIS")
                .aciklama(dto.getAciklama())
                .periyot(dto.getPeriyot().toUpperCase())
                .baslangicTarihi(dto.getBaslangicTarihi())
                .bitisTarihi(dto.getBitisTarihi())
                .sonrakiCalistirma(dto.getSonrakiCalistirma() != null ? dto.getSonrakiCalistirma() : dto.getBaslangicTarihi())
                .aktif(dto.getAktif() != null ? dto.getAktif() : true)
                .build();
        tf.setKalemler(dto.getKalemler().stream().map(k -> kalemDTOToEntity(k, tf)).collect(Collectors.toList()));
        return entityToDTO(repository.save(tf));
    }

    @Transactional
    public TekrarlayanFaturaDTO guncelle(Long id, TekrarlayanFaturaDTO dto) {
        TekrarlayanFatura tf = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tekrarlayan fatura", id));
        tf.setCariHesapId(dto.getCariHesapId());
        tf.setTur(dto.getTur() != null ? dto.getTur().toUpperCase() : tf.getTur());
        tf.setAciklama(dto.getAciklama());
        tf.setPeriyot(dto.getPeriyot() != null ? dto.getPeriyot().toUpperCase() : tf.getPeriyot());
        tf.setBaslangicTarihi(dto.getBaslangicTarihi());
        tf.setBitisTarihi(dto.getBitisTarihi());
        if (dto.getAktif() != null) tf.setAktif(dto.getAktif());
        if (dto.getKalemler() != null && !dto.getKalemler().isEmpty()) {
            tf.getKalemler().clear();
            tf.getKalemler().addAll(dto.getKalemler().stream()
                    .map(k -> kalemDTOToEntity(k, tf)).collect(Collectors.toList()));
        }
        return entityToDTO(repository.save(tf));
    }

    @Transactional
    public void sil(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Tekrarlayan fatura", id);
        repository.deleteById(id);
    }

    // ---------- OTOMATİK İŞLEME ----------

    @Scheduled(cron = "0 0 5 * * *")
    public void vadesiGelenleriIsle() {
        List<TekrarlayanFatura> vadesiGelen = repository.findByAktifTrueAndSonrakiCalistirmaLessThanEqual(LocalDate.now());
        log.info("Tekrarlayan fatura taraması: {} tanım vadesi geldi", vadesiGelen.size());
        for (TekrarlayanFatura tf : vadesiGelen) {
            try {
                self.faturaUret(tf.getId());
            } catch (Exception e) {
                log.warn("Tekrarlayan fatura üretilemedi (ID: {}): {}", tf.getId(), e.getMessage());
            }
        }
    }

    /** Vadesi gelen bir tanım için fatura keser ve sonraki çalışma tarihini ilerletir. */
    @Transactional
    public void faturaUret(Long id) {
        TekrarlayanFatura tf = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tekrarlayan fatura", id));
        if (tf.getBitisTarihi() != null && tf.getSonrakiCalistirma() != null
                && tf.getSonrakiCalistirma().isAfter(tf.getBitisTarihi())) {
            tf.setAktif(false);
            repository.save(tf);
            return;
        }

        List<FaturaKalemDTO> kalemler = tf.getKalemler().stream().map(k -> FaturaKalemDTO.builder()
                .aciklama(k.getAciklama())
                .adet(k.getAdet())
                .birimFiyat(k.getBirimFiyat())
                .kdvOrani(k.getKdvOrani())
                .iskontoOrani(k.getIskontoOrani())
                .stokId(k.getStokId())
                .build()).collect(Collectors.toList());

        FaturaDTO faturaDTO = FaturaDTO.builder()
                .cariHesapId(tf.getCariHesapId())
                .tur(tf.getTur())
                .aciklama(tf.getAciklama())
                .tarih(LocalDate.now())
                .durum("KESILDI")
                .kalemler(kalemler)
                .build();

        faturaService.faturaOlustur(faturaDTO, tf.getSirketId(), null, "Tekrarlayan Fatura");

        tf.setSonrakiCalistirma(sonrakiTarih(tf.getSonrakiCalistirma(), tf.getPeriyot()));
        repository.save(tf);
        log.info("Tekrarlayan fatura üretildi - ID: {}, Cari: {}", id, tf.getCariHesapId());
    }

    // ---------- YARDIMCILAR ----------

    private TekrarlayanFaturaKalem kalemDTOToEntity(TekrarlayanFaturaDTO.TekrarlayanFaturaKalemDTO dto, TekrarlayanFatura tf) {
        return TekrarlayanFaturaKalem.builder()
                .tekrarlayanFatura(tf)
                .aciklama(dto.getAciklama())
                .adet(dto.getAdet())
                .birimFiyat(dto.getBirimFiyat())
                .kdvOrani(dto.getKdvOrani())
                .iskontoOrani(dto.getIskontoOrani())
                .stokId(dto.getStokId())
                .build();
    }

    private LocalDate sonrakiTarih(LocalDate tarih, String periyot) {
        if (tarih == null) return null;
        return switch (periyot) {
            case "GUNLUK" -> tarih.plusDays(1);
            case "HAFTALIK" -> tarih.plusWeeks(1);
            case "AYLIK" -> tarih.plusMonths(1);
            case "YILLIK" -> tarih.plusYears(1);
            default -> tarih.plusMonths(1);
        };
    }

    private TekrarlayanFaturaDTO entityToDTO(TekrarlayanFatura tf) {
        String cariAd = null;
        if (tf.getCariHesapId() != null) {
            cariAd = cariHesapRepository.findById(tf.getCariHesapId())
                    .map(c -> c.getAd()).orElse(null);
        }
        return TekrarlayanFaturaDTO.builder()
                .id(tf.getId())
                .sirketId(tf.getSirketId())
                .cariHesapId(tf.getCariHesapId())
                .cariHesapAd(cariAd)
                .tur(tf.getTur())
                .aciklama(tf.getAciklama())
                .periyot(tf.getPeriyot())
                .baslangicTarihi(tf.getBaslangicTarihi())
                .bitisTarihi(tf.getBitisTarihi())
                .sonrakiCalistirma(tf.getSonrakiCalistirma())
                .aktif(tf.getAktif())
                .kalemler(tf.getKalemler().stream().map(k -> TekrarlayanFaturaDTO.TekrarlayanFaturaKalemDTO.builder()
                        .id(k.getId())
                        .aciklama(k.getAciklama())
                        .adet(k.getAdet())
                        .birimFiyat(k.getBirimFiyat())
                        .kdvOrani(k.getKdvOrani())
                        .iskontoOrani(k.getIskontoOrani())
                        .stokId(k.getStokId())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
