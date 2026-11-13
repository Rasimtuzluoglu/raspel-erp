# Depolar Operasyon Merkezi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Depolar ekranını; filtrelenebilir/sayfalanabilir depo listesi, depo stok yönetimi, SLA'lı transfer akışı ve hareket geçmişiyle üç rolün ortak kullandığı bir operasyon merkezine dönüştürmek.

**Architecture:** Önce yalnızca ek kolon/indeks ve yeni uçlarla backend genişletilir (mevcut uçlar bozulmaz). Ardından 1253 satırlık `Depolar.vue`; store + yetki composable'ı + beş panel bileşenine ayrılır. Tüm listeler sunucu taraflı sayfalanır; KPI'lar gruplu sorgularla hesaplanır.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, Flyway, PostgreSQL/H2; Vue 3, Vite, PrimeVue 4, Pinia, Vitest, Cypress.

**Spec:** `docs/superpowers/specs/2026-11-13-depolar-operasyon-merkezi-design.md`

## Global Constraints

- Migration **V183**; yalnızca nullable kolon + indeks ekler, veri silmez.
- Mevcut uçlar korunur: `GET /api/depolar` **Page** dönmeye devam eder; `GET /api/depolar/{id}/stoklar` **dizi** kalır.
- Sayfalama: varsayılan `size=20`, üst sınır **100**, `page` 0 tabanlı.
- SLA: `app.depo.transfer-sla-saat: 24`; kovalar `NORMAL < 4s`, `YAKLASIYOR 4–24s`, `GECIKTI > 24s`.
- Kapasite: `kapasiteKalem` (SKU) opsiyonel; `null`/0 ise doluluk `null`.
- CSV: UTF-8 BOM, `;` ayırıcı, `= + - @` ile başlayan hücrelere `'` öneki (formül enjeksiyonu).
- Red sebebi en az **3** karakter; onay/red yalnız ADMIN.
- Stok ekle/çıkar yalnız ADMIN veya depo sorumlusu; miktar **> 0**, çıkışta yeterli stok.
- Tüm yeni metinler `depolar.*` altında tr/en; mobilde 44px dokunma hedefi.
- Bitirmeden: `mvn clean verify`, `npm run lint`, `npm run i18n:check`, `npm run test`, `npm run build`, Cypress.

## Review Focus

- Negatif/sıfır miktar ve stok aşımı: sunucu reddetmeli, arayüz yalnızca görsel uyarı vermemeli.
- Aynı kaynak/hedef depo ve yetersiz kaynak stoğu: hem talep hem onay anında engellenmeli.
- `minMiktar`, `kapasiteKalem`, `ortalamaMaliyet` null: NPE/NaN yok; “—” veya gizli alan.
- Zaten işlenmiş transfer, gerekçesiz red, yetkisiz onay: iş kuralı hatası dönmeli.
- Sayfa sınırı, Türkçe büyük/küçük harf arama, CSV formül enjeksiyonu: güvenli ve tutarlı.

---

### Task 1: V183 migration + entity/DTO alanları

**Files:**
- Create: `backend/src/main/resources/db/migration/V183__depo_operasyon.sql`
- Modify: `backend/src/main/java/com/raspel/erp/entity/sube/Depo.java`
- Modify: `backend/src/main/java/com/raspel/erp/entity/sube/DepoTransfer.java`
- Modify: `backend/src/main/java/com/raspel/erp/entity/envanter/StokHareket.java`
- Modify: `backend/src/main/java/com/raspel/erp/dto/sube/DepoDTO.java`
- Modify: `backend/src/main/java/com/raspel/erp/dto/sube/DepoStokDTO.java`
- Modify: `backend/src/main/java/com/raspel/erp/dto/sube/DepoTransferDTO.java`
- Modify: `backend/src/main/java/com/raspel/erp/dto/envanter/StokHareketDTO.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/envanter/StokService.java`
- Create: `backend/src/main/java/com/raspel/erp/dto/sube/DepoDetayOzetDTO.java`
- Create: `backend/src/main/java/com/raspel/erp/dto/sube/DepoHareketDTO.java`

