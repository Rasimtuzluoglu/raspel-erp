package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResimIslemeServiceTest {

    private final ResimIslemeService servis = new ResimIslemeService();

    private byte[] buyukJpeg() throws Exception {
        BufferedImage img = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 120, 200));
        g.fillRect(0, 0, 2000, 1000);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 120));
        g.drawString("RASPEL", 120, 520);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    @Test
    void isle_kuculturVeThumbnailUretir() throws Exception {
        byte[] kaynak = buyukJpeg();

        var sonuc = servis.isle(kaynak, "test-foto", 1600, 320, 0.80f);

        // Ana görsel 1600px'e ölçeklenmeli (2000x1000 -> 1600x800)
        BufferedImage ana = ImageIO.read(new ByteArrayInputStream(sonuc.icerik()));
        assertEquals(1600, ana.getWidth());
        assertEquals(800, ana.getHeight());
        assertTrue(sonuc.dosyaAdi().endsWith(".jpg"));
        assertEquals("image/jpeg", sonuc.contentType());

        // Thumbnail 320px (320x160)
        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(sonuc.thumbIcerik()));
        assertEquals(320, thumb.getWidth());
        assertEquals(160, thumb.getHeight());
        assertTrue(sonuc.thumbDosyaAdi().endsWith("_t.jpg"));
        assertTrue(sonuc.thumbIcerik().length < sonuc.icerik().length);
    }

    @Test
    void isle_kucukGorselBuyutulmez() throws Exception {
        BufferedImage img = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);

        var sonuc = servis.isle(out.toByteArray(), "kucuk", 1600, 320, 0.80f);

        BufferedImage ana = ImageIO.read(new ByteArrayInputStream(sonuc.icerik()));
        assertEquals(100, ana.getWidth());
        assertEquals(80, ana.getHeight());
    }

    @Test
    void isle_gecersizIcerikHataFirlatir() {
        byte[] gecersiz = "bu bir resim degil".getBytes();
        assertThrows(java.io.IOException.class, () -> servis.isle(gecersiz, "x", 1600, 320, 0.8f));
    }
}
