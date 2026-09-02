# CHANGELOG — RasPel ERP

Tüm önemli değişiklikler ve sürüm notları bu dosyada takip edilir.

## [1.15.0] - 2026-09-02 (Pivot Rapor, Aktivite Akışı, İş Emri)
### Eklenenler
- **Dinamik pivot tablo**: `GET /api/raporlar/pivot` + Raporlar'da "Pivot Tablo" sekmesi. Satır (cari/ürün/kategori/tür/ödeme/ay), sütun ve değer (tutar/adet) seçilerek çapraz rapor.
- **Aktivite akışı**: Bildirimlere kullanıcı adı eklendi (V67) — "Ahmet yeni fatura kesti" görünür.
- **İş emri**: Siparişten iş emri oluşturup personele atama (`POST /api/siparisler/{id}/is-emri`, V67).
- **Yönetici Kokpiti tema düzeltmesi**: AI analiz kutusu ve header artık tema değişkenlerini kullanıyor (açık/koyu temada okunabilir).

### Eklenen Testler
- Backend: 831 → 832 (SiparisService iş emri).

## [1.14.0] - 2026-09-02 (Ölçeklenebilirlik & Büyük Veri Performansı)
### Eklenenler
- **Stok server-side filtre/pagination**: 5000+ kayıt için `GET /api/stoklar/filtreli` — arama, kategori, marka, stok grubu ve fiyat aralığı sunucu tarafında filtrelenir, lazy pagination + debounce ile.
- **Cari server-side filtre**: `GET /api/cari-hesaplar/filtreli` — arama, tür ve bakiye yönü sunucu tarafında.
- **Cari istatistik şeridi**: `GET /api/cari-hesaplar/ozet` — toplam kayıt, alacaklı ve borçlu tutarları.
- Stoklar ve Cariler artık tüm kayıtları çekmek yerine sayfalı çalışır (performans).

## [1.13.0] - 2026-09-02 (UX & Kullanım Kolaylığı)
### Eklenenler
- **Canlı senkron**: Fiş yazdırma ayarları sekmeler arası anlık senkronize olur (storage event).
- **Klavye kısayol rehberi**: `?` tuşuyla açılan kısayol paneli.
- **POS satış sonrası özet**: Satış tamamlanınca özet dialog (fatura no, toplam, ödenen, para üstü).
- **Para üstü hesaplama**: Nakit ödemede alınan tutara göre otomatik para üstü.
- **Sepet kaydet/yükle**: POS'ta sepeti kaydedip sonra geri yükleme.
- **Gün sonu (Z raporu)**: Kasa ekranında günlük satış adedi, nakit/kart/havale dağılımı ve kasa bakiyeleri.
- **Dashboard hızlı işlemler**: Hızlı Satış, Yeni Fatura, Kasa, Cari, Stok kısayol butonları.
- **Cari not önem derecesi**: Görüşme notlarına Normal/Yüksek/Kritik etiketi.

### Eklenen Testler
- Frontend: 160 → 163 (useCanliAyar).

## [1.12.0] - 2026-09-02 (POS & Cari Geliştirmeleri)
### POS (Hızlı Satış)
- **Ödeme yöntemi**: Nakit/Kart/Havale butonları.
- **Kasa seçimi**: Satış hangi kasaya işlendi seçilebilir; tahsilat otomatik kasa girişi olur (V65).
- **Son satışı iptal**: Tek tıkla son satışı iptal edip stoğu geri alma.
- **Müşteri bakiye/kredi limiti uyarısı**: Müşteri seçince borç/alacak ve limit aşımı uyarısı.
- **Günlük satış geçmişi**: "Bugün" sekmesiyle günün satışlarını görme.
- **Adet klavye girişi**: Sepette adedi doğrudan yazma.

### Cari Hesaplar
- **Hızlı işlem butonları**: Yeni Fatura, Tahsilat, Düzenle, Detay tek tıkla.
- **Bakiye durum rozeti**: Alacak/Borç renkli rozet.
- **Filtreler**: Tür (müşteri/tedarikçi) ve bakiye (alacaklı/borçlu) filtresi.
- **Toplu e-posta**: Seçili carilere toplu e-posta istemcisi açma.
- **Cariye özel fiyat**: Cari+ürün bazında özel fiyat tanımı (V66), satışta kullanılır.

### Eklenen Testler
- Backend: 830 → 831 (cariFaturalari).

