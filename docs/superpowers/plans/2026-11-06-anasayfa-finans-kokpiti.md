# Anasayfa Finans Kokpiti Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the homepage as a dense, executive “Finance Desk” cockpit fed by one backend endpoint.

**Architecture:** A new `KokpitService` composes existing `DashboardService`/`RaporService` data into a single `GET /api/dashboard/kokpit` response. The frontend gets a dedicated `kokpitStore` and focused `components/dashboard/kokpit/*` sections; `views/Dashboard.vue` becomes orchestration only.

**Tech Stack:** Java 21 + Spring Boot 3.5, PostgreSQL, Redis cache, Vue 3 Composition API, Pinia, Chart.js, Vitest, Cypress.

**Spec:** `docs/superpowers/specs/2026-11-06-anasayfa-finans-kokpiti-design.md`

## Global Constraints

- Preserve teal brand color; dark and light themes must both work.
- Desktop-first, 1440–1920px; mobile must have no horizontal overflow at 390px.
- KPI/table numbers use tabular numerals; amounts are right aligned.
- Row height in dense mini tables is 32–34px; body text is 12px.
- Touch targets are at least 44px on mobile.
- No new runtime dependency is allowed.
- Backend must reuse `DashboardService`/`RaporService` logic; do not duplicate business calculations.
- All amounts are returned in TRY; currency conversion stays in the existing frontend doviz flow.
- Role-sensitive `karMarj` data is returned only to `ADMIN` and `MUHASEBE`.
- Cache name is `kokpit`; TTL is 30 seconds and is tenant-isolated through the existing `TenantCacheManager`.
- `prefers-reduced-motion` disables cockpit animations.
- Existing legacy `/api/dashboard` contract must not change.
- Frontend commands: `npm run lint`, `npm run i18n:check`, `npm run test`, `npm run build`.
- Backend command: `mvn -B test`.

## Review Focus

- **Zero previous period:** KPI `degisimYuzde` must be `null`, not division by zero — Task 1 test `degisimOncekiSifirsaNullDoner`.
- **Unauthorized USER role:** `karMarj` must be `null`, not partially populated — Task 1 test `userRolundeKarMarjNullDoner`.
- **Large tenant:** pipeline/aging must use SQL aggregation, never load all invoices into memory — Task 2 tests assert repository aggregate methods are called.
- **Redis outage:** cockpit request must still return data; cache errors are logged and skipped — Task 3 test `redisHatasiIstemeyiBozmaz`.
- **390px viewport:** no horizontal overflow and all dense tables enter summary mode — Task 11 Cypress check.
- **Empty company data:** metrics show `0`, skeleton/error states stay valid, no `NaN`/`Infinity` — Tasks 7/8 empty-state tests.

---

### Task 1: Backend `KokpitDTO` and `KokpitService` composition

**Files:**
- Create: `backend/src/main/java/com/raspel/erp/dto/sistem/KokpitDTO.java`
- Create: `backend/src/main/java/com/raspel/erp/service/sistem/KokpitService.java`
- Test: `backend/src/test/java/com/raspel/erp/service/sistem/KokpitServiceTest.java`

**Interfaces:**
- Consumes:
  - `DashboardService.ozetGetir(Long sirketId, Long kullaniciId, String kullaniciAd, String rol, String donem)`
  - `DashboardService.grafiklerGetir(Long sirketId)`
  - `DashboardService.karAnaliziGetir(Long sirketId, String rol, String donem)`
  - `DashboardService.icgorulerGetir(Long sirketId)`
  - `DashboardService.vadelerGetir(Long sirketId, LocalDate baslangic, LocalDate bitis)`
  - `RaporService.yaslandirmaRaporu(Long sirketId, LocalDate referansTarih)`
  - `RaporService.nakitAkisiProjeksiyonu(int gunSayisi, Long sirketId)`
  - New repository methods from Task 2 are not used yet; this task mocks pipeline data.
