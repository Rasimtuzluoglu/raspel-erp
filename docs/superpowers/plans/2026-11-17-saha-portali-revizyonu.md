# Saha Portalı Revizyonu — Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Saha personelinin telefonunda tek ekranda “Bugün” özetini görüp teslimat/ziyaret/talep/tahsilat işlerini az dokunuşla yapmasını ve bağlantı koptuğunda veri kaybetmemesini sağlamak.

**Architecture:** Tek yeni backend özet ucu (`GET /api/saha/ozet`) kullanıcı bazlı sayıları döner; frontend’de özet + aktif sekme paralel yüklenir, diğer sekmeler lazy’dir. Çevrimdışı işlemler IndexedDB kuyruğunda saklanır ve `online`/zamanlayıcı ile sıralı gönderilir; para hareketleri 24 saatlik `X-Idempotency-Key` ile korunur.

**Tech Stack:** Java 21 + Spring Boot 3.5 (JPA, Redis cache, JUnit 5, Mockito), Vue 3 `<script setup>` + Pinia + PrimeVue 4, Vitest, Cypress, IndexedDB.

**Spec:** `docs/superpowers/specs/2026-11-17-saha-portali-revizyonu-design.md`

## Global Constraints

- Yeni şema/migration **yok** (idempotency Redis TTL ile çözülür).
- Mevcut uçlar bozulmaz; yalnız ek uç ve opsiyonel DTO alanı (`musteriTelefon`).
- Açılışta en fazla **2 API isteği** (özet + aktif sekme); `size=500` toplu yüklemeler kaldırılır.
- Tüm birincil dokunma hedefleri ≥ **44 px**; 390 px’te yatay taşma yok.
- Çevrimdışı kuyrukta tahsilat yalnız `clientKey` (idempotency) ile gönderilir.
- Mevcut lint kuralları: `npm run lint` sıfır uyarı; `npm run i18n:check` temiz; `mvn -B clean verify` yeşil.

## Review Focus

1. **Aynı tahsilatın iki kez yazılması** — ağ zaman aşımı + kuyruk yeniden denemesi; aynı `clientKey` 24 saat içinde ikinci kayıt üretmemeli.
2. **Başka sürücünün teslimatının görünmesi** — özet ve liste yalnız oturum kullanıcısının `driverId` kayıtlarını döndürmeli.
3. **Kuyruk kaydının sessizce kaybolması** — IndexedDB kapalı/kota dolu veya eski localStorage kuyruğu taşınmazsa kullanıcı uyarılmalı; kayıt silinmemeli.
4. **Yanıtı kaybolan isteğin mükerrer gönderimi** — kuyruk yalnız ağ hatasında yeniden dener; 4xx’te “HATA”ya düşer ve otomatik tekrar etmez.
5. **Mobil imza sheet’inde taşma/kayma** — 390 px’te sheet tam ekran, klavye açılınca imza alanı erişilebilir, yatay taşma yok.

---

### Task 1: Saha özeti (DTO + repository + servis + cache)

**Files:**
- Create: `backend/src/main/java/com/raspel/erp/dto/sistem/SahaOzetDTO.java`
- Create: `backend/src/main/java/com/raspel/erp/service/sistem/SahaOzetService.java`
- Modify: `backend/src/main/java/com/raspel/erp/repository/ticaret/TeslimatRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/repository/sistem/NotRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/config/RedisConfig.java` (yeni `sahaOzet` = 60 sn)
- Test: `backend/src/test/java/com/raspel/erp/service/sistem/SahaOzetServiceTest.java`