**Interfaces:**
- Produces: `Depo.getKapasiteKalem(): Integer`, `Depo.getSorumluKullaniciId(): Long`,
  `DepoTransfer.getOnaylayanKullaniciId(): Long`, `getRedTarihi(): LocalDateTime`,
  `getRedSebebi(): String`, `StokHareket.getKullaniciId(): Long`.
- Produces DTO alanları: `DepoDTO` (+`kapasiteKalem, sorumluKullaniciId, sorumluKullaniciAd,
  kalemSayisi, sifirStokSayisi, kritikStokSayisi, stokDegeri, dolulukYuzde, kritikMi`),
  `DepoStokDTO` (+`minMiktar, ortalamaMaliyet, deger, kritikMi`),
  `DepoTransferDTO` (+`kaynakDepoAd, hedefDepoAd, stokAd, olusturanKullaniciAd,
  onaylayanKullaniciAd, yasSaat, slaDurumu, redSebebi`).

- [ ] **Step 1: Migration dosyasını yaz**

```sql
ALTER TABLE sube.depo ADD COLUMN IF NOT EXISTS kapasite_kalem INT;
ALTER TABLE sube.depo ADD COLUMN IF NOT EXISTS sorumlu_kullanici_id BIGINT;
ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS onaylayan_kullanici_id BIGINT;
ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS red_tarihi TIMESTAMPTZ;
ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS red_sebebi VARCHAR(500);
ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS kullanici_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_depo_transfer_durum
  ON sube.depo_transfer (sirket_id, durum, olusturma_tarihi);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_depo
  ON stok.stok_hareket (depo_id, olusturma_tarihi);
```

- [ ] **Step 2: Entity alanlarını ekle** (Lombok `@Data`; `Depo` ve `DepoTransfer` için `LocalDateTime`, `StokHareket` için `Long kullaniciId`).

- [ ] **Step 3: DTO alanlarını ekle**; `DepoDetayOzetDTO` (`depoId, kalemSayisi, sifirStokSayisi, kritikStokSayisi, stokDegeri, dolulukYuzde, sonHareketler: List<DepoHareketDTO>`) ve `DepoHareketDTO` (`id, tarih, tur, kaynakTip, stokId, stokKod, stokAd, miktar, kullaniciAd, aciklama`) oluştur.

- [ ] **Step 4: Derle ve context/migration testini çalıştır**

Run: `mvn -B -q -DskipTests compile && mvn -B -q -Dtest=RaspelErpApplicationTests test`
Expected: BUILD SUCCESS; Flyway V183 uygulanır.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V183__depo_operasyon.sql backend/src/main/java/com/raspel/erp/entity backend/src/main/java/com/raspel/erp/dto/sube
git commit -m "feat(depolar): V183 depo operasyon alanlari"
```

---

### Task 2: Depo listesi filtreleri + detay özeti

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/repository/sube/DepoRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/sube/DepoService.java`
- Modify: `backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java`
- Modify: `backend/src/test/java/com/raspel/erp/service/sube/DepoServiceTest.java`
- Modify: `backend/src/test/java/com/raspel/erp/controller/sube/DepoControllerTest.java`

**Interfaces:**
- Produces: `Page<DepoDTO> tumunuGetir(Long sirketId, String q, Long subeId, Boolean aktif, Boolean kritik, Pageable pageable)`
- Produces: `DepoDetayOzetDTO detayOzet(Long depoId, Long sirketId)`
- Consumes: `DepoRepository`, `DepoStokRepository`, `StokRepository`.

- [ ] **Step 1: Failing test yaz** (`DepoServiceTest`)
  - `q="merkez"` araması `ad`/`yetkili` üzerinde case-insensitive eşleşir.
  - `kapasiteKalem=null` iken `dolulukYuzde` null; `kapasite=10, kalem=4` iken 40.
  - `kritik=true` yalnız en az bir `miktar <= minMiktar` olan depoları döner.
  - `stokDegeri` `miktar * (ortalamaMaliyet ?? fiyat)` toplamıdır; ikisi de null ise 0.
  - Son sayfadan büyük `page` boş içerik + doğru `totalElements` döner.