- Produces:
  - `public KokpitDTO kokpitGetir(Long sirketId, String rol, String donem)`
  - `KokpitDTO` nested contracts:
    - `List<KpiDTO> getOzet()`
    - `List<KritikDurumDTO> getKritikDurum()`
    - `NakitDTO getNakit()`
    - `AlacakVerecekDTO getAlacakVerecek()`
    - `KarMarjDTO getKarMarj()`
    - `FaturaSiparisDTO getFaturaSiparis()`
  - `KpiDTO`: `anahtar`, `etiket`, `deger`, `onceki`, `degisimYuzde`, `sparkline`, `kritik`, `hedef`
  - `HedefDTO`: `route`, `query`
  - `KritikDurumDTO`: `anahtar`, `etiket`, `seviye`, `tutar`, `adet`, `hedef`
  - `NakitDTO`: `kasa`, `banka`, `toplam`, `trendYuzde`, `sparkline`, `gunlukAkis`, `projeksiyon`
  - `AlacakVerecekDTO`: `alacakToplam`, `verecekToplam`, `net`, `yaslanma`, `borclular`, `tedarikciler`, `tahsilatOrani`, `hedef`
  - `KarMarjDTO`: `ciro`, `maliyet`, `kar`, `marj`, `trend`, `urunler`, `cariler`, `maliyetsizKalem`, `hedef`
  - `FaturaDurumDTO`: `durum`, `adet`, `tutar`
  - `FaturaSiparisDTO`: `durumlar`, `vadesiGecenTutar`, `vadesiGecenAdet`, `vadesiGecenFaturalar`, `tahsilatOrani`, `siparisDurumlari`, `hedef`

- [ ] **Step 1: Write failing `KokpitServiceTest`**

Create tests named:

```java
kokpitTumBolumleriDoldurur()
degisimOncekiSifirsaNullDoner()
userRolundeKarMarjNullDoner()
hedefRouteVeQueryTasiyor()
bosVerideSifirVeBosListelerDoner()
```

Assertions must include:
- `ozet` has exactly 8 KPI keys: `netNakit`, `alacak`, `verecek`, `netCalismaSermayesi`, `ciro`, `brutKar`, `marj`, `vadesiGecen`.
- For zero previous value, `degisimYuzde` is `null`.
- For role `USER`, `karMarj` is `null`.
- For role `ADMIN`, `karMarj` is populated.
- Empty mocked data yields zero amounts and empty lists, never null top-level sections.

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -B -q "-Dtest=KokpitServiceTest" test`
Expected: compile failure because `KokpitDTO` and `KokpitService` do not exist.

- [ ] **Step 3: Implement DTO and service**

Implementation notes:
- Build with Lombok `@Data`, `@Builder`, nested static classes, matching the interfaces above.
- `KokpitService` uses `@RequiredArgsConstructor` and `@Slf4j`.
- Map existing `DashboardOzetDTO`, `DashboardGrafikDTO`, `DashboardKarAnaliziDTO`, and `DashboardVadeDTO` values into the new nested DTOs.
- Limit lists: 8 debtor/creditor rows, 5 profitable products/accounts, 8 overdue invoices.
- KPI target examples:
  - `netNakit` → `/kasa`
  - `alacak` → `/cari-hesaplar?bakiyeDurum=ALACAK`
  - `verecek` → `/cari-hesaplar?bakiyeDurum=VERECEK`
  - `ciro` → `/faturalar`
  - `marj` → `/raporlar`
  - `vadesiGecen` → `/faturalar?vadesiGecen=true`
- Pipeline data in this task may be an empty list; Task 2 supplies repository aggregation.

- [ ] **Step 4: Run test and verify pass**

Run: `mvn -B -q "-Dtest=KokpitServiceTest" test`
Expected: all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/dto/sistem/KokpitDTO.java \
        backend/src/main/java/com/raspel/erp/service/sistem/KokpitService.java \
        backend/src/test/java/com/raspel/erp/service/sistem/KokpitServiceTest.java
git commit -m "feat(dashboard): kokpit sozlesmesi ve servis kompozisyonu"
```

---

