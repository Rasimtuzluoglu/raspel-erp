package com.raspel.erp.controller.sistem;

import com.raspel.erp.entity.sistem.Belge;
import com.raspel.erp.repository.sistem.BelgeRepository;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Belge Yönetimi", description = "Fatura, sipariş gibi kayıtlara belge ekleme API")
@RestController
@RequestMapping("/api/belgeler")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BelgeController {

    private static final String BELGE_KLASOR = "belgeler";

    private static final Set<String> IZIN_VERILEN_UZANTILAR = Set.of(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".csv",
        ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp",
        ".txt", ".rtf", ".zip"
    );
    private static final Set<String> IZIN_VERILEN_MIME = Set.of(
        "application/pdf",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/csv", "text/plain", "application/rtf",
        "image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp",
        "application/zip", "application/x-zip-compressed"
    );

    private final BelgeRepository belgeRepository;
    private final DosyaDepolamaService dosyaDepolama;

    @PostMapping("/yukle")
    @Operation(summary = "Belge yükle", description = "Bir kayda (fatura, sipariş vb.) dosya iliştirir")
    public ResponseEntity<?> yukle(@RequestParam("file") MultipartFile file,
                                   @RequestParam("entityAdi") String entityAdi,
                                   @RequestParam("entityId") Long entityId,
                                   HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dosya boş"));
        }
        String contentType = file.getContentType();
        String orjinalAd = file.getOriginalFilename() != null ? file.getOriginalFilename() : "dosya";
        String uzanti = "";
        if (orjinalAd.contains(".")) {
            uzanti = orjinalAd.substring(orjinalAd.lastIndexOf(".")).toLowerCase();
        }
        if ((contentType != null && !IZIN_VERILEN_MIME.contains(contentType.toLowerCase()))
                || !IZIN_VERILEN_UZANTILAR.contains(uzanti)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Geçersiz dosya tipi. İzin verilenler: PDF, Office, resim, metin, ZIP"));
        }
        Long sirketId = (Long) request.getAttribute("sirketId");
        try {
            String filename = dosyaDepolama.kaydet(BELGE_KLASOR, file);
            String url = "/api/belgeler/indir/" + filename;
            Belge belge = belgeRepository.save(Belge.builder()
                    .entityAdi(entityAdi).entityId(entityId)
                    .dosyaAdi(orjinalAd).url(url).sirketId(sirketId)
                    .build());

            return ResponseEntity.ok(belge);
        } catch (IOException e) {
            log.error("Belge yüklenemedi", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Dosya yüklenemedi: " + e.getMessage()));
        }
    }

    @GetMapping("/kayit/{entityAdi}/{entityId}")
    @Operation(summary = "Kayda ait belgeleri getir", description = "Bir kayda iliştirilmiş belgeleri listeler")
    public ResponseEntity<List<Belge>> kayitBelgeleri(@PathVariable String entityAdi, @PathVariable Long entityId, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(belgeRepository.findByEntityAdiAndEntityIdAndSirketIdOrderByOlusturmaTarihiDesc(entityAdi, entityId, sirketId));
    }

    @GetMapping
    @Operation(summary = "Tüm belgeleri getir", description = "Şirketin tüm belgelerini listeler")
    public ResponseEntity<List<Belge>> tumBelgeler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(belgeRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId));
    }

    @GetMapping("/indir/{filename}")
    @Operation(summary = "Belge indir", description = "Belgeyi indirir")
    public ResponseEntity<byte[]> indir(@PathVariable String filename, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        boolean aitMi = belgeRepository.findByUrlEndingWith(filename).stream()
                .anyMatch(b -> b.getSirketId() == null || b.getSirketId().equals(sirketId));
        if (!aitMi) {
            return ResponseEntity.notFound().build();
        }
        DosyaDepolamaService.DepolananDosya dosya = dosyaDepolama.getir(BELGE_KLASOR, filename);
        if (dosya == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(dosya.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(dosya.icerik());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Belge sil", description = "Belgeyi siler")
    public ResponseEntity<Void> sil(@PathVariable Long id, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        Belge belge = belgeRepository.findById(id).orElse(null);
        if (belge == null || (belge.getSirketId() != null && !belge.getSirketId().equals(sirketId))) {
            return ResponseEntity.notFound().build();
        }
        if (belge.getUrl() != null && belge.getUrl().contains("/")) {
            String filename = belge.getUrl().substring(belge.getUrl().lastIndexOf("/") + 1);
            dosyaDepolama.sil(BELGE_KLASOR, filename);
        }
        belgeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
