package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.entity.muhasebe.Irsaliye;
import com.raspel.erp.entity.muhasebe.IrsaliyeKalem;
import com.raspel.erp.repository.muhasebe.IrsaliyeKalemRepository;
import com.raspel.erp.repository.muhasebe.IrsaliyeRepository;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.entity.ticaret.Siparis;
import com.raspel.erp.entity.ticaret.SiparisKalem;
import com.raspel.erp.repository.ticaret.SiparisKalemRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.envanter.Stok;

@Service
@RequiredArgsConstructor
public class PdfRaporService {

    private final SiparisRepository siparisRepository;
    private final SiparisKalemRepository siparisKalemRepository;
    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeKalemRepository irsaliyeKalemRepository;
    private final FaturaRepository faturaRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final SirketRepository sirketRepository;
    private final TenantChecker tenantChecker;

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

    /** Türkçe karakterleri destekleyen gömülü Unicode font çifti (DejaVuSans, OFL). */
    private static final class FontSet {
        final PDType0Font regular;
        final PDType0Font bold;

        FontSet(PDType0Font regular, PDType0Font bold) {
            this.regular = regular;
            this.bold = bold;
        }
    }

    private FontSet fontlar(PDDocument doc) throws IOException {
        try (InputStream regular = fontStream("DejaVuSans.ttf");
             InputStream bold = fontStream("DejaVuSans-Bold.ttf")) {
            return new FontSet(PDType0Font.load(doc, regular), PDType0Font.load(doc, bold));
        }
    }

    private static InputStream fontStream(String ad) {
        InputStream is = PdfRaporService.class.getResourceAsStream("/fonts/" + ad);
        if (is == null) {
            throw new IllegalStateException("PDF fontu bulunamadı: /fonts/" + ad);
        }
        return is;
    }

