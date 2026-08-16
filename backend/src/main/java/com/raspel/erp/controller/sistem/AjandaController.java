package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.service.sistem.AjandaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Ajanda", description = "Ajanda/takvim API")
@RestController
@RequestMapping("/api/ajanda")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AjandaController {

    private final AjandaService ajandaService;

    @GetMapping
    @Operation(summary = "Ajanda olaylarını getir", description = "Belirtilen tarih aralığındaki görev ve vade olaylarını getirir")
    public ResponseEntity<List<AjandaOlayDTO>> olaylar(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(ajandaService.olaylar(sirketId, baslangic, bitis));
    }
}
