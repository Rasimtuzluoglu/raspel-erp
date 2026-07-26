package com.raspel.erp.controller;

import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.service.HareketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Hareket Controller
 * Hareket işlemleri için REST API endpoint'lerini sağlar.
 */
@RestController
@RequestMapping("/api/hareketler")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class HareketController {
    
    private final HareketService hareketService;
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     * GET /api/hareketler/cari/{cariHesapId}
     */
    @GetMapping("/cari/{cariHesapId}")
    public ResponseEntity<List<HareketDTO>> cariHesapHareketleriGetir(@PathVariable Long cariHesapId) {
        log.info("GET /api/hareketler/cari/{} - Cari hesap hareketleri getiriliyor", cariHesapId);
        List<HareketDTO> hareketler = hareketService.cariHesapHareketleriGetir(cariHesapId);
        return ResponseEntity.ok(hareketler);
    }
    
    /**
     * Son n hareketi getir (Dashboard için)
     * GET /api/hareketler/son/{limit}
     */
    @GetMapping("/son/{limit}")
    public ResponseEntity<List<HareketDTO>> sonHareketleriGetir(@PathVariable int limit) {
        log.info("GET /api/hareketler/son/{} - Son {} hareket getiriliyor", limit, limit);
        List<HareketDTO> hareketler = hareketService.sonHareketleriGetir(limit);
        return ResponseEntity.ok(hareketler);
    }

    /**
     * Hareketleri CSV olarak dışa aktar
     * GET /api/hareketler/export/csv
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> hareketlerCsv(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/hareketler/export/csv - CSV dışa aktarım, sirketId: {}", sirketId);
        List<HareketDTO> liste = hareketService.tumHareketleriGetir(sirketId);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Cari Hesap,Tür,Tutar,Tarih,Açıklama\n");
        for (HareketDTO h : liste) {
            csv.append(h.getId()).append(",")
               .append("\"").append(csvSafe(h.getCariHesapAd())).append("\",")
               .append(h.getTur()).append(",")
               .append(h.getTutar()).append(",")
               .append(h.getHareketTarihi() != null ? h.getHareketTarihi() : "").append(",")
               .append("\"").append(csvSafe(h.getAciklama())).append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "hareketler.csv");
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
     * Tüm hareketleri getir (veya filtrele)
     * GET /api/hareketler?cariHesapId=1&baslangic=2024-01-01&bitis=2024-12-31
     */
    @GetMapping
    public ResponseEntity<List<HareketDTO>> tumHareketleriGetir(
            @RequestParam(required = false) Long cariHesapId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (cariHesapId != null || baslangic != null || bitis != null) {
            log.info("GET /api/hareketler - Filtreleme: cariId={}, tarih={}-{}", cariHesapId, baslangic, bitis);
            List<HareketDTO> hareketler = hareketService.hareketleriFiltrele(cariHesapId, baslangic, bitis);
            return ResponseEntity.ok(hareketler);
        }
        log.info("GET /api/hareketler - Tüm hareketler getiriliyor, sirketId: {}", sirketId);
        List<HareketDTO> hareketler = hareketService.tumHareketleriGetir(sirketId);
        return ResponseEntity.ok(hareketler);
    }

    /**
     * Yeni hareket oluştur
     * POST /api/hareketler
     */
    @PostMapping
    public ResponseEntity<HareketDTO> hareketOlustur(@RequestBody @jakarta.validation.Valid HareketDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("POST /api/hareketler - Yeni hareket oluşturuluyor, sirketId: {}", sirketId);
        HareketDTO olusturulanHareket = hareketService.hareketOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanHareket);
    }

    /**
     * Hareket güncelle
     * PUT /api/hareketler/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<HareketDTO> hareketGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid HareketDTO dto) {
        log.info("PUT /api/hareketler/{} - Hareket güncelleniyor", id);
        HareketDTO guncellenen = hareketService.hareketGuncelle(id, dto);
        return ResponseEntity.ok(guncellenen);
    }

    /**
     * Hareket sil
     * DELETE /api/hareketler/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long id) {
        log.info("DELETE /api/hareketler/{} - Hareket siliniliyor", id);
        hareketService.hareketSil(id);
        return ResponseEntity.noContent().build();
    }
}
