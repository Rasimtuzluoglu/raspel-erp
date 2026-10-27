# İrsaliye Revizyonu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** İrsaliyeyi hazırlık → sevk → teslim akışıyla yöneten, stok ve teslimatı doğru zamanda işleyen, KPI + sekmeli ve toplu faturalamayı destekleyen yeni ekranı teslim etmek.

**Architecture:** Backend’de irsaliye durum geçişleri tek bir servis katmanında toplanır; stok yalnızca sevk geçişinde hareket eder, teslimat kaydı aynı transaction’da oluşturulur. Frontend, sunucu filtreli liste + sekmeler + detay drawer + sevk/teslim diyaloglarıyla donatılır.

**Tech Stack:** Java 21, Spring Boot 3.5, PostgreSQL/Flyway, H2 (test), JUnit 5/Mockito, Vue 3, PrimeVue 4, Pinia, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-27-irsaliye-revizyonu-design.md`

## Global Constraints

- Durumlar: `TASLAK | HAZIRLANDI | SEVK_EDILDI | TESLIM_EDILDI | IPTAL`; stok yalnızca `SEVK_EDILDI` geçişinde hareket eder.
- Mevcut `KESILDI` verisi migration ile `SEVK_EDILDI` olur; stok bakiyeleri değiştirilmez.
- Durum değişikliği yalnızca açık aksiyon uçlarından yapılır; `PUT /{id}` durum değiştiremez.
- Fatura iptal dışındaki her durumda kesilebilir; `TASLAK/HAZIRLANDI` fatura öncesi otomatik sevk edilir.
- Toplu faturada tüm kayıtlar aynı cariye ait olmalı; her irsaliye için ayrı fatura üretilir.
- Dönem kilidi (`donemService.kilitKontrol`) tüm geçişlerde korunur.
- i18n anahtarları hem `tr.json` hem `en.json`’a eklenir.
- Her faz sonunda: ESLint 0, `npm run i18n:check`, Vitest + kapsam, `mvn -B clean verify` (JaCoCo), Docker build/deploy.
- Migration sıradaki sürüm: **V169**.

## Review Focus

- **Eşzamanlı sevk:** Aynı irsaliye için iki sevk isteği stokı iki kez düşürmemeli; ikincisi durum/lock ile reddedilmeli.
- **Legacy KESILDI:** Eski istemci `KESILDI` gönderdiğinde TASLAK otomatik hazırlanıp sevk edilmeli; SEVK idempotent kalmalı.
- **Erken fatura + eksik şoför/adres:** Fatura isteği stok hareketi yapmadan anlaşılır hatayla dönmeli.
- **Farklı cari toplu fatura:** İstek tümüyle reddedilmeli, kısmi fatura oluşmamalı.
- **Canlı fatura varken sevk geri alma/iptal:** Stok iadesi yapılmamalı; fatura iptal edilmişse iade çalışmalı.

---

### Task 1: Durum modeli, V169 migration ve `hazirla` geçişi

**Files:**
- Create: `backend/src/main/resources/db/migration/V169__irsaliye_sevk_teslim.sql`
- Create: `backend/src/main/java/com/raspel/erp/entity/muhasebe/IrsaliyeDurumLog.java`
- Create: `backend/src/main/java/com/raspel/erp/repository/muhasebe/IrsaliyeDurumLogRepository.java`
- Create: `backend/src/main/java/com/raspel/erp/dto/muhasebe/IrsaliyeDurumLogDTO.java`
- Modify: `backend/.../entity/muhasebe/Irsaliye.java` (sevk/teslim alanları)
- Modify: `backend/.../dto/muhasebe/IrsaliyeDTO.java`
- Modify: `backend/.../repository/muhasebe/IrsaliyeRepository.java` (`findByIdForUpdate`)
- Modify: `backend/.../service/muhasebe/IrsaliyeService.java`
- Modify: `backend/.../controller/muhasebe/IrsaliyeController.java`
- Test: `backend/src/test/java/com/raspel/erp/service/muhasebe/IrsaliyeServiceTest.java`

**Interfaces:**
- Produces: `IrsaliyeDTO hazirla(Long id, Long kullaniciId)`; `List<IrsaliyeDurumLogDTO> gecmis(Long id)`.
- Produces entity alanları: `plaka, soforId, teslimAlan, teslimTarihi, teslimatAdresi, koliAdedi, desi, brutKg, tasiyiciFirma, sevkTarihi, teslimatId`.

- [ ] **Step 1: Failing testleri yaz**

`IrsaliyeServiceTest`’e ekle:
```java
@Test void olustur_taslakDurumundaVeIlkLoguYazar()
// when olustur(dto, 1L); then durum TASLAK, gecmis ilk kayit OLUSTUR
@Test void hazirla_taslakIrsaliyeyiHazirlarVeLogYazar()
// given TASLAK; when hazirla(1L, 7L); then durum HAZIRLANDI, log islem HAZIRLA
@Test void hazirla_taslakDegilseBusinessException()
// given KESILDI/SEVK; then assertThrows
@Test void gecmis_loglariTarihSirasiylaDoner()
```

- [ ] **Step 2: Testleri çalıştır, fail gör**

Run: `mvn -B test -Dtest=IrsaliyeServiceTest -q`
Expected: FAIL (`hazirla`/`gecmis` yok).

- [ ] **Step 3: Migration + entity/DTO/repo + servis metotlarını ekle**

Migration:
- `muhasebe.irsaliye` alanları (spec §4), `teslimat_id` FK, `irsaliye_durum_log` tablosu + indeks.
- `ticaret.teslimat.irsaliye_id` + kısmi unique indeks.
- `UPDATE muhasebe.irsaliye SET durum='SEVK_EDILDI', sevk_tarihi=COALESCE(sevk_tarihi, tarih::timestamp) WHERE durum='KESILDI';`

Servis:
- `hazirla`: `findByIdForUpdate`, tenant + dönem kontrolü, yalnız `TASLAK`, kaydet, `logYaz(i, eski, "HAZIRLANDI", kullaniciId, null)`.
- `olustur`: kayıt sonrası `logYaz(i, null, "TASLAK", null, "İrsaliye oluşturuldu")`.
- `logYaz` yardımcı metodu; `gecmis` DTO dönüşümü.
- `entityToDTO` yeni alanlar + `gecmis` (detayda dolu, listede boş).

Controller: `POST /{id}/hazirla`, `GET /{id}/gecmis`.

- [ ] **Step 4: Testleri çalıştır, pass gör**

Run: `mvn -B test -Dtest=IrsaliyeServiceTest,IrsaliyeControllerTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V169__irsaliye_sevk_teslim.sql backend/src/main/java/com/raspel/erp/{entity,repository,dto,service,controller}/muhasebe backend/src/test/java/com/raspel/erp/{service,controller}/muhasebe
git commit -m "feat(irsaliye): yeni durum modeli, V169 migration ve hazirlama gecisi"
```

---

### Task 2: `sevk` geçişi — stok, şoför/adres doğrulaması, otomatik teslimat

**Files:**
- Create: `backend/.../dto/muhasebe/IrsaliyeSevkIstek.java`
- Modify: `backend/.../service/muhasebe/IrsaliyeService.java`
- Modify: `backend/.../service/ticaret/TeslimatService.java`
- Modify: `backend/.../entity/ticaret/Teslimat.java` (`irsaliyeId`)
- Modify: `backend/.../controller/muhasebe/IrsaliyeController.java`
- Test: `IrsaliyeServiceTest`, `TeslimatServiceTest`

**Interfaces:**
- Produces: `IrsaliyeDTO sevk(Long id, IrsaliyeSevkIstek istek, Long kullaniciId)`.
- Produces: `TeslimatDTO irsaliyedenOlustur(Irsaliye irsaliye)`; `IrsaliyeSevkIstek(String plaka, Long soforId, String teslimatAdresi, String tasiyiciFirma, Integer koliAdedi, BigDecimal desi, BigDecimal brutKg)`.
- `Teslimat` alanı: `Long irsaliyeId`.

- [ ] **Step 1: Failing testler**

```java
@Test void sevk_hazirIrsaliyedeStokDuserVeTeslimatOlusur()
@Test void sevk_soforYoksaHataVerirVeStokDegismez()
@Test void sevk_ikinciKezCagrilirsaReddeder() // status SEVK
@Test void sevk_geriAlSonrasiTekrarSevkStokDusurur()
@Test void durumGuncelle_kesildiAliasTaslaktaHazirlaSevkYapar()
```

- [ ] **Step 2: Fail gör**

Run: `mvn -B test -Dtest=IrsaliyeServiceTest,TeslimatServiceTest -q`
Expected: FAIL.

- [ ] **Step 3: Uygula**

- `sevk`: lock + tenant + dönem; `HAZIRLANDI` değilse `BusinessException`; şoför zorunlu; adres boşsa cari adresi (`cariHesapRepository`), o da boşsa hata; sevk alanlarını yaz; **mevcut KESILDI stok mantığını** `stokHareketiIsle(i, yon)` yardımcısına taşı; `sevkTarihi=now`; teslimat oluştur; `teslimatId` bağla; log `SEVK`.
- `TeslimatService.irsaliyedenOlustur`: `gecerliSurucu` kontrolü, `faturaId` varsa bağla, `durum=YOLDA`, `irsaliyeId` unique; `findByIrsaliyeId` varsa mevcut döner (idempotent).
- `Kullanici` id'si `soforId`; `musteriAdi` cari adı.
- Legacy alias: `durumGuncelle("KESILDI")` → TASLAK ise `hazirla`+`sevk`, HAZIRLANDI ise `sevk`, SEVK ise no-op.

- [ ] **Step 4: Pass + regresyon**

Run: `mvn -B test -Dtest=IrsaliyeServiceTest,TeslimatServiceTest,IrsaliyeControllerTest -q`
Expected: PASS; mevcut irsaliye stok testleri de yeşil.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(irsaliye): sevk gecisi, stok hareketi ve otomatik teslimat"
```

