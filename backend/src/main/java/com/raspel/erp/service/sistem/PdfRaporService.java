package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.finans.CariHesap;
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
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.envanter.Stok;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PDF üretimi. Tüm raporlar ortak bir düzen altyapısı kullanır: metin kaydırma,
 * sağa hizalı sayısal sütunlar, gerçek şirket/müşteri blokları, KDV dökümü,
 * Türkçe para biçimi ve sayfa numaralı alt bilgi.
 */
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
    private final TeslimatRepository teslimatRepository;
    private final DosyaDepolamaService dosyaDepolamaService;
    private final QRService qrService;

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final float ALT_SINIR = MARGIN + 30;

    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TARIH_SAAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DecimalFormat PARA_FMT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("tr-TR"));
        PARA_FMT = new DecimalFormat("#,##0.00", sym);
    }

    // ------------------------------------------------------------------ Fonts

    static final class FontSet {
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

    // ------------------------------------------------------------------ Fatura

    /** Geriye dönük uyumluluk: varsayılan dil Türkçe. */
    public byte[] faturaRaporu(Long faturaId) {
        return faturaRaporu(faturaId, PdfMetin.tr());
    }

    public byte[] faturaRaporu(Long faturaId, PdfMetin m) {
        Fatura f = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new ResourceNotFoundException(m.t("fatura"), faturaId));
        tenantChecker.check(f.getSirketId(), m.t("fatura"));
        List<FaturaKalem> kalemler = faturaKalemRepository.findByFaturaId(faturaId);

        boolean alis = f.getTur() == Fatura.FaturaTur.ALIS;
        FaturaSablonu sablon = faturaSablonuOku(f.getSirketId());
        Sirket sirket = sirketBul(f.getSirketId());
        String baslik = sablon.faturaBasligi != null && !sablon.faturaBasligi.isBlank()
                ? sablon.faturaBasligi
                : (alis ? m.t("alisFaturasi") : m.t("satisFaturasi"));

        CariHesap cari = cariHesap(f);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            Belge b = new Belge(doc, font, sablon, sirket, baslik, sablon.altBaslik);

            float y = b.y;
            // Müşteri/tedarikçi bloğu (sol) + belge künyesi (sağ)
            List<String> cariSatir = new ArrayList<>();
            if (cari != null) {
                if (!bosMu(cari.getVergiDairesi()) || !bosMu(cari.getVergiNumarasi())) {
                    cariSatir.add(birlestir(birlestir(cari.getVergiDairesi(), " " + m.t("vd")), cari.getVergiNumarasi(), " - "));
                }
                if (!bosMu(cari.getAdres())) cariSatir.add(cari.getAdres());
                String il = birlestir(cari.getIl(), cari.getIlce(), " / ");
                if (!il.isBlank()) cariSatir.add(il);
                if (!bosMu(cari.getTelefon())) cariSatir.add(m.t("tel") + ": " + cari.getTelefon());
                if (!bosMu(cari.getEmail())) cariSatir.add(cari.getEmail());
            }
            float ySol = b.bilgiBlogu(MARGIN, y, PAGE_WIDTH * 0.53f,
                    (alis ? m.t("tedarikci") : m.t("musteri")) + (cari != null && !bosMu(cari.getAd()) ? " / " + cari.getAd() : ""),
                    cariSatir);

            List<String> kunye = new ArrayList<>();
            kunye.add(m.t("faturaNo") + ": " + boslukTemizle(f.getFaturaNumarasi() != null ? f.getFaturaNumarasi() : String.valueOf(f.getId())));
            kunye.add(m.t("faturaTarihi") + ": " + tarih(f.getTarih(), f.getOlusturmaTarihi()));
            if (f.getVadeTarihi() != null) kunye.add(m.t("vadeTarihi") + ": " + f.getVadeTarihi().format(TARIH));
            kunye.add(m.t("durum") + ": " + (f.getDurum() != null ? f.getDurum().name() : "-"));
            kunye.add(m.t("paraBirimi") + ": " + (f.getParaBirimi() != null && !f.getParaBirimi().isBlank() ? f.getParaBirimi() : "TL"));
            if (f.getOlusturanKullaniciAdi() != null && !f.getOlusturanKullaniciAdi().isBlank()) {
                kunye.add(m.t("olusturan") + ": " + f.getOlusturanKullaniciAdi());
            }
            float ySag = b.bilgiBlogu(MARGIN + PAGE_WIDTH * 0.55f, y, PAGE_WIDTH * 0.45f, m.t("belgeBilgileri"), kunye);

            b.y = Math.min(ySol, ySag) - 8;

            // Kalem tablosu
            List<Kolon> kolonlar = List.of(
                    new Kolon(m.t("sira"), PAGE_WIDTH * 0.055f, false),
                    new Kolon(m.t("urunHizmet"), PAGE_WIDTH * 0.43f, false),
                    new Kolon(m.t("miktar"), PAGE_WIDTH * 0.11f, true),
                    new Kolon(m.t("birimFiyat"), PAGE_WIDTH * 0.15f, true),
                    new Kolon(m.t("kdvYuzde"), PAGE_WIDTH * 0.09f, true),
                    new Kolon(m.t("tutar"), PAGE_WIDTH * 0.165f, true));

            String birim = f.getParaBirimi() != null && !f.getParaBirimi().isBlank() ? f.getParaBirimi() : "TL";
            List<String[]> satirlar = new ArrayList<>();
            int sira = 1;
            BigDecimal toplamAgirlik = BigDecimal.ZERO;
            for (FaturaKalem k : kalemler) {
                satirlar.add(new String[]{
                        String.valueOf(sira++),
                        k.getAciklama() != null ? k.getAciklama() : "-",
                        sayi(k.getAdet()),
                        paraBare(k.getBirimFiyat()),
                        k.getKdvOrani() != null ? sayi(k.getKdvOrani()) : "-",
                        paraBare(k.getTutar())});
                if (k.getAgirlik() != null && k.getAdet() != null) {
                    toplamAgirlik = toplamAgirlik.add(k.getAgirlik().multiply(k.getAdet()));
                }
            }
            if (satirlar.isEmpty()) satirlar.add(new String[]{"-", m.t("kalemBulunmuyor"), "", "", "", ""});
            b.tablo(kolonlar, satirlar);

            // Toplamlar
            b.y -= 4;
            b.toplamSatiri(m.t("araToplam"), para(f.getAraToplam(), birim), false);
            if (f.getGenelIskontoTutari() != null && f.getGenelIskontoTutari().compareTo(BigDecimal.ZERO) > 0) {
                b.toplamSatiri(m.t("iskonto"), "-" + para(f.getGenelIskontoTutari(), birim), false);
            }
            b.toplamSatiri(m.t("kdv"), para(f.getKdv(), birim), false);
            b.toplamSatiri(m.t("genelToplam"), para(f.getGenelToplam(), birim), true);

            if (sablon.odemeDurumuGoster != null && sablon.odemeDurumuGoster) {
                b.toplamSatiri(m.t("odenen"), para(f.getOdenenTutar(), birim), false);
                b.toplamSatiri(m.t("kalan"), para(f.getKalanTutar(), birim), false);
                b.toplamSatiri(m.t("odemeDurumu"), f.getOdemeDurumu() != null ? f.getOdemeDurumu() : "-", false);
            }
            if (toplamAgirlik.compareTo(BigDecimal.ZERO) > 0) {
                b.toplamSatiri(m.t("toplamAgirlik"), sayi(toplamAgirlik) + " kg", false);
            }

            // QR kod (şablon seçeneği)
            if (sablon.qrKodGoster != null && sablon.qrKodGoster) {
                b.y -= 6;
                String qrIcerik = "Fatura: " + boslukTemizle(f.getFaturaNumarasi())
                        + " | " + para(f.getGenelToplam(), birim) + " | " + tarih(f.getTarih(), f.getOlusturmaTarihi());
                b.qrEkle(qrIcerik);
            }

            // İmza kutusu (şablon seçeneği)
            if (sablon.imzaKutusuGoster != null && sablon.imzaKutusuGoster) {
                b.imzaKutusu(m.t("imzaKase"));
            }

            b.kapat();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Fatura PDF oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ Fatura görseli (PNG)

    /** Faturayı müşteriye göndermek için tek sayfalık PNG görsel üretir. */
    public byte[] faturaGorselPng(Long faturaId) {
        return faturaGorselPng(faturaId, PdfMetin.tr());
    }

    public byte[] faturaGorselPng(Long faturaId, PdfMetin m) {
        byte[] pdf = faturaRaporu(faturaId, m);
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            java.awt.image.BufferedImage img = renderer.renderImageWithDPI(0, 160);
            javax.imageio.ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Fatura görseli oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ Sipariş

    public byte[] siparisRaporu(Long siparisId) {
        return siparisRaporu(siparisId, PdfMetin.tr());
    }

    public byte[] siparisRaporu(Long siparisId, PdfMetin m) {
        Siparis s = siparisRepository.findById(siparisId)
                .orElseThrow(() -> new ResourceNotFoundException(m.t("siparisFormu"), siparisId));
        tenantChecker.check(s.getSirketId(), m.t("siparisFormu"));
        List<SiparisKalem> kalemler = siparisKalemRepository.findBySiparisId(siparisId);

        FaturaSablonu sablon = faturaSablonuOku(s.getSirketId());
        Sirket sirket = sirketBul(s.getSirketId());
        CariHesap cari = s.getCariHesapId() != null ? cariHesapRepository.findById(s.getCariHesapId()).orElse(null) : null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            Belge b = new Belge(doc, font, sablon, sirket, m.t("siparisFormu"), sablon.altBaslik);

            List<String> kunye = new ArrayList<>();
            kunye.add(m.t("siparisNo") + ": " + boslukTemizle(s.getSiparisNo()));
            kunye.add(m.t("tarih") + ": " + tarih(s.getTarih(), s.getOlusturmaTarihi()));
            kunye.add(m.t("durum") + ": " + (s.getDurum() != null ? s.getDurum() : "-"));
            kunye.add(m.t("musteri") + ": " + (cari != null && !bosMu(cari.getAd()) ? cari.getAd() : ("#" + s.getCariHesapId())));
            if (!bosMu(s.getAciklama())) kunye.add(m.t("aciklama") + ": " + s.getAciklama());
            b.y = b.bilgiBlogu(MARGIN, b.y, PAGE_WIDTH, m.t("siparisBilgileri"), kunye) - 8;

            List<Kolon> kolonlar = List.of(
                    new Kolon(m.t("sira"), PAGE_WIDTH * 0.05f, false),
                    new Kolon(m.t("urunHizmet"), PAGE_WIDTH * 0.36f, false),
                    new Kolon(m.t("miktar"), PAGE_WIDTH * 0.10f, true),
                    new Kolon(m.t("birim"), PAGE_WIDTH * 0.09f, false),
                    new Kolon(m.t("birimFiyat"), PAGE_WIDTH * 0.13f, true),
                    new Kolon(m.t("kdvYuzde"), PAGE_WIDTH * 0.09f, true),
                    new Kolon(m.t("tutar"), PAGE_WIDTH * 0.18f, true));

            List<String[]> satirlar = new ArrayList<>();
            int sira = 1;
            for (SiparisKalem k : kalemler) {
                satirlar.add(new String[]{
                        String.valueOf(sira++),
                        k.getAciklama() != null ? k.getAciklama() : "-",
                        sayi(k.getMiktar()),
                        k.getBirim() != null ? k.getBirim() : "-",
                        paraBare(k.getBirimFiyat()),
                        k.getKdvOrani() != null ? sayi(k.getKdvOrani()) : "-",
                        paraBare(k.getTutar())});
            }
            if (satirlar.isEmpty()) satirlar.add(new String[]{"-", m.t("kalemBulunmuyor"), "", "", "", "", ""});
            b.tablo(kolonlar, satirlar);

            b.y -= 4;
            b.toplamSatiri(m.t("araToplam"), para(s.getAraToplam(), "TL"), false);
            b.toplamSatiri(m.t("kdv"), para(s.getKdv(), "TL"), false);
            b.toplamSatiri(m.t("genelToplam"), para(s.getGenelToplam(), "TL"), true);

            b.kapat();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Sipariş PDF oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ İrsaliye

    public byte[] irsaliyeRaporu(Long irsaliyeId) {
        return irsaliyeRaporu(irsaliyeId, PdfMetin.tr());
    }

    public byte[] irsaliyeRaporu(Long irsaliyeId, PdfMetin m) {
        Irsaliye i = irsaliyeRepository.findById(irsaliyeId)
                .orElseThrow(() -> new ResourceNotFoundException(m.t("irsaliye"), irsaliyeId));
        tenantChecker.check(i.getSirketId(), m.t("irsaliye"));
        List<IrsaliyeKalem> kalemler = irsaliyeKalemRepository.findByIrsaliyeId(irsaliyeId);

        FaturaSablonu sablon = faturaSablonuOku(i.getSirketId());
        Sirket sirket = sirketBul(i.getSirketId());
        CariHesap cari = i.getCariHesapId() != null ? cariHesapRepository.findById(i.getCariHesapId()).orElse(null) : null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            Belge b = new Belge(doc, font, sablon, sirket, m.t("irsaliye"), sablon.altBaslik);

            List<String> kunye = new ArrayList<>();
            kunye.add(m.t("irsaliyeNo") + ": " + boslukTemizle(i.getIrsaliyeNo()));
            kunye.add(m.t("tarih") + ": " + tarih(i.getTarih(), i.getOlusturmaTarihi()));
            kunye.add(m.t("durum") + ": " + (i.getDurum() != null ? i.getDurum() : "-"));
            kunye.add(m.t("musteri") + ": " + (cari != null && !bosMu(cari.getAd()) ? cari.getAd() : ("#" + i.getCariHesapId())));
            if (!bosMu(i.getAciklama())) kunye.add(m.t("aciklama") + ": " + i.getAciklama());
            b.y = b.bilgiBlogu(MARGIN, b.y, PAGE_WIDTH, m.t("irsaliyeBilgileri"), kunye) - 8;

            List<Kolon> kolonlar = List.of(
                    new Kolon(m.t("sira"), PAGE_WIDTH * 0.06f, false),
                    new Kolon(m.t("urunHizmet"), PAGE_WIDTH * 0.60f, false),
                    new Kolon(m.t("miktar"), PAGE_WIDTH * 0.17f, true),
                    new Kolon(m.t("birim"), PAGE_WIDTH * 0.17f, false));

            List<String[]> satirlar = new ArrayList<>();
            int sira = 1;
            for (IrsaliyeKalem k : kalemler) {
                satirlar.add(new String[]{
                        String.valueOf(sira++),
                        k.getAciklama() != null ? k.getAciklama() : "-",
                        sayi(k.getMiktar()),
                        k.getBirim() != null ? k.getBirim() : "-"});
            }
            if (satirlar.isEmpty()) satirlar.add(new String[]{"-", m.t("kalemBulunmuyor"), "", ""});
            b.tablo(kolonlar, satirlar);

            b.y -= 14;
            b.imzaKutusu(m.t("imzaKaseIrsaliye"));

            b.kapat();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("İrsaliye PDF oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ Teslimat fişi

    public byte[] teslimatFisiRaporu(Long teslimatId) {
        return teslimatFisiRaporu(teslimatId, PdfMetin.tr());
    }

    public byte[] teslimatFisiRaporu(Long teslimatId, PdfMetin m) {
        Teslimat t = teslimatRepository.findById(teslimatId)
                .orElseThrow(() -> new ResourceNotFoundException("Teslimat", teslimatId));
        tenantChecker.check(t.getSirketId(), "Teslimat");
        Fatura f = t.getFaturaId() != null ? faturaRepository.findById(t.getFaturaId()).orElse(null) : null;
        List<FaturaKalem> kalemler = f != null ? faturaKalemRepository.findByFaturaId(f.getId()) : List.of();

        FaturaSablonu sablon = faturaSablonuOku(t.getSirketId());
        Sirket sirket = sirketBul(t.getSirketId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            Belge b = new Belge(doc, font, sablon, sirket, m.t("teslimatFisi"), sablon.altBaslik);

            List<String> kunye = new ArrayList<>();
            kunye.add(m.t("fatura") + " No: " + (t.getFaturaNumarasi() != null ? boslukTemizle(t.getFaturaNumarasi())
                    : (t.getFaturaId() != null ? "#" + t.getFaturaId() : "-")));
            kunye.add(m.t("musteri") + ": " + (t.getMusteriAdi() != null ? t.getMusteriAdi() : "-"));
            kunye.add(m.t("teslimatAdresi") + ": " + (t.getTeslimatAdresi() != null ? t.getTeslimatAdresi() : "-"));
            kunye.add(m.t("teslimEden") + ": " + (t.getTeslimEdenAd() != null ? t.getTeslimEdenAd() : "-"));
            kunye.add(m.t("teslimAlan") + ": " + (t.getTeslimAlanAd() != null ? t.getTeslimAlanAd() : "-"));
            kunye.add(m.t("teslimTarihi") + ": " + (t.getTeslimTarihi() != null ? t.getTeslimTarihi().format(TARIH_SAAT) : "-"));
            if (t.getTeslimNotu() != null && !t.getTeslimNotu().isBlank()) kunye.add("Not: " + t.getTeslimNotu());
            b.y = b.bilgiBlogu(MARGIN, b.y, PAGE_WIDTH, m.t("teslimatBilgileri"), kunye) - 8;

            List<Kolon> kolonlar = List.of(
                    new Kolon(m.t("sira"), PAGE_WIDTH * 0.055f, false),
                    new Kolon(m.t("urunHizmet"), PAGE_WIDTH * 0.43f, false),
                    new Kolon(m.t("miktar"), PAGE_WIDTH * 0.11f, true),
                    new Kolon(m.t("birimFiyat"), PAGE_WIDTH * 0.15f, true),
                    new Kolon(m.t("kdvYuzde"), PAGE_WIDTH * 0.09f, true),
                    new Kolon(m.t("tutar"), PAGE_WIDTH * 0.165f, true));

            List<String[]> satirlar = new ArrayList<>();
            int sira = 1;
            for (FaturaKalem k : kalemler) {
                satirlar.add(new String[]{
                        String.valueOf(sira++),
                        k.getAciklama() != null ? k.getAciklama() : "-",
                        sayi(k.getAdet()),
                        paraBare(k.getBirimFiyat()),
                        k.getKdvOrani() != null ? sayi(k.getKdvOrani()) : "-",
                        paraBare(k.getTutar())});
            }
            if (satirlar.isEmpty()) satirlar.add(new String[]{"-", m.t("bagliKalemYok"), "", "", "", ""});
            b.tablo(kolonlar, satirlar);

            b.y -= 20;
            PDImageXObject imza = imzaGorseli(doc, t.getTeslimImzaUrl());
            b.imzaAlani(imza, m.t("teslimAlanImzasi"));

            b.kapat();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Teslimat fişi oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ Genel tablo raporu

    public byte[] tabloRaporu(String baslik, String[] kolonlar, List<String[]> satirlar) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            FaturaSablonu sablon = FaturaSablonu.varsayilan();
            Sirket sirket = sirketBul(null);
            Belge b = new Belge(doc, font, sablon, sirket, baslik, null);

            int adet = kolonlar != null ? kolonlar.length : 0;
            float[] genislikler = new float[adet];
            boolean[] sag = new boolean[adet];
            for (int i = 0; i < adet; i++) genislikler[i] = PAGE_WIDTH / adet;
            // Bir sutundaki tum degerler sayisal ise saga hizala.
            if (satirlar != null) {
                for (int c = 0; c < adet; c++) {
                    boolean hepsiSayi = true;
                    boolean dolu = false;
                    for (String[] s : satirlar) {
                        if (s != null && c < s.length && s[c] != null && !s[c].isBlank()) {
                            dolu = true;
                            if (!sayisalMi(s[c])) { hepsiSayi = false; break; }
                        }
                    }
                    sag[c] = dolu && hepsiSayi;
                }
            }
            List<Kolon> kol = new ArrayList<>();
            for (int i = 0; i < adet; i++) kol.add(new Kolon(kolonlar[i], genislikler[i], sag[i]));
            b.tablo(kol, satirlar != null ? satirlar : List.of());

            b.kapat();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    // ------------------------------------------------------------------ Belge (sayfa akışı)

    /** Sayfa taşmasını, ortak başlığı ve alt bilgiyi yöneten yazıcı. */
    private final class Belge {
        final PDDocument doc;
        final FontSet font;
        final FaturaSablonu sablon;
        final Sirket sirket;
        final String baslik;
        final String altBaslik;
        PDPage page;
        PDPageContentStream cs;
        float y;
        int sayfaNo = 0;

        Belge(PDDocument doc, FontSet font, FaturaSablonu sablon, Sirket sirket, String baslik, String altBaslik) throws IOException {
            this.doc = doc;
            this.font = font;
            this.sablon = sablon;
            this.sirket = sirket;
            this.baslik = baslik;
            this.altBaslik = altBaslik;
            yeniSayfa();
        }

        void yeniSayfa() throws IOException {
            if (cs != null) {
                altBilgi();
                cs.close();
            }
            sayfaNo++;
            page = new PDPage(sablon.sayfaBoyutu());
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
            y = sirketBasligi(cs, doc, y, sirket, font, sablon, baslik, altBaslik);
        }

        /** Gerekli yukseklik yoksa yeni sayfa acar; true ise sayfa degisti. */
        boolean gerekirseYer(float gerekli) throws IOException {
            if (y - gerekli < ALT_SINIR) {
                yeniSayfa();
                return true;
            }
            return false;
        }

        void altBilgi() throws IOException {
            float fy = MARGIN - 18;
            cs.setFont(font.regular, 8.5f);
            cs.setNonStrokingColor(0.45f, 0.5f, 0.56f);
            String sol = (sirket != null && !bosMu(sirket.getAd())) ? sirket.getAd() + "  •  RasPel ERP" : "RasPel ERP";
            cs.beginText(); cs.newLineAtOffset(MARGIN, fy); cs.showText(boslukTemizle(sol)); cs.endText();
            String sp = "Sayfa " + sayfaNo;
            float w = metinGenislik(sp, font.regular, 8.5f);
            cs.beginText(); cs.newLineAtOffset(MARGIN + PAGE_WIDTH - w, fy); cs.showText(sp); cs.endText();
            cs.setLineWidth(0.4f);
            cs.moveTo(MARGIN, fy + 10);
            cs.lineTo(MARGIN + PAGE_WIDTH, fy + 10);
            cs.stroke();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        void kapat() throws IOException {
            altBilgi();
            cs.close();
        }

        float bilgiBlogu(float x, float yBas, float genislik, String blokBaslik, List<String> satirlar) throws IOException {
            float yy = yBas;
            cs.setFont(font.bold, 9.5f);
            cs.setNonStrokingColor(0.35f, 0.4f, 0.47f);
            cs.beginText(); cs.newLineAtOffset(x, yy); cs.showText(boslukTemizle(blokBaslik)); cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            yy -= 14;
            cs.setFont(font.regular, 9.5f);
            if (satirlar == null || satirlar.isEmpty()) return yy;
            for (String s : satirlar) {
                for (String p : sar(s, font.regular, 9.5f, genislik)) {
                    if (yy < ALT_SINIR) break;
                    cs.beginText(); cs.newLineAtOffset(x, yy); cs.showText(p); cs.endText();
                    yy -= 12.5f;
                }
            }
            return yy;
        }

        void tablo(List<Kolon> kolonlar, List<String[]> satirlar) throws IOException {
            tabloBasligi(kolonlar);
            int idx = 0;
            for (String[] satir : satirlar) {
                List<List<String>> hucreler = new ArrayList<>();
                float yuk = 15f;
                for (int c = 0; c < kolonlar.size(); c++) {
                    String ham = (satir != null && c < satir.length && satir[c] != null) ? satir[c] : "";
                    List<String> parc = sar(ham, font.regular, 9.5f, kolonlar.get(c).genislik - 8f);
                    hucreler.add(parc);
                    yuk = Math.max(yuk, parc.size() * 12f + 5f);
                }
                boolean yeni = gerekirseYer(yuk + 3f);
                if (yeni) tabloBasligi(kolonlar);
                if (idx % 2 == 1) {
                    cs.setNonStrokingColor(0.96f, 0.97f, 0.99f);
                    cs.addRect(MARGIN, y - yuk + 11f, PAGE_WIDTH, yuk);
                    cs.fill();
                    cs.setNonStrokingColor(0f, 0f, 0f);
                }
                float x = MARGIN;
                cs.setFont(font.regular, 9.5f);
                for (int c = 0; c < kolonlar.size(); c++) {
                    Kolon k = kolonlar.get(c);
                    float sy = y;
                    for (String p : hucreler.get(c)) {
                        float w = metinGenislik(p, font.regular, 9.5f);
                        float px = k.sag ? (x + k.genislik - 4f - w) : (x + 4f);
                        cs.beginText(); cs.newLineAtOffset(px, sy); cs.showText(p); cs.endText();
                        sy -= 12f;
                    }
                    x += k.genislik;
                }
                y -= yuk;
                idx++;
            }
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, y + 4f);
            cs.lineTo(MARGIN + PAGE_WIDTH, y + 4f);
            cs.stroke();
            y -= 6f;
        }

        private void tabloBasligi(List<Kolon> kolonlar) throws IOException {
            cs.setNonStrokingColor(0.93f, 0.95f, 0.98f);
            cs.addRect(MARGIN, y - 4f, PAGE_WIDTH, 16f);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.setFont(font.bold, 9.5f);
            float x = MARGIN;
            for (Kolon k : kolonlar) {
                float w = metinGenislik(k.baslik, font.bold, 9.5f);
                float px = k.sag ? (x + k.genislik - 4f - w) : (x + 4f);
                cs.beginText(); cs.newLineAtOffset(px, y); cs.showText(k.baslik); cs.endText();
                x += k.genislik;
            }
            y -= 18f;
        }

        void toplamSatiri(String etiket, String deger, boolean vurgu) throws IOException {
            gerekirseYer(vurgu ? 22f : 18f);
            float sagX = MARGIN + PAGE_WIDTH;
            PDType0Font f = vurgu ? font.bold : font.regular;
            float boyut = vurgu ? 12f : 10.5f;
            cs.setFont(f, boyut);
            float w1 = metinGenislik(etiket, f, boyut);
            float w2 = metinGenislik(deger, f, boyut);
            cs.beginText(); cs.newLineAtOffset(sagX - 150f - w1, y); cs.showText(etiket); cs.endText();
            cs.beginText(); cs.newLineAtOffset(sagX - w2, y); cs.showText(deger); cs.endText();
            y -= vurgu ? 21f : 16f;
        }

        void qrEkle(String icerik) throws IOException {
            byte[] png;
            try {
                png = qrService.qrPng(icerik, 150);
            } catch (Exception e) {
                return;
            }
            if (png == null || png.length == 0) return;
            float boyut = 90f;
            gerekirseYer(boyut + 20f);
            PDImageXObject qr = PDImageXObject.createFromByteArray(doc, png, "qr");
            cs.drawImage(qr, MARGIN, y - boyut, boyut, boyut);
            cs.setFont(font.regular, 8f);
            cs.beginText(); cs.newLineAtOffset(MARGIN, y - boyut - 10); cs.showText("Faturayı karekod ile doğrulayın"); cs.endText();
            y -= boyut + 16f;
        }

        void imzaKutusu(String etiket) throws IOException {
            float kutuGenislik = 200f;
            float kutuYukseklik = 60f;
            gerekirseYer(kutuYukseklik + 24f);
            float x = MARGIN + PAGE_WIDTH - kutuGenislik;
            float boxY = y - kutuYukseklik;
            cs.setLineWidth(0.6f);
            cs.addRect(x, boxY, kutuGenislik, kutuYukseklik);
            cs.stroke();
            cs.setFont(font.regular, 9f);
            float w = metinGenislik(etiket, font.regular, 9f);
            cs.beginText(); cs.newLineAtOffset(x + (kutuGenislik - w) / 2f, boxY + 6f); cs.showText(etiket); cs.endText();
            y = boxY - 16f;
        }

        void imzaAlani(PDImageXObject imza, String etiket) throws IOException {
            if (imza != null) {
                float gen = 180f;
                float yuk = gen * imza.getHeight() / imza.getWidth();
                gerekirseYer(yuk + 24f);
                float x = MARGIN + PAGE_WIDTH - gen;
                cs.drawImage(imza, x, y - yuk, gen, yuk);
                cs.setFont(font.regular, 9f);
                float w = metinGenislik(etiket, font.regular, 9f);
                cs.beginText(); cs.newLineAtOffset(x + (gen - w) / 2f, y - yuk - 12); cs.showText(etiket); cs.endText();
                y -= yuk + 20f;
            } else {
                imzaKutusu(etiket);
            }
        }
    }

    private static final class Kolon {
        final String baslik;
        final float genislik;
        final boolean sag;

        Kolon(String baslik, float genislik, boolean sag) {
            this.baslik = baslik;
            this.genislik = genislik;
            this.sag = sag;
        }
    }

    // ------------------------------------------------------------------ Başlık / logo

    private float sirketBasligi(PDPageContentStream cs, PDDocument doc, float y, Sirket s, FontSet font,
                                FaturaSablonu sablon, String baslik, String altBaslik) throws IOException {
        float solGenislik = PAGE_WIDTH - 100f;
        PDImageXObject logo = logoYukle(doc, sablon, s);
        if (logo != null) {
            try {
                float lg = 84f;
                float ly = lg * logo.getHeight() / logo.getWidth();
                cs.drawImage(logo, MARGIN + PAGE_WIDTH - lg, y - ly + 16f, lg, ly);
            } catch (Exception ignored) {
                // logo çizilemezse başlık yine üretilir
            }
        }

        // Urun (RasPel) logosu kucuk olarak sol ustte; sirket logosu sag ustte kalir.
        float solX = MARGIN;
        PDImageXObject urunLogo = urunLogoYukle(doc);
        if (urunLogo != null) {
            try {
                float lg = 34f;
                float ly = lg * urunLogo.getHeight() / urunLogo.getWidth();
                cs.drawImage(urunLogo, MARGIN, y - ly + 16f, lg, ly);
                solX = MARGIN + 42f;
            } catch (Exception ignored) {
                // urun logosu cizilemezse metin normal konumda kalir
            }
        }

        float yy = y;
        String ad = (s != null && !bosMu(s.getAd())) ? s.getAd() : "RasPel ERP";
        cs.setFont(font.bold, 15f);
        cs.beginText(); cs.newLineAtOffset(solX, yy); cs.showText(boslukTemizle(ad)); cs.endText();
        yy -= 15f;
        cs.setFont(font.regular, 9f);
        List<String> bilgi = new ArrayList<>();
        if (s != null) {
            String vd = birlestir(birlestir(s.getVergiDairesi(), " V.D."), s.getVergiNo(), " - ");
            if (!vd.isBlank()) bilgi.add(vd);
            if (!bosMu(s.getAdres())) bilgi.add(s.getAdres());
            String iletisim = birlestir2(s.getTelefon(), s.getEmail(), "  •  ");
            if (!iletisim.isBlank()) bilgi.add(iletisim);
            if (!bosMu(s.getWebSite())) bilgi.add(s.getWebSite());
        }
        for (String b : bilgi) {
            for (String p : sar(b, font.regular, 9f, solGenislik - (solX - MARGIN))) {
                cs.beginText(); cs.newLineAtOffset(solX, yy); cs.showText(p); cs.endText();
                yy -= 12f;
            }
        }

        yy -= 8f;
        float[] rgb = hexToRgb(sablon != null && sablon.renk != null ? sablon.renk : "#1e40af");
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.addRect(MARGIN, yy, PAGE_WIDTH, 2.5f);
        cs.fill();
        yy -= 26f;

        cs.setFont(font.bold, 18f);
        float bt = metinGenislik(baslik, font.bold, 18f);
        cs.beginText(); cs.newLineAtOffset(MARGIN + (PAGE_WIDTH - bt) / 2f, yy); cs.showText(baslik); cs.endText();
        cs.setNonStrokingColor(0f, 0f, 0f);
        yy -= 20f;

        if (altBaslik != null && !altBaslik.isBlank()) {
            cs.setFont(font.regular, 10f);
            cs.setNonStrokingColor(0.4f, 0.45f, 0.52f);
            float w = metinGenislik(altBaslik, font.regular, 10f);
            cs.beginText(); cs.newLineAtOffset(MARGIN + (PAGE_WIDTH - w) / 2f, yy); cs.showText(altBaslik); cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            yy -= 16f;
        }
        yy -= 8f;
        return yy;
    }

    /** Urun (RasPel) logosunu classpath'ten yukler; yoksa null. */
    private PDImageXObject urunLogoYukle(PDDocument doc) {
        try (java.io.InputStream in = getClass().getResourceAsStream("/brand/logo-icon.png")) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) return null;
            return PDImageXObject.createFromByteArray(doc, bytes, "raspel-logo");
        } catch (Exception e) {
            return null;
        }
    }

    private PDImageXObject logoYukle(PDDocument doc, FaturaSablonu sablon, Sirket s) {
        if (sablon != null && Boolean.FALSE.equals(sablon.logoGoster)) return null;
        if (s == null || bosMu(s.getLogoUrl())) return null;
        try {
            String filename = s.getLogoUrl().substring(s.getLogoUrl().lastIndexOf('/') + 1);
            // Logo tenant klasorune (sirket-logos/s{id}) yazilir; eski kayitlar kok
            // klasorde olabilecegi icin once tenant, sonra kok denenir.
            DosyaDepolamaService.DepolananDosya d = s.getId() != null
                    ? dosyaDepolamaService.getir("sirket-logos/s" + s.getId(), filename)
                    : null;
            if (d == null || d.icerik() == null || d.icerik().length == 0) {
                d = dosyaDepolamaService.getir("sirket-logos", filename);
            }
            if (d == null || d.icerik() == null || d.icerik().length == 0) return null;
            return PDImageXObject.createFromByteArray(doc, d.icerik(), "logo");
        } catch (Exception e) {
            return null;
        }
    }

    /** Teslim imzası PNG'sini PDF gömülebilir görsele çevirir; bulunamazsa null. */
    private PDImageXObject imzaGorseli(PDDocument doc, String imzaUrl) {
        try {
            if (imzaUrl == null || imzaUrl.isBlank()) return null;
            String filename = imzaUrl.substring(imzaUrl.lastIndexOf('/') + 1);
            DosyaDepolamaService.DepolananDosya dosya = dosyaDepolamaService.getir("teslimat-imzalari", filename);
            if (dosya == null || dosya.icerik() == null || dosya.icerik().length == 0) return null;
            return PDImageXObject.createFromByteArray(doc, dosya.icerik(), "imza");
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ Etiketler (raf etiketi)

    public byte[] stokEtiketi(Stok stok, byte[] qrPng) {
        return stokEtiketi(stok, qrPng, null, "IKISI");
    }

    public byte[] stokEtiketi(Stok stok, byte[] qrPng, byte[] barkodPng, String tip) {
        return stokEtiketleri(List.of(new EtiketVeri(stok, qrPng, barkodPng, tip)));
    }

    public record EtiketVeri(Stok stok, byte[] qrPng, byte[] barkodPng, String tip) {}

    public byte[] stokEtiketleri(List<EtiketVeri> etiketler) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PDDocument doc = new PDDocument()) {
            FontSet font = fontlar(doc);
            for (EtiketVeri v : etiketler) {
                etiketSayfasi(doc, v, font);
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }
    }

    private void etiketSayfasi(PDDocument doc, EtiketVeri v, FontSet font) throws IOException {
        Stok stok = v.stok();
        boolean barkodGoster = v.barkodPng() != null && v.barkodPng().length > 0 && !"QR".equalsIgnoreCase(v.tip());
        boolean qrGoster = v.qrPng() != null && v.qrPng().length > 0 && !"BARKOD".equalsIgnoreCase(v.tip());
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = PDRectangle.A4.getHeight() - MARGIN;
            cs.setFont(font.bold, 16f);
            cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("RAF ETİKETİ"); cs.endText();
            y -= 16f;
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(MARGIN + PAGE_WIDTH, y);
            cs.stroke();
            y -= 26f;

            String ad = stok.getAd() != null ? stok.getAd() : "-";
            String kod = stok.getStokKodu() != null ? stok.getStokKodu() : "-";
            String barkod = stok.getBarkod() != null ? stok.getBarkod() : "-";
            String raf = stok.getRafNo() != null ? stok.getRafNo() : "-";
            String fiyat = stok.getSatisFiyati() != null ? para(stok.getSatisFiyati(), "TL") : "-";

            cs.setFont(font.bold, 20f);
            for (String p : sar(ad, font.bold, 20f, PAGE_WIDTH)) {
                cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(p); cs.endText();
                y -= 24f;
            }
            y -= 6f;

            cs.setFont(font.regular, 14f);
            cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Kod: " + kod); cs.endText();
            y -= 20f;
            cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Barkod: " + barkod); cs.endText();
            y -= 20f;
            cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Raf No: " + raf); cs.endText();
            y -= 20f;
            cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("Fiyat: " + fiyat); cs.endText();
            y -= 30f;

            if (barkodGoster) {
                PDImageXObject b = PDImageXObject.createFromByteArray(doc, v.barkodPng(), "barkod");
                float bGenislik = 240;
                float bYukseklik = 80;
                cs.drawImage(b, MARGIN, y - bYukseklik, bGenislik, bYukseklik);
                y -= (bYukseklik + 18);
            }

            if (qrGoster) {
                PDImageXObject qr = PDImageXObject.createFromByteArray(doc, v.qrPng(), "qr");
                float qrBoyut = 140;
                cs.drawImage(qr, PAGE_WIDTH - qrBoyut + MARGIN, y - qrBoyut, qrBoyut, qrBoyut);
                cs.setFont(font.regular, 9f);
                cs.beginText();
                cs.newLineAtOffset(PAGE_WIDTH - qrBoyut + MARGIN, y - qrBoyut - 12);
                cs.showText("Karekod ile tarayıp say");
                cs.endText();
            }
            cs.setFont(font.regular, 9f);
            cs.beginText(); cs.newLineAtOffset(MARGIN, MARGIN - 18); cs.showText("RasPel ERP - Otomatik Oluşturulmuştur"); cs.endText();
        }
    }

    // ------------------------------------------------------------------ Yardımcılar

    private CariHesap cariHesap(Fatura f) {
        try {
            if (f.getCariHesap() == null) return null;
            // Lazy proxy'yi baslatmadan id alinir; cari ayrica yuklenir (open-in-view kapali).
            Long cariId = f.getCariHesap().getId();
            if (cariId == null) return null;
            return cariHesapRepository.findById(cariId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Sirket sirketBul(Long sirketId) {
        try {
            if (sirketId != null) {
                Sirket s = sirketRepository.findById(sirketId).orElse(null);
                if (s != null) return s;
            }
            return sirketRepository.findFirstByAktifTrueOrderByIdAsc();
        } catch (Exception e) {
            return null;
        }
    }

    private String tarih(LocalDate d, LocalDateTime dt) {
        if (d != null) return d.format(TARIH);
        if (dt != null) return dt.format(TARIH);
        return "-";
    }

    private String para(BigDecimal v, String birim) {
        BigDecimal deger = v != null ? v : BigDecimal.ZERO;
        String b = (birim == null || birim.isBlank()) ? "TL" : birim;
        return PARA_FMT.format(deger) + " " + b;
    }

    /** Birim fiyat/tutar gibi tablo hücreleri: para birimi eklenmez. */
    private String paraBare(BigDecimal v) {
        return v != null ? PARA_FMT.format(v) : "-";
    }

    private String sayi(BigDecimal v) {
        if (v == null) return "-";
        BigDecimal s = v.stripTrailingZeros();
        if (s.scale() < 0) s = s.setScale(0);
        return PARA_FMT.format(s);
    }

    private static boolean sayisalMi(String s) {
        String t = s.trim();
        if (t.isEmpty()) return false;
        if (!t.matches("-?[0-9][0-9., ]*[%₺TLkg]*")) return false;
        return t.chars().anyMatch(Character::isDigit);
    }

    private float metinGenislik(String s, PDType0Font f, float boyut) throws IOException {
        if (s == null || s.isEmpty()) return 0f;
        return f.getStringWidth(s) / 1000f * boyut;
    }

    private List<String> sar(String metin, PDType0Font f, float boyut, float maxGenislik) throws IOException {
        List<String> out = new ArrayList<>();
        if (metin == null) return out;
        String temiz = boslukTemizle(metin);
        if (temiz.isEmpty()) return out;
        if (maxGenislik <= 0) {
            out.add(temiz);
            return out;
        }
        StringBuilder satir = new StringBuilder();
        for (String kelime : temiz.split(" ")) {
            String aday = satir.length() == 0 ? kelime : satir + " " + kelime;
            if (metinGenislik(aday, f, boyut) <= maxGenislik) {
                satir.setLength(0);
                satir.append(aday);
                continue;
            }
            if (satir.length() > 0) {
                out.add(satir.toString());
                satir.setLength(0);
            }
            if (metinGenislik(kelime, f, boyut) <= maxGenislik) {
                satir.append(kelime);
            } else {
                StringBuilder parca = new StringBuilder();
                for (int i = 0; i < kelime.length(); i++) {
                    char ch = kelime.charAt(i);
                    if (metinGenislik(parca.toString() + ch, f, boyut) <= maxGenislik) {
                        parca.append(ch);
                    } else {
                        out.add(parca.toString());
                        parca.setLength(0);
                        parca.append(ch);
                    }
                }
                satir.append(parca);
            }
        }
        if (satir.length() > 0) out.add(satir.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    private static String boslukTemizle(String s) {
        if (s == null) return "";
        return s.replace("\r", " ").replace("\n", " ").replace("\t", " ").replaceAll("\\s+", " ").trim();
    }

    private static boolean bosMu(String s) {
        return s == null || s.isBlank();
    }

    private static String birlestir(String a, String b, String ayrac) {
        String x = bosMu(a) ? "" : a.trim();
        String y = bosMu(b) ? "" : b.trim();
        if (x.isEmpty()) return y;
        if (y.isEmpty()) return x;
        return x + ayrac + y;
    }

    /** "a V.D." + "123" → "a V.D. - 123" gibi; a boşsa ek/ayraç yazılmaz. */
    private static String birlestir(String a, String ek) {
        if (bosMu(a)) return "";
        return a.trim() + ek;
    }

    private static String birlestir2(String a, String b, String ayrac) {
        return birlestir(a, b, ayrac);
    }

    private static float[] hexToRgb(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int bl = Integer.parseInt(h.substring(4, 6), 16);
            return new float[]{r / 255f, g / 255f, bl / 255f};
        } catch (Exception e) {
            return new float[]{0f, 0f, 0f};
        }
    }

    // ------------------------------------------------------------------ Şablon

    private FaturaSablonu faturaSablonuOku(Long sirketId) {
        if (sirketId == null) return FaturaSablonu.varsayilan();
        try {
            return sirketRepository.findById(sirketId)
                    .map(Sirket::getFaturaSablonu)
                    .filter(s -> s != null && !s.isBlank())
                    .map(this::parseSablon)
                    .orElseGet(FaturaSablonu::varsayilan);
        } catch (Exception e) {
            return FaturaSablonu.varsayilan();
        }
    }

    private FaturaSablonu parseSablon(String json) {
        try {
            JsonNode n = new ObjectMapper().readTree(json);
            FaturaSablonu v = FaturaSablonu.varsayilan();
            if (n.hasNonNull("faturaBasligi")) v.faturaBasligi = n.get("faturaBasligi").asText();
            if (n.hasNonNull("altBaslik")) v.altBaslik = n.get("altBaslik").asText();
            if (n.hasNonNull("renk")) v.renk = n.get("renk").asText();
            if (n.hasNonNull("kagitBoyutu")) v.kagitBoyutu = n.get("kagitBoyutu").asText();
            if (n.hasNonNull("sayfaYonu")) v.sayfaYonu = n.get("sayfaYonu").asText();
            if (n.hasNonNull("logoGoster")) v.logoGoster = n.get("logoGoster").asBoolean();
            if (n.hasNonNull("imzaKutusuGoster")) v.imzaKutusuGoster = n.get("imzaKutusuGoster").asBoolean();
            if (n.hasNonNull("odemeDurumuGoster")) v.odemeDurumuGoster = n.get("odemeDurumuGoster").asBoolean();
            if (n.hasNonNull("qrKodGoster")) v.qrKodGoster = n.get("qrKodGoster").asBoolean();
            return v;
        } catch (Exception e) {
            return FaturaSablonu.varsayilan();
        }
    }

    static final class FaturaSablonu {
        String faturaBasligi;
        String altBaslik;
        String renk = "#1e40af";
        String kagitBoyutu = "a4";
        String sayfaYonu = "portrait";
        Boolean logoGoster = true;
        Boolean imzaKutusuGoster = false;
        Boolean odemeDurumuGoster = false;
        Boolean qrKodGoster = false;

        static FaturaSablonu varsayilan() {
            return new FaturaSablonu();
        }

        PDRectangle sayfaBoyutu() {
            if (kagitBoyutu == null) return PDRectangle.A4;
            boolean yatay = "landscape".equalsIgnoreCase(sayfaYonu);
            return switch (kagitBoyutu.toLowerCase()) {
                case "a5" -> yatay
                        ? new PDRectangle(PDRectangle.A5.getHeight(), PDRectangle.A5.getWidth())
                        : PDRectangle.A5;
                case "letter" -> yatay
                        ? new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth())
                        : PDRectangle.LETTER;
                default -> yatay
                        ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                        : PDRectangle.A4;
            };
        }
    }
}
