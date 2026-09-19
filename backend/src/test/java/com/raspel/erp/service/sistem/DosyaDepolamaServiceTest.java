package com.raspel.erp.service.sistem;

import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Yerel depolama modunda (MinIO kapali) dosya kaydetme/okuma davranislari.
 */
class DosyaDepolamaServiceTest {

    @TempDir
    Path tempDir;

    private DosyaDepolamaService service;

    @BeforeEach
    void setUp() {
        service = new DosyaDepolamaService(mock(MinioClient.class));
        ReflectionTestUtils.setField(service, "storageType", "local");
        ReflectionTestUtils.setField(service, "localRoot", tempDir.toAbsolutePath().normalize());
    }

    @AfterEach
    void tearDown() {
        // TempDir JUnit tarafindan temizlenir.
    }

    @Test
    void kaydetVeGetir_icerigiKorur() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "merhaba".getBytes());

        String filename = service.kaydet("belgeler", file);
        assertNotNull(filename);
        assertTrue(filename.endsWith(".txt"));

        DosyaDepolamaService.DepolananDosya dosya = service.getir("belgeler", filename);
        assertNotNull(dosya);
        assertEquals("merhaba", new String(dosya.icerik()));
    }

    @Test
    void getir_olmayanDosyaIcinNullDoner() {
        assertNull(service.getir("belgeler", "yok-12345.txt"));
    }

    @Test
    void getir_yolGezinmeDenemesiniReddeder() {
        assertNull(service.getir("belgeler", "../gizli.txt"));
    }

    @Test
    void kaydetBytes_dosyayiYazar() throws Exception {
        service.kaydetBytes("belgeler", "sabit.txt", "icerik".getBytes(), "text/plain");

        DosyaDepolamaService.DepolananDosya dosya = service.getir("belgeler", "sabit.txt");
        assertNotNull(dosya);
        assertEquals("icerik", new String(dosya.icerik()));
    }
}
