package com.raspel.erp.controller.sistem;

import com.raspel.erp.service.sistem.PdfRaporService;
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

    @GetMapping("/siparis/{id}")
    @Operation(summary = "Sipariş raporu PDF", description = "Sipariş detaylarını PDF olarak indirir")
    public ResponseEntity<byte[]> siparisRaporu(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.siparisRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=siparis_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}")
    @Operation(summary = "Fatura PDF", description = "Fatura detaylarını profesyonel PDF olarak indirir")
    public ResponseEntity<byte[]> faturaRaporu(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.faturaRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fatura_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}/onizleme")
    @Operation(summary = "Fatura önizleme (görsel)", description = "Fatura PDF'ini tarayıcıda görüntülenmek üzere (inline) döndürür")
    public ResponseEntity<byte[]> faturaOnizleme(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.faturaRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fatura_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fatura/{id}/gorsel")
    @Operation(summary = "Fatura görseli (PNG)", description = "Faturayı müşteriye göndermek için PNG görsel olarak döndürür")
    public ResponseEntity<byte[]> faturaGorsel(@PathVariable Long id) {
        byte[] png = pdfRaporService.faturaGorselPng(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fatura_" + id + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @GetMapping("/irsaliye/{id}")
    @Operation(summary = "İrsaliye raporu PDF", description = "İrsaliye detaylarını PDF olarak indirir")
    public ResponseEntity<byte[]> irsaliyeRaporu(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.irsaliyeRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=irsaliye_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}