- [ ] **Step 2: Testi çalıştır, FAIL gör**

Run: `mvn -B -q -Dtest=DepoServiceTest test`
Expected: FAIL (metot yok / hesap yanlış).

- [ ] **Step 3: Repository sorgularını ve servisi uygula**
  - `DepoRepository`: `findBySirketIdAndFiltre(...)` JPQL (`lower(d.ad) like :q or lower(d.yetkili) like :q`, `subeId`, `aktif`).
  - `DepoService.tumunuGetir`: sayfa + tek gruplu kalem/sıfır/kritik/değer toplamı; DTO map.
  - `DepoService.detayOzet`: depo KPI'ları + son 5 hareket.

- [ ] **Step 4: Controller'ı güncelle**

`GET /api/depolar?q&subeId&aktif&kritik&page&size` → `tumunuGetir`; `GET /api/depolar/{id}/ozet` → `detayOzet`.

- [ ] **Step 5: Test PASS + commit**

Run: `mvn -B -q -Dtest=DepoServiceTest,DepoControllerTest test`
Expected: PASS.

```bash
git add backend/src/main/java/com/raspel/erp/repository/sube/DepoRepository.java backend/src/main/java/com/raspel/erp/service/sube/DepoService.java backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java backend/src/test/java/com/raspel/erp/service/sube/DepoServiceTest.java
git commit -m "feat(depolar): filtreli depo listesi ve detay ozeti"
```

---

