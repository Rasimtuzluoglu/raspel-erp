package com.raspel.erp.controller.finans;

import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.service.finans.CariHesapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.stream.Collectors;
import com.raspel.erp.entity.finans.CariHesap;

/**
 * Cari Hesap Controller
 * Cari hesap işlemleri için REST API endpoint'lerini sağlar.
 */
@Tag(name = "Cari Hesaplar", description = "Cari hesap yönetimi API")
@RestController
@RequestMapping("/api/cari-hesaplar")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class CariHesapController {
    
    private final CariHesapService cariHesapService;
    
    @GetMapping
    @Operation(summary = "Tüm cari hesapları getir (sayfalı)", description = "Şirkete ait tüm cari hesapları sayfalı olarak listeler")
    public ResponseEntity<Page<CariHesapDTO>> tumCariHesaplariGetir(
            HttpServletRequest request,
            @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar - Tüm cari hesaplar getiriliyor, sirketId: {}", sirketId);
        Page<CariHesapDTO> cariHesaplar = cariHesapService.tumCariHesaplariGetir(sirketId, pageable);
        return ResponseEntity.ok(cariHesaplar);
    }
    
    @GetMapping("/export/csv")
    @Operation(summary = "Cari hesapları CSV dışa aktar", description = "Cari hesapları CSV dosyası olarak dışa aktarır")
    public ResponseEntity<byte[]> cariHesaplarCsv(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar/export/csv - CSV dışa aktarım, sirketId: {}", sirketId);
        List<CariHesapDTO> liste = cariHesapService.tumCariHesaplariGetir(sirketId, Pageable.unpaged()).getContent();

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
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("cari-hesaplar.csv").build());
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

    @GetMapping("/search")
    @Operation(summary = "Cari hesap ara", description = "Cari hesapları ada göre arar")
    public ResponseEntity<List<CariHesapDTO>> cariHesapAra(@RequestParam String q, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("GET /api/cari-hesaplar/search - Arama: {}, sirketId: {}", q, sirketId);
        List<CariHesapDTO> sonuclar = cariHesapService.cariHesapAra(q, sirketId);
        return ResponseEntity.ok(sonuclar);
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre cari hesap getir", description = "Cari hesap ID'sine göre detayları getirir")
    public ResponseEntity<CariHesapDTO> cariHesapGetir(@PathVariable Long id) {
        log.info("GET /api/cari-hesaplar/{} - Cari hesap getiriliyor", id);
        CariHesapDTO cariHesap = cariHesapService.cariHesapGetir(id);
        return ResponseEntity.ok(cariHesap);
    }

    @PostMapping
    @Operation(summary = "Yeni cari hesap oluştur", description = "Yeni bir cari hesap oluşturur")
    public ResponseEntity<CariHesapDTO> cariHesapOlustur(@RequestBody @jakarta.validation.Valid CariHesapDTO dto, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        log.info("POST /api/cari-hesaplar - Yeni cari hesap oluşturuluyor: {}, sirketId: {}", dto.getAd(), sirketId);
        CariHesapDTO olusturulanCariHesap = cariHesapService.cariHesapOlustur(dto, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanCariHesap);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cari hesap güncelle", description = "Cari hesap bilgilerini günceller")
    public ResponseEntity<CariHesapDTO> cariHesapGuncelle(@PathVariable Long id, @RequestBody @jakarta.validation.Valid CariHesapDTO dto) {
        log.info("PUT /api/cari-hesaplar/{} - Cari hesap güncelleniyor", id);
        CariHesapDTO guncellenenCariHesap = cariHesapService.cariHesapGuncelle(id, dto);
        return ResponseEntity.ok(guncellenenCariHesap);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cari hesap sil", description = "Cari hesabı siler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cariHesapSil(@PathVariable Long id) {
        log.info("DELETE /api/cari-hesaplar/{} - Cari hesap siliniliyor", id);
        cariHesapService.cariHesapSil(id);
        return ResponseEntity.noContent().build();
    }

    // CARİYE ÖZEL FİYAT

    @GetMapping("/{id}/fiyatlar")
    @Operation(summary = "Cariye özel fiyatlar", description = "Bir cariye özel tanımlanmış ürün fiyatlarını listeler")
    public ResponseEntity<List<com.raspel.erp.dto.finans.CariFiyatDTO>> cariFiyatlar(@PathVariable Long id) {
        return ResponseEntity.ok(cariHesapService.cariFiyatlari(id));
    }

    @PostMapping("/{id}/fiyatlar")
    @Operation(summary = "Cariye özel fiyat ekle", description = "Cariye özel ürün fiyatı ekler/günceller")
    public ResponseEntity<com.raspel.erp.dto.finans.CariFiyatDTO> cariFiyatKaydet(
            @PathVariable Long id,
            @RequestBody com.raspel.erp.dto.finans.CariFiyatDTO dto,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(cariHesapService.cariFiyatKaydet(id, dto, sirketId));
    }

    @DeleteMapping("/fiyatlar/{fiyatId}")
    @Operation(summary = "Cariye özel fiyat sil", description = "Cariye özel fiyatı siler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cariFiyatSil(@PathVariable Long fiyatId) {
        cariHesapService.cariFiyatSil(fiyatId);
        return ResponseEntity.noContent().build();
    }
}