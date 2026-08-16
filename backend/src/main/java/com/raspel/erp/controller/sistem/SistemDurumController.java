package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.HataLogDTO;
import com.raspel.erp.service.sistem.SistemDurumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Sistem Durumu", description = "Sistem sağlık, bakım ve hata izleme API")
@RestController
@RequestMapping("/api/sistem")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SistemDurumController {

    private final SistemDurumService sistemDurumService;

    @GetMapping("/durum")
    @Operation(summary = "Sistem durumu", description = "Uptime, bellek, disk, bileşen durumu ve son hataları getirir")
    public ResponseEntity<Map<String, Object>> durum() {
        return ResponseEntity.ok(sistemDurumService.durum());
    }

    @GetMapping("/hata-log")
    @Operation(summary = "Son hatalar", description = "Sunucuda oluşan son hata kayıtlarını getirir")
    public ResponseEntity<List<HataLogDTO>> hataLog() {
        return ResponseEntity.ok(sistemDurumService.sonHatalar(50));
    }
}
