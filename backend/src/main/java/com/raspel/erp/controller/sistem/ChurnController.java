package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.ChurnRiskDTO;
import com.raspel.erp.service.sistem.ChurnAnalizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Churn Analizi", description = "Müşteri kayıp (churn) riski skorlama API")
@RestController
@RequestMapping("/api/churn")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ChurnController {

    private final ChurnAnalizService churnAnalizService;

    @GetMapping
    @Operation(summary = "Churn risk analizi", description = "Müşterilerin son işlem tarihlerine göre kayıp riski skorunu getirir")
    public ResponseEntity<List<ChurnRiskDTO>> analiz(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(churnAnalizService.churnRiskiAnaliz(sirketId));
    }
}
