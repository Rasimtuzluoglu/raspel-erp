# Anasayfa Revizyonu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Anasayfayı premium komuta merkezine dönüştür: bento KPI, görev merkezi, içgörüler, hedefler, drill-down grafikler, canlı akış, kişiselleştirilebilir düzen.
**Spec:** `docs/superpowers/specs/2026-10-28-anasayfa-revizyonu-design.md`

## Global Constraints

- Migration **V174** (V173 korunur), idempotent.
- Mevcut `/api/dashboard` payload'ı **değişmez** (geriye uyum).
- Parçalı uçlar: `/ozet`, `/gorevler`, `/grafikler`, `/akis`, `/icgoruler`, `/tercihler`.
- Rol: ADMIN/MUHASEBE/USER; görev ve tercih backend'de filtrelenir.
- Chart.js korunur; yeni grafik kütüphanesi eklenmez.
- Ctrl+K mevcut `QuickSearch` üzerinden çalışır; yeni palet yazılmaz.
- i18n tr/en simetrik; her faz sonunda ilgili kapılar.

## Review Focus

- Özet uçunun hızı (ağır sorgular içermemeli) ve KPI doğruluğu.
- Görev sayılarının mevcut modül durumlarıyla birebir uyuşması.
- Akış birleştirmesinde tenant ve limit.
- Tercih JSON doğrulama / boyut / kullanıcı izolasyonu.
- Eski `/api/dashboard` sözleşmesinin bozulmaması.
- Sürükle-bırak + klavye + mobil davranışı.

---

### Task 1: V174 + tercih API + özet/görev uçları

**Files:** `V174__dashboard_tercih.sql`, entity/repo/DTO, `DashboardService`/`DashboardController`, testler.

- [ ] `sistem.dashboard_tercih` + unique/index.
- [ ] `DashboardTercih` entity/repo; `DashboardTercihDTO`.
- [ ] `GET/PUT /api/dashboard/tercihler` (kullanıcı izolasyonu, JSON doğrulama, 20 KB sınır).
- [ ] `DashboardOzetDTO` + `GET /ozet`: hero, bugün, 4 KPI (likidite/net cari/ciro/kâr), stok, görev sayaçları.
- [ ] `DashboardGorevDTO` + `GET /gorevler`: 8 tür, öncelik sırası, rol filtresi.
- [ ] Cache: `dashboard` cache'i; tercih cache'siz.
- [ ] Testler: migration, tercih, KPI, görev sayıları/rol, cache anahtarı, tenant.
- [ ] Pass: `mvn -B test -Dtest="DashboardServiceTest,DashboardControllerTest,DashboardTercihTest" -q`.
- [ ] Commit: `feat(dashboard): V174 tercih, ozet ve gorev uclari`.

### Task 2: Grafik, akış, içgörü uçları

**Files:** `DashboardGrafikDTO`, `DashboardAkisDTO`, `DashboardIcgoruDTO`, servis/controller, testler.

- [ ] `GET /grafikler`: mevcut hesaplar (12 ay, 7/30 gün, ürünler, kategoriler, yaşlandırma, borçlu/alacaklı, kasa-banka, hedef).
- [ ] `GET /akis?limit=20`: onay kararı + hareket + fatura + sipariş + stok hareketi birleşimi; tarih desc; tenant.
- [ ] `GET /icgoruler`: kural tabanlı 3 içgörü (hedef, geciken alacak, kritik stok, likidite trendi); seviye.
- [ ] Testler: boş veri, sıralama, limit, tenant, içgörü eşikleri.
- [ ] Pass + commit: `feat(dashboard): grafik, akis ve icgoru uclari`.

### Task 3: Frontend kabuk + bölümler

**Files:** `api/modules/sistem.js`, `stores/dashboardStore.js`, `views/Dashboard.vue`, `components/dashboard/*`, `App.vue` (arama olayı), i18n, testler.

- [ ] API istemcileri: ozet, gorevler, grafikler, akis, icgoruler, tercihler (get/put).
- [ ] Store: parçalı yükleme durumları (`ozetYukleniyor`, `grafiklerYukleniyor`...), hata/boş durumlar.
- [ ] Hero: karşılama, şirket, tarih-saat, canlı rozet, bugün şeridi, döviz ticker, ara butonu, yenile, düzenle.
- [ ] Bento KPI: likidite (sparkline), net cari, ciro (hedef halkası), kâr (marj).
- [ ] Görev merkezi: öncelik sıralı kartlar, adet/tutar, drill-down, boş durum.
- [ ] İçgörü kartı: 3 madde, seviye rengi, bağlantı.
- [ ] Hedefler: ciro halkası, kâr çubuğu, 30/60/90 projeksiyon.
- [ ] Grafik kartları (Chart.js): nakit akışı, gelir/gider, kategori donut, ürün bar + “Tüm analizler”.
- [ ] Canlı akış: zaman çizelgesi, tür ikonu, göreli zaman, 60 sn sessiz yenileme.
- [ ] Hızlı işlemler + `window` olayıyla QuickSearch tetikleme (`App.vue` dinleyicisi).
- [ ] Skeleton’lar bölüm bazlı; `aria-live`; mobil kırılım.
- [ ] Testler: bölüm render, progresif yükleme, drill-down, boş durum, a11y.
- [ ] Pass: eslint + ilgili vitest + i18n + build.
- [ ] Commit: `feat(dashboard): premium kabuk, bento KPI, gorev, icgoru, grafik ve akis`.

### Task 4: Kişiselleştirme + rol varsayılanları

**Files:** `components/dashboard/DashboardWidget.vue`, `DashboardDuzenPaneli.vue`, `views/Dashboard.vue`, `stores/dashboardStore.js`, testler.

- [ ] Widget registry + rol bazlı varsayılan sıra/boyut/görünürlük.
- [ ] Düzenle modu: native sürükle-bırak, yukarı/aşağı (klavye), boyut 1x/2x, gizle.
- [ ] Kaydet: `PUT /tercihler`; hata → localStorage; “rol varsayılanına sıfırla”.
- [ ] Yükleme: backend tercihi → yoksa rol varsayılanı → yoksa localStorage.
- [ ] Testler: sıralama, boyut, gizle, kaydetme/fallback, sıfırlama, rol varsayılanları, klavye.
- [ ] Pass + commit: `feat(dashboard): surukle-birak kisiselleştirme ve rol varsayilanlari`.

### Task 5: Kapanış

- [ ] `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] `mvn -B clean verify`
- [ ] `docker compose build backend frontend && docker compose up -d backend frontend`
- [ ] V174 success; uçlar 401; health; canlı bundle.
- [ ] `git push origin main`; rapor.

## Self-Review

- Spec §2 → Task 3/4; §3 → Task 1/2; §4.3 → Task 4; §4.4/4.5 → Task 3; §5 → Task 5.
- Review Focus maddeleri Task 1–2 testlerine bağlandı.
