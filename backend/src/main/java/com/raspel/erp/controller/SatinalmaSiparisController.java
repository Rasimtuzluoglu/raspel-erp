package com.raspel.erp.controller;

import com.raspel.erp.dto.SatinalmaSiparisDTO;
import com.raspel.erp.service.SatinalmaSiparisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/satinalma-siparisler")
@RequiredArgsConstructor
public class SatinalmaSiparisController {

    private final SatinalmaSiparisService satinalmaSiparisService;

    @GetMapping
    public ResponseEntity<List<SatinalmaSiparisDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(satinalmaSiparisService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SatinalmaSiparisDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(satinalmaSiparisService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SatinalmaSiparisDTO> olustur(@RequestBody SatinalmaSiparisDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(satinalmaSiparisService.olustur(dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<SatinalmaSiparisDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(satinalmaSiparisService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        satinalmaSiparisService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
