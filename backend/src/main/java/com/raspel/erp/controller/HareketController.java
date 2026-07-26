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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDate;
import java.util.List;

/**
 * Hareket Controller
 * Hareket işlemleri için REST API endpoint'lerini sağlar.
 */
@Tag(name = "Hareketler", description = "Cari hesap hareketleri API")
@RestController
@RequestMapping("/api/hareketler")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class HareketController {
    
    private final HareketService hareketService;
    
    @GetMapping("/cari/{cariHesapId}")
    @Operation(summary = "Cari hesap hareketlerini getir", description = "Belirli bir cari hesaba ait hareketleri listeler")
    public ResponseEntity<List<HareketDTO>> cariHesapHareketleriGetir(@PathVariable Long cariHesapId) {
        log.info("GET /api/hareketler/cari/{} - Cari hesap hareketleri getiriliyor", cariHesapId);
        List<HareketDTO> hareketler = hareketService.cariHesapHareketleriGetir(cariHesapId);
        return ResponseEntity.ok(hareketler);
    }
    
    @GetMapping("/son/{limit}")
    @Operation(summary = "Son hareketleri getir", description = "Son n hareketi getirir (Dashboard için)")
    public ResponseEntity<List<HareketDTO>> sonHareketleriGetir(@PathVariable int limit) {
        log.info("GET /api/hareketler/son/{} - Son {} hareket getiriliyor", limit, limit);
        List<HareketDTO> hareketler = hareketService.sonHareketleriGetir(limit);
        return ResponseEntity.ok(hareketler);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Hareketleri CSV dışa aktar", description = "Hareketleri CSV dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> hareketlerCsv(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/hareketler/export/csv - CSV dışa aktarım, sirketId: {}", sirketId);
        List<HareketDTO> liste = hareketService.tumHareketleriGetir(sirketId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

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

    @GetMapping
    @Operation(summary = "Tüm hareketleri getir/filtrele", description = "Tüm hareketleri getirir veya filtreleme yapar")
    public ResponseEntity<?> tumHareketleriGetir(
            @RequestParam(required = false) Long cariHesapId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        if (cariHesapId != null || baslangic != null || bitis != null) {
            log.info("GET /api/hareketler - Filtreleme: cariId={}, tarih={}-{}", cariHesapId, baslangic, bitis);
            List<HareketDTO> hareketler = hareketService.hareketleriFiltrele(cariHesapId, baslangic, bitis);
            return ResponseEntity.ok(hareketler);
        }
        log.info("GET /api/hareketler - Tüm hareketler getiriliyor, sirketId: {}", sirketId);
        Page<HareketDTO> hareketler = hareketService.tumHareketleriGetir(sirketId, pageable);
        return ResponseEntity.ok(hareketler);
    }

    @PostMapping
    @Operation(summary = "Yeni hareket oluştur", description = "Cari hesaba yeni bir hareket (tahsilat/ödeme) oluşturur")
    public ResponseEntity<HareketDTO> hareketOlustur(@RequestBody @jakarta.validation.Valid HareketDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("POST /api/hareketler - Yeni hareket oluşturuluyor, sirketId: {}", sirketId);
        HareketDTO olusturulanHareket = hareketService.hareketOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanHareket);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Hareket güncelle", description = "Hareket bilgilerini günceller")
    public ResponseEntity<HareketDTO> hareketGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid HareketDTO dto) {
        log.info("PUT /api/hareketler/{} - Hareket güncelleniyor", id);
        HareketDTO guncellenen = hareketService.hareketGuncelle(id, dto);
        return ResponseEntity.ok(guncellenen);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hareket sil", description = "Hareketi siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hareketSil(@PathVariable Long id) {
        log.info("DELETE /api/hareketler/{} - Hareket siliniliyor", id);
        hareketService.hareketSil(id);
        return ResponseEntity.noContent().build();
    }
}