---

### Task 3: `teslim`, `sevki-geri-al`, `iptal` ve fatura korumaları

**Files:**
- Create: `backend/.../dto/muhasebe/IrsaliyeTeslimIstek.java`
- Modify: `IrsaliyeService`, `TeslimatService`, `IrsaliyeController`
- Test: `IrsaliyeServiceTest`, `TeslimatServiceTest`

**Interfaces:**
- Produces: `IrsaliyeDTO teslim(Long id, IrsaliyeTeslimIstek istek, Long kullaniciId)`.
- Produces: `IrsaliyeDTO sevkiGeriAl(Long id, Long kullaniciId)`; `IrsaliyeDTO iptal(Long id, String aciklama, Long kullaniciId)`.
- Produces: `TeslimatService.irsaliyeTeslimEdildi(Long teslimatId, String teslimAlan, LocalDateTime tarih)`; `irsaliyeIptal(Long teslimatId)`.

- [ ] **Step 1: Failing testler**

```java
@Test void teslim_sevkEdilmisiTeslimEderVeTeslimatKapatir()
@Test void sevkiGeriAl_stokIadesiYaparVeTeslimatiIptalEder()
@Test void sevkiGeriAl_canliFaturaVarsaReddederVeStokDegismez()
@Test void iptal_sevkEdilmisIrsaliyedeStokIadesiYapar()
@Test void iptal_faturasiIptalEdilmisseIadeyeIzinVerir()
@Test void teslim_edilmemisIrsaliyeyiTeslimEdemez()
```