## [1.11.0] - 2026-08-31 (Satış & Finans İyileştirmeleri)
### Eklenenler
- **Çoklu fiyat**: Bir ürüne birden fazla fiyat tanımı (`StokFiyat`, V64) — Perakende/Toptan/Kurumsal vb. Stok kartından yönetilir, POS satışında satıcı seçebilir.
- **Cari geçmiş ürünler**: Cari hesap detayında "Geçmişte Aldığı Ürünler" paneli (hover/tıklama ile).
- **Cari-ürün fiyat geçmişi**: `GET /api/faturalar/cari/{cariId}/stok/{stokId}/fiyat-gecmisi` — carinin bir ürünü geçmişte hangi fiyattan aldığını listeler.
- **Kasa aktarımı**: `POST /api/kasalar/aktar` — kasalar arası para transferi (her iki taraf için hareket kaydı).
- **POS tam ekran düzen**: Hızlı satış sayfası tam genişlikte ve daha ferah düzenlendi.

### Eklenen Testler
- Backend: 825 → 830 (StokService çoklu fiyat, KasaService aktarım).

## [1.10.0] - 2026-08-31 (POS / Hızlı Satış İyileştirmeleri)
### Eklenenler
- **Global barkod girişi**: Hızlı Satış'ta her an aktif barkod input'u (Enter ile ekleme + autofocus); USB barkod tarayıcıyla kesintisiz okuma.
- **Sürekli okuma modu**: Kameralı barkod okuyucu artık "sürekli mod" ile art arda okuma yapabiliyor (aynı kodu tekrar okumayı engelleyen debounce).
- **Sunucu-taraflı barkod arama**: Barkod yerel listede bulunamazsa `GET /api/stoklar/ara` ile backend'de aranır.
- **Çok satanlar hızlı erişim**: `GET /api/stoklar/en-cok-satanlar` + POS'ta tek dokunuşla sepete eklenen çok satanlar paneli.
- **Offline satış kuyruğu**: Ağ yoksa satış localStorage kuyruğuna alınır; bağlantı gelince otomatik senkronize edilir.
- **ESC/POS termal yazıcı**: Fişi termal yazıcıya gönderme (WebUSB) + tarayıcı yazdırma fallback'i.
- **Stok hareketi batch kayıt**: Fatura kalemleri tek `saveAll` ile kaydedilir (kalem başına INSERT yerine).

### Eklenen Testler
- Backend: 823 → 825 (StokService.enCokSatanlar).
- Frontend: 158 → 160 (escpos fiş üretici).

## [1.9.0] - 2026-08-31 (Yeni Özellikler & Entegrasyon Katmanı)
### Eklenenler
- **E-Fatura GİB durum sorgulama**: `POST /api/e-fatura/{id}/durum-sorgula` — GİB/entegratörden güncel durum kodunu çeker; uç nokta yoksa yerel onay (simülasyon) ile 1200→1300 geçişi yapılır.
- **Rapor PDF dışa aktarım**: `GET /api/raporlar/butce-gerceklesen/pdf` — Bütçe vs Gerçekleşen raporunu PDF olarak indirir; `PdfRaporService.tabloRaporu` genel tablo render'ı eklendi.
- **REST API erişim token'ları**: Kişisel erişim token'ı (`raspel_pat_...`) üretimi/listeleme/silme (`/api/api-tokenlar`). Token SHA-256 hash olarak saklanır; `Authorization: Bearer raspel_pat_...` ile kimlik doğrulanır (V62).
- **Müşteri kayıp (churn) riski skorlama**: `GET /api/churn` — son işlem tarihine göre müşteri kayıp riskini 0-100 skorlar (YUKSEK/ORTA/DUSUK) ve aksiyon önerisi üretir.
- **Onay iş akışı eşikleri**: `GET/POST /api/onay-ayarlari` — modül bazlı (MASRAF/SATINALMA/IZIN) onay eşiği ve otomatik onay kuralı yapılandırması (V63). Masraf talepleri artık eşik altındaysa otomatik onaylanıp finans masraf modülüne işlenir.
- **Feature flag sistemi**: `FeatureFlagService` — `app.features.enabled/disabled` ile yeni özellikleri kademeli açma.
- **Tema "system" modu**: Karanlık/aydınlık tema artık işletim sistemi tercihine otomatik bağlanabilir (Oto modu).

### Eklenen Testler
- Backend: 795 → 823 (ApiTokenService, ChurnAnalizService, OnayAyariService, FeatureFlagService, EFatura durum sorgulama, masraf otomatik onay).
- Frontend: 157 → 158 (tema system modu).

## [1.8.0] - 2026-08-16 (Güvenlik & Ölçeklenebilirlik İyileştirmeleri)
### Düzeltmeler (güvenlik)
- **Tenant izolasyonu**: `GET /api/hareketler/son/{limit}`, `GET /api/hareketler/cari/{id}`, hareket filtreleme, `GET /api/stoklar/hareketler/tum`, stok hareketleri ve `GET /api/kategoriler/tur/{tur}` uç noktalarına firma filtresi/doğrulaması eklendi (önceden başka firmaların verilerini görebiliyordu).
- **Prod secret zorlaması**: `application-prod.properties`'te DB/SMTP/yedekleme secret'ları artık ortam değişkeni zorunlu (fail-fast); `ProdGuvenlikKontrolu` prod profilinde zayıf JWT_SECRET ile başlamayı engeller.

