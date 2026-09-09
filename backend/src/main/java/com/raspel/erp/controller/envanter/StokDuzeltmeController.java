package com.raspel.erp.controller.envanter;

import com.raspel.erp.dto.envanter.StokDuzeltmeDTO;
import com.raspel.erp.service.envanter.StokDuzeltmeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stok Düzeltme", description = "Stok düzeltme ve düzeltme geçmişi API")
@RestController
@RequestMapping("/api/stok-duzeltme")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class StokDuzeltmeController {

    private final StokDuzeltmeService duzeltmeService;

    @GetMapping
    @Operation(summary = "Düzeltme geçmişi")
    public ResponseEntity<List<StokDuzeltmeDTO>> gecmis(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(duzeltmeService.gecmis(sirketId));
    }

    @PostMapping
    @Operation(summary = "Stok düzelt", description = "Stok miktarını düzeltir ve geçmişe kaydeder")
    public ResponseEntity<StokDuzeltmeDTO> duzelt(@RequestBody StokDuzeltmeDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.status(HttpStatus.CREATED).body(duzeltmeService.duzelt(dto, sirketId, kullaniciId));
    }
}