- [ ] **Step 2: Fail gör** — `mvn -B test -Dtest=IrsaliyeServiceTest,TeslimatServiceTest -q`

- [ ] **Step 3: Uygula**

- `teslim`: yalnız `SEVK_EDILDI`; `teslimAlan`, `teslimTarihi` (varsayılan now); durum `TESLIM_EDILDI`; teslimat `TESLIM_EDILDI`, teslim alan/tarih; log.
- `sevkiGeriAl`: yalnız `SEVK_EDILDI`; canlı fatura kontrolü (faturaRepository, IPTAL değilse hata); ters stok hareketi; teslimat `IPTAL`; durum `HAZIRLANDI`; log.
- `iptal`: `TASLAK/HAZIRLANDI` → stoksuz; `SEVK/TESLIM` → canlı fatura yoksa ters stok + teslimat iptal; log `IPTAL` + açıklama.
- Ters stok mantığını mevcut IPTAL dalından `stokHareketiGeriAl` yardımcısına taşı.

- [ ] **Step 4: Pass** — aynı komut; PASS.

- [ ] **Step 5: Commit** — `feat(irsaliye): teslim, sevk iadesi ve iptal korumalari`

---

### Task 4: Erken faturalama + toplu fatura

**Files:**
- Create: `backend/.../dto/muhasebe/TopluFaturaIstek.java`, `TopluFaturaSonucDTO.java`
- Modify: `IrsaliyeService.faturayaDonustur`, `topluFatura`
- Modify: `IrsaliyeController`
- Test: `IrsaliyeServiceTest`, `IrsaliyeControllerTest`

**Interfaces:**
- Produces: `TopluFaturaSonucDTO topluFatura(List<Long> ids, Long sirketId, Long kullaniciId)`.
- `TopluFaturaSonucDTO` alanları: `basarili` (`irsaliyeId, irsaliyeNo, faturaId, faturaNo`), `atlanan` (`irsaliyeId, irsaliyeNo, sebep`).