**Interfaces:**
- Produces:
  - `SahaOzetDTO` alanları: `LocalDate tarih`, `Teslimat teslimat`, `Ziyaret ziyaret`, `Talepler talepler`; iç sınıflar `Teslimat(long bekleyen, long yolda, long bugunTeslimEdilen, long geciken)`, `Ziyaret(long bugun, LocalDateTime sonZiyaretTarihi)`, `Talepler(long bekleyenIzin, long bekleyenMasraf)`; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
  - `SahaOzetService.ozet(Long sirketId, Long kullaniciId) -> SahaOzetDTO`
  - `TeslimatRepository.countBySirketIdAndDriverIdAndDurum(Long, Long, String) -> long`
  - `TeslimatRepository.countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum(Long, Long, LocalDateTime, LocalDateTime, String) -> long`
  - `TeslimatRepository.countBySirketIdAndDriverIdAndBeklenenTeslimTarihiBeforeAndDurumNotIn(Long, Long, LocalDate, Collection<String>) -> long`
  - `NotRepository.countBySirketIdAndKullaniciIdAndOlusturmaTarihiBetween(Long, Long, LocalDateTime, LocalDateTime) -> long`
  - `NotRepository.findTop1BySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc(Long, Long) -> Optional<Not>`

- [ ] **Step 1: Write the failing test**

`SahaOzetServiceTest` (Mockito): `ozet_surucuSayilariniDondurur`, `ozet_baskaSurucuVerisiniKullanmaz`, `ozet_bosVerideSifirDoner`, `ozet_bugunSinirlariniKullanir`.
Örnek gövde:

```java
when(teslimatRepository.countBySirketIdAndDriverIdAndDurum(1L, 9L, "BEKLEMEDE")).thenReturn(3L);
when(teslimatRepository.countBySirketIdAndDriverIdAndDurum(1L, 9L, "YOLDA")).thenReturn(1L);
when(teslimatRepository.countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum(
        eq(1L), eq(9L), any(), any(), eq("TESLIM_EDILDI"))).thenReturn(2L);
when(teslimatRepository.countBySirketIdAndDriverIdAndBeklenenTeslimTarihiBeforeAndDurumNotIn(
        eq(1L), eq(9L), any(), any())).thenReturn(1L);
when(notRepository.countBySirketIdAndKullaniciIdAndOlusturmaTarihiBetween(eq(1L), eq(9L), any(), any()))
        .thenReturn(2L);
var sonuc = service.ozet(1L, 9L);
assertEquals(3, sonuc.getTeslimat().getBekleyen());
assertEquals(2, sonuc.getZiyaret().getBugun());
verify(teslimatRepository).countBySirketIdAndDriverIdAndDurum(1L, 9L, "BEKLEMEDE");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -q -Dtest=SahaOzetServiceTest`
Expected: derleme hatası (`SahaOzetService`/DTO yok).

- [ ] **Step 3: Implement DTO, repository metotları, servis, cache TTL**

