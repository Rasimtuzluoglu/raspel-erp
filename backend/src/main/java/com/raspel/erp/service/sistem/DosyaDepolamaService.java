package com.raspel.erp.service.sistem;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dosya depolama soyutlaması. app.storage.type=minio ise MinIO'ya, değilse yerel
 * dosya sistemine yazar. Üst katman (FileUploadController, BelgeController) bu farkı bilmez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DosyaDepolamaService {

    private final MinioClient minioClient;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.minio.bucket:raspel-erp}")
    private String bucket;

    private final Path localRoot = Paths.get("uploads").toAbsolutePath().normalize();

    private boolean minioAktif() {
        return "minio".equalsIgnoreCase(storageType);
    }

    public boolean isMinioAktif() {
        return minioAktif();
    }

    /**
     * MinIO için süreli (imzalı) erişim URL'i üretir. Yerel modda null döner.
     */
    public String presignedUrl(String klasor, String filename, int saniye) {
        if (!minioAktif()) return null;
        // Path traversal ve anahtar kacisi engeli: klasor/dosya adi '..' veya '/'
        // iceremez; sure MaxIO siniri olan 7 gune (604800 sn) kisitlanir.
        String k = guvenliNesneParcasi(klasor);
        String d = guvenliNesneParcasi(filename);
        if (k == null || d == null) {
            log.warn("İmzalı URL reddedildi (geçersiz nesne yolu)");
            return null;
        }
        int sure = Math.max(60, Math.min(saniye, 604800));
        try {
            bucketOlustur();
            return minioClient.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucket)
                    .object(k + "/" + d)
                    .expiry(sure)
                    .build());
        } catch (Exception e) {
            log.warn("İmzalı URL üretilemedi: {}", e.getMessage());
            return null;
        }
    }

    /** Klasor/dosya adi icin tek segment ve '..'-guvenli deger dondurur; gecersizse null. */
    private static String guvenliNesneParcasi(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.contains("..") || t.contains("/") || t.contains("\\")
                || t.contains("\u0000") || t.startsWith(".")) {
            return null;
        }
        return t;
    }

    /**
     * Depolama kullanım bilgisi (MinIO aktifse nesne sayısı ve toplam boyut).
     */
    public java.util.Map<String, Object> kullanim() {
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("tip", minioAktif() ? "minio" : "local");
        r.put("bucket", bucket);
        if (!minioAktif()) {
            r.put("nesneSayisi", 0);
            r.put("toplamBoyut", 0);
            return r;
        }
        try {
            bucketOlustur();
            long sayi = 0;
            long boyut = 0;
            Iterable<io.minio.Result<io.minio.messages.Item>> items = minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
            for (io.minio.Result<io.minio.messages.Item> res : items) {
                io.minio.messages.Item item = res.get();
                sayi++;
                boyut += item.size();
            }
            r.put("nesneSayisi", sayi);
            r.put("toplamBoyut", boyut);
        } catch (Exception e) {
            r.put("hata", e.getMessage());
        }
        return r;
    }

    /**
     * Ham byte dizisini kaydeder (yedekleme gibi multipart olmayan akışlar için).
     */
    public void kaydetBytes(String klasor, String filename, byte[] icerik, String contentType) {
        try {
            if (minioAktif()) {
                bucketOlustur();
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(icerik)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(klasor + "/" + filename)
                            .stream(bis, icerik.length, -1)
                            .contentType(contentType)
                            .build());
                }
            } else {
                Path dir = localRoot.resolve(klasor).normalize();
                Files.createDirectories(dir);
                Files.write(dir.resolve(filename), icerik);
            }
        } catch (Exception e) {
            log.warn("Dosya kaydedilemedi ({}): {}", filename, e.getMessage());
            throw new RuntimeException("Dosya kaydedilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Resim içerik imzası (magic byte) doğrulaması. Tarayıcı-beyanlı MIME/uzantıya
     * güvenmek yerine içerik gerçekten resim mi diye kontrol eder (polyglot/stored XSS engeli).
     */
    public static boolean resimMagicByteGecerli(byte[] b) {
        if (b == null || b.length < 12) return false;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) return true;
        // GIF: "GIF8"
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38) return true;
        // WEBP: "RIFF"...."WEBP"
        if (b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) return true;
        return false;
    }

    /** İçerik imzası geçerli resim değilse hata fırlatan kaydetme (upload uçları için). */
    public String kaydetResimDogrulamali(String klasor, MultipartFile file) throws IOException {
        byte[] bas;
        try (InputStream in = file.getInputStream()) {
            bas = in.readNBytes(12);
        }
        if (!resimMagicByteGecerli(bas)) {
            throw new IOException("Dosya içeriği geçerli bir resim değil");
        }
        return kaydet(klasor, file);
    }

    /**
     * Dosyayı ilgili klasör altına kaydeder ve üretilen benzersiz dosya adını döndürür.
     * Sunucu tarafında content-type'ı uzantıya göre belirler (istemci beyanına güvenmez).
     */
    public String kaydet(String klasor, MultipartFile file) throws IOException {
        String orjinalAd = file.getOriginalFilename() != null ? file.getOriginalFilename() : "dosya";
        String uzanti = "";
        if (orjinalAd.contains(".")) {
            uzanti = orjinalAd.substring(orjinalAd.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID().toString() + uzanti;

        // Sunucu-tarafi content-type: uzantidan turetilir (istemci Content-Type'ina guvenilmez).
        String sunucuMime = mimeFromUzanti(uzanti);
        if (minioAktif()) {
            bucketOlustur();
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(klasor + "/" + filename)
                        .stream(is, file.getSize(), -1)
                        .contentType(sunucuMime)
                        .build());
            } catch (Exception e) {
                throw new IOException("MinIO'ya yüklenemedi: " + e.getMessage(), e);
            }
        } else {
            Path dir = localRoot.resolve(klasor).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new IOException("Geçersiz dosya yolu");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return filename;
    }

    /** Uzantiya karsilik gelen MIME turu (bilinmeyenler octet-stream). */
    private static String mimeFromUzanti(String uzanti) {
        if (uzanti == null) return "application/octet-stream";
        return switch (uzanti.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".sv\u0067" -> "image/svg+xml";
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".csv" -> "text/csv";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    /**
     * Dosyayı içerik + içerik tipi olarak döndürür.
     */
    public DepolananDosya getir(String klasor, String filename) {
        try {
            if (minioAktif()) {
                String objectName = klasor + "/" + filename;
                String contentType = "application/octet-stream";
                try {
                    contentType = minioClient.statObject(StatObjectArgs.builder()
                            .bucket(bucket).object(objectName).build()).contentType();
                } catch (Exception ignored) {
                }
                try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket).object(objectName).build())) {
                    return new DepolananDosya(okununca(is), contentType);
                }
            } else {
                Path file = localRoot.resolve(klasor).resolve(filename).normalize();
                Path dir = localRoot.resolve(klasor).normalize();
                if (!file.startsWith(dir) || !Files.exists(file)) {
                    return null;
                }
                byte[] icerik = Files.readAllBytes(file);
                String contentType = Files.probeContentType(file);
                if (contentType == null) contentType = "application/octet-stream";
                return new DepolananDosya(icerik, contentType);
            }
        } catch (Exception e) {
            log.warn("Dosya okunamadı ({}): {}", filename, e.getMessage());
            return null;
        }
    }

    public void sil(String klasor, String filename) {
        try {
            if (minioAktif()) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket).object(klasor + "/" + filename).build());
            } else {
                Path file = localRoot.resolve(klasor).resolve(filename).normalize();
                Files.deleteIfExists(file);
            }
        } catch (Exception e) {
            log.warn("Dosya silinemedi ({}): {}", filename, e.getMessage());
        }
    }

    /**
     * MinIO'da bir klasör altındaki nesneleri (ad, boyut, değiştirme zamanı) listeler.
     * Yerel modda boş liste döner.
     */
    public List<NesneBilgi> listele(String klasor) {
        List<NesneBilgi> sonuc = new ArrayList<>();
        if (!minioAktif()) return sonuc;
        try {
            bucketOlustur();
            Iterable<io.minio.Result<io.minio.messages.Item>> items = minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder().bucket(bucket).prefix(klasor + "/").recursive(true).build());
            for (io.minio.Result<io.minio.messages.Item> res : items) {
                io.minio.messages.Item item = res.get();
                String ad = item.objectName();
                if (ad.startsWith(klasor + "/")) {
                    ad = ad.substring(klasor.length() + 1);
                }
                java.time.ZonedDateTime sonDeg = item.lastModified();
                sonuc.add(new NesneBilgi(ad, item.size(), sonDeg == null ? 0L : sonDeg.toInstant().toEpochMilli()));
            }
        } catch (Exception e) {
            log.warn("MinIO nesneleri listelenemedi ({}): {}", klasor, e.getMessage());
        }
        return sonuc;
    }

    public record NesneBilgi(String ad, long boyut, long sonDegistirme) {}

    private void bucketOlustur() {
        try {
            boolean var = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!var) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket oluşturuldu: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket kontrolü/oluşturma başarısız: {}", e.getMessage());
        }
    }

    private byte[] okununca(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toByteArray();
    }

    public record DepolananDosya(byte[] icerik, String contentType) {}
}
