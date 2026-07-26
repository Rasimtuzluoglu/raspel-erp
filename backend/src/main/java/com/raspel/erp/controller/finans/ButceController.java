package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.service.finans.ButceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/butceler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ButceController {

    private final ButceService butceService;

    @GetMapping
    public ResponseEntity<List<ButceDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(butceService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ButceDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(butceService.getir(id));
    }

    @PostMapping
    public ResponseEntity<ButceDTO> olustur(@Valid @RequestBody ButceDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(butceService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ButceDTO> guncelle(@PathVariable Long id, @Valid @RequestBody ButceDTO dto) {
        return ResponseEntity.ok(butceService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        butceService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
