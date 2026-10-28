# Kategoriler Revizyonu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Tüm kategori türlerini tek registry'de yönet; 2 seviyeli hiyerarşi, renk/ikon, arşiv, kullanım sayımı ve güvenli birleştirme ekle.
**Spec:** `docs/superpowers/specs/2026-10-28-kategoriler-revizyonu-design.md`

## Global Constraints

- Migration **V173** (V172 korunur), idempotent.
- Kapsamlar: `KASA_GELIR, KASA_GIDER, STOK, MASRAF, BUTCE`; `MASRAF/BUTCE` adları uppercase.
- Legacy string kolonlar korunur ve senkron tutulur.
- Hiyerarşi en fazla 2 seviye; aynı kapsam zorunlu.
- Yetki: CRUD `ADMIN/USER`, silme/arşiv/merge `ADMIN`.
- Personel masraf kodları sabit ve salt-okunur.
- i18n tr/en simetrik; her faz sonunda ilgili kapılar.

## Review Focus

- Backfill'de id korunumu ve sequence hizalaması.
- Rename/merge'in 5 modüldeki legacy string kolonlara yayılması.
- Kasa `kategori_id` yönlendirmesi ve tür uyumu.
- 2 seviye / döngü guard'ları.
- Kullanımdaki kategorinin silinememesi ve arşiv kuralları.
- Masraf/bütçe uppercase normalizasyonu ve bütçe–masraf eşleşmesi.

---

### Task 1: V173 + registry + backfill

**Files:** `V173__kategori_registry.sql`, `entity/sistem/Kategori.java`, `repository/sistem/KategoriRepository.java`, DTO'lar, `PostgresEntegrasyonTest`.

- [ ] `sistem.kategori` + unique/index + `kategori_id` kolonları (stok, masraf, bütçe, iskonto) + indeksler.
- [ ] `gelir_gider_kategori` id'leri korunarak taşınır, sequence `setval`, eski tablo düşürülür.
- [ ] Stok/masraf/bütçe/iskonto distinct adları backfill + `kategori_id` bağlama; `MASRAF/BUTCE` uppercase.
- [ ] Entity/repo/DTO (`KategoriDTO`, `KategoriAgacDTO`, `KategoriOzetDTO`, `KategoriKullanimDTO`, `KategoriBirlestirRequest`, `SistemKategoriDTO`, `KategoriKapsam` enum).
- [ ] `GelirGiderKategori` kaldırılır; `KasaHareket.kategori` → `Kategori`.
- [ ] Test: kapsam eşlemesi, id korunumu, distinct/uppercase, idempotency.
- [ ] Pass: `mvn -B test -Dtest="PostgresEntegrasyonTest,KasaServiceTest" -q`.
- [ ] Commit: `feat(kategoriler): V173 kategori registry ve backfill`.

### Task 2: Servis + API + testler

**Files:** `KategoriService`, `KategoriController`, repo sorguları, testler.

- [ ] `ozet`, `agac`, `secenekler`, `listele`, `getir`, `kullanim`.
- [ ] `olustur/guncelle`: ad tekrarı, kapsam, 2 seviye, palet/ikon, uppercase.
- [ ] `sil` (kullanım/alt kategori engeli), `arsivle/aktiflestir`, `birlestir`.
- [ ] Rename/merge'de legacy ad güncellemesi (bulk).
- [ ] `/api/kategoriler/**` uçları; yetki ayrımı; `sistem-kategorileri`.
- [ ] Testler: CRUD, guard'lar, kullanım, merge, tenant, yetki.
- [ ] Pass + commit: `feat(kategoriler): servis, agac, kullanim, arsiv ve birlestirme`.

### Task 3: Domain entegrasyonu

**Files:** `StokService`, `MasrafService`, `ButceService`, `IskontoMotoruService`, `KasaService`, repo'lar, testler.

- [ ] `KategoriService.resolve(sirketId, kapsam, ad)` get-or-create; domain kayıtlarında `kategoriId` + legacy ad.
- [ ] Kasa hareketi yalnız aktif ve tür uyumlu kategori kabul eder.
- [ ] Testler: get-or-create, senkron, pasif kategori reddi, kasa tür uyumu, mevcut davranışlar.
- [ ] Pass + commit: `feat(kategoriler): stok, masraf, butce, iskonto ve kasa entegrasyonu`.

### Task 4: Frontend

**Files:** `api/modules/stok.js`, `stores/kategoriStore.js`, `views/Kategoriler.vue`, `views/Kasa.vue`, `components/KategoriFormDialog.vue`, `components/KategoriKullanimDrawer.vue`, i18n, testler.

- [ ] API istemcileri: liste, agac, ozet, secenekler, kullanim, CRUD, arsiv, birlestir, sistem kategorileri.
- [ ] PageHeader + 4 KPI + kapsam sekmeleri (hafıza) + ağaç tablo + arama/filtre/sayfalama.
- [ ] Form dialog (üst kategori, palet, ikon, sıra, açıklama, aktif), merge dialog, kullanım drawer.
- [ ] Arşiv/aktifleştir, silme engeli, sıralama; sistem kategorileri salt-okunur.
- [ ] Kasa dropdown gruplu/aktif.
- [ ] Mobil/a11y, i18n; testler (Kategoriler, dialog, drawer, Kasa, faz1/faz2).
- [ ] Pass: eslint + i18n + vitest + build.
- [ ] Commit: `feat(kategoriler): kapsam sekmeleri, agac, form, kullanim ve birlestirme UI`.

### Task 5: Kapanış

- [ ] `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] `mvn -B clean verify`
- [ ] `docker compose build backend frontend && docker compose up -d backend frontend`
- [ ] V173 success; yeni uçlar 401; health; canlı bundle.
- [ ] `git push origin main`; rapor.

## Self-Review

- Spec §2 → Task 1; §3 → Task 2; §4 → Task 3; §5 → Task 4; §6 → Task 5.
- Review Focus maddeleri Task 1–3 testlerine bağlandı.
