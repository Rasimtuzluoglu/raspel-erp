package com.raspel.erp.controller;

import com.raspel.erp.dto.DonemDTO;
import com.raspel.erp.service.DonemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donemler")
@RequiredArgsConstructor
public class DonemController {

    private final DonemService donemService;

    @GetMapping
    public ResponseEntity<List<DonemDTO>> tumu() {
        return ResponseEntity.ok(donemService.tumunuGetir());
    }

    @GetMapping("/sirket/{sirketId}")
    public ResponseEntity<List<DonemDTO>> sirketeGore(@PathVariable Long sirketId) {
        return ResponseEntity.ok(donemService.sirketeGoreGetir(sirketId));
    }

    @GetMapping("/sirket/{sirketId}/aktif")
    public ResponseEntity<List<DonemDTO>> aktifDonemler(@PathVariable Long sirketId) {
        return ResponseEntity.ok(donemService.aktifDonemler(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonemDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(donemService.getir(id));
    }

    @PostMapping
    public ResponseEntity<DonemDTO> olustur(@RequestBody DonemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donemService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DonemDTO> guncelle(@PathVariable Long id, @RequestBody DonemDTO dto) {
        return ResponseEntity.ok(donemService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        donemService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
