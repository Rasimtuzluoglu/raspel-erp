package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decompression bomb koruması testleri.
 *
 * <p>{@code ImageIO.read} decode sırasında tam piksel tamponu ayırır. Sıkıştırılmış
 * boyutu küçük ama decode boyutu çok büyük görseller (PNG "zip bomb") JVM'i
 * OOM ile düşürüyordu; multipart limiti (10 MB) bunu engellemiyordu. Ölçekleme
 * decode SONRASINDA yapıldığı için koruma ancak başlık bilgisi okunarak
 * decode'dan önce uygulanabildi.
 */
class ResimIslemeServiceTest {

    private final ResimIslemeService servis = new ResimIslemeService();

    @Test
    void normalBoyutluGorselIslenir() throws IOException {
        byte[] png = olustur(800, 600);

        var sonuc = servis.isle(png, "test-uuid", 1200, 200, 0.85f);

        assertNotNull(sonuc);
        assertNotNull(sonuc.dosyaAdi());
        assertTrue(sonuc.icerik().length > 0);
        assertTrue(sonuc.thumbIcerik().length > 0);
        // Opak görseller JPEG'e dönüştürülür (dosya boyutu/bandwidth için).
        assertEquals(".jpg", dosyaUzantisi(sonuc.dosyaAdi()));
        assertEquals("image/jpeg", sonuc.contentType());
    }

    @Test
    void buyukPikselGorseliDecodeOncesiReddedilir() throws IOException {
        // 8000x8000 = 64.000.000 piksel > MAKS_PIKSEL (40M).
        // Gerçek piksel verisi OOM'e yol açacağı için yalnızca PNG başlığı
        // (IHDR) üretiliyor: koruma decode'dan ÖNCE çalıştığı için
        // IDAT verisine hiç dokunulmadan reddedilmesi bekleniyor.
        byte[] bomb = sahtePngBasligi(8000, 8000);

        IOException hata = assertThrows(IOException.class,
                () -> servis.isle(bomb, "bomb", 1200, 200, 0.85f));
        assertTrue(hata.getMessage().contains("piksel"),
                "Hata mesajı piksel sınırını belirtmeli, alınan: " + hata.getMessage());
    }

    @Test
    void sinirIcindeBaslikliKucukGorselKabulEdilir() throws IOException {
        byte[] png = olustur(400, 300);
        var sonuc = servis.isle(png, "kucuk", 1200, 200, 0.85f);
        assertNotNull(sonuc);
        assertTrue(sonuc.icerik().length > 0);
    }

    @Test
    void okunamayanGorsiHataVerir() {
        byte[] bozuk = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        IOException hata = assertThrows(IOException.class,
                () -> servis.isle(bozuk, "test", 1200, 200, 0.85f));
        assertTrue(hata.getMessage().contains("okunamadı") || hata.getMessage().contains("boyut"));
    }

    @Test
    void bosGirdiHataVerir() {
        assertThrows(IOException.class, () -> servis.isle(new byte[0], "test", 1200, 200, 0.85f));
        assertThrows(IOException.class, () -> servis.isle(null, "test", 1200, 200, 0.85f));
    }

    @Test
    void ogelerGriVeKirpilmaz() throws IOException {
        // 2000x800 -> maxKenar 1200: oran korunur (2000/800 = 2.5 -> 1200x480)
        byte[] png = olustur(2000, 800);

        var sonuc = servis.isle(png, "oran", 1200, 200, 0.85f);

        BufferedImage okunan = ImageIO.read(new ByteArrayInputStream(sonuc.icerik()));
        assertNotNull(okunan);
        assertEquals(1200, okunan.getWidth());
        assertEquals(480, okunan.getHeight());
    }

    @Test
    void kucukGorselBuyutulmez() throws IOException {
        // Ölçekleme yalnızca küçültür; 800x600 sınırın altındaysa dokunulmaz.
        byte[] png = olustur(800, 600);

        var sonuc = servis.isle(png, "kucultme", 1200, 200, 0.85f);

        BufferedImage okunan = ImageIO.read(new ByteArrayInputStream(sonuc.icerik()));
        assertNotNull(okunan);
        assertEquals(800, okunan.getWidth());
        assertEquals(600, okunan.getHeight());
    }