### İyileştirmeler
- **N+1 giderildi**: `StokService` tedarikçi adını artık batch (`findAllById`) çözer (liste başına N sorgu → 1 sorgu).
- **WebSocket broker seçeneği**: `app.websocket.relay-enabled=true` ile RabbitMQ STOMP relay (çok instance'ta senkron yayın); varsayılan bellek içi simple broker korunur.

## [1.7.1] - 2026-08-16 (Yedek Geri Yükleme & Hata Uyarısı)
### Eklenenler
- **Yedekten geri yükleme**: `POST /api/backups/restore/{filename}` + Yedekler ekranına "Geri Yükle" butonu (onaylı). `pg_dump` artık `--clean --if-exists` ile alındığı için geri yükleme mevcut veriyi temizleyip yazar.
- **Hata e-posta uyarısı**: `HataBildirimService` — sunucuda 500 hatası oluştuğunda firma e-postasına (veya `app.alert.email`) otomatik uyarı gönderir; spam'i önlemek için en az 5 dk arayla.

## [1.7.0] - 2026-08-16 (Bakım & Hata İzleme)
### Eklenenler
- **Sistem Durumu ekranı**: `/sistem-durum` sayfası — genel durum (UP/DOWN), uptime, sürüm, bellek/disk kullanımı, bileşen sağlığı (DB/Redis/RabbitMQ), yedekleme özeti ve tek tıkla yedek alma.
- **Hata kayıtları**: `HataLog` entity (V47); sunucudaki 500 hataları otomatik kaydedilir (tür, mesaj, uç nokta, firma) ve "Son Hatalar" bölümünde listelenir.
- `SistemDurumService` + `GET /api/sistem/durum` ve `GET /api/sistem/hata-log`.
- `GlobalExceptionHandler` 500 hatalarını artık kalıcı olarak kaydeder (isteğe bağlı enjeksiyon, test uyumlu).

## [1.6.1] - 2026-08-16 (Hata Düzeltmeleri)
### Düzeltmeler
- **İade güncelleme akışı**: `IadeService.guncelle` içindeki bozuk durum-geçiş mantığı düzeltildi (ters ternary `? null :` hatası). Artık TASLAK→TAMAMLANDI stoğu işler, TAMAMLANDI→IPTAL stoğu tersine çevirir.
- **Bildirim kuyruğu gerilemesi**: `BildirimService` yeniden yazılırken kaybolan RabbitMQ entegrasyonu (`kuyrugaGonder` + `RabbitTemplate`) geri eklendi; bildirimler artık hem DB'ye kaydedilir hem WebSocket'e hem de kuyruğa taşınır.
- **Bildirim testi**: `BildirimRepository` mock'u ve kalıcılık doğrulaması eklendi.
- **Sohbet**: Boş mesaj gönderimi engellendi (BusinessException).
- **Bildirim rozeti**: Yeni WebSocket bildirimi geldiğinde okunmamış sayacı artık artar.

## [1.6.0] - 2026-08-16 (Kullanıcı Dostu & Her Şey Tek Uygulamada)
### Eklenenler
- **Ekip içi sohbet**: `SohbetMesaj` + REST/WebSocket (gerçek zamanlı), `/sohbet` sayfası (V43).
- **Ajanda/takvim**: `GET /api/ajanda` (görev + fatura vade olayları), aylık takvim görünümü.
- **Onay akışı**: `/onaylar` sayfası (bekleyen izin + satın alma talepleri, tek tıkla onay/red).
- **Dosya yöneticisi**: `GET /api/belgeler` + `/belgeler` sayfası (yükleme, indirme, önizleme).
- **Cari/ürün görseli**: `foto_url` alanları (V44) + `POST /api/upload/foto`.
- **Bildirim merkezi**: `Bildirim` entity (V45) + geçmiş + okunmamış sayacı.
- **WhatsApp hatırlatma**: vade bildirimine `cariTelefon`, dashboard'da WhatsApp butonu.
- **Cari görüşme notları**: Not'a `cariHesapId` (V46) + cari detayında not bölümü.
- **Global arama**: belge (dosya) araması eklendi (12 varlık).
- **Komut paleti**: QuickSearch boşken hızlı komutlar (Yeni Fatura, Hızlı Satış, Sohbet...).
- **Geri al (undo)**: Stok silmeye "Geri Al" desteği.
- **Günlük özet e-postası**: `GunlukOzetService` (sabah 07:00, kritik stok + vadesi geçen fatura).
- **Rapor favori + parametre**: favorilere tarih aralığı parametresi kaydedilir.
- **Mobil onay**: Sidebar'da bekleyen onay sayacı rozeti.

### Zaten mevcut (ek kod gerekmedi)
- Özelleştirilebilir dashboard (widget göster/gizle), PWA offline (offline-first cache).

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