- [ ] **Step 1: Failing testler**

```java
@Test void faturayaDonustur_taslaktaOtomatikSevkEderVeStokDuser()
@Test void faturayaDonustur_soforYoksaHataVerirVeStokDegismez()
@Test void faturayaDonustur_canliFaturaVarsaReddeder()
@Test void topluFatura_farkliCariSecilirseHicFaturaOlusturmaz()
@Test void topluFatura_ayniCarideHerIrsaliyeIcinAyriFaturaUretir()
@Test void topluFatura_iptalVeFaturalilariAtlar()
```

- [ ] **Step 2: Fail** — `mvn -B test -Dtest=IrsaliyeServiceTest,IrsaliyeControllerTest -q`

- [ ] **Step 3: Uygula**

- `faturayaDonustur`: `IPTAL` red; canlı fatura red; `TASLAK/HAZIRLANDI` ise `hazirla`+`sevk` (aynı transaction); sonra mevcut fatura oluşturma; teslimat `faturaBaglaByIrsaliye`.
- `topluFatura`: boş liste red; kayıtları yükle; aynı `cariHesapId` değilse tüm istek red; her kayıt için `faturayaDonustur` benzeri işlem (kayıt bazında try/catch) ve özet; transaction ayrımı için `TransactionTemplate` veya ayrı bean kullan.
- Controller `POST /toplu-fatura`.

- [ ] **Step 4: Pass** — aynı komut.

- [ ] **Step 5: Commit** — `feat(irsaliye): erken fatura ve toplu fatura donusumu`

---

### Task 5: `/filtreli`, `/ozet`, Excel ve DTO zenginleştirme

**Files:**
- Create: `backend/.../dto/muhasebe/IrsaliyeOzetDTO.java`
- Modify: `IrsaliyeRepository` (JPQL filtre + count), `IrsaliyeService`, `IrsaliyeController`, `ExcelExportController`
- Test: `IrsaliyeServiceTest`, `IrsaliyeControllerTest`, `ExcelExportControllerTest`

**Interfaces:**
- Produces: `Page<IrsaliyeDTO> filtreli(Long sirketId, String q, String durum, String tur, Long cariId, LocalDate bas, LocalDate bit, Boolean faturali, Pageable pageable)`.
- Produces: `IrsaliyeOzetDTO ozet(Long sirketId, LocalDate bas, LocalDate bit)`.
- Endpoint: `GET /api/irsaliyeler/filtreli`, `GET /api/irsaliyeler/ozet`, `GET /api/exports/irsaliyeler`.

- [ ] **Step 1: Failing testler**

```java
@Test void filtreli_parametreleriIletirVeSayfaliDoner()
@Test void ozet_durumSayilariniVeFaturasizlariDoner()
@Test void irsaliyeler_excelDoner()
```

- [ ] **Step 2: Fail**

- [ ] **Step 3: Uygula**

- Repository JPQL: `q` → irsaliyeNo/cari adı alt sorgusu; tarih aralığı; `faturali` null/true/false; sıralama `tarih desc`.
- `ozet`: `TASLAK`, `HAZIRLANDI`, `SEVK_EDILDI`, `IPTAL`, `TESLIM_EDILDI` (bu ay), `faturasiz` (faturaId null ve durum != IPTAL).
- Excel kolonları spec §5.
- DTO’ya `cariHesapAdi`, `siparisNo`, `faturaNumarasi`, `teslimatDurum` alanlarını doldur.

- [ ] **Step 4: Pass** — `mvn -B test -Dtest=IrsaliyeServiceTest,IrsaliyeControllerTest,ExcelExportControllerTest -q`

- [ ] **Step 5: Commit** — `feat(irsaliye): filtreli liste, ozet ve excel`

---

### Task 6: Frontend API + KPI/sekme/sunucu liste iskeleti

**Files:**
- Modify: `frontend/src/api/modules/ticaret.js`, `frontend/src/api/modules/dosya.js`
- Rewrite: `frontend/src/views/Irsaliyeler.vue` (temel iskelet)
- Test: `frontend/src/views/__tests__/Irsaliyeler.spec.js`, `frontend/src/__tests__/irsaliye-faz1.spec.js`

**Interfaces:**
- `irsaliyeAPI`: `filtreli(params)`, `ozet(params)`, `update(id,data)`, `hazirla(id)`, `sevk(id,data)`, `teslim(id,data)`, `sevkiGeriAl(id)`, `iptal(id,aciklama)`, `topluFatura(ids)`.
- `excelAPI.irsaliyeler(params)`.