Servis: `LocalDate bugun = LocalDate.now();` gün aralığı `bugun.atStartOfDay()`–`bugun.plusDays(1).atStartOfDay()`; teslimat durumları `BEKLEMEDE`, `YOLDA`, `TESLIM_EDILDI`; geciken durum dışlaması `TESLIM_EDILDI, IPTAL`; izin `BEKLEMEDE` `countByDurumAndSirketId`; masraf `BEKLEMEDE` `countBySirketIdAndDurum`. `@Cacheable(cacheNames="sahaOzet", key="#sirketId + ':' + #kullaniciId")`. `RedisConfig`’e `.withCacheConfiguration("sahaOzet", defaultCacheConfig.entryTtl(Duration.ofSeconds(60)))`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -q -Dtest=SahaOzetServiceTest`
Expected: `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/dto/sistem/SahaOzetDTO.java backend/src/main/java/com/raspel/erp/service/sistem/SahaOzetService.java backend/src/main/java/com/raspel/erp/repository/ticaret/TeslimatRepository.java backend/src/main/java/com/raspel/erp/repository/sistem/NotRepository.java backend/src/main/java/com/raspel/erp/config/RedisConfig.java backend/src/test/java/com/raspel/erp/service/sistem/SahaOzetServiceTest.java
git commit -m "feat(saha): kullanici bazli saha ozeti servisi"
```

---

### Task 2: Saha özet ucu + yetki

**Files:**
- Create: `backend/src/main/java/com/raspel/erp/controller/sistem/SahaController.java`
- Test: `backend/src/test/java/com/raspel/erp/controller/sistem/SahaControllerTest.java`
- Test: `backend/src/test/java/com/raspel/erp/config/security/SahaYetkiTest.java`

**Interfaces:**
- Consumes: `SahaOzetService.ozet(Long sirketId, Long kullaniciId)`.
- Produces: `GET /api/saha/ozet` → `200 SahaOzetDTO`; sınıf `@PreAuthorize("hasAnyRole('ADMIN','USER','MUHASEBE','DRIVER','SAHA')")`; `sirketId`/`kullaniciId` request attribute’larından.

- [ ] **Step 1: Write the failing tests**

`SahaControllerTest` (`@WebMvcTest`, `addFilters=false`): `ozet_donar` — `$.teslimat.bekleyen=3`, `$.ziyaret.bugun=2`.
`SahaYetkiTest` (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@MockBean SahaOzetService`): `user_ve_saha_erisir` (`user("u").roles("USER")` → 200; `roles("SAHA")` → 200), `satis_rolu_403` (`roles("SATIS")` → 403).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -q -Dtest=SahaControllerTest,SahaYetkiTest`
Expected: FAIL (controller yok).

- [ ] **Step 3: Implement controller**

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -q -Dtest=SahaControllerTest,SahaYetkiTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/controller/sistem/SahaController.java backend/src/test/java/com/raspel/erp/controller/sistem/SahaControllerTest.java backend/src/test/java/com/raspel/erp/config/security/SahaYetkiTest.java
git commit -m "feat(saha): GET /api/saha/ozet ucu ve yetki testleri"
```

---

### Task 3: Teslimat DTO’suna müşteri telefonu

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/dto/ticaret/TeslimatDTO.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/ticaret/TeslimatService.java` (`toDTO`)
- Test: `backend/src/test/java/com/raspel/erp/service/ticaret/TeslimatServiceTest.java`

**Interfaces:**
- Produces: `TeslimatDTO.getMusteriTelefon()`; `toDTO` fatura → `cariHesap.telefon` ile doldurur; fatura yoksa `null`.

- [ ] **Step 1: Write the failing test**

`toDTO_musteriTelefonunuFaturaCarisindenDoldurur` ve `toDTO_cariYoksaTelefonNull`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -q -Dtest=TeslimatServiceTest#toDTO_musteriTelefonunuFaturaCarisindenDoldurur`
Expected: FAIL (alan yok).

- [ ] **Step 3: Implement alan + doldurma**

`faturaId != null` ise `faturaRepository.findById(...).map(Fatura::getCariHesap).map(CariHesap::getTelefon)`; istisna yutulur (`orElse(null)`).

- [ ] **Step 4: Run tests**

Run: `mvn -B test -q -Dtest=TeslimatServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/dto/ticaret/TeslimatDTO.java backend/src/main/java/com/raspel/erp/service/ticaret/TeslimatService.java backend/src/test/java/com/raspel/erp/service/ticaret/TeslimatServiceTest.java
git commit -m "feat(saha): teslimat DTO'suna musteri telefonu"
```

---

### Task 4: Idempotency TTL aşırı yüklemesi + tahsilat 24 saat

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/service/sistem/IdempotencyService.java`
- Modify: `backend/src/main/java/com/raspel/erp/controller/finans/TahsilatController.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/com/raspel/erp/service/sistem/IdempotencyServiceTest.java`

**Interfaces:**
- Produces:
  - `boolean deneKilit(String anahtar, Duration ttl)`
  - `void tamamla(String anahtar, Long kayitId, Duration ttl)`
  - Mevcut tek argümanlı metotlar `VARSAYILAN_TTL` (10 dk) ile delege eder.
  - `TahsilatController` alanı: `@Value("${app.saha.tahsilat-idempotency-saat:24}") private long sahaIdempotencySaat;` ve tahsilat POST’unda `Duration.ofHours(sahaIdempotencySaat)`.

- [ ] **Step 1: Write the failing test**

`IdempotencyServiceTest`: `ttlAsimindaAnahtarYenidenKullanilabilir`, `ttlIcindeIkinciKilitAlinamaz`, `tamamlaTtlIleSonucDoner`. Yerel map ile (Redis bean yok) test edilir; süre için `Duration.ofMillis(50)` + kısa bekleme yerine `ReflectionTestUtils` ile son kullanma veya `Duration.ZERO` kullanımı — testte `deneKilit(k, Duration.ZERO)` sonrası ikinci çağrı `true` dönmeli.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -q -Dtest=IdempotencyServiceTest`
Expected: FAIL (metot yok).

