# Anasayfa Finans Kokpiti — Tasarım

**Tarih:** 2026-11-06
**Durum:** Tasarım onayı alındı; uygulama planı bekleniyor.
**Kapsam:** Anasayfanın “Finans Masası Kokpiti” olarak yeniden kurgulanması. Görsel sistem anasayfadan doğar; diğer ekranlara sonraki fazlarda yayılır.

## 1. Niyet ve hedef kitlesi

- Anasayfa bir **finans kokpitidir**. İlk bakışta şirketin finansal durumu anlaşılmalı.
- Görsel yön: **yönetici kokpiti** — yoğun ama okunabilir kart ızgarası, mini tablolar ve mikro grafikler.
- Öncelikli yüzey: **masaüstü** (1440–1920px). Mobilde aynı bilgi mimarisi mantıklı alt dizilime iner.
- Tema: **koyu ve açık eşdeğer**; tek bileşen seti, tema token’ları.
- Kullanıcı şikâyeti: mevcut anasayfada **veri yoğunluğu az**; aynı alanda daha fazla anlamlı veri olmalı.

### Başarı ölçütleri

- İlk ekranda finansal sağlık (nakit, alacak/verecek, kâr, risk) 5 saniyede okunabilmeli.
- Her KPI/satır anlamlı bir detay hedefine gitmeli; “bakıp kalma” değil “tıkla ve çalış” olmalı.
- 1440px ekranda veri yoğunluğu belirgin şekilde artmalı; beyaz alan israfı olmamalı.
- Koyu/açık temada aynı bileşenler kusursuz çalışmalı.
- Anasayfa ilk yükleme hedefi 10 kullanıcılı ölçekte ~1 sn altı.

## 2. Bilgi mimarisi

### 2.1 Global kontrol çubuğu (sticky üst)

- Şirket, dönem (`BU_AY`, `GECEN_AY`, `BU_CEYREK`, `BU_YIL`), para birimi ve canlı durum.
- Tüm sayfa bu filtrelerle güncellenir.
- Excel/PDF indirme buradan alınır.
- Canlı güncelleme göstergesi korunur.

### 2.2 Finans özet bandı (ilk ekran)

6–8 kompakt KPI kartı:

1. Net Nakit
2. Alacak
3. Verecek
4. Net Çalışma Sermayesi
5. Dönem Ciro
6. Brüt Kâr
7. Marj %
8. Vadesi Geçen

Her KPI kartı:

- Ana değer (tabular rakam)
- Önceki döneme göre değişim yüzdesi
- 12 ay mini sparkline
- Kritik eş ikonu
- Detay hedefi (`route + query`)

### 2.3 Kritik durum şeridi

Tek satırda tıklanabilir kritik bilgiler:

- Vadesi geçen fatura tutarı/adet
- Kritik stok sayısı
- Bekleyen onay sayısı
- Likidite eğilimi (iyileşiyor / bozuluyor)

Kritik öğeler kırmızı/amber ile görselleştirilir; renk yalnızca taşıyıcı değildir, ikon/metin eşlik eder.

### 2.4 Dört yoğun finans bölümü

#### Nakit & Likidite
- Banka/kasa dağılımı (mini bar/segment)
- 30 gün nakit projeksiyonu
- Nakit giriş/çıkış grafiği
- Toplam likidite ve trend

#### Alacak / Verecek
- Yaşlanma kovaları (vadesi gelmemiş, 0–30, 31–60, 61–90, 90+)
- İlk 8 müşteri borçlusu ve tedarikçi borcu tablosu
- Tahsilat performansı (dönem tahsilatı / alacak)

#### Kâr & Marj
- Aylık ciro–maliyet–kâr grafiği
- Marj trendi
- En kârlı 5 ürün ve 5 cari
- Maliyetsiz kalem uyarısı

#### Fatura / Sipariş pipeline
- Fatura durum kovaları (taslak/bekliyor/kesildi/vadesi geçen/tahsil edildi)
- Açık sipariş durumu
- Tahsilat oranı
- Vadesi geçen ilk faturalar

## 3. Görsel sistem

### 3.1 Yüzey ve renk

- Koyu tema: derin slate zemin, teal ana vurgu; açık tema aynı hiyerarşiyle eşdeğer.
- Yüzeyler: 1px ince kenarlıklı kartlar; düşük/hiç gölge.
- Renk semantiği:
  - Teal: olumlu/ana vurgu
  - Kırmızı: negatif/kritik
  - Amber: uyarı
  - Mavi: bilgi
- Tüm renkler koyu/açık temada AA kontrast sağlar.

### 3.2 Tipografi ve yoğunluk

- KPI değerleri: 22–26px, tabular rakam.
- Etiketler: 11–12px.
- Bölüm başlıkları: 15–16px.
- Mini tablo satırları: 32–34px, metin 12px.
- Rakamlar sağa hizalı ve tabular; birim/para birimi küçük etiketle.

### 3.3 Kart deseni

Her bölüm kartı:

1. Başlık + mini sekme/filtre
2. Gövde (mini tablo veya mikro grafik)
3. Alt aksiyon / “tümünü gör”

Kart içi hızlı işlemler hover ile görünür; klavye ile de erişilebilir.

### 3.4 Grafik dili

- Minimal eksen, ağır dekor yok.
- Sparkline yüksekliği 40–60px.
- Ana grafik yüksekliği 160–220px.
- Renkler token’dan gelir; tema değişiminde grafik yeniden renklendirilir.

## 4. Veri ve API mimarisi

### 4.1 Tek kokpit ucu

`GET /api/dashboard/kokpit?donem=BU_AY`

Yanıt bölümleri:

