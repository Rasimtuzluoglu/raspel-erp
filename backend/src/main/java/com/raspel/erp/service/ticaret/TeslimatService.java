package com.raspel.erp.service.ticaret;

import com.raspel.erp.dto.ticaret.SurucuDTO;
import com.raspel.erp.dto.ticaret.TeslimatDTO;
import com.raspel.erp.dto.ticaret.TeslimatDurumLogDTO;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.entity.ticaret.TeslimatDurumLog;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.TeslimatDurumLogRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.service.sistem.BildirimService;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeslimatService {

    private final TeslimatRepository teslimatRepository;
    private final KullaniciRepository kullaniciRepository;
    private final FaturaRepository faturaRepository;
    private final DosyaDepolamaService dosyaDepolama;
    private final BildirimService bildirimService;
    private final TeslimatDurumLogRepository durumLogRepository;

    private static final List<String> BEKLEYEN_DURUMLAR = List.of("BEKLEMEDE", "YOLDA");
    private static final String FOTO_KLASOR = "teslimat-fotolari";

    @Transactional(readOnly = true)
    public List<SurucuDTO> suruculer(Long sirketId, Long kullaniciId) {
        List<Kullanici> suruculer = kullaniciRepository.findBySirketIdAndRole(sirketId, "DRIVER");
        boolean driverMi = driverMi(kullaniciId);
        return suruculer.stream()
                .filter(s -> !driverMi || s.getId().equals(kullaniciId))
                .map(s -> SurucuDTO.builder()
                        .id(s.getId())
                        .ad(s.getDisplayName() != null ? s.getDisplayName() : s.getUsername())
                        .bekleyenTeslimatSayisi(teslimatRepository
                                .countBySirketIdAndDriverIdAndDurumIn(sirketId, s.getId(), BEKLEYEN_DURUMLAR))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeslimatDTO> teslimatlar(Long sirketId, Long driverId, Long kullaniciId) {
        Long hedefDriverId = driverId;
        if (driverMi(kullaniciId)) {
            // Şoför yalnızca kendi teslimatlarını görebilir.
            hedefDriverId = kullaniciId;
        }
        if (hedefDriverId == null) {
            throw new BusinessException("Şoför belirtilmedi");
        }
        String driverAd = kullaniciRepository.findById(hedefDriverId)
                .map(k -> k.getDisplayName() != null ? k.getDisplayName() : k.getUsername())
                .orElse(null);
        return teslimatRepository.findBySirketIdAndDriverIdOrderByOlusturmaTarihiDesc(sirketId, hedefDriverId)
                .stream()
                .map(t -> toDTO(t, driverAd))
                .collect(Collectors.toList());
    }

    @Transactional
    public TeslimatDTO olustur(TeslimatDTO dto, Long sirketId) {
        if (dto.getFaturaId() == null) {
            throw new BusinessException("Fatura (saleId) belirtilmedi");
        }
        if (dto.getDriverId() == null) {
            throw new BusinessException("Şoför seçilmedi");
        }
        Fatura fatura = faturaRepository.findById(dto.getFaturaId())
                .orElseThrow(() -> new ResourceNotFoundException("Fatura", dto.getFaturaId()));
        if (sirketId != null && !sirketId.equals(fatura.getSirketId())) {
            throw new BusinessException("Bu faturaya erişim yetkiniz yok");
        }
        Kullanici surucu = kullaniciRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", dto.getDriverId()));
        if (!"DRIVER".equalsIgnoreCase(surucu.getRole())) {
            throw new BusinessException("Seçilen kullanıcı şoför (DRIVER) değil");
        }
        if (dto.getTeslimatAdresi() == null || dto.getTeslimatAdresi().isBlank()) {
            throw new BusinessException("Teslimat adresi zorunludur");
        }

        String musteriAdi = dto.getMusteriAdi();
        if ((musteriAdi == null || musteriAdi.isBlank()) && fatura.getCariHesap() != null) {
            musteriAdi = fatura.getCariHesap().getAd();
        }

        Teslimat t = Teslimat.builder()
                .sirketId(sirketId)
                .faturaId(fatura.getId())
                .faturaNumarasi(fatura.getFaturaNumarasi())
                .driverId(surucu.getId())
                .teslimatAdresi(dto.getTeslimatAdresi().trim())
                .beklenenTeslimTarihi(dto.getBeklenenTeslimTarihi())
                .teslimatFoto(dto.getTeslimatFoto())
                .musteriAdi(musteriAdi)
                .durum(dto.getDurum() != null && gecerliDurum(dto.getDurum()) ? dto.getDurum() : Teslimat.Durum.BEKLEMEDE.name())
                .notlar(dto.getNotlar())
                .build();
        t = teslimatRepository.save(t);

        try {
            if (sirketId != null) {
                bildirimService.bildirimGonder(sirketId, "TESLIMAT",
                        "Yeni teslimat atandı: " + surucu.getDisplayName(),
                        (musteriAdi != null ? musteriAdi + " - " : "") + (t.getTeslimatAdresi() != null ? t.getTeslimatAdresi() : ""));
            }
        } catch (Exception e) {
            log.warn("Teslimat bildirimi gönderilemedi: {}", e.getMessage());
        }

        return toDTO(t, surucu.getDisplayName() != null ? surucu.getDisplayName() : surucu.getUsername());
    }

    @Transactional
    public TeslimatDTO fotoYukle(Long id, MultipartFile file, Long sirketId, Long kullaniciId) {
        Teslimat t = teslimatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teslimat", id));
        if (sirketId != null && !sirketId.equals(t.getSirketId())) {
            throw new BusinessException("Bu teslimata erişim yetkiniz yok");
        }
        if (driverMi(kullaniciId) && !kullaniciId.equals(t.getDriverId())) {
            throw new BusinessException("Yalnızca kendi teslimatlarınıza fotoğraf yükleyebilirsiniz");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fotoğraf dosyası boş");
        }
        try {
            String filename = dosyaDepolama.kaydet(FOTO_KLASOR, file);
            t.setTeslimatFoto("/api/uploads/teslimat-fotolari/" + filename);
        } catch (IOException e) {
            throw new BusinessException("Fotoğraf yüklenemedi: " + e.getMessage());
        }
        t = teslimatRepository.save(t);
        return toDTO(t, null);
    }

    @Transactional
    public TeslimatDTO durumGuncelle(Long id, String durum, Long sirketId, Long kullaniciId) {
        if (durum == null || !gecerliDurum(durum)) {
            throw new BusinessException("Geçersiz teslimat durumu");
        }
        Teslimat t = teslimatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teslimat", id));
        if (sirketId != null && !sirketId.equals(t.getSirketId())) {
            throw new BusinessException("Bu teslimata erişim yetkiniz yok");
        }
        if (driverMi(kullaniciId) && !kullaniciId.equals(t.getDriverId())) {
            throw new BusinessException("Yalnızca kendi teslimatlarınızı güncelleyebilirsiniz");
        }
        String oncekiDurum = t.getDurum();
        t.setDurum(durum);
        if (Teslimat.Durum.TESLIM_EDILDI.name().equals(durum)) {
            t.setTeslimTarihi(LocalDateTime.now());
        } else {
            t.setTeslimTarihi(null);
        }
        t = teslimatRepository.save(t);
        durumLogRepository.save(TeslimatDurumLog.builder()
                .teslimatId(t.getId())
                .oncekiDurum(oncekiDurum)
                .yeniDurum(durum)
                .kullaniciId(kullaniciId)
                .build());
        return toDTO(t, null);
    }

    @Transactional(readOnly = true)
    public List<TeslimatDurumLogDTO> gecmis(Long id, Long sirketId, Long kullaniciId) {
        Teslimat t = teslimatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teslimat", id));
        if (sirketId != null && !sirketId.equals(t.getSirketId())) {
            throw new BusinessException("Bu teslimata erişim yetkiniz yok");
        }
        if (driverMi(kullaniciId) && !kullaniciId.equals(t.getDriverId())) {
            throw new BusinessException("Yalnızca kendi teslimatlarınızın geçmişini görebilirsiniz");
        }
        return durumLogRepository.findByTeslimatIdOrderByOlusturmaTarihiDesc(id).stream()
                .map(l -> TeslimatDurumLogDTO.builder()
                        .id(l.getId()).teslimatId(l.getTeslimatId())
                        .oncekiDurum(l.getOncekiDurum()).yeniDurum(l.getYeniDurum())
                        .kullaniciId(l.getKullaniciId()).olusturmaTarihi(l.getOlusturmaTarihi())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Teslimat> gecikmisTeslimatlar() {
        return teslimatRepository.findByDurumInAndBeklenenTeslimTarihiBeforeAndGecikmeBildirildiFalse(
                List.of("BEKLEMEDE", "YOLDA"), LocalDate.now());
    }

    @Transactional
    public void gecikmeBildirildiIsaretle(Long id) {
        teslimatRepository.findById(id).ifPresent(t -> {
            t.setGecikmeBildirildi(true);
            teslimatRepository.save(t);
        });
    }

    private boolean gecerliDurum(String durum) {
        for (Teslimat.Durum d : Teslimat.Durum.values()) {
            if (d.name().equals(durum)) return true;
        }
        return false;
    }

    private boolean driverMi(Long kullaniciId) {
        if (kullaniciId == null) return false;
        return kullaniciRepository.findById(kullaniciId)
                .map(k -> "DRIVER".equalsIgnoreCase(k.getRole()))
                .orElse(false);
    }

    private TeslimatDTO toDTO(Teslimat t, String driverAd) {
        boolean gecikti = t.getBeklenenTeslimTarihi() != null
                && t.getBeklenenTeslimTarihi().isBefore(LocalDate.now())
                && !Teslimat.Durum.TESLIM_EDILDI.name().equals(t.getDurum())
                && !Teslimat.Durum.IPTAL.name().equals(t.getDurum());
        return TeslimatDTO.builder()
                .id(t.getId())
                .sirketId(t.getSirketId())
                .faturaId(t.getFaturaId())
                .faturaNumarasi(t.getFaturaNumarasi())
                .driverId(t.getDriverId())
                .driverAd(driverAd)
                .teslimatAdresi(t.getTeslimatAdresi())
                .beklenenTeslimTarihi(t.getBeklenenTeslimTarihi())
                .teslimatFoto(t.getTeslimatFoto())
                .musteriAdi(t.getMusteriAdi())
                .durum(t.getDurum())
                .notlar(t.getNotlar())
                .olusturmaTarihi(t.getOlusturmaTarihi())
                .teslimTarihi(t.getTeslimTarihi())
                .gecikti(gecikti)
                .build();
    }
}
