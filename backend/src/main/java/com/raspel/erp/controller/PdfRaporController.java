package com.raspel.erp.controller;

import com.raspel.erp.service.PdfRaporService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rapor")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class PdfRaporController {

    private final PdfRaporService pdfRaporService;

    @GetMapping("/siparis/{id}")
    public ResponseEntity<byte[]> siparisRaporu(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.siparisRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=siparis_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/irsaliye/{id}")
    public ResponseEntity<byte[]> irsaliyeRaporu(@PathVariable Long id) {
        byte[] pdf = pdfRaporService.irsaliyeRaporu(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=irsaliye_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