### Task 2: SQL aggregates for fatura pipeline and critical counts

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/repository/ticaret/FaturaRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/sistem/KokpitService.java`
- Test: `backend/src/test/java/com/raspel/erp/service/sistem/KokpitServiceTest.java`
- Test: `backend/src/test/java/com/raspel/erp/repository/PostgresEntegrasyonTest.java`

**Interfaces:**
- Consumes: Task 1 `KokpitService`.
- Produces:
  - `List<Map<String, Object>> kokpitFaturaDurumDagilimi(Long sirketId)`
    - Map keys: `durum`, `adet`, `tutar`
  - `BigDecimal toplamKalanKesilmisSatis(Long sirketId)`
  - `BigDecimal toplamOdenenKesilmisSatis(Long sirketId)`

- [ ] **Step 1: Add failing tests**

In `KokpitServiceTest`, add:

```java
faturaPipelineRepositoryAggregateKullanir()
tahsilatOraniSifirCiroIleBolunmez()
```

Assertions:
- `kokpitFaturaDurumDagilimi` is called once.
- No method returning `List<Fatura>` is used for pipeline aggregation.
- When total sales amount is zero, `tahsilatOrani` is zero, not `NaN`/exception.

In `PostgresEntegrasyonTest`, add `kokpitFaturaDurumDagilimiKesilmisFaturalariGruplar()` and verify grouped status rows for seeded tenant data.

- [ ] **Step 2: Run tests and verify failure**

Run:
`mvn -B -q "-Dtest=KokpitServiceTest,PostgresEntegrasyonTest" test`
Expected: new methods do not exist.

- [ ] **Step 3: Implement repository aggregates and service use**

Repository queries must:
- Group by `f.durum`
- Return `COUNT(f)` and `SUM(f.kalanTutar)`
- Exclude tenant-foreign rows via `f.sirketId = :sirketId`
- Use SQL aggregation; no full entity-list loading

`KokpitService` maps aggregate rows to `FaturaSiparisDTO.durumlar` and computes:
- `tahsilatOrani = odenen / (odenen + kalan) * 100`, returning `0` when denominator is zero.

- [ ] **Step 4: Run tests and verify pass**

Run:
`mvn -B -q "-Dtest=KokpitServiceTest,PostgresEntegrasyonTest" test`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/repository/ticaret/FaturaRepository.java \
        backend/src/main/java/com/raspel/erp/service/sistem/KokpitService.java \
        backend/src/test/java/com/raspel/erp/service/sistem/KokpitServiceTest.java \
        backend/src/test/java/com/raspel/erp/repository/PostgresEntegrasyonTest.java
git commit -m "feat(dashboard): fatura pipeline sql agregasyonlari"
```

---