- [ ] **Step 1: Failing test** — PageHeader, 4 KPI, sekmeler (Tümü/Hazırlanan/Sevk/Teslim/Faturalanan), varsayılan Hazırlanan, `filtreli` çağrısı, hafıza.
- [ ] **Step 2: Fail** — `npx vitest run src/views/__tests__/Irsaliyeler.spec.js`
- [ ] **Step 3: Uygula** — PageHeader, KPI, TabView (hafıza `raspel_irsaliye_tab`), filtreler (`raspel_irsaliye_filtre`), AppDataTable lazy + mobil kart, skeleton, `aria-live`.
- [ ] **Step 4: Pass** — Vitest + `npm run lint` + `npm run i18n:check`.
- [ ] **Step 5: Commit** — `feat(irsaliye): KPI, sekmeler ve sunucu filtreli liste`

---

### Task 7: Yeni/düzenle + sevk/teslim/iptal diyalogları + detay drawer

**Files:**
- Create: `frontend/src/components/IrsaliyeDetayDrawer.vue`
- Modify: `Irsaliyeler.vue`
- Test: `frontend/src/components/__tests__/IrsaliyeDetayDrawer.spec.js`, `Irsaliyeler.spec.js`, `irsaliye-faz2.spec.js`

**Interfaces:**
- Drawer props: `visible`, `kayit`, `gecmis`, `gecmisYukleniyor`; emits `duzenle`, `hazirla`, `sevk`, `teslim`, `sevki-geri-al`, `iptal`, `faturaya-donustur`.
- Sevk formu: `plaka, soforId, tasiyiciFirma, teslimatAdresi, koliAdedi, desi, brutKg`; şoför listesi `siparisTakipAPI.soforler()`.

- [ ] **Step 1: Failing test** — drawer durum/aksiyonları, sevk formu zorunlu alanları, düzenleme yalnız TASLAK/HAZIRLANDI.
- [ ] **Step 2: Fail**
- [ ] **Step 3: Uygula** — drawer + diyaloglar + `⋮` menü + onay/toast + PDF; responsive breakpoints.
- [ ] **Step 4: Pass** — ilgili Vitest dosyaları.
- [ ] **Step 5: Commit** — `feat(irsaliye): detay drawer, duzenleme ve sevk/teslim aksiyonlari`

---

### Task 8: Toplu fatura UI + teslimat bağlantıları + mobil/a11y

**Files:**
- Modify: `Irsaliyeler.vue`, `IrsaliyeDetayDrawer.vue`
- Test: `Irsaliyeler.spec.js`, `irsaliye-faz3.spec.js`

- [ ] **Step 1: Failing test** — seçim çubuğu, farklı cari seçiminde buton disabled, sonuç modalı, `faturaId`/`teslimatId` linkleri, mobil kart, `aria-pressed`/`aria-live`.
- [ ] **Step 2: Fail**
- [ ] **Step 3: Uygula** — seçim kolonu + toplu işlem + sonuç modalı; fatura/sipariş/teslimat bağlantıları (`/faturalar/:id`, teslimatlar); mobil kart; a11y.
- [ ] **Step 4: Pass** — Vitest + lint + i18n.
- [ ] **Step 5: Commit** — `feat(irsaliye): toplu fatura, teslimat baglantilari ve mobil uyum`

---

### Task 9: Kapanış — tam doğrulama ve dağıtım

- [ ] **Step 1:** `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] **Step 2:** `cd backend && mvn -B clean verify`
- [ ] **Step 3:** `docker compose build backend frontend && docker compose up -d backend frontend`
- [ ] **Step 4:** Flyway `V169` success; yeni kolon/tablolar; `/api/irsaliyeler/filtreli`, `/ozet`, `/exports/irsaliyeler` 401/rota kontrolü; health.
- [ ] **Step 5:** `git add -A && git commit -m "feat(irsaliye): revizyon (Faz 1-5)" && git push origin main`

---

## Self-Review Notları

- Spec §2–§6 → Task 1–5; §7 → Task 6–8; §9–§10 → Task 9.
- Tipler: `hazirla/sevk/teslim/sevkiGeriAl/iptal` tümü `IrsaliyeDTO`; `topluFatura` `TopluFaturaSonucDTO`; `irsaliyedenOlustur` `TeslimatDTO`.
- Review Focus maddeleri Task 1–4 testlerine bağlandı.
- Kapsam dışı özellik (Kanban, takvim, kısmi sevk) plana alınmadı.
