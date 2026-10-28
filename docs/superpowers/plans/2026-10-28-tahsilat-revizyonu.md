# Tahsilat Revizyonu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tahsilatı işlem bazında izleyen, gerekçeli geri alınabilen, hedef–performans ölçen ve sunucu filtreli/Excel destekli yeni ekranı teslim etmek.

**Architecture:** Yeni `tahsilat_islem` kohortu tüm Hareket satırlarını birleştirir; iptal mevcut `hareketIptal` ile stok dışı finansal etkileri tersine çevirir. Frontend KPI + 3 sekme + drawer + toplu e-posta ile yenilenir.

**Tech Stack:** Java 21, Spring Boot 3.5, PostgreSQL/Flyway, JUnit 5/Mockito, Vue 3, PrimeVue 4, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-28-tahsilat-revizyonu-design.md`

## Global Constraints

- Migration sürümü **V170**; mevcut `V169` korunur.
- İşlem iptali yalnız ADMIN/MUHASEBE; gerekçe zorunlu; dönem kilidi korunur.
- Legacy her TAHSILAT hareketi tek işlem olarak backfill edilir.
- Hedef yalnız şirket + ay; para birimi TRY.
- Mevcut tahsilat giriş davranışı (vade sırası, fazla/avans, kasa/banka, taksit, idempotency, muhasebe) korunur.
- i18n anahtarları tr/en simetrik.
- Her faz sonunda ESLint 0, i18n temiz, Vitest, `mvn clean verify`, Docker.

## Review Focus

- **Legacy backfill:** Aynı hareketin iki işleme bağlanmaması; unique `legacy_hareket_id`.
- **İptal bütünlüğü:** Fatura/cari/kasa/banka tüm etkilerin tek transaction’da dönmesi; ikinci iptalin engellenmesi.
- **Dönem kilidi:** Kilitli aydaki işlemin iptal edilememesi.
- **Hedefsiz performans:** Hedef yoksa gerçekleşme `null`, sıfır bölme yok.
- **Toplu e-posta:** E-postasız carilerin atlanıp diğerlerine gönderilmesi, kısmi hata raporu.

---

### Task 1: V170 + işlem/hedef modeli + tahsilat girişine bağlama

**Files:**
- Create: `backend/src/main/resources/db/migration/V170__tahsilat_islem_hedef.sql`
- Create: `entity/finans/TahsilatIslem.java`, `TahsilatHedef.java`
- Create: `repository/finans/TahsilatIslemRepository.java`, `TahsilatHedefRepository.java`
- Create: `dto/finans/TahsilatIslemDTO.java`
- Modify: `entity/finans/Hareket.java` (`tahsilatIslemId`), `HareketDTO`
- Modify: `service/finans/TahsilatService.java` (işlem oluştur, hareketlere bağla)
- Test: `service/TahsilatServiceTest.java`, `repository/TahsilatIslemRepositoryTest.java`

**Interfaces:**
- Produces: `TahsilatIslem yeniIslem(CariHesap cari, BigDecimal tutar, ...)`; `Long tahsilatIslemId` sonuç map’inde.
- Entity: `id, sirketId, cariId, tarih, tutar, odemeYontemi, kasaId, bankaId, durum, iptalGerekce, iptalEdenKullaniciId, iptalTarihi, olusturanKullaniciId, legacyHareketId`.

- [ ] **Step 1: Failing test** — `tahsilatGir_tekIslemOlustururVeTumHareketleriBaglar` (2 fatura → 2 hareket aynı `tahsilatIslemId`); `legacy_backfill` repository testi.
- [ ] **Step 2: Fail** — `mvn -B test -Dtest=TahsilatServiceTest,TahsilatIslemRepositoryTest -q`
- [ ] **Step 3: Uygula** — migration (tablolar, Hareket kolonu, legacy backfill), entity/repo, `TahsilatService.tahsilatGir` içinde işlem kaydı; her `hareketOlustur` DTO’suna `tahsilatIslemId`.
- [ ] **Step 4: Pass** — aynı komut.
- [ ] **Step 5: Commit** — `feat(tahsilat): islem modeli, V170 ve tahsilat baglantisi`

---

### Task 2: İşlem iptali + geçmiş filtreleri + Excel

**Files:**
- Create: `dto/finans/TahsilatIptalIstek.java`
- Modify: `TahsilatService`, `TahsilatController`, `HareketRepository`, `ExcelExportController`
- Test: `TahsilatServiceTest`, `controller/TahsilatControllerTest`, `ExcelExportControllerTest`

**Interfaces:**
- `TahsilatIslemDTO iptalEt(Long islemId, String gerekce, Long kullaniciId)`; `Page<TahsilatIslemDTO> gecmisFiltreli(...)`; `byte[]` Excel ucu.

- [ ] **Step 1: Failing test**
```java
@Test void iptal_cariFaturaKasaBankaBakiyeleriniGeriAlir()
@Test void iptal_ikinciKezReddeder()
@Test void iptal_donemKilitliyseReddeder()
@Test void gecmisFiltreleriVeDagilimlariDoner()
```
- [ ] **Step 2: Fail**
- [ ] **Step 3: Uygula** — `POST /islem/{id}/iptal`; işlemdeki hareketler için `hareketService.hareketIptal`; durum/gerekçe/eden; dönem kilidi; `GET /gecmis` parametreleri + DTO; `/exports/tahsilat-gecmis`.
- [ ] **Step 4: Pass**
- [ ] **Step 5: Commit** — `feat(tahsilat): islem iptali, filtreli gecmis ve excel`

---

### Task 3: Hedef/performans + yaşlandırma filtreleri + toplu hatırlatma

**Files:**
- Create: `dto/finans/TahsilatPerformansDTO.java`, `TahsilatHedefDTO.java`, `TopluHatirlatmaIstek/Sonuc.java`
- Modify: `TahsilatService`, `TahsilatController`, `ExcelExportController`
- Test: `TahsilatServiceTest`, `TahsilatControllerTest`

- [ ] **Step 1: Failing test**
```java
@Test void performans_hedefeGoreGerceklesmeHesaplar()
@Test void performans_hedefYoksaNullDoner()
@Test void hedef_upsertEder()
@Test void ozetFiltreleriUygular()
@Test void topluHatirlatma_epostasizlariAtlar()
```
- [ ] **Step 2: Fail**
- [ ] **Step 3: Uygula** — `GET /performans`, `PUT /hedef`, `GET /` filtre parametreleri (`q, aralik, sadeceGecikmis, minTutar`), `POST /toplu-hatirlat`; Excel yaşlandırma.
- [ ] **Step 4: Pass**
- [ ] **Step 5: Commit** — `feat(tahsilat): hedef performans, yaslandirma filtreleri ve toplu hatirlatma`

---

### Task 4: Frontend revizyonu

**Files:**
- Modify: `frontend/src/api/modules/finans.js` (tahsilatAPI), `dosya.js` (excel)
- Rewrite: `frontend/src/views/Tahsilat.vue`
- Create: `frontend/src/components/TahsilatCariDrawer.vue`
- Test: `views/__tests__/Tahsilat.spec.js`, `components/__tests__/TahsilatCariDrawer.spec.js`, `__tests__/tahsilat-faz1.spec.js`, `tahsilat-faz2.spec.js`

- [ ] **Step 1: Failing test** — 5 KPI, 3 sekme, sunucu parametreleri, hafıza, iptal, toplu e-posta, performans hedefi.
- [ ] **Step 2: Fail**
- [ ] **Step 3: Uygula** — PageHeader + KPI + TabView; yaşlandırma filtreleri/seçim; işlem geçmişi + iptal modalı; performans sekmesi/hedef düzenleme; drawer; toplu e-posta; skeleton, mobil, a11y, i18n.
- [ ] **Step 4: Pass** — `npx vitest run` ilgili dosyalar + `npm run lint` + `npm run i18n:check`.
- [ ] **Step 5: Commit** — `feat(tahsilat): KPI, sekmeler, drawer, iptal ve hedef performans UI`

---

### Task 5: Kapanış

- [ ] `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] `mvn -B clean verify`
- [ ] `docker compose build backend frontend && docker compose up -d backend frontend`
- [ ] V170 success; işlem/hedef tabloları; yeni uçlar 401; health; bundle.
- [ ] `git add -A && git commit -m "feat(tahsilat): revizyon (Faz 1-5)" && git push origin main`

---

## Self-Review

- Spec §2–§5 → Task 1–2; §6–§7 → Task 3–4; §10 → Task 5.
- Tipler: `TahsilatIslemDTO`, `TahsilatPerformansDTO`, `iptalEt`, `gecmisFiltreli`, `hedefKaydet`, `topluHatirlat`.
- Review Focus testleri Task 1–3’e bağlandı.
- Kapsam dışı (personel hedefi, otomatik şablon) plana alınmadı.