- [ ] **Step 3: Implement overload’lar + controller TTL + property**

`application.properties`: `app.saha.tahsilat-idempotency-saat=${SAHA_TAHSILAT_IDEMPOTENCY_SAAT:24}`.

- [ ] **Step 4: Run test + ilgili controller testleri**

Run: `mvn -B test -q -Dtest=IdempotencyServiceTest,TahsilatControllerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/raspel/erp/service/sistem/IdempotencyService.java backend/src/main/java/com/raspel/erp/controller/finans/TahsilatController.java backend/src/main/resources/application.properties backend/src/test/java/com/raspel/erp/service/sistem/IdempotencyServiceTest.java
git commit -m "feat(saha): tahsilat idempotency TTL 24 saat"
```

---

### Task 5: Frontend API + özet composable + özet kartları

**Files:**
- Create: `frontend/src/api/modules/saha.js`
- Modify: `frontend/src/api/index.js` (`sahaAPI` export)
- Create: `frontend/src/composables/useSahaOzet.js`
- Create: `frontend/src/components/saha/SahaOzetKartlari.vue`
- Test: `frontend/src/composables/__tests__/useSahaOzet.spec.js`, `frontend/src/components/__tests__/SahaOzetKartlari.spec.js`

**Interfaces:**
- Produces:
  - `sahaAPI.ozet() -> Promise<AxiosResponse<SahaOzetDTO>>` (`GET /saha/ozet`)
  - `useSahaOzet() -> { ozet, yukleniyor, hata, yukle }`; `yukle()` 60 sn throttle (son yükleme zamanı).
  - `SahaOzetKartlari` props: `ozet` (Object), `yukleniyor` (Boolean); emit `sec` (`'teslimat' | 'ziyaret' | 'talepler'`); data-test: `saha-ozet-teslimat`, `saha-ozet-ziyaret`, `saha-ozet-talepler`.

- [ ] **Step 1: Write failing tests**

Composable: `ozetYuklenir`, `hataDurumundaSifirDoner`, `60SaniyeIcindeTekrarIstekAtmaz`.
Kartlar: `ucKartiDegerleriyleGosterir`, `kartaTiklanincaSecYayar`, `yuklenirkenIskeletGosterir`.

- [ ] **Step 2: Run to verify fail**

Run: `npx vitest run src/composables/__tests__/useSahaOzet.spec.js src/components/__tests__/SahaOzetKartlari.spec.js`
Expected: FAIL (modül yok).

- [ ] **Step 3: Implement**

Tarih/saat biçimi `utils/format.js`; metinler `t('sahaPortali.*')` (Task 11’de eklenecek anahtarlar; geçici olarak mevcut anahtarlar kullanılabilir, Task 11’de tamamlanır).

- [ ] **Step 4: Run tests**

