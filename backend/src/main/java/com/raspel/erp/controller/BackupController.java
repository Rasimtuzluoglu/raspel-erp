package com.raspel.erp.controller;

import com.raspel.erp.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> manualBackup(@RequestParam(defaultValue = "DAILY") String type) {
        String filename = backupService.manualBackup(type);
        return ResponseEntity.ok(Map.of(
                "message", "Yedekleme başarıyla tamamlandı",
                "filename", filename,
                "type", type
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String filename) {
        byte[] data = backupService.downloadBackup(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> deleteBackup(@PathVariable String filename) {
        backupService.deleteBackup(filename);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getSchedule() {
        return ResponseEntity.ok(backupService.getSchedule());
    }
}
