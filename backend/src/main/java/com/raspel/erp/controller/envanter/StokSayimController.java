package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.service.envanter.StokSayimService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/stok-sayim")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSayimController {

    private final StokSayimService stokSayimService;

    @GetMapping
    public ResponseEntity<List<StokSayimDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSayimService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StokSayimDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSayimService.getir(id));
    }

    @PostMapping
    public ResponseEntity<StokSayimDTO> olustur(@Valid @RequestBody StokSayimDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSayimService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StokSayimDTO> guncelle(@PathVariable Long id, @Valid @RequestBody StokSayimDTO dto) {
        return ResponseEntity.ok(stokSayimService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSayimService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
