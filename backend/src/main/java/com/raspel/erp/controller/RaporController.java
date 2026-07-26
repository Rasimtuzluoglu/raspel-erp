package com.raspel.erp.controller;

import com.raspel.erp.dto.RaporDTO;
import com.raspel.erp.service.RaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RaporController {

    private final RaporService raporService;

    @GetMapping("/cari-ekstre")
    public ResponseEntity<RaporDTO.CariEkstreDTO> cariEkstre(
            @RequestParam Long cariHesapId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.cariEkstreGetir(cariHesapId, baslangic, bitis));
    }

    @GetMapping("/gelir-gider")
    public ResponseEntity<RaporDTO.GelirGiderOzetDTO> gelirGider(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.gelirGiderOzeti(baslangic, bitis));
    }

    @GetMapping("/kdv")
    public ResponseEntity<RaporDTO.KdvRaporDTO> kdvRaporu(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ResponseEntity.ok(raporService.kdvRaporu(baslangic, bitis));
    }

    @GetMapping("/yaslandirma")
    public ResponseEntity<List<RaporDTO.YaslandirmaDTO>> yaslandirma() {
        return ResponseEntity.ok(raporService.yaslandirmaRaporu());
    }
}
