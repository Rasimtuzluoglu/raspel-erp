package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.FaturaDTO;
import com.raspel.erp.dto.ticaret.SiparisDTO;
import com.raspel.erp.dto.ticaret.TeklifDTO;
import com.raspel.erp.service.ticaret.TeklifService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Teklif Yönetimi", description = "Satış teklifleri ve proforma işlemleri")
@RestController
@RequestMapping("/api/teklifler")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class TeklifController {

    private final TeklifService teklifService;

    @GetMapping
    @Operation(summary = "Tüm teklifleri sayfalı listele")
    public ResponseEntity<Page<TeklifDTO>> tumunuGetir(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(teklifService.tumunuGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Teklif detayı getir")
    public ResponseEntity<TeklifDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(teklifService.getir(id));
    }

    @PostMapping
    @Operation(summary = "Yeni teklif oluştur")
    public ResponseEntity<TeklifDTO> olustur(@RequestBody TeklifDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.status(HttpStatus.CREATED).body(teklifService.olustur(dto, sirketId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Teklif güncelle")
    public ResponseEntity<TeklifDTO> guncelle(@PathVariable Long id, @RequestBody TeklifDTO dto) {
        return ResponseEntity.ok(teklifService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Teklif sil")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        teklifService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/durum")
    @Operation(summary = "Teklif durumu güncelle")
    public ResponseEntity<TeklifDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String durum = body.get("durum");
        return ResponseEntity.ok(teklifService.durumGuncelle(id, durum));
    }

    @PostMapping("/{id}/revizyon")
    @Operation(summary = "Tekliften yeni revizyon oluştur")
    public ResponseEntity<TeklifDTO> revizyonOlustur(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teklifService.revizyonOlustur(id));
    }

    @PostMapping("/{id}/siparise-donustur")
    @Operation(summary = "Teklifi siparişe dönüştür")
    public ResponseEntity<SiparisDTO> sipariseDonustur(@PathVariable Long id) {
        return ResponseEntity.ok(teklifService.sipariseDonustur(id));
    }

    @PostMapping("/{id}/faturaya-donustur")
    @Operation(summary = "Teklifi faturaya dönüştür")
    public ResponseEntity<FaturaDTO> faturayaDonustur(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teklifService.faturayaDonustur(id, kullaniciId));
    }
}