### Task 3: Depo stok sayfalama, hareketler, yetki ve doğrulama

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/repository/sube/DepoStokRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/repository/envanter/StokHareketRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/sube/DepoService.java` (stok ekle/cikar + hareket)
- Create: `backend/src/main/java/com/raspel/erp/service/sube/DepoYetki.java`
- Modify: `backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java`
- Modify: `backend/src/test/java/com/raspel/erp/service/sube/DepoServiceTest.java` (stok islem testleri)
- Modify: `backend/src/test/java/com/raspel/erp/controller/sube/DepoControllerTest.java`

**Interfaces:**
- Produces: `Page<DepoStokDTO> sayfali(Long depoId, Long sirketId, String q, Boolean kritik, Boolean sifir, Pageable pageable)`
- Produces: `Page<DepoHareketDTO> hareketler(Long depoId, Long sirketId, String tip, Pageable pageable)`
- Produces: `DepoStokDTO stokEkle(Long depoId, Long stokId, BigDecimal miktar, Long kullaniciId)`
- Produces: `DepoStokDTO stokCikar(...)`; `boolean islemYapabilir(Depo depo, Long kullaniciId)`.

- [ ] **Step 1: Failing testler**
  - `miktar <= 0` → `BusinessException` (“Miktar 0'dan büyük olmalı”).
  - Çıkışta mevcut < istenen → hata; stok değişmez, hareket yazılmaz.
  - Yetkisiz kullanıcı (ADMIN değil, sorumlu değil) → hata.
  - `minMiktar=null` stok “kritik” sayılmaz; `miktar=0` sıfır filtresine girer.
  - Başarılı giriş/çıkışta `stok_hareket.kullanici_id` dolar.

- [ ] **Step 2: FAIL doğrula**

Run: `mvn -B -q -Dtest=DepoStokServiceTest test`
Expected: FAIL.

- [ ] **Step 3: Repository + servis uygula**
  - `DepoStokRepository`: `Stok` join’li sayfalı sorgu (`q`, `kritik`, `sifir`).
  - `StokHareketRepository`: `findByDepoIdOrderByOlusturmaTarihiDesc` + tip filtresi + Pageable.
  - `DepoYetki`: ADMIN veya `depo.sorumluKullaniciId == kullaniciId`.
  - `DepoService.stokEkle/Cikar`: miktar>0, yetki, kilitli satır, `StokHareketDTO.kullaniciId` ile hareket.

- [ ] **Step 4: Controller uçları**
  - `GET /api/depolar/{id}/stoklar/sayfali?q&kritik&sifir&page&size`
  - `GET /api/depolar/{id}/hareketler?tip&page&size`
  - `POST /{id}/stok-ekle|cikar` gövdelerine kullanıcı kimliği servise geçirilir.

- [ ] **Step 5: PASS + commit**

Run: `mvn -B -q -Dtest=DepoStokServiceTest,DepoControllerTest test`

```bash
git add backend/src/main/java/com/raspel/erp/repository backend/src/main/java/com/raspel/erp/service/sube backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java backend/src/test/java/com/raspel/erp/service/sube/DepoStokServiceTest.java
git commit -m "feat(depolar): stok sayfalama, hareketler ve yetki dogrulamasi"
```

---

### Task 4: Transfer filtreleri, SLA ve onay/red denetimi

**Files:**
- Modify: `backend/src/main/java/com/raspel/erp/repository/sube/DepoTransferRepository.java`
- Modify: `backend/src/main/java/com/raspel/erp/service/sube/DepoTransferService.java`
- Modify: `backend/src/main/java/com/raspel/erp/controller/sube/DepoTransferController.java`
- Modify: `backend/src/main/resources/application.yml` (`app.depo.transfer-sla-saat: 24`)
- Modify: `backend/src/main/resources/application-prod.yml` (aynı anahtar, prod profili)
- Modify: `backend/src/main/java/com/raspel/erp/dto/sube/DepoTransferDTO.java`
- Modify: `backend/src/test/java/com/raspel/erp/service/sube/DepoTransferServiceTest.java`
- Modify: `backend/src/test/java/com/raspel/erp/controller/sube/DepoTransferControllerTest.java`

**Interfaces:**
- Produces: `Page<DepoTransferDTO> sayfali(Long sirketId, Long depoId, String durum, LocalDate bas, LocalDate bit, Pageable p)`
- Produces: `DepoTransferDTO onayla(Long id, Long kullaniciId)`
- Produces: `DepoTransferDTO reddet(Long id, Long kullaniciId, String sebep)`
- Produces: `String slaDurumu(LocalDateTime olusturma)`.

- [ ] **Step 1: Failing testler**
  - SLA: 2s → `NORMAL`, 6s → `YAKLASIYOR`, 30s → `GECIKTI`.
  - `BEKLIYOR` olmayan transfer onaylanınca hata.
  - `sebep` boş/2 karakter → hata.
  - ADMIN olmayan kullanıcı onay/red → hata.
  - Onay/red sonrası `onaylayanKullaniciId`, `onayTarihi`/`redTarihi`, `redSebebi` dolar.
  - Aynı kaynak/hedef → hata (talep aşaması).

- [ ] **Step 2: FAIL doğrula**

Run: `mvn -B -q -Dtest=DepoTransferServiceTest test`

- [ ] **Step 3: Repository + servis + config**
  - Filtreli Page sorgusu (`depoId` kaynak veya hedef, `durum`, tarih aralığı).
  - `slaDurumu` `@Value("${app.depo.transfer-sla-saat:24}")`.
  - DTO map: depo/stok adları, oluşturan/onaylayan adları (batch).

- [ ] **Step 4: Controller**
  - `GET /api/depo-transferler/sayfali`
  - `POST /{id}/onayla` (kullanıcı attr), `POST /{id}/reddet` gövde `{sebep}`.

- [ ] **Step 5: PASS + commit**

Run: `mvn -B -q -Dtest=DepoTransferServiceTest,DepoTransferControllerTest test`

```bash
git add backend/src/main/java/com/raspel/erp/repository/sube/DepoTransferRepository.java backend/src/main/java/com/raspel/erp/service/sube/DepoTransferService.java backend/src/main/java/com/raspel/erp/controller/sube/DepoTransferController.java backend/src/main/java/com/raspel/erp/dto/sube/DepoTransferDTO.java backend/src/main/resources/application.yml backend/src/test/java/com/raspel/erp/service/sube/DepoTransferServiceTest.java
git commit -m "feat(depolar): transfer filtreleri, SLA ve denetim"
```

---

### Task 5: CSV dışa aktarma

**Files:**
- Create: `backend/src/main/java/com/raspel/erp/service/sube/DepoExportService.java`
- Modify: `backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java`
- Create: `backend/src/test/java/com/raspel/erp/service/sube/DepoExportServiceTest.java`

**Interfaces:**
- Produces: `byte[] csv(String tur, Long sirketId, Long depoId, String q, Long subeId, Boolean aktif, Boolean kritik, String durum)`
- `tur` ∈ `liste|stok|transfer`; `stok` için `depoId` zorunlu.

- [ ] **Step 1: Failing test**
  - Çıktı UTF-8 BOM (`EF BB BF`) ile başlar, `;` ayırıcı, başlık satırı doğru.
  - `=HYPERLINK(...)` hücresi `'=HYPERLINK...` olarak kaçırılır.
  - `tur=stok` depoId yoksa `BusinessException`.