### Task 3: `GET /api/dashboard/kokpit` endpoint, role guard, and 30s cache

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/controller/sistem/DashboardController.java`
- Modify: `backend/src/main/java/com/raspel/erp/config/RedisConfig.java`
- Test: `backend/src/test/java/com/raspel/erp/controller/sistem/DashboardControllerKokpitTest.java`
- Test: `backend/src/test/java/com/raspel/erp/config/RedisConfigCacheTtlTest.java`

**Interfaces:**
- Consumes: `KokpitService.kokpitGetir(Long sirketId, String rol, String donem)`.
- Produces: public endpoint contract:

```http
GET /api/dashboard/kokpit?donem=BU_AY
```

Accepted `donem` values: `BU_AY`, `GECEN_AY`, `BU_CEYREK`, `BU_YIL`. Invalid/blank values fall back to `BU_AY`.

- [ ] **Step 1: Write failing controller and cache tests**

Controller tests:
- `kokpitDonemParametresiyleCagirilir()`
- `kokpitVarsayilanDonemKullanir()`
- `kokpitTenantBaglamiZorunlu()`
- `redisHatasiIstemeyiBozmaz()` (unit-level service call still succeeds when cache manager throws)

Cache test:
- `kokpitCacheTtl30Saniyedir()`

- [ ] **Step 2: Run tests and verify failure**

Run:
`mvn -B -q "-Dtest=DashboardControllerKokpitTest,RedisConfigCacheTtlTest" test`
Expected: endpoint and cache config do not exist.

- [ ] **Step 3: Implement endpoint and cache**

Controller:
- Add `@GetMapping("/kokpit")`.
- Resolve `sirketId`, `rol`, and `donem` exactly like existing `/ozet`.
- Return `ResponseEntity.ok(kokpitService.kokpitGetir(...))`.

Service:
- Add `@Cacheable(cacheNames = "kokpit", key = "#sirketId + ':' + #rol + ':' + #donem")`.

Redis config:
- Add `.withCacheConfiguration("kokpit", defaultCacheConfig.entryTtl(Duration.ofSeconds(30)))` in both cache manager builder locations.

- [ ] **Step 4: Run tests and verify pass**

Run:
`mvn -B -q "-Dtest=DashboardControllerKokpitTest,RedisConfigCacheTtlTest,KokpitServiceTest" test`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/controller/sistem/DashboardController.java \
        backend/src/main/java/com/raspel/erp/config/RedisConfig.java \
        backend/src/main/java/com/raspel/erp/service/sistem/KokpitService.java \
        backend/src/test/java/com/raspel/erp/controller/sistem/DashboardControllerKokpitTest.java \
        backend/src/test/java/com/raspel/erp/config/RedisConfigCacheTtlTest.java
git commit -m "feat(dashboard): kokpit ucu ve 30 saniyelik tenant cache"
```

---

### Task 4: Frontend `kokpitAPI` and `kokpitStore`

**Files:**
- Modify: `frontend/src/api/modules/rapor.js`
- Create: `frontend/src/stores/kokpitStore.js`
- Test: `frontend/src/stores/__tests__/kokpitStore.spec.js`

**Interfaces:**
- Consumes backend `GET /api/dashboard/kokpit` and existing `dashboardAPI.tercihGetir()` / `dashboardAPI.tercihKaydet()`.
- Produces:
  - `dashboardAPI.kokpit(params)` → Axios GET
  - `useKokpitStore()` state:
    - `kokpit`, `donem`, `paraBirimi`, `yukleniyor`, `hata`, `sonGuncelleme`
    - `tercih`, `tercihYukleniyor`, `tercihHata`
    - `gorunurBolumler` (`['nakit', 'alacakVerecek', 'karMarj', 'faturaSiparis']`)
  - Actions:
    - `getKokpit(donem = donem.value)`
    - `donemDegistir(kod)`
    - `paraBirimiDegistir(kod)`
    - `tercihYukle()`
    - `tercihKaydet(duzen)`
    - `bolumSirala(sira)`
    - `bolumGorunurlukDegistir(anahtar, gorunur)`

- [ ] **Step 1: Write failing store tests**

Tests:
- `kokpitApiDonemleCagirir()`
- `donemDegistirYeniIstekAtar()`
- `apiHatasiHataMesajiBirakir()`
- `varsayilanBolumlerKurali()`
- `bolumGorunurlukDegistirTercihiGunceller()`

Assertions:
- `getKokpit('GECEN_AY')` calls API with `{ donem: 'GECEN_AY' }`.
- On API rejection, `hata` is set and `kokpit` remains previous value.
- `bolumGorunurlukDegistir('karMarj', false)` removes it from `gorunurBolumler`.

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/stores/__tests__/kokpitStore.spec.js`
Expected: store file does not exist.

- [ ] **Step 3: Implement API and store**

Use existing Pinia composition-store style from `dashboardStore.js`.
Do not duplicate `dashboardStore`; keep the new store focused on the cockpit contract.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/stores/__tests__/kokpitStore.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/modules/rapor.js \
        frontend/src/stores/kokpitStore.js \
        frontend/src/stores/__tests__/kokpitStore.spec.js
git commit -m "feat(dashboard): kokpit api ve pinia store"
```

---

