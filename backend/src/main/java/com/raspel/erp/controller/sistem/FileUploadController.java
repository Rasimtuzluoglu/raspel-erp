package com.raspel.erp.controller.sistem;

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
import com.raspel.erp.service.sistem.DosyaDepolamaService;

@Tag(name = "Dosya Yükleme", description = "Dosya yükleme ve sunma API")
@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final String AVATAR_KLASOR = "avatars";
    private static final String LOGO_KLASOR = "sirket-logos";
    private static final String FOTO_KLASOR = "fotolar";

    private static final List<String> IZIN_VERILEN_UZANTILAR = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> IZIN_VERILEN_MIME = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final DosyaDepolamaService dosyaDepolama;

    public FileUploadController(DosyaDepolamaService dosyaDepolama) {
        this.dosyaDepolama = dosyaDepolama;
    }

    @PostMapping("/upload/avatar")
    @Operation(summary = "Avatar yükle", description = "Kullanıcı avatarı yükler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, AVATAR_KLASOR, "/api/uploads/avatars/");
    }

    @GetMapping("/uploads/avatars/{filename}")
    @Operation(summary = "Avatar getir", description = "Kullanıcı avatarını döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        return dosyaGetir(filename, AVATAR_KLASOR);
    }

    @PostMapping("/upload/sirket-logo")
    @Operation(summary = "Şirket logosu yükle", description = "Şirket logosu yükler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadSirketLogo(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, LOGO_KLASOR, "/api/uploads/sirket-logos/");
    }

    @PostMapping("/upload/foto")
    @Operation(summary = "Cari/ürün fotoğrafı yükle", description = "Cari hesap veya ürün fotoğrafı yükler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, String>> uploadFoto(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, FOTO_KLASOR, "/api/uploads/fotolar/");
    }

    @GetMapping("/uploads/fotolar/{filename}")
    @Operation(summary = "Fotoğraf getir", description = "Cari/ürün fotoğrafını döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> getFoto(@PathVariable String filename) {
        return dosyaGetir(filename, FOTO_KLASOR);
    }

    @GetMapping("/uploads/sirket-logos/{filename}")
    @Operation(summary = "Şirket logosu getir", description = "Şirket logosunu döndürür (public)")
    public ResponseEntity<byte[]> getSirketLogo(@PathVariable String filename) {
        return dosyaGetir(filename, LOGO_KLASOR);
    }

    @GetMapping("/uploads/teslimat-fotolari/{filename}")
    @Operation(summary = "Teslimat fotoğrafı getir", description = "Teslimat fotoğrafını döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<byte[]> getTeslimatFoto(@PathVariable String filename) {
        return dosyaGetir(filename, "teslimat-fotolari");
    }

    private ResponseEntity<Map<String, String>> dosyaYukle(MultipartFile file, String klasor, String urlPrefix) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dosya boş"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !IZIN_VERILEN_MIME.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Geçersiz dosya tipi. Yalnızca resim yükleyebilirsiniz (JPG, PNG, WEBP, GIF)."));
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!IZIN_VERILEN_UZANTILAR.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Geçersiz dosya uzantısı. Yalnızca resim yükleyebilirsiniz (JPG, PNG, WEBP, GIF)."));
        }

        try {
            String filename = dosyaDepolama.kaydet(klasor, file);
            return ResponseEntity.ok(Map.of("url", urlPrefix + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Dosya yüklenemedi: " + e.getMessage()));
        }
    }

    private ResponseEntity<byte[]> dosyaGetir(String filename, String klasor) {
        DosyaDepolamaService.DepolananDosya dosya = dosyaDepolama.getir(klasor, filename);
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
}
