# ROADMAP — RasPel ERP

> Sürüm geçmişi için `CHANGELOG.md` dosyasına bakın. Bu belge planlanan çalışmaları takip eder.

## Planlı Epikler (v2.0)

### Epik 1 — Ürün Maliyet/ Kârlılık Analiz Sistemi (YARININ ODAĞI)

Gerçek alış/satış hareketlerinden ağırlıklı ortalama maliyet, satış ortalaması, tedarikçi/müşteri bazlı analiz ve kârlılık hesaplayan sistem.

- [ ] **Faz 0-1:** Mevcut Product/Stock/Purchase/PurchaseItem/Sale/SaleItem/Cari/StokHareket yapısını analiz et ve raporla (entity, migration, API, servis, DTO, frontend, raporlama)
- [ ] **Faz 2:** Veritabanı/entity tasarımı — gereken yeni tablolar + index + migrationlar (Flyway); mevcut tabloları bozma
- [ ] **Faz 3:** InventoryTransaction/stok hareket sistemini oluştur veya mevcut yapıyı genişlet (PURCHASE/SALE/PURCHASE_RETURN/SALE_RETURN/ADJUSTMENT)
- [ ] **Faz 4:** Moving weighted average maliyet hesabı (BigDecimal, HALF_UP)
- [ ] **Faz 5:** Alış/satış analiz servisleri (ort, ağırlıklı ort, ilk/son/min/max, toplam, tedarikçi/müşteri bazında, tarih aralığı)
- [ ] **Faz 6:** Kârlılık hesabı (birim/toplam brüt kâr, brüt marj; iade/iskonto/KDV kuralları dahil)
- [ ] **Faz 7:** REST API'leri (`/api/products/{id}/analysis` vb., mevcut naming'e uygun)
- [ ] **Faz 8:** Unit + integration testler (ort. 533.33, satış sonrası maliyet sabit, iade, sıfır bölme, boş kayıt, tarih filtresi, tedarikçi/müşteri, kâr)
- [ ] **Faz 9:** Vue ürün analiz ekranı
- [ ] **Faz 10:** Filtreleme ve raporlama (grafik verisi: ay-bazlı ortalama alış/satış)
- [ ] **Faz 11-12:** Performans/N+1 + security/auth kontrol
- [ ] **Faz 13:** Production gözden geçirme
- [ ] **Not:** İki epik de "önce analiz, sonra kademeli uygulama, mevcut sistemi bozma" metodolojisiyle yürütülecek. Her faz sonunda rapor.

### Epik 2 — Grup Sohbet Odaları · İkonlar · Ajanda · Kurumsal Dashboard · AI Altyapı

- [ ] **Bölüm A:** Grup sohbet odaları (ChatRoom/Member/Message + rollere OWNER/ADMIN/MEMBER, backend yetki, WebSocket gerçek zamanlı, last-50 + cursor pagination); birebir sohbet korunacak
- [ ] **Bölüm B:** WhatsApp/Mail/sohbet/ajanda/dashboard menü ikon tamamlama (mevcut ikon kütüphanesi)
- [ ] **Bölüm C:** Ajanda görev + hatırlatıcı (AgendaTask/AgendaReminder, takvim badge'i, bildirim, backend yetki)
- [ ] **Bölüm D:** Kurumsal tasarım dili (Tailwind config + PrimeVue tema token'ları) ve gerçek verili yeni dashboard grafikleri
- [ ] **Bölüm E:** AI altyapı iskeleti (AiService arayüzü, env tabanlı anahtar, asenkron çağrı; dashboard özeti / ajanda NL / sohbet özeti)

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
