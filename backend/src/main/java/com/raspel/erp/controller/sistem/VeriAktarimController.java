package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.VeriAktarimDTO;
import com.raspel.erp.dto.sistem.VeriAktarimSonucDTO;
import com.raspel.erp.service.sistem.VeriAktarimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Veri Aktarim", description = "Sirketler arasi stok ve cari aktarimi")
@RestController
@RequestMapping("/api/veri-aktarim")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class VeriAktarimController {

    private final VeriAktarimService veriAktarimService;

    @PostMapping("/sirketler-arasi")
    @Operation(summary = "Sirketler arasi stok ve cari aktarimi baslat")
    public ResponseEntity<VeriAktarimSonucDTO> aktarimYap(@RequestBody VeriAktarimDTO dto) {
        return ResponseEntity.ok(veriAktarimService.aktarimYap(dto));
    }

    @GetMapping("/onizleme")
    @Operation(summary = "Aktarim oncesi onizleme")
    public ResponseEntity<VeriAktarimSonucDTO> onizleme(
            @RequestParam Long kaynakSirketId,
            @RequestParam Long hedefSirketId) {
        return ResponseEntity.ok(veriAktarimService.onizleme(kaynakSirketId, hedefSirketId));
    }
}
