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

@Tag(name = "Akıllı Anomali Tespiti", description = "Mükerrer fatura, ödeme ve finansal anomali tespit API")
@RestController
@RequestMapping({"/api/anomaliler", "/api/v1/anomaliler"})
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
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
