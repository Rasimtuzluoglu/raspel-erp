package com.raspel.erp.controller;

import com.raspel.erp.dto.DashboardDTO;
import com.raspel.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard Controller
 * Dashboard'un ihtiyaç duyduğu veriler için API endpoint'lerini sağlar.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * Dashboard verilerini getir
     * GET /api/dashboard
     */
    @GetMapping
    public ResponseEntity<DashboardDTO> dashboardVerileriGetir() {
        log.info("GET /api/dashboard - Dashboard verileri getiriliyor");
        DashboardDTO dashboardDTO = dashboardService.dashboardVerileriGetir();
        return ResponseEntity.ok(dashboardDTO);
    }
}
