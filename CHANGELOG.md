# CHANGELOG — RasPel ERP

Tüm önemli değişiklikler ve sürüm notları bu dosyada takip edilir.

## [1.5.1] - 2026-08-15 (Cari Bakiye Yönü Tutarlılığı)
### Düzeltmeler
- **Cari bakiye kuralı birleştirildi**: Bakiye artık tek bir kural izler — **Alacak pozitif (+), Borç negatif (−)**.
  - `HareketService`: Tahsilat bakiyeyi **azaltır** (−), Ödeme bakiyeyi **artırır** (+) (önceden tersiydi).
  - `RaporService`: Cari ekstre ve yaşlandırma raporu yönü bu kurala göre düzeltildi (yaşlandırma artık alacakları listeler).
  - Fatura entegrasyonu (satış=alacak+, alış=borç−) zaten bu kuralla uyumluydu.
- **Frontend**: Dashboard "Vadesi Geçen Cari" artık pozitif bakiyeleri (alacak) listeler; bakiye grafiği ve etiketler "Alacak/Borç" olarak güncellendi; yaşlandırma raporu "Alacak Bakiyesi" olarak etiketlendi.

## [1.5.0] - 2026-08-15 (Alış/Satış Faturası & Tedarikçi Takibi Geliştirmeleri)
### Eklenenler
- **Cari bakiye entegrasyonu**: Fatura kesilince cari bakiyesi otomatik güncellenir (satış=alacak +, alış=borç −); iptalde ters işlem.
- **Satın alma siparişi → alış faturası**: `POST /api/satinalma-siparisler/{id}/faturaya-cevir`; sipariş kalemleri faturaya kopyalanır, stok artar.
- **Tedarikçi bazlı ürün raporu**: `GET /api/raporlar/tedarikci-urunler` + Raporlar'a tedarikçi filtreli "Tedarikçi Ürünleri" sekmesi.
- **Ağırlıklı ortalama maliyet**: Alış faturasında stok maliyeti son fiyat yerine ağırlıklı ortalama ile hesaplanır.
- **Depo seçimi**: Alış faturasına "Giriş Deposu" seçimi; ürünler ilgili depo stoğuna eklenir (V40).
- **Alış iadesi**: İade türü (SATIS/ALIS); alış iadesi stoğu düşürür (V41).
- **Alış faturası PDF**: Türüne göre "ALIŞ FATURASI"/"SATIŞ FATURASI" başlığı + tedarikçi/depo bilgisi.
- **Stok kartında tedarikçi**: Stok DTO'ya `tedarikciAd`, Stoklar ekranına "Tedarikçi" kolonu.
- **Kritik stok → tedarik önerisi**: Kritik stok ekranına tek tıkla satın alma talebi oluşturma.
- **Alış faturası CSV import**: `POST /api/import/alis-fatura` (faturaNo bazında gruplama, otomatik stok ekleme).
- **Dövizli alış faturası**: Faturaya `paraBirimi` (V42); yabancı para birimli alış faturasında birim fiyat TL'ye çevrilip stok maliyeti TL kaydedilir.
- **Ürün kârlılık raporu**: `GET /api/raporlar/urun-karlilik` + Raporlar'a "Ürün Kârlılığı" sekmesi (alış maliyeti vs satış fiyatı, kâr marjı).

### Eklenen Testler
- Backend: 7 yeni test (toplam 584 → 591).

## [1.4.1] - 2026-08-15 (Alış Faturası Stok Düzeltmesi)
### Düzeltmeler
- **Alış faturası stok yönü**: `FaturaService` satış faturalarında stoğu düşürüyor, alış faturalarında ise artık stoğa **ekliyor** (önceden her iki türde de yanlışlıkla stok düşülüyordu). Alış faturası kesildiğinde ürünün maliyet fiyatı (`fiyat`), tedarikçi fiyatı ve tedarikçi ID'si (`tedarikciId`) güncellenir; iptalde ters işlem uygulanır.
- **Fatura ekranı**: Cari etiketi türe göre "Tedarikçi (Fabrika)"/"Müşteri" olarak değişir; yardım metni alış faturasının stoğa eklendiğini açıklar.

### Eklenen Testler
- Backend: `FaturaServiceTest.faturaDurumGuncelle_alis_increasesStock` (toplam 583 → 584).

## [1.4.0] - 2026-08-15 (Faz 5 — İlk Kurulum & Demo Veri Kaldırma)
### Eklenenler
- **İlk kurulum akışı**: Sistem boşken (hiç firma yokken) giriş sayfası firma adı, vergi no, vergi dairesi, telefon, e-posta ve yönetici hesabı (kullanıcı adı/şifre) bilgilerini ister; kurulum sonrası otomatik giriş yapılır. `POST /api/kurulum/baslat` + `GET /api/kurulum/durum` (herkese açık).
- **Demo veriler kaldırıldı**: `IlkKullaniciInitializer` (varsayılan admin/admin123 + RasPel Şirketi) ve `DataSeeder` (ABC/DEF firmaları, demo kullanıcılar) kaldırıldı.
- **Giriş/ilk kurulum i18n**: `Giris.vue` tamamen `$t()` anahtarlarına taşındı (TR/EN), `giris` ve `kurulum` çeviri bölümleri eklendi.