Run: `npx vitest run src/composables/__tests__/useSahaOzet.spec.js src/components/__tests__/SahaOzetKartlari.spec.js`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/modules/saha.js frontend/src/api/index.js frontend/src/composables/useSahaOzet.js frontend/src/components/saha/SahaOzetKartlari.vue frontend/src/composables/__tests__/useSahaOzet.spec.js frontend/src/components/__tests__/SahaOzetKartlari.spec.js
git commit -m "feat(saha): ozet API, composable ve Bugun kartlari"
```

---

### Task 6: IndexedDB çevrimdışı kuyruk composable

**Files:**
- Create: `frontend/src/composables/useSahaKuyruk.js`
- Test: `frontend/src/composables/__tests__/useSahaKuyruk.spec.js`

**Interfaces:**
- Produces:
  - `useSahaKuyruk({ gondericiler })` → `{ kuyruk, yukleniyor, senkronizeEdiliyor, ekle, sil, senkronize, baslat, durdur }`
  - `gondericiler`: `{ TESLIMAT: (payload, dosyalar) => Promise, ZIYARET: ..., MASRAF: ..., IZIN: ..., TAHSILAT: ... }`
  - `ekle({ tur, payload, dosyalar, clientKey }) -> Promise<id>`; `clientKey` yoksa `crypto.randomUUID()`.
  - Kayıt alanları: spec §5.4 (`id`, `clientKey`, `tur`, `payload`, `dosyalar`, `durum`, `deneme`, `sonHata`, `olusturma`).
  - `baslat()` → `online` dinleyicisi + 60 sn aralık; `durdur()` temizler.
  - `senkronize()`: sıralı; başarıda sil; `4xx` → `HATA` (otomatik tekrar yok); ağ/timeout → `BEKLIYOR`.
  - Eski `localStorage['raspel_saha_kuyrugu']` kayıtlarını `ZIYARET`/`TESLIMAT` olarak taşır ve anahtarı siler.

- [ ] **Step 1: Write failing tests**

`fake-indexeddb` kullanılır (`import 'fake-indexeddb/auto'`). Testler: `ekleVeListele`, `basariliSenkronKaydiSilerveGondericiyiCagirir`, `agHatasindaKayitBekliyorKalir` (gonderici `TypeError` fırlatır), `4xxHatasindaDurumHataOlur` (`{ response: { status: 422 } }`), `clientKeyUretilir`, `eskiLocalStorageKuyruguTasinir`, `senkronizeEdiliyorBayragiKilitler`, `indexedDbYoksaLocalStorageFallback` (db açılışı reddedilir → kayıt `localStorage['raspel_saha_kuyrugu_yedek']` içinde, kullanıcıya `depolamaKisitli` bayrağı).

- [ ] **Step 2: Run to verify fail**

Run: `npx vitest run src/composables/__tests__/useSahaKuyruk.spec.js`
Expected: FAIL.

- [ ] **Step 3: Implement**

DB `raspel-saha`, store `kuyruk`; `openDb()` promise; işlemler `readwrite`; `senkronize` `if (senkronizeEdiliyor.value) return`. `4xx` tespiti `err?.response?.status >= 400 && < 500`.

- [ ] **Step 4: Run tests**

Run: `npx vitest run src/composables/__tests__/useSahaKuyruk.spec.js`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/composables/useSahaKuyruk.js frontend/src/composables/__tests__/useSahaKuyruk.spec.js
git commit -m "feat(saha): IndexedDB cevrimdisi kuyruk"
```

---

### Task 7: Kuyruk çubuğu bileşeni

**Files:**
- Create: `frontend/src/components/saha/SahaKuyrukCubugu.vue`
- Test: `frontend/src/components/__tests__/SahaKuyrukCubugu.spec.js`

**Interfaces:**
- Consumes: kuyruk kayıt dizisi (spec §5.4).
- Produces: props `kuyruk` (Array), `senkronizeEdiliyor` (Boolean); emit `gonder`; data-test `saha-kuyruk-cubugu`, `saha-kuyruk-gonder`; sayaç + tür kırılımı (`3 kayıt · 2 teslimat · 1 tahsilat`); hata varsa `sonHata` ipucu.

- [ ] **Step 1: Write failing test** `kuyrukVarkenSayaciGosterir`, `gonderTiklanincaEmit`, `kuyrukYoksaGorunmez`, `hataVarsaUyariGosterir`.
- [ ] **Step 2: Run fail** `npx vitest run src/components/__tests__/SahaKuyrukCubugu.spec.js`.
- [ ] **Step 3: Implement** (aria-live polite; 44 px buton).
- [ ] **Step 4: Run pass.**
- [ ] **Step 5: Commit** `git commit -m "feat(saha): kuyruk durum cubugu"`.

