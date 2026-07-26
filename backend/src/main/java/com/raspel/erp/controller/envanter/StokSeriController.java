package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.service.envanter.StokSeriService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stok-seri")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokSeriController {

    private final StokSeriService stokSeriService;

    @GetMapping
    public ResponseEntity<List<StokSeriDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(stokSeriService.tumunuGetir(sirketId));
    }

    @GetMapping("/stok/{stokId}")
    public ResponseEntity<List<StokSeriDTO>> stokIcinGetir(@PathVariable Long stokId) {
        return ResponseEntity.ok(stokSeriService.stokIcinGetir(stokId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StokSeriDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(stokSeriService.getir(id));
    }

    @PostMapping
    public ResponseEntity<StokSeriDTO> olustur(@RequestBody StokSeriDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stokSeriService.olustur(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StokSeriDTO> guncelle(@PathVariable Long id, @RequestBody StokSeriDTO dto) {
        return ResponseEntity.ok(stokSeriService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        stokSeriService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
