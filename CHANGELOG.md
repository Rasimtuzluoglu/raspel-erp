# CHANGELOG — RasPel ERP

Tüm önemli değişiklikler ve sürüm notları bu dosyada takip edilir.

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
