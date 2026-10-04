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
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_WRITE')")
    public ResponseEntity<TeklifDTO> olustur(@jakarta.validation.Valid @RequestBody TeklifDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        TeklifDTO olusan = com.raspel.erp.support.MukerrerKayitRetry.calistir(
                () -> teklifService.olustur(dto, sirketId),
                e -> com.raspel.erp.support.MukerrerKayitRetry.kisitMi(e, "uk_teklif_no_sirket"));
        return ResponseEntity.status(HttpStatus.CREATED).body(olusan);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Teklif güncelle")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_WRITE')")
    public ResponseEntity<TeklifDTO> guncelle(@PathVariable Long id, @jakarta.validation.Valid @RequestBody TeklifDTO dto) {
        return ResponseEntity.ok(teklifService.guncelle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Teklif sil")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_DELETE')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        teklifService.sil(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/durum")
    @Operation(summary = "Teklif durumu güncelle")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_WRITE')")
    public ResponseEntity<TeklifDTO> durumGuncelle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String durum = body.get("durum");
        return ResponseEntity.ok(teklifService.durumGuncelle(id, durum));
    }

    @PostMapping("/{id}/revizyon")
    @Operation(summary = "Tekliften yeni revizyon oluştur")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_WRITE')")
    public ResponseEntity<TeklifDTO> revizyonOlustur(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teklifService.revizyonOlustur(id));
    }

    @PostMapping("/{id}/siparise-donustur")
    @Operation(summary = "Teklifi siparişe dönüştür")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'SIPARIS_WRITE')")
    public ResponseEntity<SiparisDTO> sipariseDonustur(@PathVariable Long id) {
        return ResponseEntity.ok(teklifService.sipariseDonustur(id));
    }

    @PostMapping("/{id}/faturaya-donustur")
    @Operation(summary = "Teklifi faturaya dönüştür")
    @PreAuthorize("hasRole('ADMIN') or @yetkiKontrol.kontrol(authentication, 'FATURA_WRITE')")
    public ResponseEntity<FaturaDTO> faturayaDonustur(@PathVariable Long id, HttpServletRequest request) {
        Long kullaniciId = (Long) request.getAttribute("kullaniciId");
        return ResponseEntity.ok(teklifService.faturayaDonustur(id, kullaniciId));
    }
}
