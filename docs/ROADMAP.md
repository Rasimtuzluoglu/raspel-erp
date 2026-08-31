# ROADMAP — RasPel ERP

> Sürüm geçmişi için `CHANGELOG.md` dosyasına bakın. Bu belge planlanan çalışmaları takip eder.

## Kısa Vadeli (v1.9.0)

- [ ] Kalan servis/controller test kapsamını tamamlama (Ajanda, Bildirim, Crm, Tahsilat, TekrarlayanFatura, Yetki vb.)
- [ ] Büyük view dosyalarının (Dashboard, HizliSatis, Stoklar, Teklifler) alt bileşenlere ayrıştırılması
- [ ] CI'da JaCoCo/Vitest coverage eşik (gate) tanımlanması
- [ ] i18n eksik anahtar otomatik kontrol script'i

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