    @Test
    void thumbnailKucukTurler() throws IOException {
        byte[] png = olustur(2000, 2000);

        var sonuc = servis.isle(png, "thumb", 1200, 200, 0.85f);

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(sonuc.thumbIcerik()));
        assertNotNull(thumb);
        assertTrue(thumb.getWidth() <= 200 && thumb.getHeight() <= 200,
                "Thumbnail küçültülmeli, ölçü: " + thumb.getWidth() + "x" + thumb.getHeight());
    }

    @Test
    void formatTipiUzantiyaUyarisi() throws IOException {
        byte[] png = olustur(400, 400);
        var sonuc = servis.isle(png, "tip", 1200, 200, 0.85f);
        // Opak görsel -> JPEG (PNG değil), çünkü saydam kanal yok.
        assertEquals(".jpg", dosyaUzantisi(sonuc.dosyaAdi()));
        assertEquals("image/jpeg", sonuc.contentType());
    }

    @Test
    void saydamGorselPngKalir() throws IOException {
        byte[] png = olustur(400, 400, true);
        var sonuc = servis.isle(png, "saydam", 1200, 200, 0.85f);
        assertEquals(".png", dosyaUzantisi(sonuc.dosyaAdi()));
        assertEquals("image/png", sonuc.contentType());
    }

    private String dosyaUzantisi(String ad) {
        int nokta = ad.lastIndexOf('.');
        return nokta < 0 ? "" : ad.substring(nokta);
    }

    /**
     * Yalnızca PNG imza + IHDR başlığı üretir (piksel verisi yoktur).
     * {@code ImageReader.getWidth/getHeight} başlıktan okur; koruma decode'dan
     * önce çalıştığı için gerçek piksel verisi olmadan da sınır uygulanır.
     */
    private byte[] sahtePngBasligi(int g, int y) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        byte[] ihdrVeri = new byte[13];
        ihdrVeri[0] = (byte) ((g >>> 24) & 0xFF);
        ihdrVeri[1] = (byte) ((g >>> 16) & 0xFF);
        ihdrVeri[2] = (byte) ((g >>> 8) & 0xFF);
        ihdrVeri[3] = (byte) (g & 0xFF);
        ihdrVeri[4] = (byte) ((y >>> 24) & 0xFF);
        ihdrVeri[5] = (byte) ((y >>> 16) & 0xFF);
        ihdrVeri[6] = (byte) ((y >>> 8) & 0xFF);
        ihdrVeri[7] = (byte) (y & 0xFF);
        ihdrVeri[8] = 8;  // bit depth
        ihdrVeri[9] = 2;  // renk tipi: truecolor RGB
        ihdrVeri[10] = 0; // sıkıştırma
        ihdrVeri[11] = 0; // filtre
        ihdrVeri[12] = 0; // interlace

        CRC32 crc = new CRC32();
        crc.update("IHDR".getBytes(StandardCharsets.US_ASCII));
        crc.update(ihdrVeri);

        byte[] uzunluk = new byte[]{
                (byte) 0, (byte) 0, (byte) 0, (byte) 13};
        out.write(uzunluk);
        out.write("IHDR".getBytes(StandardCharsets.US_ASCII));
        out.write(ihdrVeri);
        out.write(new byte[]{
                (byte) (crc.getValue() >>> 24), (byte) (crc.getValue() >>> 16),
                (byte) (crc.getValue() >>> 8), (byte) crc.getValue()});
        return out.toByteArray();
    }

    private byte[] olustur(int g, int y) throws IOException {
        return olustur(g, y, false);
    }

    private byte[] olustur(int g, int y, boolean saydam) throws IOException {
        BufferedImage img = new BufferedImage(g, y,
                saydam ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        try {
            if (saydam) {
                gr.setComposite(java.awt.AlphaComposite.Src);
                gr.setColor(new Color(0, 0, 0, 0));
                gr.fillRect(0, 0, g, y);
                gr.setComposite(java.awt.AlphaComposite.SrcOver);
            }
            gr.setColor(new Color(30, 120, 200));
            gr.fillRect(0, 0, g, y);
        } finally {
            gr.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, saydam ? "png" : "png", out);
        return out.toByteArray();
    }
}