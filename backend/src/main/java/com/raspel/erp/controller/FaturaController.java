package com.raspel.erp.controller;

import com.raspel.erp.dto.FaturaDTO;
import com.raspel.erp.service.FaturaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/faturalar")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class FaturaController {

    private final FaturaService faturaService;

    @GetMapping
    public ResponseEntity<List<FaturaDTO>> tumFaturalariGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(faturaService.tumFaturalariGetir(sirketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FaturaDTO> faturaGetir(@PathVariable Long id) {
        return ResponseEntity.ok(faturaService.faturaGetir(id));
    }

    @PostMapping
    public ResponseEntity<FaturaDTO> faturaOlustur(@RequestBody @jakarta.validation.Valid FaturaDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        FaturaDTO olusturulan = faturaService.faturaOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FaturaDTO> faturaGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid FaturaDTO dto) {
        return ResponseEntity.ok(faturaService.faturaGuncelle(id, dto));
    }

    @PutMapping("/{id}/durum")
    public ResponseEntity<FaturaDTO> faturaDurumGuncelle(@PathVariable Long id, @RequestBody DurumRequest request) {
        return ResponseEntity.ok(faturaService.faturaDurumGuncelle(id, request.durum));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> faturaSil(@PathVariable Long id) {
        faturaService.faturaSil(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(HttpServletRequest request) {
        List<FaturaDTO> liste = faturaService.tumFaturalariGetir((Long) request.getAttribute("sirketId"));
        StringBuilder csv = new StringBuilder("Fatura No,Tarih,Müşteri,Tutar,Durum\n");
        for (FaturaDTO f : liste) {
            csv.append(f.getFaturaNumarasi()).append(",")
               .append(f.getTarih()).append(",")
               .append(f.getCariHesapAd() != null ? "\"" + f.getCariHesapAd().replace("\"", "\"\"") + "\"" : "")
               .append(",").append(f.getGenelToplam())
               .append(",").append(f.getDurum()).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "faturalar.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    record DurumRequest(String durum) {}
}
