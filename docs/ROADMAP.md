# ROADMAP — RasPel ERP

> Sürüm geçmişi için `CHANGELOG.md` dosyasına bakın. Bu belge planlanan çalışmaları takip eder.

## Planlı Epikler (v2.0)

### Epik 1 — Ürün Maliyet/ Kârlılık Analiz Sistemi (YARININ ODAĞI)

Gerçek alış/satış hareketlerinden ağırlıklı ortalama maliyet, satış ortalaması, tedarikçi/müşteri bazlı analiz ve kârlılık hesaplayan sistem.

- [x] **Faz 0-1:** Mevcut Product/Stock/Purchase/PurchaseItem/Sale/SaleItem/Cari/StokHareket yapısını analiz et ve raporla (entity, migration, API, servis, DTO, frontend, raporlama)
- [x] **Faz 2:** Veritabanı/entity tasarımı — yeni tablo gerekmedi; `V82` ile yalnızca analiz indexleri (`fatura_kalem(stok_id)`, `iade_kalem(stok_id)`, `iade(tur,durum,tarih)`); mevcut tablolar bozulmadı
- [x] **Faz 3:** Hareket kaynağı kararı — PURCHASE/SALE/RETURN rolünü mevcut `fatura_kalem` (KESILDI) + `iade_kalem` (TAMAMLANDI) üstlenir; `StokAnalizService` bu kaynakları birleştirir
- [x] **Faz 4:** Moving weighted average — alış/satış ortalamaları `Σ(miktar×netBirimFiyat)/Σ(miktar)` ile gerçek hareketlerden; kârlılık maliyeti `stok.fiyat` (mevcut moving avg) + `tedarikciFiyat` fallback
- [x] **Faz 5:** Alış/satış analiz servisleri (ort, ağırlıklı ort, ilk/son/min/max, toplam, tedarikçi/müşteri bazında, tarih aralığı)
- [x] **Faz 6:** Kârlılık hesabı (birim/toplam brüt kâr, brüt marj; iade/iskonto kuralları dahil)
- [x] **Faz 7:** REST API'leri — `StokController` altında `/api/stoklar/{id}/analiz|alis-ozet|satis-ozet|karlilik|tedarikci-analiz|musteri-analiz|islem-gecmisi|aylik-fiyat` (tarih aralıklı, tenant `sirketId`)
- [x] **Faz 8:** Unit (16) + H2 entegrasyon (1) + Postgres entegrasyon testleri (CI'da); 893 backend testi + JaCoCo gate yeşil
- [x] **Faz 9:** Vue ürün analiz ekranı
- [x] **Faz 10:** Filtreleme ve raporlama (grafik verisi: ay-bazlı ortalama alış/satış)
- [x] **Faz 11-12:** Performans/N+1 + security/auth kontrol (islem-gecmisi `limit` tavanı, ters tarih doğrulaması, tüm analiz endpoint'lerinde `@PreAuthorize` + tenant guard teyit)
- [x] **Faz 13:** Production gözden geçirme (899 backend test + JaCoCo gate, 170 frontend test + coverage, lint + i18n temiz, canlıya deploy + smoke test; işlem geçmişi sunucu tarafı sayfalama + cari/aylık liste üst sınırları)
- [ ] **Not:** İki epik de "önce analiz, sonra kademeli uygulama, mevcut sistemi bozma" metodolojisiyle yürütülecek. Her faz sonunda rapor.

### Epik 2 — Grup Sohbet Odaları · İkonlar · Ajanda · Kurumsal Dashboard · AI Altyapı

- [ ] **Bölüm A:** Grup sohbet odaları (ChatRoom/Member/Message + rollere OWNER/ADMIN/MEMBER, backend yetki, WebSocket gerçek zamanlı, last-50 + cursor pagination); birebir sohbet korunacak
- [ ] **Bölüm B:** WhatsApp/Mail/sohbet/ajanda/dashboard menü ikon tamamlama (mevcut ikon kütüphanesi)
- [ ] **Bölüm C:** Ajanda görev + hatırlatıcı (AgendaTask/AgendaReminder, takvim badge'i, bildirim, backend yetki)
- [ ] **Bölüm D:** Kurumsal tasarım dili (Tailwind config + PrimeVue tema token'ları) ve gerçek verili yeni dashboard grafikleri
- [ ] **Bölüm E:** AI altyapı iskeleti (AiService arayüzü, env tabanlı anahtar, asenkron çağrı; dashboard özeti / ajanda NL / sohbet özeti)

### Epik 3 — KOBİ Özellik Paketi (QR Sayım · Taksit Takvimi · Müşteri 360 · PWA Push)

Kullanıcının seçtiği 4 özellik; her faz keşif → backend → frontend → test/lint/i18n → canlı deploy + smoke.

- [x] **Faz 1 — QR/Barkod Hızlı Stok Sayımı + Raf Etiketleri:** Backend `GET /api/stoklar/barkod/{kod}` (mevcut `barkodIleBul`'u endpoint'e taşı), `GET /api/stoklar/{id}/etiket` (PDF + ZXing QR, rafNo/stokKodu/barkod/fiyat), `POST /api/stok-sayim/tarama` (barkod ile TASLAK upsert/sayım artırma); `durumGuncelle`'e cache evict + kritik stok bildirimi bağlantısı. Frontend `StokSayim.vue`'a `BarcodeScannerModal` tarama modu, `BarkodEtiketDialog`'da harici qrserver yerine backend QR PNG. **Ek:** `PdfRaporService` gömülü Unicode font (DejaVuSans) ile Türkçe karakter/PDF 500 hatası giderildi; `stokAdi` alan uyumsuzluğu düzeltildi. Canlı smoke: barkod arama, tarama TASLAK artırma, PNG/PDF etiket ✓
- [x] **Faz 2 — Taksit Planı & Tahsilat Takvimi:** V68/V69 altyapısı üzerine `finans.taksit` plan satırları (V83; vade/tutar/durum/planNo), `GET /api/taksitler` + `/yaklasan` + `/takvim` + `/ozet`, `POST /api/taksitler/plan` (otomatik vade bölme), `POST /api/taksitler/{id}/ode`, silme; TAKSIT tahsilatında `taksitId` ile kalem atama; frontend `TaksitTakvimi.vue` (aylık takvim + yaklaşan liste + yeni plan diyaloğu). Canlı smoke: plan/takvim/yaklaşan/öde/sil ✓
- [x] **Faz 3 — Müşteri 360 Kartı:** `CariHesap`'a `temsilciId`/`temsilciAd` (V84), `CariKartDTO` + `GET /api/cari-hesaplar/{id}/kart` (cari + kredi durumu/kullanılabilir limit/risk + sipariş/fatura/iade özeti + son kayıtlar + fırsatlar + notlar + özel fiyatlar); frontend `CariKart360Dialog` (tablı) + Cari listesinde 360 butonu. Canlı smoke ✓
- [x] **Faz 4 — PWA Web Push Bildirimi:** `sistem.push_abonelik` tablosu (V85), `nl.martijndwars:web-push` + BouncyCastle + VAPID env (gitignored `.env`), `PushSubscriptionController` (`/api/push/vapid-public-key`, `/abone`, `/test`), `BildirimKuyrukConsumer` → push köprüsü (WebPushService); frontend `injectManifest` SW (`src/sw.js`: precache + push + notificationclick), `usePushBildirim` composable + Bildirim Zili'nde izin/kapat UI, giriş sonrası otomatik abone. Canlı smoke: VAPID aktif, abone/abone-sil/test ✓ (gerçek push tarayıcı izni/HTTPS gerektirir)

- [x] **Ek iyileştirmeler (Epik 3 sonrası):** Müşteri 360 kartına **Taksitler** sekmesi eklendi (`CariKartDTO.taksitler` + `TaksitRepository` cari sorgusu); push bildirimleri artık **tür bazlı** hedefleniyor — `WebPushService` kullanıcı bildirim tercihlerini (`Kullanici.bildirimTercihleri`) whitelist olarak uygular, boş liste = tüm tipler açık; `BildirimZili` ve `HesapAyarları` tercihleri tek kanonik tip listesinde (STOK/SIPARIS/TEKLIF/TESLIMAT/FATURA/VADE/TAKSILAT/ODEME/MASRAF_TALEBI) birleştirildi ve Bildirim Zili tercihleri backend'e kalıcı yazıyor. Canlı smoke: 360→taksit listesi, tercih whitelist round-trip ✓

## Kısa Vadeli (v1.9.0)

- [x] Kalan servis/controller test kapsamını tamamlama (Ajanda, Bildirim, Crm, Tahsilat, TekrarlayanFatura, Yetki vb.) — 59 controller + 146 test sinifi mevcut
- [ ] Büyük view dosyalarının (Dashboard, HizliSatis, Stoklar, Teklifler) alt bileşenlere ayrıştırılması
- [x] CI'da JaCoCo/Vitest coverage eşik (gate) tanımlanması (JaCoCo INSTR %50 / BRANCH %30 / LINE %55; Vitest global eşikler)
- [x] i18n eksik anahtar otomatik kontrol script'i (`scripts/check-i18n.mjs` + CI'de `npm run i18n:check`)
- [x] Cypress E2E suite'in CI'ye bağlanması (7 spec, dev-server üzerinde, auto-retry)

## Orta Vadeli

- [ ] API sürümleme (`/api/v1/`) tutarlı şekilde devreye alınması (şu an tümü `/api/`)
- [ ] SonarQube / SpotBugs statik analiz entegrasyonu
- [ ] Bulut yedekleme (S3/GDrive/Dropbox) uç nokta doğrulaması ve otomatik restore testi

## Uzun Vadeli

- [ ] Çoklu bölge (multi-region) yedeklilik ve failover
- [ ] Detaylı metrik/alert kural genişletme (SLO, hata oranı, p95 gecikme)
- [ ] Performans/load test otomasyonunun CI'ye bağlanması (`scripts/load-test.mjs`)

## Prod Hazırlık Kontrol Listesi

Canlıya geçiş öncesi zorunlu adımlar `docs/GO-LIVE.md` dosyasında tutulur.
