package com.raspel.erp.controller;

import com.raspel.erp.dto.RaporDTO;
import com.raspel.erp.service.RaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Raporlar", description = "Raporlama API")
@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RaporController {

    private final RaporService raporService;

    @GetMapping("/cari-ekstre")
    @Operation(summary = "Cari ekstre getir", description = "Belirli bir cari hesabın belirtilen tarih aralığındaki ekstresini getirir")
    public ResponseEntity<RaporDTO.CariEkstreDTO> cariEkstre(
            @RequestParam Long cariHesapId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.cariEkstreGetir(cariHesapId, baslangic, bitis));
    }

    @GetMapping("/gelir-gider")
    @Operation(summary = "Gelir gider raporu", description = "Belirtilen tarih aralığındaki gelir/gider özetini getirir")
    public ResponseEntity<RaporDTO.GelirGiderOzetDTO> gelirGider(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.gelirGiderOzeti(baslangic, bitis));
    }

    @GetMapping("/kdv")
    @Operation(summary = "KDV raporu", description = "Belirtilen tarih aralığındaki KDV raporunu getirir")
    public ResponseEntity<RaporDTO.KdvRaporDTO> kdvRaporu(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.kdvRaporu(baslangic, bitis));
    }

    @GetMapping("/yaslandirma")
    @Operation(summary = "Yaşlandırma raporu", description = "Cari hesap yaşlandırma raporunu getirir")
    public ResponseEntity<List<RaporDTO.YaslandirmaDTO>> yaslandirma() {
        return ResponseEntity.ok(raporService.yaslandirmaRaporu());
    }
}
