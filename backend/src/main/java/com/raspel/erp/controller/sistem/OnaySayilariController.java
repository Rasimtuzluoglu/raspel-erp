package com.raspel.erp.controller.sistem;

import com.raspel.erp.service.sistem.OnaySayilariService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Onay Sayıları", description = "Bekleyen onay sayaçları API")
@RestController
@RequestMapping("/api/onay-sayilari")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class OnaySayilariController {

    private final OnaySayilariService onaySayilariService;

    @GetMapping
    @Operation(summary = "Bekleyen onay sayıları", description = "İzin, satınalma talebi ve sipariş için bekleyen onay sayılarını döner")
    public ResponseEntity<Map<String, Long>> sayilari(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(onaySayilariService.sayilariGetir(sirketId));
    }
}
