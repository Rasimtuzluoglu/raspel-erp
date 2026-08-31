package com.raspel.erp.controller.sistem;

import com.raspel.erp.dto.sistem.ApiTokenDTO;
import com.raspel.erp.service.sistem.ApiTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "API Token", description = "Kişisel erişim token'ları (REST API entegrasyonu)")
@RestController
@RequestMapping("/api/api-tokenlar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    @GetMapping
    @Operation(summary = "Tokenları listele", description = "Mevcut kullanıcının API token'larını listeler")
    public ResponseEntity<List<ApiTokenDTO>> listele(HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(apiTokenService.listele(kullaniciId));
    }

    @PostMapping
    @Operation(summary = "Yeni token oluştur", description = "Yeni bir kişisel erişim token'ı oluşturur (token yalnızca bu yanıtta görünür)")
    public ResponseEntity<ApiTokenDTO> olustur(HttpServletRequest request, @RequestBody(required = false) Map<String, String> govde) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        String ad = govde != null ? govde.getOrDefault("ad", "API Token") : "API Token";
        return ResponseEntity.status(HttpStatus.CREATED).body(apiTokenService.olustur(kullaniciId, ad));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Token sil", description = "Belirtilen API token'ını siler")
    public ResponseEntity<Void> sil(HttpServletRequest request, @PathVariable Long id) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        apiTokenService.sil(id, kullaniciId);
        return ResponseEntity.noContent().build();
    }
}
