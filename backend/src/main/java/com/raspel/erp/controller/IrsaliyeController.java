package com.raspel.erp.controller;

import com.raspel.erp.dto.IrsaliyeDTO;
import com.raspel.erp.service.IrsaliyeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/irsaliyeler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class IrsaliyeController {

    private final IrsaliyeService irsaliyeService;

    @GetMapping
    public ResponseEntity<List<IrsaliyeDTO>> tumu(@RequestParam(required = false) Long sirketId) {
        return ResponseEntity.ok(irsaliyeService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IrsaliyeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(irsaliyeService.getir(id));
    }

    @PostMapping
    public ResponseEntity<IrsaliyeDTO> olustur(@Valid @RequestBody IrsaliyeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(irsaliyeService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IrsaliyeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody IrsaliyeDTO dto) {
        return ResponseEntity.ok(irsaliyeService.guncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<IrsaliyeDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(irsaliyeService.durumGuncelle(id, body.get("durum")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        irsaliyeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
