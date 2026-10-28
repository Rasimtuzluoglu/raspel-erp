# Onaylar Revizyonu Implementation Plan

**Goal:** Birleşik onay API’si + karar geçmişi + toplu karar + ayrı onay ayarları ekranı.
**Spec:** `docs/superpowers/specs/2026-10-28-onaylar-revizyonu-design.md`

## Global Constraints
- Migration V172; mevcut modül uçları korunur.
- Yetki ADMIN/MUHASEBE; onaylayan JWT/context’ten.
- RED gerekçesi zorunlu (min 3); toplu karar aynı tür.
- Yalnız yeni kararlar loglanır; geriye dönük backfill yok.

## Task 1 — V172 + karar modeli + birleşik API
- [ ] `sistem.onay_karar` tablosu/entity/repo/DTO.
- [ ] `OnayService`: ozet, bekleyen (tür/arama/sayfalama), karar (4 tür eşleme), log.
- [ ] Controller: `/api/onaylar/ozet`, `/bekleyen`, `/karar`.
- [ ] Testler: tür eşleme, RED gerekçe, log, ozet/bekleyen; `mvn -B test -Dtest=OnayServiceTest,OnayControllerTest -q`.
- [ ] Commit.

## Task 2 — Toplu karar + geçmiş + Excel
- [ ] `/toplu` (aynı tür, kısmi sonuç), `/gecmis` filtreli sayfalama, `/exports/onaylar`.
- [ ] Testler + commit.

## Task 3 — Frontend Onaylar
- [ ] API: onaylarAPI (ozet, bekleyen, karar, toplu, gecmis); excelAPI.onaylar.
- [ ] `Onaylar.vue` yeniden: PageHeader, 5 KPI, 6 sekme, sunucu filtre/sayfalama, kart+liste, toplu seçim, ret modalı, drawer, geçmiş, mobil/a11y, hafıza.
- [ ] Testler: Onaylar.spec, OnayDetayDrawer.spec, kaynak spec’ler; eslint+i18n.
- [ ] Commit.

## Task 4 — Onay ayarları ayrı ekran
- [ ] `views/OnayAyarlari.vue` (mevcut ayar tablosu/aksiyonlar), `/onay-ayarlari` rotası + menü + i18n.
- [ ] Onaylar’dan ayar sekmesini kaldır.
- [ ] Testler + commit.

## Task 5 — Kapanış
- [ ] `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] `mvn -B clean verify`
- [ ] Docker build/up; V172 ve uçlar; push.