    public byte[] faturaRaporu(Long faturaId) {
        Fatura f = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Fatura", faturaId));
        tenantChecker.check(f.getSirketId(), "Fatura");
        List<FaturaKalem> kalemler = faturaKalemRepository.findByFaturaId(faturaId);
        String cariAd = "";
        String cariId = "-";
        try {
            if (f.getCariHesap() != null) {
                cariAd = f.getCariHesap().getAd();
                cariId = String.valueOf(f.getCariHesap().getId());
            }
        } catch (Exception ignored) {}

        boolean alis = f.getTur() == Fatura.FaturaTur.ALIS;
        String baslik = alis ? "ALIŞ FATURASI" : "SATIŞ FATURASI";
        String cariLabel = alis ? "Tedarikçi:" : "Müşteri:";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            FontSet font = fontlar(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                y = header(cs, y, baslik, font);
                y -= 10;
                y = infoSatiri(cs, y, "Fatura No:", "#" + (f.getFaturaNumarasi() != null ? f.getFaturaNumarasi() : String.valueOf(f.getId())), font);
                y = infoSatiri(cs, y, "Tarih:", f.getOlusturmaTarihi() != null ? f.getOlusturmaTarihi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", font);
                y = infoSatiri(cs, y, "Durum:", f.getDurum() != null ? f.getDurum().name() : "-", font);
                y = infoSatiri(cs, y, cariLabel, cariAd, font);
                y = infoSatiri(cs, y, "Cari Hesap ID:", cariId, font);
                y = infoSatiri(cs, y, "İşlemi Yapan:", f.getOlusturanKullaniciAdi() != null ? f.getOlusturanKullaniciAdi() : "-", font);
                if (alis && f.getDepoId() != null) {
                    y = infoSatiri(cs, y, "Depo ID:", String.valueOf(f.getDepoId()), font);
                }
                y = infoSatiri(cs, y, "Teslim Eden:", f.getTeslimEden() != null && !f.getTeslimEden().isBlank() ? f.getTeslimEden() : "-", font);
                y = infoSatiri(cs, y, "Teslim Durumu:", f.getTeslimDurumu() != null ? f.getTeslimDurumu() : "-", font);
                if (f.getTeslimNotu() != null && !f.getTeslimNotu().isBlank()) {
                    y = infoSatiri(cs, y, "Teslim Notu:", f.getTeslimNotu(), font);
                }
                y -= 20;

                y = cizgi(cs, y);
                y -= 8;

                y = siraBasligi(cs, y, font, "Sıra", "Ürün / Hizmet", "Miktar", "Birim Fiyat", "Tutar");
                y -= 4;
                y = cizgi(cs, y);
                y -= 6;

                int sira = 1;
                for (FaturaKalem k : kalemler) {
                    String aciklama = k.getAciklama() != null ? k.getAciklama() : "-";
                    String miktar = k.getAdet() != null ? k.getAdet().toString() : "0";
                    String birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat().toString() : "0";
                    String tutar = k.getTutar() != null ? k.getTutar().toString() : "0";
                    y = siraSatiri(cs, y, font, String.valueOf(sira++), aciklama, miktar, birimFiyat, tutar);
                    if (y < 100) { y = yeniSayfa(doc, cs, y); }
                }

                y -= 10;
                y = cizgi(cs, y);
                y -= 8;

                String genelToplam = f.getGenelToplam() != null ? f.getGenelToplam().toString() : "0";
                cs.setFont(font.bold, 12);
                cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 120 + MARGIN, y); cs.showText("Genel Toplam:"); cs.endText();
                cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 40 + MARGIN, y); cs.showText(genelToplam + " TL"); cs.endText();
                y -= 20;

                BigDecimal toplamAgirlik = kalemler.stream()
                        .map(k -> k.getAgirlik() != null ? k.getAgirlik().multiply(BigDecimal.valueOf(k.getAdet())) : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (toplamAgirlik.compareTo(BigDecimal.ZERO) > 0) {
                    cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 120 + MARGIN, y); cs.showText("Toplam Ağırlık:"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 40 + MARGIN, y); cs.showText(toplamAgirlik.stripTrailingZeros().toPlainString() + " kg"); cs.endText();
                    y -= 20;
                }
                y -= 20;

                cs.setFont(font.bold, 9);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("RasPel ERP - Otomatik Oluşturulmuştur"); cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    public byte[] siparisRaporu(Long siparisId) {
        Siparis s = siparisRepository.findById(siparisId)
                .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Siparis", siparisId));
        tenantChecker.check(s.getSirketId(), "Siparis");
        List<SiparisKalem> kalemler = siparisKalemRepository.findBySiparisId(siparisId);
        return generatePdf("SIPARIS RAPORU", "Siparis No: " + s.getSiparisNo(),
                "Tarih: " + s.getTarih(), "Durum: " + s.getDurum(), "Cari ID: " + s.getCariHesapId(),
                kalemler.stream().map(k ->
                        (k.getAciklama() != null ? k.getAciklama() : "") + " | " + k.getMiktar() + " x " + k.getBirimFiyat() + " TL"
                ).toList());
    }

    public byte[] irsaliyeRaporu(Long irsaliyeId) {
        Irsaliye i = irsaliyeRepository.findById(irsaliyeId)
                .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Irsaliye", irsaliyeId));
        tenantChecker.check(i.getSirketId(), "Irsaliye");
        List<IrsaliyeKalem> kalemler = irsaliyeKalemRepository.findByIrsaliyeId(irsaliyeId);
        return generatePdf("IRSALIYE RAPORU", "Irsaliye No: " + i.getIrsaliyeNo(),
                "Tarih: " + i.getTarih(), "Durum: " + i.getDurum(), "Cari ID: " + i.getCariHesapId(),
                kalemler.stream().map(k ->
                        (k.getAciklama() != null ? k.getAciklama() : "") + " | " + k.getMiktar() + " adet"
                ).toList());
    }

    private float header(PDPageContentStream cs, float y, String title, FontSet font) throws IOException {
        cs.setFont(font.bold, 22);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("RasPel ERP"); cs.endText();

        PDImageXObject logo = sirketLogosuBul();
        if (logo != null) {
            try {
                float logoGenislik = 90;
                float logoYukseklik = logoGenislik * logo.getHeight() / logo.getWidth();
                cs.drawImage(logo, PAGE_WIDTH - logoGenislik + MARGIN, y - logoYukseklik + 18, logoGenislik, logoYukseklik);
            } catch (Exception ignored) {}
        }

        y -= 28;
        cs.setFont(font.bold, 16);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(title); cs.endText();
        y -= 30;
        return y;
    }

    private PDImageXObject sirketLogosuBul() {
        try {
            Sirket sirket = sirketRepository.findFirstByAktifTrueOrderByIdAsc();
            if (sirket == null || sirket.getLogoUrl() == null || sirket.getLogoUrl().isBlank()) return null;
            String filename = sirket.getLogoUrl().substring(sirket.getLogoUrl().lastIndexOf('/') + 1);
            Path logoYolu = Paths.get("uploads/sirket-logos").toAbsolutePath().normalize().resolve(filename);
            File logoDosyasi = logoYolu.toFile();
            if (!logoDosyasi.exists() || !logoDosyasi.isFile()) return null;
            return PDImageXObject.createFromFileByContent(logoDosyasi, null);
        } catch (Exception e) {
            return null;
        }
    }

    private float infoSatiri(PDPageContentStream cs, float y, String label, String value, FontSet font) throws IOException {
        cs.setFont(font.regular, 11);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(label + " " + value); cs.endText();
        return y - 18;
    }

    private float cizgi(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y); cs.lineTo(PAGE_WIDTH + MARGIN, y); cs.stroke();
        return y;
    }

