package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.service.sistem.RaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.raspel.erp.entity.sistem.Donem;

@Tag(name = "Raporlar", description = "Raporlama API")
@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
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

    @GetMapping("/kdv-beyanname")
    @Operation(summary = "KDV beyanname hazırlığı", description = "YYYY-MM dönemi için KDV beyannameye hazırlık listesi üretir (matrah + KDV oran bazlı)")
    public ResponseEntity<RaporDTO.KdvBeyannameDTO> kdvBeyanname(@RequestParam String donem) {
        return ResponseEntity.ok(raporService.kdvBeyannameGetir(donem));
    }

    @GetMapping("/ba-bs")
    @Operation(summary = "BA/BS bildirimi", description = "YYYY-MM dönemi için BA (alış) veya BS (satış) bildirim formu listesi üretir")
    public ResponseEntity<RaporDTO.BaBsDTO> baBs(
            @RequestParam String donem,
            @RequestParam(defaultValue = "BS") String tur,
            @RequestParam(required = false) BigDecimal esik) {
        return ResponseEntity.ok(raporService.baBsGetir(donem, tur, esik));
    }
}