### Task 5: Core cockpit visual primitives

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/FinansKarti.vue`
- Create: `frontend/src/components/dashboard/kokpit/MetrikKutu.vue`
- Create: `frontend/src/components/dashboard/kokpit/MiniTablo.vue`
- Create: `frontend/src/components/dashboard/kokpit/MikroGrafik.vue`
- Test: `frontend/src/components/__tests__/CekirdekGorseller.spec.js`

**Interfaces:**
- `FinansKarti` props: `baslik`, `altBaslik`, `yukleniyor`, `hata`; slots: `default`, `ust`, `alt`.
- `MetrikKutu` props: `etiket`, `deger`, `degisimYuzde`, `sparkline`, `kritik`, `to`, `dataTest`.
- `MiniTablo` props: `kolonlar`, `satirlar`, `bosMetin`, `yukleniyor`; emits `satir-sec`.
- `MikroGrafik` props: `tip` (`sparkline` | `bar` | `line`), `etiketler`, `seriler`.

- [ ] **Step 1: Write failing component tests**

Tests:
- `finansKartiBaslikVeHataGosterir()`
- `metrikKutuDegisimVeKritikTasiyor()`
- `miniTabloSatirSecmeOlayiYayar()`
- `mikroGrafikBosVerideCizmez()`
- `reducedMotionIleAnimasyonKapali()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/CekirdekGorseller.spec.js`
Expected: components do not exist.

- [ ] **Step 3: Implement primitives**

Use existing theme CSS variables from `app.css`.
Set dense typography/spacing exactly as in Global Constraints.
Chart rendering uses existing Chart.js imports and lazy patterns; no new dependency.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/CekirdekGorseller.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit \
        frontend/src/components/__tests__/CekirdekGorseller.spec.js
git commit -m "feat(dashboard): kokpit cekirdek gorsel bilesenleri"
```

---

### Task 6: Bucket visualizations

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/YaslanmaKovalari.vue`
- Create: `frontend/src/components/dashboard/kokpit/PipelineKovalari.vue`
- Test: `frontend/src/components/__tests__/KovaGorselleri.spec.js`

**Interfaces:**
- `YaslanmaKovalari` props: `kovalar: [{anahtar, etiket, tutar, adet}]`.
- `PipelineKovalari` props: `kovalar: [{anahtar, etiket, adet, tutar}]`.
- Both emit `kova-sec` with the selected bucket object.

- [ ] **Step 1: Write failing tests**

Tests:
- `yaslanmaKovalariEtiketVeTutarGosterir()`
- `pipelineKovalariSifirAdetteSolukGosterir()`
- `kovaSecimOlayiYayilir()`
- `bosKovaListesindeBosDurumGosterilir()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/KovaGorselleri.spec.js`
Expected: components do not exist.

- [ ] **Step 3: Implement bucket components**

Buckets are compact chips/segments, not large cards. Zero-count buckets stay visible but dimmed.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/KovaGorselleri.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/YaslanmaKovalari.vue \
        frontend/src/components/dashboard/kokpit/PipelineKovalari.vue \
        frontend/src/components/__tests__/KovaGorselleri.spec.js
git commit -m "feat(dashboard): yaslanma ve pipeline kova bilesenleri"
```

---

