package com.raspel.erp.controller.sistem;

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
import java.util.UUID;
import com.raspel.erp.entity.sistem.Sirket;

@Tag(name = "Dosya Yükleme", description = "Dosya yükleme ve sunma API")
@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final Path avatarDir = Paths.get("uploads/avatars").toAbsolutePath().normalize();
    private final Path sirketLogoDir = Paths.get("uploads/sirket-logos").toAbsolutePath().normalize();
    private final Path fotoDir = Paths.get("uploads/fotolar").toAbsolutePath().normalize();

    private static final List<String> IZIN_VERILEN_UZANTILAR = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> IZIN_VERILEN_MIME = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @PostMapping("/upload/avatar")
    @Operation(summary = "Avatar yükle", description = "Kullanıcı avatarı yükler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, avatarDir, "/api/uploads/avatars/");
    }

    @GetMapping("/uploads/avatars/{filename}")
    @Operation(summary = "Avatar getir", description = "Kullanıcı avatarını döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        return dosyaGetir(filename, avatarDir);
    }

    @PostMapping("/upload/sirket-logo")
    @Operation(summary = "Şirket logosu yükle", description = "Şirket logosu yükler (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadSirketLogo(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, sirketLogoDir, "/api/uploads/sirket-logos/");
    }

    @PostMapping("/upload/foto")
    @Operation(summary = "Cari/ürün fotoğrafı yükle", description = "Cari hesap veya ürün fotoğrafı yükler")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, String>> uploadFoto(@RequestParam("file") MultipartFile file) {
        return dosyaYukle(file, fotoDir, "/api/uploads/fotolar/");
    }

    @GetMapping("/uploads/fotolar/{filename}")
    @Operation(summary = "Fotoğraf getir", description = "Cari/ürün fotoğrafını döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Resource> getFoto(@PathVariable String filename) {
        return dosyaGetir(filename, fotoDir);
    }

    @GetMapping("/uploads/sirket-logos/{filename}")
    @Operation(summary = "Şirket logosu getir", description = "Şirket logosunu döndürür (public)")
    public ResponseEntity<Resource> getSirketLogo(@PathVariable String filename) {
        return dosyaGetir(filename, sirketLogoDir);
    }

    private ResponseEntity<Map<String, String>> dosyaYukle(MultipartFile file, Path baseDir, String urlPrefix) {
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
            Files.createDirectories(baseDir);

            String filename = UUID.randomUUID().toString() + ext;
            Path target = baseDir.resolve(filename).normalize();

            if (!target.startsWith(baseDir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Geçersiz dosya yolu"));
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(Map.of("url", urlPrefix + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Dosya yüklenemedi: " + e.getMessage()));
        }
    }

    private ResponseEntity<Resource> dosyaGetir(String filename, Path baseDir) {
        try {
            Path file = baseDir.resolve(filename).normalize();

            // Path Traversal koruması (../../ engelleme)
            if (!file.startsWith(baseDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) contentType = "application/octet-stream";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}