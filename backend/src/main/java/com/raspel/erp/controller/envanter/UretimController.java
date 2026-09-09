package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.ReceteDTO;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.service.envanter.UretimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Üretim", description = "Reçete (ürün ağacı) ve üretim emri yönetimi API")
@RestController
@RequestMapping("/api/uretim")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class UretimController {

    private final UretimService uretimService;

    @GetMapping("/receteler")
    @Operation(summary = "Reçeteleri listele")
    public ResponseEntity<List<ReceteDTO>> receteler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(uretimService.receteler(sirketId));
    }

    @PostMapping("/receteler")
    @Operation(summary = "Reçete oluştur")
    public ResponseEntity<ReceteDTO> receteOlustur(@RequestBody ReceteDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(uretimService.receteOlustur(dto, sirketId));
    }

    @DeleteMapping("/receteler/{id}")
    @Operation(summary = "Reçete sil")
    public ResponseEntity<Void> receteSil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        uretimService.receteSil(id, sirketId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/emirler")
    @Operation(summary = "Üretim emirlerini listele")
    public ResponseEntity<List<UretimEmriDTO>> emirler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(uretimService.uretimEmirleri(sirketId));
    }

    @PostMapping("/emirler")
    @Operation(summary = "Üretim emri oluştur")
    public ResponseEntity<UretimEmriDTO> emirOlustur(@RequestBody UretimEmriDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(uretimService.uretimEmriOlustur(dto, sirketId));
    }

    @PostMapping("/emirler/{id}/tamamla")
    @Operation(summary = "Üretim emrini tamamla", description = "Hammaddeleri tüketir ve mamul stoğunu artırır")
    public ResponseEntity<UretimEmriDTO> tamamla(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(uretimService.uretimEmriTamamla(id, sirketId));
    }
}
