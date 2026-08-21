package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.SirketHedefDTO;
import com.raspel.erp.dto.sistem.YoneticiKokpitDTO;
import com.raspel.erp.service.sistem.YoneticiKokpitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Yönetici Kokpiti", description = "Patron / Yönetici canlı nabız ve hedef takip metrikleri")
@RestController
@RequestMapping("/api/yonetici-kokpit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class YoneticiKokpitController {

    private final YoneticiKokpitService kokpitService;

    @GetMapping
    @Operation(summary = "Yönetici kokpiti finansal nabız ve hedef verilerini getir")
    public ResponseEntity<YoneticiKokpitDTO> getKokpitVerileri(
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Integer ay,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kokpitService.getKokpitVerileri(sirketId, yil, ay));
    }

    @PostMapping("/hedef")
    @Operation(summary = "Aylık ciro ve kâr hedefini kaydet / güncelle")
    public ResponseEntity<SirketHedefDTO> hedefKaydet(
            @RequestBody SirketHedefDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(kokpitService.hedefKaydet(dto, sirketId));
    }
}
