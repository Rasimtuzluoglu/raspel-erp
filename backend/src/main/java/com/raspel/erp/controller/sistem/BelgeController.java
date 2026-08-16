package com.raspel.erp.controller.sistem;

import com.raspel.erp.entity.sistem.Belge;
import com.raspel.erp.repository.sistem.BelgeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.raspel.erp.entity.ticaret.Fatura;

@Tag(name = "Belge Yönetimi", description = "Fatura, sipariş gibi kayıtlara belge ekleme API")
@RestController
@RequestMapping("/api/belgeler")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BelgeController {

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
    private final Path belgeDir = Paths.get("uploads/belgeler").toAbsolutePath().normalize();

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
            Files.createDirectories(belgeDir);
            String filename = UUID.randomUUID().toString() + uzanti;
            Path target = belgeDir.resolve(filename).normalize();
            if (!target.startsWith(belgeDir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Geçersiz dosya yolu"));
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

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
    public ResponseEntity<List<Belge>> kayitBelgeleri(@PathVariable String entityAdi, @PathVariable Long entityId) {
        return ResponseEntity.ok(belgeRepository.findByEntityAdiAndEntityIdOrderByOlusturmaTarihiDesc(entityAdi, entityId));
    }

    @GetMapping
    @Operation(summary = "Tüm belgeleri getir", description = "Şirketin tüm belgelerini listeler")
    public ResponseEntity<List<Belge>> tumBelgeler(HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(belgeRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId));
    }

    @GetMapping("/indir/{filename}")
    @Operation(summary = "Belge indir", description = "Belgeyi indirir")
    public ResponseEntity<Resource> indir(@PathVariable String filename) {
        try {
            Path file = belgeDir.resolve(filename).normalize();
            if (!file.startsWith(belgeDir)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Belge sil", description = "Belgeyi siler")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        belgeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}