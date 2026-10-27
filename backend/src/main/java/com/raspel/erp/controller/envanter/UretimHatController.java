package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.UretimHatDTO;
import com.raspel.erp.service.envanter.UretimHatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Üretim hattı / iş merkezi CRUD ve doluluk özeti. */
@Tag(name = "Üretim Hatları", description = "Üretim hattı / iş merkezi yönetimi ve doluluk API")
@RestController
@RequestMapping("/api/uretim/hatlar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class UretimHatController {

    private final UretimHatService hatService;

    private Long sirket(HttpServletRequest request) {
        return (Long) request.getAttribute("sirketId");
    }

    @GetMapping
    @Operation(summary = "Hatları doluluk özetiyle listele")
    public ResponseEntity<List<UretimHatDTO>> listele(HttpServletRequest request) {
        return ResponseEntity.ok(hatService.listele(sirket(request)));
    }

    @PostMapping
    @Operation(summary = "Hat oluştur")
    public ResponseEntity<UretimHatDTO> olustur(@RequestBody UretimHatDTO dto, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hatService.olustur(dto, sirket(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Hat güncelle")
    public ResponseEntity<UretimHatDTO> guncelle(
            @PathVariable Long id, @RequestBody UretimHatDTO dto, HttpServletRequest request) {
        return ResponseEntity.ok(hatService.guncelle(id, dto, sirket(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hat sil (bağlı emir yoksa)")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        hatService.sil(id, sirket(request));
        return ResponseEntity.noContent().build();
    }
}