### Task 7: Global filter, KPI band, and critical strip

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/KokpitGlobalFiltre.vue`
- Create: `frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue`
- Create: `frontend/src/components/dashboard/kokpit/KritikDurumSeridi.vue`
- Test: `frontend/src/components/__tests__/OzetBandi.spec.js`

**Interfaces:**
- `KokpitGlobalFiltre` props: `donem`, `paraBirimi`, `canli`, `sonGuncelleme`; emits `update:donem`, `update:paraBirimi`, `excel`, `pdf`, `canli-degistir`.
- `FinansOzetBandi` props: `kpi`, `yukleniyor`.
- `KritikDurumSeridi` props: `ogeler`, `yukleniyor`.

- [ ] **Step 1: Write failing tests**

Tests:
- `globalFiltreDonemOlayiYayar()`
- `kpiBandiSekizMetrikGosterir()`
- `kpiKartiHedefLinkiTasiyor()`
- `kritikSeridSeviyeyeGoreRenkVeIkonTasiyor()`
- `kritikOgeTiklanincaHedefeGider()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/OzetBandi.spec.js`
Expected: components do not exist.

- [ ] **Step 3: Implement top cockpit band**

Use `MetrikKutu` and `FinansKarti`.
KPI cards are compact; first screen shows the full 8-card band without vertical scrolling on 1440px.
Critical severity uses both icon and text, not color alone.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/OzetBandi.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KokpitGlobalFiltre.vue \
        frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue \
        frontend/src/components/dashboard/kokpit/KritikDurumSeridi.vue \
        frontend/src/components/__tests__/OzetBandi.spec.js
git commit -m "feat(dashboard): finans ozet bandi ve kritik durum seridi"
```

---

### Task 8: Cash and receivables/payables sections

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/NakitBolumu.vue`
- Create: `frontend/src/components/dashboard/kokpit/AlacakVerecekBolumu.vue`
- Test: `frontend/src/components/__tests__/NakitAlacakBolumleri.spec.js`

**Interfaces:**
- `NakitBolumu` props: `veri`, `yukleniyor`, `hata`.
- `AlacakVerecekBolumu` props: `veri`, `yukleniyor`, `hata`.
- Both emit `detay` with a target object `{ route, query }`.

- [ ] **Step 1: Write failing tests**

Tests:
- `nakitBolumuDagilimVeProjeksiyonGosterir()`
- `nakitBolumuBosVerideSifirGosterir()`
- `alacakBolumuYaslanmaKovalariniGosterir()`
- `alacakBolumuBorcluTedarikciTablolariniGosterir()`
- `bolumDetayOlayiHedefTasiyor()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/NakitAlacakBolumleri.spec.js`
Expected: components do not exist.

- [ ] **Step 3: Implement sections**

Use `FinansKarti`, `MikroGrafik`, `MiniTablo`, `YaslanmaKovalari`.
Show maximum 8 debtor/creditor rows.
Amounts use existing currency formatting utilities and the selected currency from the global filter.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/NakitAlacakBolumleri.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/NakitBolumu.vue \
        frontend/src/components/dashboard/kokpit/AlacakVerecekBolumu.vue \
        frontend/src/components/__tests__/NakitAlacakBolumleri.spec.js
git commit -m "feat(dashboard): nakit ve alacak verecek bolumleri"
```

---

### Task 9: Profit/margin and invoice/order sections

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue`
- Create: `frontend/src/components/dashboard/kokpit/FaturaSiparisBolumu.vue`
- Test: `frontend/src/components/__tests__/KarFaturaBolumleri.spec.js`

**Interfaces:**
- `KarMarjBolumu` props: `veri`, `yukleniyor`, `hata`; emits `detay`.
- `FaturaSiparisBolumu` props: `veri`, `yukleniyor`, `hata`; emits `detay`.
- Unauthorized/missing `veri` renders a permission-safe empty state.

- [ ] **Step 1: Write failing tests**

Tests:
- `karBolumuTrendVeIlkBesGosterir()`
- `karBolumuYetkisizRoldeBosDurumGosterir()`
- `maliyetsizKalemUyarisiGosterilir()`
- `faturaBolumuPipelineKovalariniGosterir()`
- `faturaBolumuVadesiGecenIlkSekizGosterir()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/KarFaturaBolumleri.spec.js`
Expected: components do not exist.

- [ ] **Step 3: Implement sections**

Use `PipelineKovalari` and `MiniTablo`.
Do not display raw backend role strings; use i18n labels.
Overdue invoice rows carry invoice number, customer, due date, and remaining amount.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/KarFaturaBolumleri.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue \
        frontend/src/components/dashboard/kokpit/FaturaSiparisBolumu.vue \
        frontend/src/components/__tests__/KarFaturaBolumleri.spec.js
git commit -m "feat(dashboard): kar marj ve fatura siparis bolumleri"
```