    private float siraBasligi(PDPageContentStream cs, float y, FontSet font, String... cols) throws IOException {
        cs.setFont(font.bold, 10);
        float[] widths = {30, 250, 60, 80, 80};
        float x = MARGIN;
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x + 2, y); cs.showText(cols[i]); cs.endText();
            x += widths[i];
        }
        return y - 16;
    }

    private float siraSatiri(PDPageContentStream cs, float y, FontSet font, String... cols) throws IOException {
        cs.setFont(font.regular, 10);
        float[] widths = {30, 250, 60, 80, 80};
        float x = MARGIN;
        boolean alternate = Integer.parseInt(cols[0]) % 2 == 0;
        cs.setNonStrokingColor(alternate ? 0.95f : 1.0f, 0.95f, 0.95f);
        cs.addRect(x, y - 2, PAGE_WIDTH, 16);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x + 2, y); cs.showText(cols[i]); cs.endText();
            x += widths[i];
        }
        return y - 16;
    }

    private float yeniSayfa(PDDocument doc, PDPageContentStream cs, float y) throws IOException {
        cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream newCs = new PDPageContentStream(doc, page);
        cs = newCs;
        return PDRectangle.A4.getHeight() - 80;
    }

    private byte[] generatePdf(String title, String line1, String line2, String line3, String line4, List<String> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            FontSet font = fontlar(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;
                y = header(cs, y, title, font);
                y = infoSatiri(cs, y, "", line1, font);
                y = infoSatiri(cs, y, "", line2, font);
                y = infoSatiri(cs, y, "", line3, font);
                y = infoSatiri(cs, y, "", line4, font);
                y -= 20;
                cs.setFont(font.bold, 13);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Kalemler:"); cs.endText();
                y -= 20;
                cs.setFont(font.regular, 11);
                for (String item : items) {
                    cs.beginText(); cs.newLineAtOffset(MARGIN + 10, y); cs.showText("- " + item); cs.endText();
                    y -= 18;
                }
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    /**
     * Genel tablo raporu: başlık + sütun başlıkları + satırlar. Rapor ekranlarındaki
     * verileri PDF olarak dışa aktarmak için kullanılır (ör. Bütçe vs Gerçekleşen).
     */
    public byte[] tabloRaporu(String baslik, String[] kolonlar, List<String[]> satirlar) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            FontSet font = fontlar(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;
                y = header(cs, y, baslik, font);
                y -= 10;

                float[] genislikler = esitGenislikler(kolonlar.length);
                y = tabloBaslikSatiri(cs, y, genislikler, kolonlar, font);
                y -= 4;
                y = cizgi(cs, y);
                y -= 6;

                int sira = 0;
                for (String[] satir : satirlar) {
                    boolean alternate = sira % 2 == 0;
                    y = tabloVeriSatiri(cs, y, genislikler, satir, alternate, font);
                    if (y < 100) {
                        y = yeniSayfa(doc, cs, y);
                        y = tabloBaslikSatiri(cs, y, genislikler, kolonlar, font);
                        y -= 4;
                        y = cizgi(cs, y);
                        y -= 6;
                    }
                    sira++;
                }
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    /**
     * Raf etiketi PDF'i: ürün adı, kod, barkod, QR (ZXing PNG), raf no ve fiyatı içerir.
     * Yazdırma ön izlemesi için yeterli, sabit yerleşimli tek etiket sayfası üretir.
     */
    public byte[] stokEtiketi(Stok stok, byte[] qrPng) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            FontSet font = fontlar(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;
                y = header(cs, y, "RAF ETİKETİ", font);
                y -= 10;
                y = cizgi(cs, y);
                y -= 20;

                String ad = stok.getAd() != null ? stok.getAd() : "-";
                String kod = stok.getStokKodu() != null ? stok.getStokKodu() : "-";
                String barkod = stok.getBarkod() != null ? stok.getBarkod() : "-";
                String raf = stok.getRafNo() != null ? stok.getRafNo() : "-";
                String fiyat = stok.getSatisFiyati() != null ? stok.getSatisFiyati().toString() + " TL" : "-";

                cs.setFont(font.bold, 20);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(ad); cs.endText();
                y -= 26;

                cs.setFont(font.regular, 14);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Kod: " + kod); cs.endText();
                y -= 20;
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Barkod: " + barkod); cs.endText();
                y -= 20;
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Raf No: " + raf); cs.endText();
                y -= 20;
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Fiyat: " + fiyat); cs.endText();
                y -= 30;

                if (qrPng != null && qrPng.length > 0) {
                    PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrPng, "qr");
                    float qrBoyut = 140;
                    cs.drawImage(qr, PAGE_WIDTH - qrBoyut + MARGIN, y - qrBoyut, qrBoyut, qrBoyut);
                    cs.setFont(font.regular, 9);
                    cs.beginText();
                    cs.newLineAtOffset(PAGE_WIDTH - qrBoyut + MARGIN, y - qrBoyut - 12);
                    cs.showText("Karekod ile tarayıp say");
                    cs.endText();
                }

                y -= 40;
                y = cizgi(cs, y);
                y -= 12;
                cs.setFont(font.regular, 9);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y);
                cs.showText("RasPel ERP - Otomatik Oluşturulmuştur");
                cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    private float tabloBaslikSatiri(PDPageContentStream cs, float y, float[] genislikler, String[] kolonlar, FontSet font) throws IOException {
        cs.setFont(font.bold, 10);
        float x = MARGIN;
        for (int i = 0; i < kolonlar.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x + 2, y); cs.showText(kolonlar[i]); cs.endText();
            x += genislikler[i];
        }
        return y - 16;
    }

    private float tabloVeriSatiri(PDPageContentStream cs, float y, float[] genislikler, String[] kolonlar, boolean alternate, FontSet font) throws IOException {
        cs.setFont(font.regular, 10);
        if (alternate) {
            cs.setNonStrokingColor(0.95f, 0.95f, 0.95f);
            cs.addRect(MARGIN, y - 2, PAGE_WIDTH, 16);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }
        float x = MARGIN;
        for (int i = 0; i < kolonlar.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x + 2, y); cs.showText(kolonlar[i] != null ? kolonlar[i] : "-"); cs.endText();
            x += genislikler[i];
        }
        return y - 16;
    }

    private float[] esitGenislikler(int kolonSayisi) {
        float[] genislikler = new float[kolonSayisi];
        float genislik = PAGE_WIDTH / kolonSayisi;
        for (int i = 0; i < kolonSayisi; i++) genislikler[i] = genislik;
        return genislikler;
    }
}
