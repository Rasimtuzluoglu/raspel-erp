package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.TekrarlayanFaturaDTO;
import com.raspel.erp.service.ticaret.TekrarlayanFaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tekrarlayan Faturalar", description = "Periyodik fatura tanımlama ve otomatik kesim API")
@RestController
@RequestMapping("/api/tekrarlayan-faturalar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class TekrarlayanFaturaController {

    private final TekrarlayanFaturaService tekrarlayanFaturaService;

    @GetMapping
    @Operation(summary = "Tekrarlayan faturaları listele", description = "Şirketin periyodik fatura tanımlarını listeler")
    public ResponseEntity<List<TekrarlayanFaturaDTO>> listele(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(tekrarlayanFaturaService.listele(sirketId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Tekrarlayan fatura getir", description = "Belirtilen periyodik fatura tanımını getirir")
    public ResponseEntity<TekrarlayanFaturaDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(tekrarlayanFaturaService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Tekrarlayan fatura oluştur", description = "Yeni periyodik fatura tanımı oluşturur")
    public ResponseEntity<TekrarlayanFaturaDTO> olustur(@Valid @RequestBody TekrarlayanFaturaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(tekrarlayanFaturaService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Tekrarlayan fatura güncelle", description = "Periyodik fatura tanımını günceller")
    public ResponseEntity<TekrarlayanFaturaDTO> guncelle(@PathVariable Long id, @Valid @RequestBody TekrarlayanFaturaDTO dto) {
        return ResponseEntity.ok(tekrarlayanFaturaService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Tekrarlayan fatura sil", description = "Periyodik fatura tanımını siler")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        tekrarlayanFaturaService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/uret")
    @Operation(summary = "Faturayı şimdi üret", description = "Vadesi gelmese bile tekrarlayan fatura tanımından bir fatura üretir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uret(@PathVariable Long id) {
        tekrarlayanFaturaService.faturaUret(id);
        return ResponseEntity.ok().build();
    }
}
