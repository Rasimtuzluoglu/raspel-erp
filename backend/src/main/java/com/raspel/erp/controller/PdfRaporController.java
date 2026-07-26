package com.raspel.erp.controller;

import com.raspel.erp.service.PdfRaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PDF Raporlar", description = "PDF rapor oluşturma API")
@RestController
@RequestMapping("/api/rapor")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
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
