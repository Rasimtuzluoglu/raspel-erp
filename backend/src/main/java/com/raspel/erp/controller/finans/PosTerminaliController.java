package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.PosOzetDTO;
import com.raspel.erp.dto.finans.PosTerminaliDTO;
import com.raspel.erp.service.finans.PosTerminaliService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "POS Terminalleri", description = "POS terminali ve komisyon/valör takibi API")
@RestController
@RequestMapping("/api/pos-terminalleri")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PosTerminaliController {

    private final PosTerminaliService posService;

    @GetMapping
    @Operation(summary = "POS terminallerini listele")
    public ResponseEntity<List<PosTerminaliDTO>> liste(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(posService.liste(sirketId));
    }

    @GetMapping("/aktif")
    @Operation(summary = "Aktif POS terminallerini listele")
    public ResponseEntity<List<PosTerminaliDTO>> aktif(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(posService.aktif(sirketId));
    }

    @GetMapping("/ozet")
    @Operation(summary = "POS bazlı özet", description = "Her POS'tan günlük/toplam çekilen tutar ve komisyon özeti")
    public ResponseEntity<List<PosOzetDTO>> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(posService.ozet(sirketId));
    }

    @PostMapping
    @Operation(summary = "POS terminali oluştur")
    public ResponseEntity<PosTerminaliDTO> olustur(@RequestBody PosTerminaliDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(posService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "POS terminalini güncelle")
    public ResponseEntity<PosTerminaliDTO> guncelle(@PathVariable Long id, @RequestBody PosTerminaliDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(posService.guncelle(id, dto, sirketId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "POS terminalini sil")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        posService.sil(id, sirketId);
        return ResponseEntity.noContent().build();
    }
}
