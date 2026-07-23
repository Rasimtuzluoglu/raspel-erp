package com.raspel.erp.controller;

import com.raspel.erp.dto.SatinalmaTalepDTO;
import com.raspel.erp.service.SatinalmaTalepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/satinalma-talepler")
@RequiredArgsConstructor
public class SatinalmaTalepController {

    private final SatinalmaTalepService satinalmaTalepService;

    @GetMapping
    public ResponseEntity<List<SatinalmaTalepDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(satinalmaTalepService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SatinalmaTalepDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(satinalmaTalepService.getir(id));
    }

    @PostMapping
    public ResponseEntity<SatinalmaTalepDTO> olustur(@RequestBody SatinalmaTalepDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(satinalmaTalepService.olustur(dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<SatinalmaTalepDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(satinalmaTalepService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        satinalmaTalepService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
