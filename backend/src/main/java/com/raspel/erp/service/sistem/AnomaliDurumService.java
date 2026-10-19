package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.sistem.AnomaliDurum;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.AnomaliDurumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anomali bulgularının kalıcı inceleme durumunu yönetir.
 * Anahtar deterministik olduğu için yeniden tarama aynı kaydı günceller.
 */
@Service
@RequiredArgsConstructor
public class AnomaliDurumService {

    private static final Set<String> GECERLI_DURUMLAR = Set.of("INCELENDI", "YOK_SAYILDI", "ACIK");

    private final AnomaliDurumRepository anomaliDurumRepository;

    @Transactional(readOnly = true)
    public Map<String, String> durumlariGetir(Long sirketId) {
        if (sirketId == null) return Map.of();
        return anomaliDurumRepository.findBySirketId(sirketId).stream()
                .collect(Collectors.toMap(AnomaliDurum::getAnahtar, AnomaliDurum::getDurum, (a, b) -> a));
    }

    /** Tarama sonucuna kalıcı durumları işler. */
    @Transactional(readOnly = true)
    public void uygula(Long sirketId, List<AnomaliDTO> anomaliler) {
        if (sirketId == null || anomaliler == null || anomaliler.isEmpty()) return;
        Map<String, String> durumlar = durumlariGetir(sirketId);
        for (AnomaliDTO a : anomaliler) {
            a.setDurum(durumlar.get(a.getId()));
        }
    }

    /** Durum işaretler; ACIK gönderilirse kayıt silinir (bulgu yeniden açılır). */
    @Transactional
    public void isaretle(Long sirketId, String anahtar, String durum, Long kullaniciId) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bağlamı bulunamadı");
        }
        if (anahtar == null || anahtar.isBlank()) {
            throw new BusinessException("Anomali anahtarı zorunludur");
        }
        String d = durum == null ? "" : durum.trim().toUpperCase(java.util.Locale.ROOT);
        if (!GECERLI_DURUMLAR.contains(d)) {
            throw new BusinessException("Geçersiz durum: " + durum);
        }
        if ("ACIK".equals(d)) {
            anomaliDurumRepository.deleteBySirketIdAndAnahtar(sirketId, anahtar);
            return;
        }
        AnomaliDurum kayit = anomaliDurumRepository.findBySirketIdAndAnahtar(sirketId, anahtar)
                .orElseGet(() -> AnomaliDurum.builder()
                        .sirketId(sirketId)
                        .anahtar(anahtar)
                        .build());
        kayit.setDurum(d);
        kayit.setKullaniciId(kullaniciId);
        anomaliDurumRepository.save(kayit);
    }
}