- [ ] **Step 2: FAIL doğrula**

Run: `mvn -B -q -Dtest=DepoExportServiceTest test`

- [ ] **Step 3: Uygula + endpoint**

`GET /api/depolar/export?tur=...` → `text/csv; charset=UTF-8`, `Content-Disposition: attachment`.

- [ ] **Step 4: PASS + commit**

```bash
git add backend/src/main/java/com/raspel/erp/service/sube/DepoExportService.java backend/src/main/java/com/raspel/erp/controller/sube/DepoController.java backend/src/test/java/com/raspel/erp/service/sube/DepoExportServiceTest.java
git commit -m "feat(depolar): CSV disa aktarma"
```

---

### Task 6: Frontend API + depoStore + yetki composable

**Files:**
- Modify: `frontend/src/api/modules/stok.js`
- Create: `frontend/src/stores/depoStore.js`
- Create: `frontend/src/composables/useDepoYetki.js`
- Create: `frontend/src/stores/__tests__/depoStore.spec.js`
- Create: `frontend/src/composables/__tests__/useDepoYetki.spec.js`

**Interfaces:**
- `depoAPI.sayfali(params)`, `depoAPI.detayOzet(id)`, `depoAPI.stokSayfali(id, params)`, `depoAPI.hareketler(id, params)`, `depoAPI.export(params)`
- `depoTransferAPI.sayfali(params)`, `onayla(id)`, `reddet(id, sebep)`
- Store: `filtre` (`q, subeId, aktif, kritik`), `sayfa`, `boyut`, `liste`, `toplam`, `ozet`; actions `yukle()`, `filtreDegistir(partial)`, `sayfaDegistir(n)`.
- `useDepoYetki().islemYapabilir(depo)` → `authStore.isAdmin || depo.sorumluKullaniciId === authStore.kullanici?.id`.

- [ ] **Step 1: Failing Vitest**
  - `filtreDegistir({ q: 'a' })` sayfayı 1’e döndürür ve `yukle` çağırır.
  - API `content/totalElements` map’i doğru.
  - Yetki: ADMIN her depo; USER yalnız sorumlu olduğu depo.

- [ ] **Step 2: FAIL doğrula**

Run: `npx vitest run src/stores/__tests__/depoStore.spec.js src/composables/__tests__/useDepoYetki.spec.js`

- [ ] **Step 3: Uygula**

`apiClient.get('/depolar', { params })` vb.; store `defineStore('depo', ...)`; composable `useAuthStore`.

- [ ] **Step 4: PASS + commit**

```bash
git add frontend/src/api/modules/stok.js frontend/src/stores/depoStore.js frontend/src/composables/useDepoYetki.js frontend/src/stores/__tests__/depoStore.spec.js frontend/src/composables/__tests__/useDepoYetki.spec.js
git commit -m "feat(depolar): frontend API, store ve yetki composable"
```

---

### Task 7: KPI şeridi + depo listesi + form

**Files:**
- Create: `frontend/src/components/depo/DepoOzetKartlari.vue`
- Create: `frontend/src/components/depo/DepoListePaneli.vue`
- Create: `frontend/src/components/depo/DepoFormDialog.vue`
- Modify: `frontend/src/views/Depolar.vue`
- Create: `frontend/src/__tests__/depolar-faz1.spec.js`

