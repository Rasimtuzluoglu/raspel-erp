package com.raspel.erp.service.sistem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Yüklenen görselleri sunucu tarafında normalize eder: en-boy oranı korunarak en fazla
 * {@code maxKenar} piksele küçültür, JPEG kalitesiyle yeniden kodlar (şeffaflık varsa PNG)
 * ve liste/kart görünümleri için küçük bir thumbnail üretir. Böylece depolama ve bant
 * genişliği küçük kalır; EXIF/gizli meta veriler de yeniden kodlama ile temizlenir.
 */
@Service
@Slf4j
public class ResimIslemeService {

    public record IslenmisResim(String dosyaAdi, byte[] icerik, String contentType,
                                String thumbDosyaAdi, byte[] thumbIcerik, String thumbContentType) {
    }

    /**
     * Ana görsel (maxKenar) + thumbnail (thumbKenar) üretir.
     *
     * @param dosyaOneki UUID gibi benzersiz önek (uzantısız)
     */
    public IslenmisResim isle(byte[] kaynak, String dosyaOneki, int maxKenar, int thumbKenar,
                              float kalite) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(kaynak));
        if (img == null) {
            throw new IOException("Görsel okunamadı. JPEG, PNG veya WEBP formatında tekrar deneyin.");
        }
        boolean saydam = img.getColorModel().hasAlpha();
        String format = saydam ? "png" : "jpg";
        String uzanti = saydam ? ".png" : ".jpg";
        String mime = saydam ? "image/png" : "image/jpeg";

        BufferedImage ana = olcekle(img, maxKenar);
        byte[] anaBayt = yaz(ana, format, kalite);

        BufferedImage thumb = olcekle(img, thumbKenar);
        byte[] thumbBayt = yaz(thumb, format, Math.min(kalite, 0.8f));

        return new IslenmisResim(dosyaOneki + uzanti, anaBayt, mime,
                dosyaOneki + "_t" + uzanti, thumbBayt, mime);
    }

    private BufferedImage olcekle(BufferedImage kaynak, int maxKenar) {
        int g = kaynak.getWidth();
        int y = kaynak.getHeight();
        int buyuk = Math.max(g, y);
        if (maxKenar <= 0 || buyuk <= maxKenar) {
            return kaynak;
        }
        double oran = (double) maxKenar / buyuk;
        int yeniG = Math.max(1, (int) Math.round(g * oran));
        int yeniY = Math.max(1, (int) Math.round(y * oran));
        int tip = kaynak.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage hedef = new BufferedImage(yeniG, yeniY, tip);
        Graphics2D g2 = hedef.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (tip == BufferedImage.TYPE_INT_RGB) {
                // JPEG'de şeffaflık olmaz; arka plan beyaz doldurulur.
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, yeniG, yeniY);
            }
            g2.drawImage(kaynak, 0, 0, yeniG, yeniY, null);
        } finally {
            g2.dispose();
        }
        return hedef;
    }

    private byte[] yaz(BufferedImage img, String format, float kalite) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> yazicilar = ImageIO.getImageWritersByFormatName(format);
        if (!yazicilar.hasNext()) {
            ImageIO.write(img, format, out);
            return out.toByteArray();
        }
        ImageWriter yazici = yazicilar.next();
        try (MemoryCacheImageOutputStream mos = new MemoryCacheImageOutputStream(out)) {
            yazici.setOutput(mos);
            ImageWriteParam param = yazici.getDefaultWriteParam();
            if ("jpg".equals(format) && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.1f, Math.min(1f, kalite)));
            }
            yazici.write(null, new IIOImage(img, null, null), param);
        } finally {
            yazici.dispose();
        }
        return out.toByteArray();
    }
}
