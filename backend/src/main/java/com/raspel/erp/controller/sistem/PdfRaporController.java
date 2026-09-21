package com.raspel.erp.controller.sistem;

import com.raspel.erp.service.sistem.PdfRaporService;
import com.raspel.erp.service.sistem.PdfMetin;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Siparis;

@Tag(name = "PDF Raporlar", description = "PDF rapor oluşturma API")
@RestController
@RequestMapping("/api/rapor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PdfRaporController {

    private final PdfRaporService pdfRaporService;

    /** Accept-Language başlığından belge dilini çözer (tr varsayılan, en desteklenir). */
    private PdfMetin dil(HttpServletRequest request) {
        return PdfMetin.of(request.getHeader("Accept-Language"));
    }

    @GetMapping("/siparis/{id}")
    @Operation(summary = "Sipariş raporu PDF", description = "Sipariş detaylarını PDF olarak indirir")
    public ResponseEntity<byte[]> siparisRaporu(@PathVariable Long id, HttpServletRequest request) {
        byte[] pdf = pdfRaporService.siparisRaporu(id, dil(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=siparis_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}")
    @Operation(summary = "Fatura PDF", description = "Fatura detaylarını profesyonel PDF olarak indirir")
    public ResponseEntity<byte[]> faturaRaporu(@PathVariable Long id, HttpServletRequest request) {
        byte[] pdf = pdfRaporService.faturaRaporu(id, dil(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fatura_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}/onizleme")
    @Operation(summary = "Fatura önizleme (görsel)", description = "Fatura PDF'ini tarayıcıda görüntülenmek üzere (inline) döndürür")
    public ResponseEntity<byte[]> faturaOnizleme(@PathVariable Long id, HttpServletRequest request) {
        byte[] pdf = pdfRaporService.faturaRaporu(id, dil(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fatura_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}/gorsel")
    @Operation(summary = "Fatura görseli (PNG)", description = "Faturayı müşteriye göndermek için PNG görsel olarak döndürür")
    public ResponseEntity<byte[]> faturaGorsel(@PathVariable Long id, HttpServletRequest request) {
        byte[] png = pdfRaporService.faturaGorselPng(id, dil(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fatura_" + id + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @GetMapping("/irsaliye/{id}")
    @Operation(summary = "İrsaliye raporu PDF", description = "İrsaliye detaylarını PDF olarak indirir")
    public ResponseEntity<byte[]> irsaliyeRaporu(@PathVariable Long id, HttpServletRequest request) {
        byte[] pdf = pdfRaporService.irsaliyeRaporu(id, dil(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=irsaliye_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}