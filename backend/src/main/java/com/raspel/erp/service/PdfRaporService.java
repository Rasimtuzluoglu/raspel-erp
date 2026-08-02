package com.raspel.erp.service;

import com.raspel.erp.entity.*;
import com.raspel.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

    public byte[] faturaRaporu(Long faturaId) {
        Fatura f = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new com.raspel.erp.exception.ResourceNotFoundException("Fatura", faturaId));
        List<FaturaKalem> kalemler = faturaKalemRepository.findByFaturaId(faturaId);
        String cariAd = "";
        String cariId = "-";
        try {
            if (f.getCariHesap() != null) {
                cariAd = f.getCariHesap().getAd();
                cariId = String.valueOf(f.getCariHesap().getId());
            }
        } catch (Exception ignored) {}

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                y = header(cs, y, "FATURA");
                y -= 10;
                y = infoSatiri(cs, y, "Fatura No:", "#" + (f.getFaturaNumarasi() != null ? f.getFaturaNumarasi() : String.valueOf(f.getId())));
                y = infoSatiri(cs, y, "Tarih:", f.getOlusturmaTarihi() != null ? f.getOlusturmaTarihi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
                y = infoSatiri(cs, y, "Durum:", f.getDurum() != null ? f.getDurum().name() : "-");
                y = infoSatiri(cs, y, "Müşteri:", cariAd);
                infoSatiri(cs, y, "Cari Hesap ID:", cariId);
                y -= 20;

                y = cizgi(cs, y);
                y -= 8;

                y = siraBasligi(cs, y, "Sıra", "Ürün / Hizmet", "Miktar", "Birim Fiyat", "Tutar");
                y -= 4;
                y = cizgi(cs, y);
                y -= 6;

                int sira = 1;
                for (FaturaKalem k : kalemler) {
                    String aciklama = k.getAciklama() != null ? k.getAciklama() : "-";
                    String miktar = k.getAdet() != null ? k.getAdet().toString() : "0";
                    String birimFiyat = k.getBirimFiyat() != null ? k.getBirimFiyat().toString() : "0";
                    String tutar = k.getTutar() != null ? k.getTutar().toString() : "0";
                    y = siraSatiri(cs, y, String.valueOf(sira++), aciklama, miktar, birimFiyat, tutar);
                    if (y < 100) { y = yeniSayfa(doc, cs, y); }
                }

                y -= 10;
                y = cizgi(cs, y);
                y -= 8;

                String genelToplam = f.getGenelToplam() != null ? f.getGenelToplam().toString() : "0";
                cs.setFont(BOLD, 12);
                cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 120 + MARGIN, y); cs.showText("Genel Toplam:"); cs.endText();
                cs.beginText(); cs.newLineAtOffset(PAGE_WIDTH - 40 + MARGIN, y); cs.showText(genelToplam + " TL"); cs.endText();
                y -= 40;

                cs.setFont(BOLD_OBLIQUE, 9);
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
        List<IrsaliyeKalem> kalemler = irsaliyeKalemRepository.findByIrsaliyeId(irsaliyeId);
        return generatePdf("IRSALIYE RAPORU", "Irsaliye No: " + i.getIrsaliyeNo(),
                "Tarih: " + i.getTarih(), "Durum: " + i.getDurum(), "Cari ID: " + i.getCariHesapId(),
                kalemler.stream().map(k ->
                        (k.getAciklama() != null ? k.getAciklama() : "") + " | " + k.getMiktar() + " adet"
                ).toList());
    }

    private float header(PDPageContentStream cs, float y, String title) throws IOException {
        cs.setFont(BOLD, 22);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("RasPel ERP"); cs.endText();
        y -= 28;
        cs.setFont(BOLD, 16);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(title); cs.endText();
        y -= 30;
        return y;
    }

    private float infoSatiri(PDPageContentStream cs, float y, String label, String value) throws IOException {
        cs.setFont(REGULAR, 11);
        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(label + " " + value); cs.endText();
        return y - 18;
    }

    private float cizgi(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y); cs.lineTo(PAGE_WIDTH + MARGIN, y); cs.stroke();
        return y;
    }

    private float siraBasligi(PDPageContentStream cs, float y, String... cols) throws IOException {
        cs.setFont(BOLD, 10);
        float[] widths = {30, 250, 60, 80, 80};
        float x = MARGIN;
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x + 2, y); cs.showText(cols[i]); cs.endText();
            x += widths[i];
        }
        return y - 16;
    }

    private float siraSatiri(PDPageContentStream cs, float y, String... cols) throws IOException {
        cs.setFont(REGULAR, 10);
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
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;
                y = header(cs, y, title);
                y = infoSatiri(cs, y, "", line1);
                y = infoSatiri(cs, y, "", line2);
                y = infoSatiri(cs, y, "", line3);
                y = infoSatiri(cs, y, "", line4);
                y -= 20;
                cs.setFont(BOLD, 13);
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Kalemler:"); cs.endText();
                y -= 20;
                cs.setFont(REGULAR, 11);
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
}
