# RasPel ERP — Kullanım Kılavuzu

## Modüller

### Finans
- **Cari Hesaplar** — Müşteri/tedarikçi tanımlama, bakiye takibi, hareket geçmişi. Kredi limiti ve vade bilgisi. Tabloda sütun göster/gizle, yoğunluk ayarı, toplu işlemler.
- **Faturalar** — Satış/alış faturası oluşturma, taslak/kesme/iptal akışı, PDF indirme (fiyatlı/fiyatsız), belge ekleme, otomatik fatura no (`FTR-2026-000001`).
- **Bankalar / Kasa** — Hesap ve bakiye takibi. IBAN'ı tıkla-kopyala.
- **Çek/Senet** — Portföy takibi, durum yönetimi.
- **Bütçe / Masraf** — Planlama ve harcama takibi.

### Ticaret
- **Hızlı Satış (POS)** — Barkod/ürün arama, sepet, indirim, ödeme. **Termal fiş yazdırma** (fiyatlı/fiyatsız önizleme + yazdır). Kısayollar: `Ctrl+S` satışı tamamla, `F2` sepeti temizle, `Ctrl+P` fiş.
- **Satış / Satın Alma / Sipariş** — Sipariş akışı, otomatik sipariş no (`SIP-2026-000001`).
- **İrsaliye / İade / Fiyat Listesi** — Sevk ve iade süreçleri.

### Envanter
- **Stoklar** — Ürün yönetimi, kritik stok uyarıları, tablo/kart görünümü, toplu fiyat güncelleme, CSV aktarım.
- **Depolar** — Depo bazlı stok, transfer, stok ekle/çıkar.
- **Seri/Lot, Stok Sayımı** — Seri takibi ve sayım süreçleri.

### İnsan Kaynakları
- **Personel, İzin, Puantaj, Maaş Bordro, Vardiya** — Personel ve İK süreçleri.

### Sistem
- **Şirket, Dönem, Kullanıcı, Yetkiler** — Çoklu şirket ve rol yönetimi.
- **Kategoriler, Notlar** — Notlar renkli etiket + önem derecesi destekler.
- **Veri Aktar** — CSV ile toplu stok/cari aktarımı (sürükle-bırak).
- **Yedekler** — Yedek alma/indirme/silme, saklama politikaları.
- **Kullanım Şartları / Gizlilik Politikası**

### Rapor
- **Raporlar** — Cari ekstre, gelir/gider, KDV, yaşlandırma. Favori raporlar (yıldız), PDF yazdırma.
- **Anomaliler** — Mükerrer fatura/hareket tespiti.
- **Denetim Log** — Tüm işlem kayıtları, filtreleme, kayıtlı filtreler, Excel export.
- **Hareketler** — Tüm finansal hareketler.

## Klavye Kısayolları

| Kısayol | İşlev |
|---|---|
| `Ctrl+K` | Hızlı arama (9 modül + son aramalar) |
| `Ctrl+S` | Form kaydet |
| `F2` | Yeni kayıt |
| `Ctrl+P` | Yazdır (fiş/fatura) |
| `Esc` | Dialog kapat |

## Kullanıcı Dostu Özellikler

- **Karanlık/Açık tema** — Sol alttan geçiş
- **Dil değiştirici** — TR/EN
- **Sık kullanılan menüler** — Menüdeki yıldız ile favorilere ekleme
- **Son görüntülenenler** — Dashboard'da son bakılan kayıtlar
- **Taslak koruma** — Fatura formu yazarken otomatik kaydedilir, sayfa yenilenirse "Taslak Geri Yüklendi"
- **Silme geri alma** — Not silindikten 8 saniye içinde "Geri Al"
- **Oturum uyarısı** — Süre bitmeden 2 dk önce uyarı
- **Masaüstü bildirimleri** — Tarayıcı izniyle WebSocket bildirimleri
- **Offline banner** — İnternet kesilince üstte uyarı
- **Şifre güç göstergesi** — Şifre değiştirme ekranında
- **Yazdırma önizlemesi** — Fiş pencerede önizlenir, "Yazdır" ile çıktı alınır

## Günlük İş Akışı Örneği

1. **Cari ekle** → Cari Hesaplar → Yeni Cari Hesap
2. **Ürün ekle** → Stoklar → Yeni Ürün
3. **Sipariş al** → Siparişler → Yeni Sipariş (no otomatik: `SIP-2026-000001`)
4. **Satışı tamamla** → Hızlı Satış → ürün ekle → Satışı Tamamla
5. **Fiş yazdır** → Fiş önizlemesinde "Yazdır" (fiyatsız seçeneği ile)
6. **Fatura kes** → Faturalar → Yeni Fatura → Kes
7. **Tahsilat al** → Cari Hesap → Hareketler → Tahsilat
8. **Raporla** → Raporlar → Cari Ekstre / Gelir-Gider
