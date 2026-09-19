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

    @GetMapping("/uploads/teslimat-imzalari/{filename}")
    @Operation(summary = "Teslimat imzası getir", description = "Dijital teslim imzası PNG'sini döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DRIVER')")
    public ResponseEntity<byte[]> getTeslimatImza(@PathVariable String filename) {
        return dosyaGetir(filename, "teslimat-imzalari");
    }

    @GetMapping("/uploads/sohbet/{filename}")
    @Operation(summary = "Sohbet dosyası getir", description = "Sohbette paylaşılan dosyayı/görseli döndürür")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> getSohbetDosya(@PathVariable String filename) {
        return dosyaGetir(filename, "sohbet", true);
    }

    @GetMapping("/dosya/imzali-url")
    @Operation(summary = "İmzalı (presigned) URL", description = "Bir dosya için süreli erişim URL'i üretir (yalnızca ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> imzaliUrl(
            @RequestParam String klasor,
            @RequestParam String dosya,
            @RequestParam(defaultValue = "3600") int sure) {
        String url = dosyaDepolama.presignedUrl(klasor, dosya, sure);
        if (url == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "İmzalı URL üretilemedi (MinIO aktif olmayabilir)"));
        }
        return ResponseEntity.ok(Map.of("url", url));
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

        // Magic-byte doğrulaması: istemcinin beyan ettiği MIME/uzantıya güvenmek yerine
        // içerik imzasını kontrol et (depolanmış XSS/polyglot dosya engeli).
        try {
            byte[] bas;
            try (java.io.InputStream in = file.getInputStream()) {
                bas = in.readNBytes(12);
            }
            if (!magicByteGecerli(bas)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Dosya içeriği geçerli bir resim değil."));
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dosya okunamadı."));
        }

        try {
            String filename = dosyaDepolama.kaydet(klasor, file);
            return ResponseEntity.ok(Map.of("url", urlPrefix + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Dosya yüklenemedi: " + e.getMessage()));
        }
    }

    /** JPEG/PNG/GIF/WEBP içerik imzası kontrolü. */
    private static boolean magicByteGecerli(byte[] b) {
        if (b == null || b.length < 12) return false;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) return true;
        // GIF: "GIF8"
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38) return true;
        // WEBP: "RIFF" .... "WEBP"
        if (b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) return true;
        return false;
    }

    private ResponseEntity<byte[]> dosyaGetir(String filename, String klasor) {
        return dosyaGetir(filename, klasor, false);
    }

    private ResponseEntity<byte[]> dosyaGetir(String filename, String klasor, boolean resimDegilseIndir) {
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
        boolean resim = "image".equalsIgnoreCase(mediaType.getType());
        String disposition = (resimDegilseIndir && !resim ? "attachment" : "inline")
                + "; filename=\"" + filename.replace("\"", "") + "\"";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(dosya.icerik());
    }
}
