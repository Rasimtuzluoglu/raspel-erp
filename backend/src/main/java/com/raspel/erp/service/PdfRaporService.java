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
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfRaporService {

    private final SiparisRepository siparisRepository;
    private final SiparisKalemRepository siparisKalemRepository;
    private final IrsaliyeRepository irsaliyeRepository;
    private final IrsaliyeKalemRepository irsaliyeKalemRepository;

    public byte[] siparisRaporu(Long siparisId) {
        Siparis s = siparisRepository.findById(siparisId)
                .orElseThrow(() -> new RuntimeException("Siparis bulunamadi: " + siparisId));
        List<SiparisKalem> kalemler = siparisKalemRepository.findBySiparisId(siparisId);
        return generatePdf("SIPARIS RAPORU",
                "Siparis No: " + s.getSiparisNo(),
                "Tarih: " + s.getTarih(),
                "Durum: " + s.getDurum(),
                "Cari Hesap ID: " + s.getCariHesapId(),
                kalemler.stream().map(k ->
                    (k.getAciklama() != null ? k.getAciklama() : "") + " | " + k.getMiktar() + " x " + k.getBirimFiyat() + " TL"
                ).toList());
    }

    public byte[] irsaliyeRaporu(Long irsaliyeId) {
        Irsaliye i = irsaliyeRepository.findById(irsaliyeId)
                .orElseThrow(() -> new RuntimeException("Irsaliye bulunamadi: " + irsaliyeId));
        List<IrsaliyeKalem> kalemler = irsaliyeKalemRepository.findByIrsaliyeId(irsaliyeId);
        return generatePdf("IRSALIYE RAPORU",
                "Irsaliye No: " + i.getIrsaliyeNo(),
                "Tarih: " + i.getTarih(),
                "Durum: " + i.getDurum(),
                "Cari Hesap ID: " + i.getCariHesapId(),
                kalemler.stream().map(k ->
                    (k.getAciklama() != null ? k.getAciklama() : "") + " | " + k.getMiktar() + " adet"
                ).toList());
    }

    private byte[] generatePdf(String title, String line1, String line2, String line3, String line4, List<String> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.beginText(); cs.newLineAtOffset(50, 750); cs.showText(title); cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                float y = 700;
                for (String l : List.of(line1, line2, line3, line4)) {
                    cs.beginText(); cs.newLineAtOffset(50, y); cs.showText(l); cs.endText();
                    y -= 20;
                }
                y -= 20;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
                cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("Kalemler:"); cs.endText();
                y -= 20;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                for (String item : items) {
                    cs.beginText(); cs.newLineAtOffset(50, y); cs.showText("- " + item); cs.endText();
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
