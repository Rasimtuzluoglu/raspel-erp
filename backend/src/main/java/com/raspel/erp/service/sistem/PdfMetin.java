package com.raspel.erp.service.sistem;

import java.util.Locale;
import java.util.Map;

/**
 * PDF/fiş belgelerindeki etiketlerin dil sözlüğü. Belge üretiminde kullanıcıya
 * görünen tüm sabit metinler buradan alınır; "tr" varsayılan, "en" desteklenir.
 * Bilinmeyen dil kodları tr'ye düşer.
 */
public final class PdfMetin {

    private final String dil;

    private PdfMetin(String dil) {
        this.dil = (dil != null && dil.toLowerCase(Locale.ROOT).startsWith("en")) ? "en" : "tr";
    }

    public static PdfMetin of(String dil) {
        return new PdfMetin(dil);
    }

    public static PdfMetin tr() {
        return new PdfMetin("tr");
    }

    public String dil() {
        return dil;
    }

    public boolean ingilizce() {
        return "en".equals(dil);
    }

    private static final Map<String, String> TR = Map.ofEntries(
            Map.entry("satisFaturasi", "SATIŞ FATURASI"),
            Map.entry("alisFaturasi", "ALIŞ FATURASI"),
            Map.entry("musteri", "MÜŞTERİ"),
            Map.entry("tedarikci", "TEDARİKÇİ"),
            Map.entry("belgeBilgileri", "BELGE BİLGİLERİ"),
            Map.entry("faturaNo", "Fatura No"),
            Map.entry("faturaTarihi", "Fatura Tarihi"),
            Map.entry("vadeTarihi", "Vade Tarihi"),
            Map.entry("durum", "Durum"),
            Map.entry("paraBirimi", "Para Birimi"),
            Map.entry("olusturan", "Oluşturan"),
            Map.entry("sira", "Sıra"),
            Map.entry("urunHizmet", "Ürün / Hizmet"),
            Map.entry("miktar", "Miktar"),
            Map.entry("birimFiyat", "Birim Fiyat"),
            Map.entry("kdvYuzde", "KDV %"),
            Map.entry("tutar", "Tutar"),
            Map.entry("kalemBulunmuyor", "Kalem bulunmuyor"),
            Map.entry("araToplam", "Ara Toplam"),
            Map.entry("iskonto", "İskonto"),
            Map.entry("kdv", "KDV"),
            Map.entry("genelToplam", "GENEL TOPLAM"),
            Map.entry("odenen", "Ödenen"),
            Map.entry("kalan", "Kalan"),
            Map.entry("odemeDurumu", "Ödeme Durumu"),
            Map.entry("toplamAgirlik", "Toplam Ağırlık"),
            Map.entry("imzaKase", "İmza / Kaşe"),
            Map.entry("imzaKaseIrsaliye", "Teslim Alan İmza / Kaşe"),
            Map.entry("teslimAlanImzasi", "Teslim Alan İmzası"),
            Map.entry("karekodIleDogrula", "Faturayı karekod ile doğrulayın"),
            Map.entry("siparisFormu", "SİPARİŞ FORMU"),
            Map.entry("siparisBilgileri", "SİPARİŞ BİLGİLERİ"),
            Map.entry("siparisNo", "Sipariş No"),
            Map.entry("tarih", "Tarih"),
            Map.entry("aciklama", "Açıklama"),
            Map.entry("birim", "Birim"),
            Map.entry("irsaliye", "İRSALİYE"),
            Map.entry("irsaliyeBilgileri", "İRSALİYE BİLGİLERİ"),
            Map.entry("irsaliyeNo", "İrsaliye No"),
            Map.entry("teslimatFisi", "TESLİMAT FİŞİ"),
            Map.entry("teslimatBilgileri", "TESLİMAT BİLGİLERİ"),
            Map.entry("teslimatAdresi", "Teslimat Adresi"),
            Map.entry("teslimEden", "Teslim Eden (Şoför)"),
            Map.entry("teslimAlan", "Teslim Alan"),
            Map.entry("teslimTarihi", "Teslim Tarihi"),
            Map.entry("fatura", "Fatura"),
            Map.entry("bagliKalemYok", "Bu teslimata bağlı fatura kalemi bulunmuyor."),
            Map.entry("rafEtiketi", "RAF ETİKETİ"),
            Map.entry("fiyat", "Fiyat"),
            Map.entry("karekodTarayipSay", "Karekod ile tarayıp say"),
            Map.entry("otomatikOlusturuldu", "RasPel ERP - Otomatik Oluşturulmuştur"),
            Map.entry("kod", "Kod"),
            Map.entry("barkod", "Barkod"),
            Map.entry("rafNo", "Raf No"),
            Map.entry("stokKoduEk", "(stok kodu)"),
            Map.entry("cariEkstre", "CARI EKSTRE"),
            Map.entry("gelirGiderRaporu", "GELIR/GIDER RAPORU"),
            Map.entry("cariKarlilikRaporu", "CARI KARLILIK RAPORU"),
            Map.entry("vadeYaslandirma", "VADE YASLANDIRMA"),
            Map.entry("butceGerceklesen", "BUTCE / GERCEKLESEN"),
            Map.entry("tur", "Tur"),
            Map.entry("borc", "Borc"),
            Map.entry("alacak", "Alacak"),
            Map.entry("bakiye", "Bakiye"),
            Map.entry("ayKalem", "Ay / Kalem"),
            Map.entry("kategori", "Kategori"),
            Map.entry("butce", "Bütçe"),
            Map.entry("gerceklesen", "Gerçekleşen"),
            Map.entry("sapma", "Sapma"),
            Map.entry("kullanimYuzdesi", "Kullanım %"),
            Map.entry("cari", "Cari"),
            Map.entry("satis", "Satış"),
            Map.entry("maliyet", "Maliyet"),
            Map.entry("kar", "Kâr"),
            Map.entry("marjYuzdesi", "Marj %"),
            Map.entry("toplam", "Toplam"),
            Map.entry("gecikmis", "Gecikmiş"),
            Map.entry("maksGun", "Maks. Gun"),
            Map.entry("toplamGelir", "Toplam Gelir"),
            Map.entry("toplamGider", "Toplam Gider"),
            Map.entry("netKarZarar", "Net Kar/Zarar"),
            Map.entry("tel", "Tel"),
            Map.entry("vd", "V.D."));

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("satisFaturasi", "SALES INVOICE"),
            Map.entry("alisFaturasi", "PURCHASE INVOICE"),
            Map.entry("musteri", "CUSTOMER"),
            Map.entry("tedarikci", "SUPPLIER"),
            Map.entry("belgeBilgileri", "DOCUMENT DETAILS"),
            Map.entry("faturaNo", "Invoice No"),
            Map.entry("faturaTarihi", "Invoice Date"),
            Map.entry("vadeTarihi", "Due Date"),
            Map.entry("durum", "Status"),
            Map.entry("paraBirimi", "Currency"),
            Map.entry("olusturan", "Created By"),
            Map.entry("sira", "No"),
            Map.entry("urunHizmet", "Product / Service"),
            Map.entry("miktar", "Quantity"),
            Map.entry("birimFiyat", "Unit Price"),
            Map.entry("kdvYuzde", "VAT %"),
            Map.entry("tutar", "Amount"),
            Map.entry("kalemBulunmuyor", "No items found"),
            Map.entry("araToplam", "Subtotal"),
            Map.entry("iskonto", "Discount"),
            Map.entry("kdv", "VAT"),
            Map.entry("genelToplam", "GRAND TOTAL"),
            Map.entry("odenen", "Paid"),
            Map.entry("kalan", "Balance"),
            Map.entry("odemeDurumu", "Payment Status"),
            Map.entry("toplamAgirlik", "Total Weight"),
            Map.entry("imzaKase", "Signature / Stamp"),
            Map.entry("imzaKaseIrsaliye", "Receiver Signature / Stamp"),
            Map.entry("teslimAlanImzasi", "Receiver Signature"),
            Map.entry("karekodIleDogrula", "Verify the invoice with the QR code"),
            Map.entry("siparisFormu", "ORDER FORM"),
            Map.entry("siparisBilgileri", "ORDER DETAILS"),
            Map.entry("siparisNo", "Order No"),
            Map.entry("tarih", "Date"),
            Map.entry("aciklama", "Description"),
            Map.entry("birim", "Unit"),
            Map.entry("irsaliye", "WAYBILL"),
            Map.entry("irsaliyeBilgileri", "WAYBILL DETAILS"),
            Map.entry("irsaliyeNo", "Waybill No"),
            Map.entry("teslimatFisi", "DELIVERY NOTE"),
            Map.entry("teslimatBilgileri", "DELIVERY DETAILS"),
            Map.entry("teslimatAdresi", "Delivery Address"),
            Map.entry("teslimEden", "Delivered By (Driver)"),
            Map.entry("teslimAlan", "Received By"),
            Map.entry("teslimTarihi", "Delivery Date"),
            Map.entry("fatura", "Invoice"),
            Map.entry("bagliKalemYok", "No invoice items linked to this delivery."),
            Map.entry("rafEtiketi", "SHELF LABEL"),
            Map.entry("fiyat", "Price"),
            Map.entry("karekodTarayipSay", "Scan the QR code to count"),
            Map.entry("otomatikOlusturuldu", "RasPel ERP - Automatically Generated"),
            Map.entry("kod", "Code"),
            Map.entry("barkod", "Barcode"),
            Map.entry("rafNo", "Shelf No"),
            Map.entry("stokKoduEk", "(stock code)"),
            Map.entry("cariEkstre", "ACCOUNT STATEMENT"),
            Map.entry("gelirGiderRaporu", "INCOME/EXPENSE REPORT"),
            Map.entry("cariKarlilikRaporu", "CUSTOMER PROFITABILITY REPORT"),
            Map.entry("vadeYaslandirma", "AGING REPORT"),
            Map.entry("butceGerceklesen", "BUDGET / ACTUAL"),
            Map.entry("tur", "Type"),
            Map.entry("borc", "Debit"),
            Map.entry("alacak", "Credit"),
            Map.entry("bakiye", "Balance"),
            Map.entry("ayKalem", "Month / Item"),
            Map.entry("kategori", "Category"),
            Map.entry("butce", "Budget"),
            Map.entry("gerceklesen", "Actual"),
            Map.entry("sapma", "Variance"),
            Map.entry("kullanimYuzdesi", "Usage %"),
            Map.entry("cari", "Account"),
            Map.entry("satis", "Sales"),
            Map.entry("maliyet", "Cost"),
            Map.entry("kar", "Profit"),
            Map.entry("marjYuzdesi", "Margin %"),
            Map.entry("toplam", "Total"),
            Map.entry("gecikmis", "Overdue"),
            Map.entry("maksGun", "Max Days"),
            Map.entry("toplamGelir", "Total Income"),
            Map.entry("toplamGider", "Total Expense"),
            Map.entry("netKarZarar", "Net Profit/Loss"),
            Map.entry("tel", "Tel"),
            Map.entry("vd", "Tax Office"));

    /** Etiket değerini geçerli dile göre döndürür; tanımsız anahtar anahtar adına düşer. */
    public String t(String anahtar) {
        Map<String, String> secili = ingilizce() ? EN : TR;
        String deger = secili.get(anahtar);
        return deger != null ? deger : anahtar;
    }
}
