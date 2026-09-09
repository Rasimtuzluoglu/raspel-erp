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
     * Dosyayı ilgili klasör altına kaydeder ve üretilen benzersiz dosya adını döndürür.
     */
    public String kaydet(String klasor, MultipartFile file) throws IOException {
        String orjinalAd = file.getOriginalFilename() != null ? file.getOriginalFilename() : "dosya";
        String uzanti = "";
        if (orjinalAd.contains(".")) {
            uzanti = orjinalAd.substring(orjinalAd.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID().toString() + uzanti;

        if (minioAktif()) {
            bucketOlustur();
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(klasor + "/" + filename)
                        .stream(is, file.getSize(), -1)
                        .contentType(file.getContentType())
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
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
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
