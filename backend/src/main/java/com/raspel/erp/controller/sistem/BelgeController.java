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
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
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
        // Uzanti/MIME istemci tarafindan taklit edilebilir; icerik imzasini (magic byte) dogrula.
        if (!icerikImzasiGecerliMi(file, uzanti)) {
            log.warn("Belge icerik imzasi uzantiyla uyusmuyor: {}", orjinalAd);
            return ResponseEntity.badRequest().body(Map.of("error", "Dosya içeriği uzantısıyla uyuşmuyor"));
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
            return ResponseEntity.internalServerError().body(Map.of("error", "Dosya yüklenemedi"));
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
        // Yol gezinme (path traversal) ve tenant disi erisimi kapali tut.
        if (filename == null || filename.contains("..") || !filename.matches("[A-Za-z0-9._-]{1,128}")) {
            return ResponseEntity.notFound().build();
        }
        boolean aitMi = sirketId != null && belgeRepository.findByUrlEndingWith(filename).stream()
                .anyMatch(b -> sirketId.equals(b.getSirketId()));
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

    /**
     * Uzantiya gore dosya icerik imzasini dogrular. Imza degilse istemci MIME/uzanti
     * taklidi yapiyor olabilir; yuklemeyi reddederiz.
     */
    private boolean icerikImzasiGecerliMi(MultipartFile file, String uzanti) {
        byte[] b;
        try (java.io.InputStream in = file.getInputStream()) {
            b = in.readNBytes(12);
        } catch (IOException e) {
            return false;
        }
        return switch (uzanti) {
            case ".pdf" -> baslar(b, 0x25, 0x50, 0x44, 0x46);
            case ".png" -> baslar(b, 0x89, 0x50, 0x4E, 0x47);
            case ".jpg", ".jpeg" -> baslar(b, 0xFF, 0xD8, 0xFF);
            case ".gif" -> baslar(b, 0x47, 0x49, 0x46, 0x38);
            case ".bmp" -> baslar(b, 0x42, 0x4D);
            case ".webp" -> b.length >= 12 && baslar(b, 0x52, 0x49, 0x46, 0x46)
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            case ".zip", ".docx", ".xlsx" -> baslar(b, 0x50, 0x4B, 0x03, 0x04)
                    || baslar(b, 0x50, 0x4B, 0x05, 0x06);
            case ".doc", ".xls" -> baslar(b, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case ".rtf" -> baslar(b, 0x7B, 0x5C, 0x72, 0x74, 0x66);
            // Metin tabanli uzantilarda imza yoktur; NUL bayti iceren ikili dosya kabul edilmez.
            default -> !nulBaytiVarMi(b);
        };
    }

    private boolean baslar(byte[] veri, int... imza) {
        if (veri.length < imza.length) return false;
        for (int i = 0; i < imza.length; i++) {
            if ((veri[i] & 0xFF) != imza[i]) return false;
        }
        return true;
    }

    private boolean nulBaytiVarMi(byte[] veri) {
        for (byte b : veri) {
            if (b == 0) return true;
        }
        return false;
    }
}
