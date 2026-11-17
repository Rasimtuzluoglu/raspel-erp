package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SahaOzetDTO;
import com.raspel.erp.service.sistem.SahaOzetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saha portalı uçları. Tüm veriler oturum kullanıcısına göre daraltılır.
 */
@Tag(name = "Saha Portalı", description = "Saha personelinin günlük özet ve işlem API'si")
@RestController
@RequestMapping("/api/saha")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE', 'DRIVER', 'SAHA')")
public class SahaController {

    private final SahaOzetService sahaOzetService;

    @GetMapping("/ozet")
    @Operation(summary = "Saha günlük özeti",
            description = "Oturum kullanıcısının bugünkü teslimat, ziyaret ve talep sayılarını döndürür")
    public ResponseEntity<SahaOzetDTO> ozet(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(sahaOzetService.ozet(sirketId, kullaniciId));
    }
}
