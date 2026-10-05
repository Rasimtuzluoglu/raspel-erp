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
- **Teslimatlar** — Teslimat kaydı **Satış faturası** oluştururken şoför + teslimat adresi girilince otomatik açılır; **Sipariş**'e şoför atandığında da ilgili teslimat otomatik oluşturulur/güncellenir. Şoför (DRIVER rolü) kendi teslimatlarını görür ve durumu **Beklemede → Yolda → Teslim Edildi** olarak günceller; teslimde **fotoğraf** eklenebilir. Teslimatlar sayfasında şoför seçimi, Tümü/Bugün/Geciken filtresi, yol tarifi ve **durum geçmişi** bulunur. Beklenen tarihi geçen teslimatlar için her sabah **gecikme bildirimi** üretilir. Durum değişiklikleri loglanır; sipariş "Teslim Edildi" yapıldığında bağlı teslimat da otomatik teslim edildi olur.

### Envanter
- **Stoklar** — Ürün yönetimi, kritik stok uyarıları, tablo/kart görünümü, toplu fiyat güncelleme, CSV aktarım. **Etiket** üretimi: satır menüsünden tek ürün, seçili ürünlerden toplu; **Barkod / QR / Her İkisi** seçilebilir, son tercih hatırlanır; yazdır veya PDF indir.
- **Depolar** — Depo bazlı stok, transfer, stok ekle/çıkar.
- **Seri/Lot, Stok Sayımı** — Seri takibi ve sayım süreçleri.
- **Üretim** — Reçete (ürün ağacı) ve üretim emri yönetimi. Emirler **Taslak → Üretimde → Tamamlandı / İptal** akışını izler; tamamlarken **kısmi üretim ve fire** girilebilir, hammaddeler otomatik stoktan düşülür, mamul stoğa eklenir ve **hammadde/işçilik/toplam maliyet** hesaplanır. **Planlama** sekmesinde ürün + miktar için hammadde **ihtiyaç analizi** (gerekli/mevcut/eksik + maliyet) yapılır; eksik kalemler için tek tıkla **satınalma talebi** oluşturulur. Siparişten otomatik üretim emri açılabilir. Emir detayında **durum geçmişi**, **Rapor** sekmesinde durum dağılımı ve fire/ortalama süre KPI'ları bulunur.

### İnsan Kaynakları
- **Personel, İzin, Puantaj, Maaş Bordro, Vardiya** — Personel ve İK süreçleri.

### Sistem
- **Şirket, Dönem, Kullanıcı, Yetkiler** — Çoklu şirket ve rol yönetimi.
- **Kategoriler, Notlar** — Notlar renkli etiket + önem derecesi destekler.
- **Veri Aktar** — CSV ile toplu stok/cari aktarımı (sürükle-bırak).
- **Yedekler** — Yedek alma/indirme/silme, saklama politikaları. **Yedek Klasörü**: "Yedek Klasörü Seç" ile bilgisayarınızda bir klasör seçin; manuel yedekler doğrudan o klasöre yazılır (satır başına "Bilgisayara Kaydet" ile de kaydedilir). Tarayıcı desteği gerekir (Chrome/Edge); desteklenmiyorsa normal indirmeye düşer. Otomatik (zamanlanmış) yedekler güvenlik nedeniyle tarayıcıdan bilgisayarınıza yazılamaz, sunucu/bulut'ta kalır.
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
| `Ctrl+S` | Form kaydet (POS sayfasında: satışı tamamla) |
| `Ctrl+P` | Yazdır (fiş/fatura) |
| `F2` | Hızlı Satış'a git (POS sayfasında: sepeti temizle) |
| `F4` | Stoklar'a git (POS sayfasında: müşteri seçimi) |
| `Esc` | Dialog kapat / iptal |
| `G` + harf | Hızlı gezinme (g+c cari, g+f fatura, g+h POS) — **POS sayfasında devre dışı** |
| `Ctrl+Shift+H` | Sunum (Müşteri) modunu aç/kapat |
| `?` | Kısayol rehberi (POS sayfasında: POS'a özel ipucu şeridi) |

### Hızlı Satış (POS) sayfası

POS tamamen klavyeyle kullanılabilir; fare zorunlu değildir.

**Sepet ve satış**

| Kısayol | İşlev |
|---|---|
| `F1` | Barkod alanına odak |
| `F2` | Sepeti temizle (8 sn geri alınabilir) |
| `F3` | Ürün arama kutusuna odak |
| `F4` | Müşteri seçimi |
| `F5` | Yeni müşteri |
| `F6` | Kamera barkod tarayıcı |
| `F7` | Bugünkü satışlar |
| `F8` | Fiş yazdır |
| `F9` | Tam ödeme + satışı tamamla |
| `F10` | Kısmi ödeme + satışı tamamla |
| `F11` | Termal fiş yazdır |
| `↑` / `↓` | Aktif satırda gezin |
| `Enter` | Aktif satırın miktar alanına odak |
| `Alt+↑` / `Alt+↓` | Aktif satırın miktarını artır/azalt |
| `Del` | Aktif satırı sil |
| `D` | Aktif satırı çoğalt |
| `G` / `Ctrl+Z` | Geri al (son satır işlemini, yoksa sepeti) |
| `N` / `K` / `H` / `T` | Ödeme yöntemi: Nakit / Kart / Havale / Taksit |
| `P` | Fiş modunu değiştir (fiyatlı ↔ fiyatsız) |

**Ürün ızgarası** — klavye ile tüm ürünlere erişilir:

| Kısayol | İşlev |
|---|---|
| `↓` (metin alanındayken) | Ürün ızgarasına gir (ilk stokta olan ürüne odaklanır) |
| `↑` `↓` `←` `→` | Izgarada ürün arasında gez |
| `Enter` | Seçili ürünü sepete ekle |
| `3` `Enter` | **3 adet** ekle (rakamlar doğrudan miktarı belirler) |
| `Shift+Enter` | Adet penceresi (herhangi bir miktar) |
| Sağ tık | Adet penceresini açar |
| `Esc` | Izgaradan çık, barkod alanına dön |

> **Not:** `G`+harf hızlı gezinmesi POS sayfasında **devre dışıdır**; çünkü
> `N`/`K`/`H`/`T`/`P`/`G` tuşları ödeme yöntemi, fiş modu ve geri alma atanır.
> POS'tan çıkmak için menüyü veya `Esc` zincirini kullanın.

**Sunum (Müşteri) Modu:** Kenar çubuğundaki göz simgesiyle veya `Ctrl+Shift+H` ile tek tıkla açılır; açıkken ekranın üstünde bir bilgilendirme şeridi görünür. Maliyet/alış, kâr/marj, cari bakiye, ciro/hedef, banka/kasa bakiyesi, tedarikçi, maaş/bordro ve muhasebe/bütçe/vergi tutarları gizlenir. Şeritten **Bulanık** veya **Gizle** maskeleme stili seçilebilir. Oturum bazlıdır; sayfa yenilenince kapanır. (Fatura/fiş çıktıları ve satış fiyatları gizlenmez.)

## Kullanıcı Dostu Özellikler

- **Karanlık/Açık tema** — Sol alttan geçiş
- **Arayüz dili** — Türkçe ve İngilizce. Görünüm (tema) menüsündeki **Dil** bölümünden TR/EN geçişi yapılır; seçim tarayıcıda saklanır ve PrimeVue bileşenleri (takvim, tablo filtreleri) de seçilen dile uyar. Çekirdek modüllerin İngilizce çevirileri tamamlanmıştır.
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
