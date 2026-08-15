package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.KurulumDTO;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.service.sistem.KurulumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * İlk kurulum API'si.
 * Sistem boşken firma + yönetici oluşturup kullanıcıyı başlatır. Uç noktalar herkese açıktır.
 */
@Tag(name = "Kurulum", description = "İlk kurulum (firma + yönetici hesabı oluşturma) API")
@RestController
@RequestMapping("/api/kurulum")
@RequiredArgsConstructor
public class KurulumController {

    private final KurulumService kurulumService;

    @GetMapping("/durum")
    @Operation(summary = "Kurulum durumu", description = "Sistemin ilk kuruluma ihtiyaç duyup duymadığını döndürür")
    public ResponseEntity<Map<String, Object>> durum() {
        return ResponseEntity.ok(kurulumService.durum());
    }

    @PostMapping("/baslat")
    @Operation(summary = "Kurulumu başlat", description = "Firma ve yönetici hesabını oluşturup giriş oturumu başlatır")
    public ResponseEntity<LoginResponse> baslat(@Valid @RequestBody KurulumDTO dto) {
        return ResponseEntity.ok(kurulumService.kurulumYap(dto));
    }
}
