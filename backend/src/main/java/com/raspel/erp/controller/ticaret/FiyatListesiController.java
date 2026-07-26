package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.FiyatListesiDTO;
import com.raspel.erp.service.ticaret.FiyatListesiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fiyat-listesi")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class FiyatListesiController {

    private final FiyatListesiService fiyatListesiService;

    @GetMapping
    public ResponseEntity<List<FiyatListesiDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(fiyatListesiService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FiyatListesiDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(fiyatListesiService.getir(id));
    }

    @PostMapping
    public ResponseEntity<FiyatListesiDTO> olustur(@RequestBody FiyatListesiDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(fiyatListesiService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FiyatListesiDTO> guncelle(@PathVariable Long id, @RequestBody FiyatListesiDTO dto) {
        return ResponseEntity.ok(fiyatListesiService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        fiyatListesiService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