---

### Task 10: New `Dashboard.vue` orchestration and drill-down

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`
- Test: `frontend/src/views/__tests__/Dashboard.spec.js`
- Test: `frontend/src/components/__tests__/DashboardKokpit.spec.js`

**Interfaces:**
- Consumes:
  - `useKokpitStore`
  - all cockpit section components from Tasks 7–9
- Produces:
  - `/` renders the new cockpit.
  - Section order comes from `gorunurBolumler`.
  - Filter changes trigger `getKokpit`.
  - Section `detay` events call `router.push({ path, query })`.

- [ ] **Step 1: Write failing integration tests**

Tests:
- `dashboardKokpitStoreIleYuklenir()`
- `donemDegisinceYeniIstekGider()`
- `bolumDetayRouterPushCagirir()`
- `bolumHataTekBolumuBozmaz()`
- `yetkisizKarBolumuGizlenir()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/views/__tests__/Dashboard.spec.js src/components/__tests__/DashboardKokpit.spec.js`
Expected: current Dashboard still uses old store/sections.

- [ ] **Step 3: Rebuild Dashboard orchestration**

`Dashboard.vue` should:
- Load cockpit and preference in parallel on mount.
- Render global filter, KPI band, critical strip, then visible sections in stored order.
- Keep legacy dashboard behavior out of the new view unless another screen imports it.
- Preserve live status indicator and export actions.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/views/__tests__/Dashboard.spec.js src/components/__tests__/DashboardKokpit.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Dashboard.vue \
        frontend/src/views/__tests__/Dashboard.spec.js \
        frontend/src/components/__tests__/DashboardKokpit.spec.js
git commit -m "feat(dashboard): yeni kokpit orkestrasyonu ve detay akisi"
```

---

### Task 11: Customization panel

**Files:**
- Create: `frontend/src/components/dashboard/kokpit/KokpitOzellestirme.vue`
- Modify: `frontend/src/views/Dashboard.vue`
- Test: `frontend/src/components/__tests__/KokpitOzellestirme.spec.js`

**Interfaces:**
- Props: `bolumler`, `gorunurBolumler`, `kaydediliyor`.
- Emits:
  - `kapat`
  - `gorunurluk-degistir` `(anahtar, gorunur)`
  - `sira-degistir` `(sira)`
  - `sifirla`

- [ ] **Step 1: Write failing tests**

Tests:
- `bolumGorunurluguDegistirir()`
- `bolumSirasiniGunceller()`
- `sifirlaVarsayilanDuzeneDoner()`
- `enAzBirBolumKalmali()`
- `klavyeIlePanelKapanir()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/components/__tests__/KokpitOzellestirme.spec.js`
Expected: component does not exist.

- [ ] **Step 3: Implement customization panel**

Use simple up/down buttons for order in this phase; drag-drop is not required unless already available.
Persist through `kokpitStore.tercihKaydet`.
Prevent hiding all sections.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/components/__tests__/KokpitOzellestirme.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KokpitOzellestirme.vue \
        frontend/src/views/Dashboard.vue \
        frontend/src/components/__tests__/KokpitOzellestirme.spec.js
git commit -m "feat(dashboard): kokpit bolum ozellestirme"
```

---

### Task 12: Live refresh and accessibility hardening

**Files:**
- Modify: `frontend/src/composables/useDashboardCanli.js`
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/assets/app.css`
- Test: `frontend/src/composables/__tests__/useDashboardCanli.spec.js`
- Test: `frontend/src/views/__tests__/Dashboard.spec.js`

**Interfaces:**
- Consumes existing STOMP subscription for `/topic/dashboard/{sirketId}`.
- Produces:
  - Cockpit refresh callback that calls `kokpitStore.getKokpit()` at most once per event debounce window.
  - Focus-visible styles and reduced-motion behavior for all cockpit components.

- [ ] **Step 1: Write failing tests**