```text
KokpitDTO
├── ozet: KPI listesi + değişim + sparkline + hedef
├── kritikDurum: kritik satır öğeleri
├── nakit: dağılım, projeksiyon, giriş/çıkış
├── alacakVerecek: yaşlanma, ilk 8 borçlu/tedarikçi, tahsilat performansı
├── karMarj: trend, ilk 5 ürün/cari, uyarılar
└── faturaSiparis: pipeline, vadesi geçen, tahsilat oranı, açık siparişler
```

- Tek istek, alt hesaplar backend içinde paralel çalışır.
- Toplam/yaşlanma/pipeline SQL aggregation ile hesaplanır; tüm tablo çekilip frontend’de filtrelenmez.
- Mevcut `DashboardService` ve `RaporService` sorguları yeniden kullanılır; kopya iş mantığı yazılmaz.

### 4.2 Filtre ve para birimi

- Dönem backend’de uygulanır.
- Şirket oturum bağlamından alınır; tenant izolasyonu bozulmaz.
- Para birimi seçimi anasayfada korunur; dönüşüm mevcut doviz akışıyla yapılır.
- Rol bazlı görünürlük: kâr/marj gibi hassas bölümler yalnız yetkili rollere döner.

### 4.3 Önbellek ve canlı güncelleme

- Kokpit yanıtı tenant + dönem + rol bazlı kısa TTL ile önbelleklenir.
- STOMP `/topic/dashboard/{sirketId}` olayı geldiğinde ilgili bölüm yenilenir.
- Canlı olay varsa “Canlı” göstergesi ve son güncelleme zamanı korunur.

### 4.4 Özelleştirme

- Mevcut `GET/PUT /api/dashboard/tercihler` sözleşmesi genişletilir:
  - Bölüm görünürlüğü
  - Bölüm sırası
- Varsayılan düzen küratörlüdür; kullanıcı istediğinde değiştirir.
- Tercih kaydı kullanıcı + şirket bazlı kalır.

## 5. Frontend mimarisi

### 5.1 Bileşenler

`views/Dashboard.vue` yalnız orkestrasyon yapar.

Yeni bileşenler `components/dashboard/kokpit/` altında:

- `KokpitGlobalFiltre.vue`
- `FinansOzetBandi.vue`
- `KritikDurumSeridi.vue`
- `NakitBolumu.vue`
- `AlacakVerecekBolumu.vue`
- `KarMarjBolumu.vue`
- `FaturaSiparisBolumu.vue`
- `KokpitOzellestirme.vue`

Ortak görsel bileşenler:

- `FinansKarti.vue`
- `MetrikKutu.vue`
- `MiniTablo.vue`
- `MikroGrafik.vue`
- `YaslanmaKovalari.vue`
- `PipelineKovalari.vue`

### 5.2 State

`kokpitStore`:

- Filtre durumu
- Bölüm verileri
- Yükleme/hata durumları
- Kullanıcı tercihi
- Canlı yenileme bayrağı

Filtre değişince tek istek atılır; her bölüm bağımsız iskelet/hata bandı taşır.

### 5.3 Etkileşimler

- KPI ve satırlar filtrelenmiş detay sayfasına gider.
- Kart içi hızlı işlemler:
  - Onay ver/reddet
  - Tahsilat gir
  - Fatura oluştur/aç
- Özelleştirme paneli bölüm görünürlüğü ve sırasını yönetir.

### 5.4 Responsive

- 1440px+: özet bandı tam genişlik; dört bölüm iki kolon.
- 1024–1439px: bölüm kartları akıcı grid.
- <1024px: bölümler tek kolon; mini tablolar özet moduna iner.
- 390px: yatay taşma yok; dokunma hedefi ≥44px.

## 6. Erişilebilirlik

- Tüm kart/aksiyon klavye erişilebilir; odak halkası net.
- Kritiklik yalnız renkle taşınmaz; ikon/metin eşlik eder.
- `prefers-reduced-motion` ile animasyonlar kapanır.
- Koyu/açık temada AA kontrast.
- Ekran okuyucu için bölüm başlıkları ve KPI etiketleri anlamlı.

## 7. Test stratejisi

### Backend
- `KokpitService` testleri:
  - KPI doğruluğu
  - Yaşlanma/pipeline hesapları
  - Dönem filtresi
  - Rol bazlı görünürlük
  - Hedef route/query doğruluğu
- Controller testleri: sözleşme, hata, yetki.
- Performans duman testi: tipik veriyle ~1 sn hedefi.

### Frontend
- Vitest:
  - Bölüm bileşenleri
  - `kokpitStore`
  - Filtre değişimi
  - Detay linkleri
  - Özelleştirme
- Cypress:
  - İlk yükleme
  - Filtre değişimi
  - Kritik şerit
  - Detaya inme
  - Özelleştirme
  - 390px taşma kontrolü

## 8. Fazlar

1. **Backend kokpit ucu + testler**
   - `KokpitDTO`, service, controller, önbellek, rol görünürlüğü
2. **Frontend iskelet + özet bandı + kritik şerit**
   - Yeni bileşen mimarisi, global filtre, temel görsel sistem
3. **Dört finans bölümü + etkileşimler**
   - Nakit, alacak/verecek, kâr/marj, fatura/sipariş; detay ve hızlı işlemler
4. **Özelleştirme, canlı yenileme, performans/a11y, dağıtım**
   - Tercih paneli, STOMP bölüm yenileme, Cypress, Docker, canlı doğrulama

## 9. Kapsam dışı

- Diğer ekranların görsel revizyonu (tasarım sistemi anasayfadan doğar, sonraki işte yayılır)
- Şube/depo bazlı filtre (mevcut şemada operasyonel tablolarda `sube_id` yok)
- SSO/passkey gibi kimlik işleri
- Raporlar ekranındaki rapor içeriklerinin değişmesi
