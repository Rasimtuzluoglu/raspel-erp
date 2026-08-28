package com.raspel.erp.controller.sistem;

import com.raspel.erp.service.sistem.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

@Tag(name = "Yedekler", description = "Veritabanı yedekleme API (yalnızca ADMIN)")
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/manual")
    @Operation(summary = "Manuel yedek al", description = "Veritabanının manuel yedeğini alır")
    public ResponseEntity<Map<String, Object>> manualBackup(@RequestParam(defaultValue = "DAILY") String type) {
        String filename = backupService.manualBackup(type);
        return ResponseEntity.ok(Map.of(
                "message", "Yedekleme başarıyla tamamlandı",
                "filename", filename,
                "type", type
        ));
    }

    @GetMapping
    @Operation(summary = "Yedek listesini getir", description = "Mevcut tüm yedek dosyalarını listeler")
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @GetMapping("/download/{filename:.+}")
    @Operation(summary = "Yedek dosyasını indir", description = "Belirtilen yedek dosyasını indirir")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String filename) {
        byte[] data = backupService.downloadBackup(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{filename:.+}")
    @Operation(summary = "Yedek dosyasını sil", description = "Belirtilen yedek dosyasını siler")
    public ResponseEntity<Void> deleteBackup(@PathVariable String filename) {
        backupService.deleteBackup(filename);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore/{filename:.+}")
    @Operation(summary = "Yedekten geri yükle", description = "Belirtilen yedek dosyasını geri yükler (dikkat: mevcut verilerin üzerine yazar)")
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String filename) {
        backupService.restoreBackup(filename);
        return ResponseEntity.ok(Map.of(
                "message", "Geri yükleme tamamlandı",
                "filename", filename
        ));
    }

    @GetMapping("/schedule")
    @Operation(summary = "Yedekleme zamanlamasını getir", description = "Otomatik yedekleme zamanlamasını döndürür")
    public ResponseEntity<Map<String, Object>> getSchedule() {
        return ResponseEntity.ok(backupService.getSchedule());
    }

    @GetMapping("/dogrula")
    @Operation(summary = "Yedek doğrula", description = "Son yedeğin varlığını, bütünlüğünü ve güncelliğini kontrol eder")
    public ResponseEntity<Map<String, Object>> dogrula() {
        return ResponseEntity.ok(backupService.yedekDogrula());
    }

    @GetMapping("/cloud-config")
    @Operation(summary = "Bulut yedekleme yapılandırması", description = "Bulut sağlayıcı (AWS S3, Google Drive, Dropbox) yapılandırma ve durumunu getirir")
    public ResponseEntity<Map<String, Object>> getCloudConfig() {
        return ResponseEntity.ok(backupService.getCloudConfig());
    }

    @PostMapping("/cloud-config")
    @Operation(summary = "Bulut yedekleme yapılandırmasını kaydet", description = "Bulut sağlayıcı ayarlarını günceller")
    public ResponseEntity<Map<String, Object>> saveCloudConfig(@RequestBody Map<String, Object> config) {
        return ResponseEntity.ok(backupService.saveCloudConfig(config));
    }

    @PostMapping("/cloud-sync")
    @Operation(summary = "Bulut senkronizasyonunu başlat", description = "Yedekleri şifrelenmiş olarak bulut deposuna aktarır")
    public ResponseEntity<Map<String, Object>> syncToCloud(@RequestParam(required = false) String filename) {
        return ResponseEntity.ok(backupService.syncToCloud(filename));
    }
}