**Interfaces:**
- `DepoOzetKartlari` emits `kpi-sec({ anahtar })`.
- `DepoListePaneli` props: `depolar, toplam, yukleniyor, filtre, sayfa, boyut, yetki`; emits `filtre-degistir`, `sayfa-degistir`, `duzenle`, `sil`, `detay`, `stoklar`.
- `DepoFormDialog` props: `visible, depo, subeler, kullanicilar`; emits `kaydet(form)`, `kapat`.

- [ ] **Step 1: Sözleşme testi yaz** (`depolar-faz1.spec.js`)
  - KPI tıklaması `kpi-sec` yayar; kart/tablo geçişi `gosterim` değiştirir.
  - Formda `kapasiteKalem` ve `sorumluKullaniciId` alanları vardır; `min=0`.

- [ ] **Step 2: FAIL doğrula**

Run: `npx vitest run src/__tests__/depolar-faz1.spec.js`

- [ ] **Step 3: Bileşenleri ve Depolar.vue sekmesini uygula**
  - KPI → store filtresi + ilgili sekme.
  - Liste: arama (300ms debounce), şube/durum/kritik, tablo/kart, sayfalama.
  - **CSV indir** butonu (`depoAPI.export({ tur: 'liste', ...filtre })` → blob indirme).
  - Form: ad, şube, sorumlu (ADMIN), kapasite, adres, aktif.

- [ ] **Step 4: PASS + commit**

```bash
git add frontend/src/components/depo frontend/src/views/Depolar.vue frontend/src/__tests__/depolar-faz1.spec.js
git commit -m "feat(depolar): KPI seridi, filtreli liste ve depo formu"
```

---

### Task 8: Depo stok paneli

**Files:**
- Create: `frontend/src/components/depo/DepoStokPaneli.vue`
- Modify: `frontend/src/views/Depolar.vue`
- Create: `frontend/src/__tests__/depolar-faz2.spec.js`

**Interfaces:**
- Props: `depo, yetki`; internal: `q, kritik, sifir, sayfa`, `stokForm`.
- Emits: `hareket-goster({ stokId })`, `ozet-yenile`.
- Buttons disabled: `!stokForm.stokId || stokForm.miktar <= 0 || kaydediliyor`.

- [ ] **Step 1: Sözleşme testi**
  - Negatif/sıfır miktarda ekle/çıkar butonu disabled.
  - Yetki yoksa panel gizli; kritik/sıfır filtreleri istek parametrelerine eklenir.
  - **CSV indir** butonu (`tur: 'stok'`, `depoId`).

- [ ] **Step 2: FAIL → uygula → PASS**

Run: `npx vitest run src/__tests__/depolar-faz2.spec.js`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/depo/DepoStokPaneli.vue frontend/src/views/Depolar.vue frontend/src/__tests__/depolar-faz2.spec.js
git commit -m "feat(depolar): stok paneli ve islem dogrulamasi"
```

---

### Task 9: Transfer paneli

**Files:**
- Create: `frontend/src/components/depo/DepoTransferPaneli.vue`
- Modify: `frontend/src/views/Depolar.vue`
- Create: `frontend/src/__tests__/depolar-faz3.spec.js`

**Interfaces:**
- Props: `depolar, adminMi`; emits `yenile`.
- Durum sekmeleri `BEKLIYOR|ONAYLANDI|REDDEDILDI|TUMU`; SLA rozeti `NORMAL|YAKLASIYOR|GECIKTI`.
- Red dialogu: `sebep.trim().length >= 3` olmadan onay butonu disabled.

- [ ] **Step 1: Sözleşme testi**
  - Onay/red butonları yalnız `adminMi`; red sebebi 3 karakterden kısa ise disabled.
  - SLA etiketi ve yaş gösterimi.
  - **CSV indir** butonu (`tur: 'transfer'`, durum filtresi).

- [ ] **Step 2: FAIL → uygula → PASS**

Run: `npx vitest run src/__tests__/depolar-faz3.spec.js`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/depo/DepoTransferPaneli.vue frontend/src/views/Depolar.vue frontend/src/__tests__/depolar-faz3.spec.js
git commit -m "feat(depolar): transfer paneli, SLA ve onay/red"
```

