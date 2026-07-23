package com.raspel.erp.controller;

import com.raspel.erp.dto.CariHesapDTO;
import com.raspel.erp.service.CariHesapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cari Hesap Controller
 * Cari hesap işlemleri için REST API endpoint'lerini sağlar.
 */
@RestController
@RequestMapping("/api/cari-hesaplar")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
public class CariHesapController {
    
    private final CariHesapService cariHesapService;
    
    /**
     * Tüm cari hesapları getir
     * GET /api/cari-hesaplar
     */
    @GetMapping
    public ResponseEntity<List<CariHesapDTO>> tumCariHesaplariGetir(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar - Tüm cari hesaplar getiriliyor, sirketId: {}", sirketId);
        List<CariHesapDTO> cariHesaplar = cariHesapService.tumCariHesaplariGetir(sirketId);
        return ResponseEntity.ok(cariHesaplar);
    }
    
    /**
     * Cari hesapları CSV olarak dışa aktar
     * GET /api/cari-hesaplar/export/csv
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> cariHesaplarCsv(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar/export/csv - CSV dışa aktarım, sirketId: {}", sirketId);
        List<CariHesapDTO> liste = cariHesapService.tumCariHesaplariGetir(sirketId);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Ad,Vergi Numarası,Telefon,Bakiye\n");
        for (CariHesapDTO c : liste) {
            csv.append(c.getId()).append(",")
               .append("\"").append(csvSafe(c.getAd())).append("\",")
               .append("\"").append(csvSafe(c.getVergiNumarasi())).append("\",")
               .append("\"").append(csvSafe(c.getTelefon())).append("\",")
               .append(c.getBakiye() != null ? c.getBakiye() : "0").append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "cari-hesaplar.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.startsWith("=") || escaped.startsWith("+") || escaped.startsWith("-") || escaped.startsWith("@")) {
            escaped = "'" + escaped;
        }
        return escaped;
    }

    /**
     * Cari hesapları ada göre ara
     * GET /api/cari-hesaplar/search?q=abc
     */
    @GetMapping("/search")
    public ResponseEntity<List<CariHesapDTO>> cariHesapAra(@RequestParam String q, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar/search - Arama: {}, sirketId: {}", q, sirketId);
        List<CariHesapDTO> sonuclar = cariHesapService.cariHesapAra(q, sirketId);
        return ResponseEntity.ok(sonuclar);
    }

    /**
     * ID'ye göre cari hesap getir
     * GET /api/cari-hesaplar/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CariHesapDTO> cariHesapGetir(@PathVariable Long id) {
        log.info("GET /api/cari-hesaplar/{} - Cari hesap getiriliyor", id);
        CariHesapDTO cariHesap = cariHesapService.cariHesapGetir(id);
        return ResponseEntity.ok(cariHesap);
    }

    /**
     * Yeni cari hesap oluştur
     * POST /api/cari-hesaplar
     */
    @PostMapping
    public ResponseEntity<CariHesapDTO> cariHesapOlustur(@RequestBody @jakarta.validation.Valid CariHesapDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("POST /api/cari-hesaplar - Yeni cari hesap oluşturuluyor: {}, sirketId: {}", dto.getAd(), sirketId);
        CariHesapDTO olusturulanCariHesap = cariHesapService.cariHesapOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanCariHesap);
    }

    /**
     * Cari hesap güncelle
     * PUT /api/cari-hesaplar/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CariHesapDTO> cariHesapGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid CariHesapDTO dto) {
        log.info("PUT /api/cari-hesaplar/{} - Cari hesap güncelleniyor", id);
        CariHesapDTO guncellenenCariHesap = cariHesapService.cariHesapGuncelle(id, dto);
        return ResponseEntity.ok(guncellenenCariHesap);
    }

    /**
     * Cari hesap sil
     * DELETE /api/cari-hesaplar/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cariHesapSil(@PathVariable Long id) {
        log.info("DELETE /api/cari-hesaplar/{} - Cari hesap siliniliyor", id);
        cariHesapService.cariHesapSil(id);
        return ResponseEntity.noContent().build();
    }
}