### Eklenen Testler
- Backend: `KurulumServiceTest`, `KurulumControllerTest` (6 yeni test, toplam 577 → 583).

## [1.3.1] - 2026-08-15 (Faz 4 — Kalite ve Tutarlılık)
### Düzeltmeler
- **PrimeVue auto-import uyumu**: `@primevue/auto-import-resolver` sürümü `5.0.0` → `4.5.5`'e sabitlendi (`primevue@4.5.5` ile eşleşti); `vite.config.js`'teki `legacyResolver` geçici çözümü kaldırıldı.
- **Vitest uyumu**: `@vitest/coverage-v8` sürümü `3.2.x` → `4.1.10`'a yükseltildi (`vitest@4.1.10` ile eşleşti); `npm install` ERESOLVE çakışması giderildi.
- **DashboardServiceTest**: Eksik `faturaRepository` mock'u ve hatalı `sonHareketleriGetir` overload'ı düzeltildi (NPE giderildi).
- **Vardiya modülü**: Eksik `GET /api/vardiyalar/personel/{personelId}` endpoint'i eklendi (frontend `vardiyaAPI.getByPersonel` ile eşleşti).

### Eklenen Testler
- Backend: `MaasBordroServiceTest`, `MaasBordroControllerTest`, `VardiyaServiceTest`, `VardiyaControllerTest`, `DovizKuruControllerTest`, `BackupControllerTest`, `BelgeControllerTest`, `BildirimKuyrukConsumerTest` (29 yeni test, toplam 548 → 577).
- Frontend: `createCrudStore.spec`, `useTheme.spec`, `useYakinZamanda.spec`, `useLocale.spec` (18 yeni test, toplam 116 → 134).

### Eklenenler
- **RabbitMQ bildirim kuyruğu**: `RabbitMQConfig` (kuyruk/exchange/binding + JSON converter) ve `BildirimKuyrukConsumer` (@RabbitListener) eklendi; bildirimler WebSocket'in yanı sıra asenkron kuyruğa da taşınıyor (`app.rabbitmq.enabled=false` ile devre dışı bırakılabilir).
- **Dil değiştirici**: `useLocale` composable'ı ve Tema menüsüne TR/EN dil bölümü eklendi (İngilizce mod yeniden erişilebilir).

## [1.3.0] - 2026-07-26 (Faz 3 — Yeni Kurumsal Özellikler)
### Eklenenler
- **E-Fatura & E-İrsaliye Modülü**: UBL-TR 2.1 XML üretimi, ETTN UUID yönetimi, GİB durum takibi (`/api/v1/e-fatura`).
- **Döviz Kuru Yönetimi**: Günlük USD, EUR, GBP TCMB alış/satış kurları takibi, Redis cache entegrasyonu (`/api/v1/doviz-kurlari`).
- **E-Posta Bildirim Servisi**: `EmailService` ile zaman uyumsuz e-posta ve fatura bildirimleri.
- **İki Faktörlü Doğrulama (2FA)**: TOTP tabanlı 2FA secret ve QR kod URI üretimi (`/api/v1/kullanicilar/setup-2fa`).
- **DevOps & İzleme**: Docker isolated bridge networks (`frontend-net`, `backend-net`, `db-net`), Prometheus alerting kuralları (`alert.rules.yml`) ve Grafana dashboard şablonu.

## [1.2.0] - 2026-07-26 (Faz 2 — Yapısal İyileştirmeler)
### Değiştirilenler
- **App.vue Refactoring**: `App.vue` monolit yapısı `AppSidebar.vue`, `PasswordChangeModal.vue` ve `ErrorBoundary.vue` bileşenlerine ayrıştırıldı.
- **API Versioning**: Tüm REST API'lere `/api/v1/` sürüm öneki desteği eklendi.
- **Kod Kalitesi**: `package.json` ismi `raspel-erp` yapıldı; `.eslintrc.cjs` ve `.prettierrc` eklendi.

## [1.1.0] - 2026-07-26 (Faz 1 — Acil Güvenlik & Performans Düzeltmeleri)
### Güvenlik & Düzeltmeler
- **Redis Şifrelemesi**: Redis container ve backend bağlantısına şifre koruması eklendi.
- **Traefik Koruması**: Dashboard router `basicAuth` ile korundu.
- **Finansal Veri Bütünlüğü**: V19 migration ile kalan `DOUBLE PRECISION` kolonlar `NUMERIC(19,2)`'ye dönüştürüldü.
- **Fatura Numarası**: `Math.random()` yerine `AtomicLong` sıralı sayaç yapısına geçildi.
- **Performans**: CSV/Excel dışa aktarımlarından `Integer.MAX_VALUE` kaldırılarak OOM riski engellendi.