---

### Task 8: Navigasyon düğmesi + teslimat kartı + sıralama yardımcısı

**Files:**
- Create: `frontend/src/components/saha/SahaNavigasyonButton.vue`
- Create: `frontend/src/components/saha/SahaTeslimatKarti.vue`
- Create: `frontend/src/utils/sahaSiralama.js`
- Test: `frontend/src/components/__tests__/SahaTeslimatKarti.spec.js`, `frontend/src/utils/__tests__/sahaSiralama.spec.js`

**Interfaces:**
- Produces:
  - `SahaNavigasyonButton` props `adres`; `data-test="saha-navigasyon"`; tıklamada `window.open(url, '_blank')`; URL: `https://www.google.com/maps/dir/?api=1&destination=<encodeURIComponent(adres)>`; adres boşsa `v-if` ile gizlenir.
  - `sahaSiralama.sirala(liste, siraIdleri)` → sıra listesine göre sıralı yeni dizi (bilinmeyenler sona, kararlı).
  - `SahaTeslimatKarti` props `teslimat` (TeslimatDTO), `sira` (Number), `ilkMi` (Boolean); emit `imza`, `durum-sec`, `telefon`; data-test `saha-teslimat-karti`; durum rozeti, adres, müşteri, `musteriTelefon` varsa `tel:`/WhatsApp, navigasyon, “İmzala ve teslim et”; `ilkMi` ise “Sıradaki” rozeti.

- [ ] **Step 1: Write failing tests** `adresVarsaNavigasyonUrlUretir`, `adresYoksaGizlenir`, `siralamaBilinmeyenleriSonaAtar`, `kartDurumVeAksiyonlariGosterir`, `imzaEmitEder`.
- [ ] **Step 2: Run fail** `npx vitest run src/components/__tests__/SahaTeslimatKarti.spec.js src/utils/__tests__/sahaSiralama.spec.js`.
- [ ] **Step 3: Implement**, `window.open` testte `vi.spyOn`.
- [ ] **Step 4: Run pass.**
- [ ] **Step 5: Commit** `git commit -m "feat(saha): navigasyon ve teslimat karti"`.

---

### Task 9: İmza/foto sheet’i

**Files:**
- Create: `frontend/src/components/saha/SahaImzaSheet.vue`
- Test: `frontend/src/components/__tests__/SahaImzaSheet.spec.js`

**Interfaces:**
- Consumes: mevcut `ImzaPad` (`@imza-degisti` dataURL) ve `teslimat` DTO.
- Produces: props `visible` (Boolean), `teslimat`, `cevrimici` (Boolean), `kaydediliyor` (Boolean); emit `update:visible`, `gonder({ teslimAlanAd, teslimNotu, imzaDataUrl, fotoDataUrl })`, `kuyrukla(ayniGovde)`; data-test `saha-imza-sheet`, `saha-imza-gonder`, `saha-imza-kuyruk`; teslim alan adı zorunlu; imza yoksa gönder kapalı.

- [ ] **Step 1: Write failing test** `alanlarZorunlu`, `cevrimiciGonderEmit`, `cevrimdisiKuyrukEmit`, `kapatEmit`.
- [ ] **Step 2: Run fail**, **Step 3: Implement** (Dialog fullscreen `<640px`), **Step 4: pass**, **Step 5: commit**.

---

### Task 10: SahaPortali düzeni + lazy sekmeler + kuyruk entegrasyonu

**Files:**
- Modify: `frontend/src/views/SahaPortali.vue`
- Test: `frontend/src/views/__tests__/SahaPortali.spec.js`

**Interfaces:**
- Consumes: Task 5–9 çıktıları; `teslimatAPI.teslimatlar(kullaniciId)`, `notAPI`, `personelIzinAPI`, `personelMasrafTalepAPI`, `tahsilatAPI`.
- Produces: `aktifSekme` (`'teslimat'|'ziyaret'|'talepler'|'tahsilat'`), `localStorage` anahtarı `raspel_saha_sekme`; `seciliTeslimat`, `imzaSheetAcik`; teslimat sırası `localStorage['raspel_saha_teslimat_sirasi']`; kuyruk `ekle` çağrıları; özet tıklaması sekme değiştirir.

