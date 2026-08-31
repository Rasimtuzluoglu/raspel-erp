package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.OnayAyariDTO;
import com.raspel.erp.service.sistem.OnayAyariService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Onay Ayarları", description = "Yapılandırılabilir onay eşikleri (iş akışı matrisi)")
@RestController
@RequestMapping("/api/onay-ayarlari")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class OnayAyariController {

    private final OnayAyariService onayAyariService;

    @GetMapping
    @Operation(summary = "Onay ayarlarını getir", description = "Şirketin modül bazlı onay eşiklerini listeler")
    public ResponseEntity<List<OnayAyariDTO>> listele(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onayAyariService.listele(sirketId));
    }

    @PostMapping
    @Operation(summary = "Onay ayarını kaydet", description = "Modül için onay eşiği ve otomatik onay kuralını kaydeder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OnayAyariDTO> kaydet(HttpServletRequest request, @RequestBody OnayAyariDTO dto) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onayAyariService.kaydet(sirketId, dto));
    }
}
