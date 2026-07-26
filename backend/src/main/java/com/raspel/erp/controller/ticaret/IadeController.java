package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.service.ticaret.IadeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/iadeler")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class IadeController {

    private final IadeService iadeService;

    @GetMapping
    public ResponseEntity<List<IadeDTO>> tumu(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(iadeService.tumunuGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IadeDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(iadeService.getir(id));
    }

    @PostMapping
    public ResponseEntity<IadeDTO> olustur(@Valid @RequestBody IadeDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(iadeService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IadeDTO> guncelle(@PathVariable Long id, @Valid @RequestBody IadeDTO dto) {
        return ResponseEntity.ok(iadeService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        iadeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
