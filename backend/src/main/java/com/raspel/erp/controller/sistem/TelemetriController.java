package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.FrontendHataDTO;
import com.raspel.erp.service.sistem.TelemetriService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontend hata telemetrisi. Oturum açmış her rol gönderebilir; kayıt
 * veritabanına yazılmaz, log + metrik olarak işlenir.
 */
@Tag(name = "Telemetri", description = "İstemci hata bildirimleri")
@RestController
@RequestMapping("/api/telemetri")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TelemetriController {

    private final TelemetriService telemetriService;

    @PostMapping("/hata")
    @Operation(summary = "Frontend hata bildirimi")
    public ResponseEntity<Void> hata(@RequestBody @Valid FrontendHataDTO dto, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (!telemetriService.kabulEt(kullaniciId)) {
            return ResponseEntity.status(429).build();
        }
        telemetriService.kaydet(dto, kullaniciId, sirketId);
        return ResponseEntity.accepted().build();
    }
}
