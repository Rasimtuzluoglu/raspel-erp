package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.MasrafDTO;
import com.raspel.erp.service.finans.MasrafService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/masraflar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MasrafController {

    private final MasrafService masrafService;

    @GetMapping
    public ResponseEntity<List<MasrafDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(masrafService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MasrafDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(masrafService.getir(id));
    }

    @PostMapping
    public ResponseEntity<MasrafDTO> olustur(@Valid @RequestBody MasrafDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(masrafService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MasrafDTO> guncelle(@PathVariable Long id, @Valid @RequestBody MasrafDTO dto) {
        return ResponseEntity.ok(masrafService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        masrafService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
