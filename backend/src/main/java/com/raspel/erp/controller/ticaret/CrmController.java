package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.CariFirsatDTO;
import com.raspel.erp.service.ticaret.CrmService;
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

@Tag(name = "CRM", description = "Potansiyel müşteri / fırsat takibi API")
@RestController
@RequestMapping({"/api/crm", "/api/v1/crm"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class CrmController {

    private final CrmService crmService;

    @GetMapping("/firsatlar")
    @Operation(summary = "Fırsatları getir", description = "Durum filtresiyle fırsatları listeler")
    public ResponseEntity<List<CariFirsatDTO>> firsatlar(HttpServletRequest request, @RequestParam(required = false) String durum) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(crmService.firsatlariGetir(sirketId, durum));
    }

    @GetMapping("/firsatlar/{id}")
    @Operation(summary = "Fırsat detayı", description = "Fırsat detayını getirir")
    public ResponseEntity<CariFirsatDTO> firsatGetir(@PathVariable Long id) {
        return ResponseEntity.ok(crmService.firsatGetir(id));
    }

    @PostMapping("/firsatlar")
    @Operation(summary = "Yeni fırsat", description = "Yeni potansiyel müşteri fırsatı oluşturur")
    public ResponseEntity<CariFirsatDTO> firsatOlustur(@Valid @RequestBody CariFirsatDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        dto.setKullaniciId((Long) request.getAttribute("kullaniciId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(crmService.firsatOlustur(dto, sirketId));
    }

    @PutMapping("/firsatlar/{id}")
    @Operation(summary = "Fırsat güncelle", description = "Fırsat bilgilerini / durumunu günceller")
    public ResponseEntity<CariFirsatDTO> firsatGuncelle(@PathVariable Long id, @Valid @RequestBody CariFirsatDTO dto) {
        return ResponseEntity.ok(crmService.firsatGuncelle(id, dto));
    }

    @DeleteMapping("/firsatlar/{id}")
    @Operation(summary = "Fırsat sil", description = "Fırsatı siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> firsatSil(@PathVariable Long id) {
        crmService.firsatSil(id);
        return ResponseEntity.noContent().build();
    }
}