Tests:
- `stompOlayiKokpitiYeniler()`
- `kisaArdisikOlaylarTekIstekAtar()`
- `baglantiKopukkenKokpitHataVermez()`
- `reducedMotionIleCokGucluAnimasyonKapali()`

- [ ] **Step 2: Run tests and verify failure**

Run: `npx vitest run src/composables/__tests__/useDashboardCanli.spec.js src/views/__tests__/Dashboard.spec.js`
Expected: current live composable refreshes legacy dashboard only.

- [ ] **Step 3: Implement live refresh**

Debounce STOMP-triggered refresh to one request per 2 seconds.
Do not create recursive refresh loops.
On disconnect, show existing status indicator and keep current data.

- [ ] **Step 4: Run tests and verify pass**

Run: `npx vitest run src/composables/__tests__/useDashboardCanli.spec.js src/views/__tests__/Dashboard.spec.js`
Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/composables/useDashboardCanli.js \
        frontend/src/views/Dashboard.vue \
        frontend/src/assets/app.css \
        frontend/src/composables/__tests__/useDashboardCanli.spec.js \
        frontend/src/views/__tests__/Dashboard.spec.js
git commit -m "feat(dashboard): kokpit canli yenileme ve a11y sertlestirme"
```

---

### Task 13: i18n, Cypress E2E, performance gates, Docker deploy

**Files:**
- Modify: `frontend/src/locales/tr.json`
- Modify: `frontend/src/locales/en.json`
- Modify: `frontend/cypress/e2e/dashboard.cy.js`
- Test: all existing gates

**Interfaces:**
- Produces complete bilingual cockpit copy and E2E coverage.

- [ ] **Step 1: Add i18n keys and Cypress tests**

Cypress scenarios:
- `kokpit-ilk-yukleme`
- `kokpit-donem-filtresi`
- `kokpit-kritik-serit-detay`
- `kokpit-ozellestirme`
- `kokpit-390px`
- `kokpit-canli-yenileme` (intercept STOMP or API refresh)

390px check:
```js
cy.viewport(390, 844)
expect(doc.documentElement.scrollWidth).to.be.at.most(doc.documentElement.clientWidth + 1)
```

- [ ] **Step 2: Run full frontend gates**

Run:
`npm run lint`
`npm run i18n:check`
`npm run test`
`npm run build`

Expected: zero warnings, i18n symmetric, all tests pass, build succeeds.

- [ ] **Step 3: Run backend gates**

Run: `mvn -B test`
Expected: all backend tests pass.

- [ ] **Step 4: Build and deploy Docker stack**

Run:
`docker compose build backend frontend`
`docker compose up -d backend frontend`

Verify:
- backend healthy
- frontend healthy
- `GET /api/dashboard/kokpit` returns 200 for an authorized test user
- `/` shows the new cockpit in the deployed bundle

- [ ] **Step 5: Commit and push**

```bash
git add frontend/src/locales/tr.json frontend/src/locales/en.json frontend/cypress/e2e/dashboard.cy.js
git commit -m "feat(dashboard): kokpit i18n, e2e ve dağıtım doğrulaması"
git push origin main
```

---

### Task 14: Legacy dashboard cleanup

**Files:**
- Modify: `frontend/src/views/Dashboard.vue` only if a stale import remains
- Delete: dashboard-only components confirmed unused after the new cockpit ships
- Test: full frontend gate

**Interfaces:**
- Produces a clean dashboard component set with no dead imports or orphaned tests.

- [ ] **Step 1: Find unused dashboard components**

Run a reference scan for each file under `frontend/src/components/dashboard/` that was used only by the old Dashboard.
Exclude any component still imported by another view or test.

- [ ] **Step 2: Delete confirmed orphans**

Delete only files with zero remaining references.
Also remove their tests and delete now-empty folders.

- [ ] **Step 3: Run full gate**

Run: `npm run lint && npm run i18n:check && npm run test && npm run build`
Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add -A frontend/src/components/dashboard frontend/src/views/Dashboard.vue
git commit -m "chore(dashboard): eski kokpit bilesenlerini temizle"
```

