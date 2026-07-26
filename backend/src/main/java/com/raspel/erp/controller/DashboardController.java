package com.raspel.erp.controller;

import com.raspel.erp.dto.DashboardDTO;
import com.raspel.erp.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard Controller
 * Dashboard'un ihtiyaç duyduğu veriler için API endpoint'lerini sağlar.
 */
@Tag(name = "Dashboard", description = "Dashboard verileri API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @GetMapping
    @Operation(summary = "Dashboard verilerini getir", description = "Dashboard için özet istatistikleri ve grafik verilerini döndürür")
    public ResponseEntity<DashboardDTO> dashboardVerileriGetir(HttpServletRequest request) {
        log.info("GET /api/dashboard - Dashboard verileri getiriliyor");
        Long sirketId = (Long) request.getAttribute("sirketId");
        DashboardDTO dashboardDTO = dashboardService.dashboardVerileriGetir(sirketId);
        return ResponseEntity.ok(dashboardDTO);
    }
}