---

### Task 10: Hareket paneli + 360 detay

**Files:**
- Create: `frontend/src/components/depo/DepoHareketPaneli.vue`
- Modify: `frontend/src/components/DepoDetayDialog.vue`
- Modify: `frontend/src/views/Depolar.vue`
- Create: `frontend/src/__tests__/depolar-faz4.spec.js`

**Interfaces:**
- Props: `depo, stokId`; filtre `tip`, sayfalama.
- Detay: doluluk, stok değeri, kritik ilk 5, son 5 hareket.

- [ ] **Step 1: Sözleşme testi**
  - Hareket tablosunda kullanıcı yoksa “—”.
  - Detay dialogu doluluk null iken çubuk göstermez.

- [ ] **Step 2: FAIL → uygula → PASS**

Run: `npx vitest run src/__tests__/depolar-faz4.spec.js`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/depo/DepoHareketPaneli.vue frontend/src/components/DepoDetayDialog.vue frontend/src/views/Depolar.vue frontend/src/__tests__/depolar-faz4.spec.js
git commit -m "feat(depolar): hareket gecmisi ve 360 detay"
```

---

### Task 11: i18n, mobil/erişilebilirlik, Cypress ve tam doğrulama

**Files:**
- Modify: `frontend/src/locales/tr.json`
- Modify: `frontend/src/locales/en.json`
- Modify: `frontend/src/views/Depolar.vue` ve `frontend/src/components/depo/*.vue` (stil)
- Create: `frontend/cypress/e2e/depolar.cy.js`

**Interfaces:**
- Yeni anahtarlar: `depolar.kpiGecikenTalep`, `depolar.slaNormal`, `depolar.slaYaklasiyor`,
  `depolar.slaGecikti`, `depolar.kapasite`, `depolar.doluluk`, `depolar.sorumluKullanici`,
  `depolar.hareketler`, `depolar.redSebebi`, `depolar.csvIndir`, `depolar.yetkiYok`.

- [ ] **Step 1: i18n anahtarlarını iki dile ekle; `npm run i18n:check`.**

- [ ] **Step 2: Mobil/erişilebilirlik**
  - `@media (pointer: coarse)` 44px hedefler; kart görünümünde sticky aksiyon; `aria-label`, `aria-live`.

- [ ] **Step 3: Cypress senaryoları** (`depolar.cy.js`)
  - Liste: arama + kritik filtresi + sayfalama.
  - Stok: 0/negatif miktar engeli; yetersiz stokta çıkış hatası.
  - Transfer: talep oluştur → ADMIN onayla; ikinci onay hatası; gerekçeli red.
  - Hareket geçmişi ve mobil (1024px) görünüm.
  - ADMIN dışı rol için `localStorage` oturumu `role: 'USER'` ile kurulur; stok ekle/çıkar gizli, onay/red butonları yok doğrulanır.

- [ ] **Step 4: Tam doğrulama**

Run: `mvn -B clean verify` (backend), `npm run lint && npm run i18n:check && npm run test && npm run build`, `npm run build && npx vite preview --port 5173` + `npx cypress run --spec cypress/e2e/depolar.cy.js`.
Expected: hepsi PASS; JaCoCo kapısı geçer.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/locales frontend/src/components/depo frontend/src/views/Depolar.vue frontend/cypress/e2e/depolar.cy.js
git commit -m "feat(depolar): i18n, mobil uyum ve e2e"
```

---

## Execution Notes

- Backend önce deploy edilebilir; frontend eski uçlarla çalışmaya devam eder.
- Her görev sonunda `git status` temiz olmalı; `tasks/` ve geçici loglar commit edilmez.
- Cypress CI'da `vite preview` üzerinde koşar (`.github/workflows/ci.yml`).
- Migration geri alınmaz; frontend geri dönüşü commit revert ile yapılır.
