package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.HataLogDTO;
import com.raspel.erp.entity.sistem.HataLog;
import com.raspel.erp.repository.sistem.HataLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sistem sağlık durumu: uptime, bellek, disk, bileşen durumu (DB/Redis/RabbitMQ) ve son hatalar.
 * Müşterinin bakım ve hata tespiti için tek noktadan görünüm sağlar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SistemDurumService {

    private final HealthEndpoint healthEndpoint;
    private final HataLogRepository hataLogRepository;
    private final BackupService backupService;

    @Value("${app.version:1.6.1}")
    private String surum;

    @Value("${app.hata-log.retention-days:30}")
    private long hataLogRetentionGun;

    /**
     * Hata loglarının sınırsız büyümesini önler. Varsayılan olarak 30 günden eski
     * kayıtları her gece 04:00'te siler. Süre app.hata-log.retention-days ile ayarlanır.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void eskiHataLoglariniTemizle() {
        try {
            LocalDateTime esik = LocalDateTime.now().minusDays(hataLogRetentionGun);
            int silinen = hataLogRepository.deleteOlderThan(esik);
            if (silinen > 0) {
                log.info("Eski hata logları temizlendi: {} kayıt ({} günden eski)", silinen, hataLogRetentionGun);
            }
        } catch (Exception e) {
            log.warn("Eski hata logları temizlenemedi: {}", e.getMessage());
        }
    }

    public Map<String, Object> durum() {
        Map<String, Object> result = new LinkedHashMap<>();

        String genelDurum = "UP";
        Map<String, Object> bilesenler = new LinkedHashMap<>();
        try {
            HealthComponent hc = healthEndpoint.health();
            if (hc != null && hc.getStatus() != null) genelDurum = hc.getStatus().getCode();
            if (hc instanceof SystemHealth sh) {
                sh.getComponents().forEach((name, comp) -> {
                    if (comp != null && comp.getStatus() != null) {
                        bilesenler.put(name, comp.getStatus().getCode());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Sağlık durumu alınamadı: {}", e.getMessage());
            genelDurum = "BILINMIYOR";
        }

        result.put("durum", genelDurum);
        result.put("surum", surum);
        result.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        result.put("bellek", bellekBilgisi());
        result.put("disk", diskBilgisi());
        result.put("bilesenler", bilesenler);
        result.put("hataSayisi", hataLogRepository.count());
        result.put("sonHatalar", sonHatalar(5));
        try {
            result.put("yedekleme", backupService.getSchedule());
        } catch (Exception e) {
            log.warn("Yedekleme bilgisi alınamadı: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> bellekBilgisi() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("toplam", rt.totalMemory());
        m.put("bos", rt.freeMemory());
        m.put("max", rt.maxMemory());
        return m;
    }

    private Map<String, Object> diskBilgisi() {
        Map<String, Object> d = new LinkedHashMap<>();
        try {
            FileStore fs = Files.getFileStore(Paths.get(".").toAbsolutePath());
            d.put("toplam", fs.getTotalSpace());
            d.put("kullanilabilir", fs.getUsableSpace());
        } catch (Exception e) {
            log.warn("Disk bilgisi alınamadı: {}", e.getMessage());
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<HataLogDTO> sonHatalar(int limit) {
        return hataLogRepository.findTop50ByOrderByOlusturmaTarihiDesc().stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private HataLogDTO toDTO(HataLog h) {
        return HataLogDTO.builder()
                .id(h.getId()).sirketId(h.getSirketId()).tur(h.getTur())
                .mesaj(h.getMesaj()).endpoint(h.getEndpoint())
                .olusturmaTarihi(h.getOlusturmaTarihi()).build();
    }
}