- [ ] **Step 1: Write failing tests** `acilistaOzetVeAktifSekmeIstegi` (2 istek), `sekmeDegisinceYalnizOSekmeYuklenir`, `ozetKartiSekmeDegistirir`, `cevrimdisiTeslimatKuyrugaGirer`, `basariliImzaSonrasiListeYenilenir`.
- [ ] **Step 2: Run fail.**
- [ ] **Step 3: Implement**: header sadeleşir, `TabView` → segmented; teslimat listesi yeni kartlarla; `useSahaKuyruk` gondericileri; `useSahaOzet`; iyimser durum güncellemesi.
- [ ] **Step 4: Run pass.**
- [ ] **Step 5: Commit** `git commit -m "feat(saha): yeni saha portali duzeni"`.

---

### Task 11: i18n + erişilebilirlik/mobil cila

**Files:**
- Modify: `frontend/src/locales/tr.json`, `frontend/src/locales/en.json`
- Modify: `frontend/src/components/saha/*.vue`, `frontend/src/views/SahaPortali.vue`
- Test: `frontend/src/__tests__/saha-i18n.spec.js`

**Interfaces:**
- Produces: spec §7 anahtarları iki dilde; `aria-live` kuyruk/özet; 44 px hedefler; 390 px taşma yok.

- [ ] **Step 1: Write failing test** anahtar varlığı + `SahaPortali.vue` içinde `aria-live` ve 44 px kuralı.
- [ ] **Step 2: Run fail** `npx vitest run src/__tests__/saha-i18n.spec.js`.
- [ ] **Step 3: Implement** anahtarlar + stiller.
- [ ] **Step 4:** `npm run i18n:check` ve `npx vitest run src/__tests__/saha-i18n.spec.js` PASS.
- [ ] **Step 5: Commit** `git commit -m "feat(saha): i18n ve mobil erisilebilirlik"`.

---

### Task 12: Cypress E2E + tam doğrulama

**Files:**
- Create: `frontend/cypress/e2e/saha-portali.cy.js`

**Interfaces:** mevcut Cypress mock desenleri (`girisYap`, intercept); 390 px görünüm.

- [ ] **Step 1: Write spec**: `ozetVeSekmeler` (3 kart, 4 sekme, taşma yok), `cevrimdisiKuyruk` (`cy.intercept` teslimat 503 → imza → kuyruk çubuğu; `online` olayı → gönderim), `navigasyonUrl`, `imzaSheet`.
- [ ] **Step 2: Run** `npm run build && npx vite preview --port 5173` + `npx cypress run --spec cypress/e2e/saha-portali.cy.js`; FAIL beklenir (eksikler).
- [ ] **Step 3: Eksikleri düzelt** (yalnız testin işaret ettiği davranışlar).
- [ ] **Step 4: Tam doğrulama**:
  - `mvn -B clean verify`
  - `npm run lint && npm run i18n:check && npm run test && npm run build`
  - Cypress saha + dashboard + depolar spec’leri.
- [ ] **Step 5: Docker yenile** `docker compose up -d --build --no-deps backend frontend`; health + canlı `GET /api/saha/ozet` 200.
- [ ] **Step 6: Commit** `git commit -m "test(saha): e2e ve tam dogrulama"`.

---

## Self-review notları

- Spec §4.1 (özet uç), §4.2 (TTL), §5.1–5.6 (bileşen/akış), §6 (hata), §7 (i18n), §8 (test) görevlere eşlendi.
- Tipler: `SahaOzetDTO` alan adları Task 5’te aynen kullanılıyor; `clientKey`/`tur` adları kuyruk boyunca sabit.
- Kapsam dışı: güzergâh optimizasyonu, çevrimdışı harita.
