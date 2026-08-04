package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.service.sistem.AnomaliTespitEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.raspel.erp.entity.ticaret.Fatura;

@Tag(name = "Akıllı Anomali Tespiti", description = "Mükerrer fatura, ödeme ve finansal anomali tespit API")
@RestController
@RequestMapping("/api/anomaliler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class AnomaliController {

    private final AnomaliTespitEngine anomaliTespitEngine;

    @GetMapping
    @Operation(summary = "Anomalileri tara ve listele", description = "Sistemdeki mükerrer kayıt ve finansal anomalileri tarar")
    public ResponseEntity<List<AnomaliDTO>> anomalileriGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(anomaliTespitEngine.anomalileriTara(sirketId));
    }
}