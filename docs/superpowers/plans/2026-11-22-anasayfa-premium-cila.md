# Anasayfa Premium Cila — Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Anasayfayı ("Finans Masası") veri sözleşmesini bozmadan daha odaklı, daha ferah ve premium hissettiren bir komuta merkezine dönüştürmek.

**Architecture:** Yalnızca frontend. Mevcut `GET /api/dashboard/kokpit` sözleşmesi, `kokpitStore`, `data-test` kancaları ve alt bölüm bileşenleri korunur. Değişiklikler üç katmanda ilerler: (1) ortak yüzey/tipografi token'ları ve ölü CSS temizliği, (2) hero + orta şerit + detay kartlarının görsel hiyerarşisi, (3) grafik, responsive ve erişilebilirlik cilası.

**Tech Stack:** Vue 3 (`<script setup>`), Pinia, PrimeVue 4, saf SVG grafikler, Vitest + Vue Test Utils, Cypress. Yeni çalışma zamanı bağımlılığı yok.

**Spec:** Bu dokümanın "Tasarım yönü" ve "Mevcut durum tespitleri" bölümleri. Öncesi görselleri: `docs/superpowers/specs/assets/2026-11-22-anasayfa-once-ust.png`, `docs/superpowers/specs/assets/2026-11-22-anasayfa-once-tam.png`.

## Global Constraints

- Backend, API ve veritabanı değişmez; `KokpitDTO` alanları ve hesaplama mantığı aynı kalır.
- Mevcut `data-test` kancaları korunur: `dashboard-kokpit`, `finans-ozet-bandi`, `kpi-*`, `kritik-oge-*`, `kokpit-akis`, `bekleyen-oge-*`, `hizli-*`, `kokpit-bolum-*`, `*-detay`.
- Yeni renk değeri eklenmez; tüm renkler `app.css` token'larından gelir. Mevcut hardcoded `rgba(...)` değerleri token'a taşınır.
- Koyu ve açık tema ayrı ayrı doğrulanır; açık temada beyaz parlama/gölge görünmez.
- Kırılımlar: 1920, 1440, 1264, 1024, 768, 390 px. 390 px'te yatay taşma olmaz.
- `prefers-reduced-motion: reduce` tüm yeni geçiş/animasyonları kapatır.
- Dokunmatik hedefler (`@media (pointer: coarse)`) en az 44×44 px.
- Sayılar `font-variant-numeric: tabular-nums`; parasal değerler sağa hizalı.
- Frontend kalite kapıları: `npm run lint` (0 uyarı), `npm run i18n:check`, `npm run test`, `npm run build`.
- Komut çalışma dizini: test/build/Cypress komutları `frontend/` içinde; `git` komutları repo kökünde çalıştırılır.
- Yeni i18n anahtarları hem `tr.json` hem `en.json` içinde tanımlanır.
- Görsel doğrulama için geçici Cypress spec'i kullanılabilir; **commit edilmez**.

## Review Focus

- **Sıfır/boş dönem:** KPI değeri 0 iken `-100%` ve boş grafik gürültüsü eziyet etmemeli; değişim rozeti yalnızca anlamlıysa görünmeli, boş kartta `NaN`/`Infinity` oluşmamalı.
- **Uzun Türkçe etiket ve tutarlar:** "Net Çalışma Sermayesi", `1.025.330,50 ₺` gibi değerler 1264 px ve 390 px'te taşmamalı; kritik sayı `text-overflow` ile gizlenmemeli.
- **Açık tema:** Kartlardaki beyaz iç parlama ve teal ışıma açık temada leke gibi görünmemeli; `--kokpit-*` token'ları tema ile değişmeli.
- **Yetkisiz rol (USER):** Kâr/marj kartı ve onay aksiyonları gizlenirken grid boşluk bırakmamalı; hızlı işlemler 2×2 düzeni bozulmamalı.
- **Ekran okuyucu:** `role="status"` canlı duyuru alanı görünür olmamalı (mevcut hata), odak halkası klavye ile görünür kalmalı.
- **Sticky üst bar + global `zoom: 0.9`:** 1024 ve 390 px'te üst bar içerikle çakışmamalı; mobilde sticky davranışı kapanmalı.

---

## Tasarım yönü

**"Sakin finans masası":** Ekranda tek bir odak metriği, onun yanında üç destek metriği; geri kalan her şey daha sessiz. Kutular değil, hiyerarşi konuşur.

- **Yüzey dili:** Sayfa zemini → kart (`--bg-card`) → kart içi hafif ışıma. Her kartta aynı 1 px kenar, aynı `--radius-lg`, aynı gölge. Gradient yalnızca üstte 2 px'lik marka çizgisi ve çok soluk köşe ışıması.
- **Tipografi:** Grafik/label 11.5–12 px; gövde 13 px; kart başlığı 16 px; odak değer 30–34 px; destek değer 20–23 px. Tüm sayılar tabular.
- **Aksan disiplini:** Teal yalnızca eylem, odak ve aktif durum için. Kırmızı/amber/yeşil yalnızca gerçek durum için; dekoratif renk yok.
- **Ritim:** 8 px tabanlı boşluk; kart içi 16 px, kart arası 14 px, bölüm arası 22 px. Masaüstünde içerik en fazla 1760 px genişliğe yayılır.
- **Hareket:** 150–200 ms, yalnızca hover/focus ve grafik çizimi. Giriş animasyonu, sayı sayacı, parallax yok.
- **Yoğunluk:** Kullanıcı tercihi (`kompakt`/`ferah`) korunur; premium görünüm "ferah"ı varsayılan yapmaz, kompaktı da nefes alacak şekilde iyileştirir.

## Mevcut durum tespitleri

Canlı uygulamadan alınan öncesi görüntüye göre:

1. **Görünür `sr-only` hatası:** `src/assets/dashboard.css` hiçbir yerden import edilmiyor; `.sr-only` yalnızca orada tanımlı. Sonuç: "Son güncelleme: …" canlı duyurusu ekranda ikinci kez görünüyor. Dosya tamamen ölü (15 KB) ve silinmeli; `.sr-only` global utility olarak `app.css`'e taşınmalı.
2. **KPI bandı odaksız:** Dört eşit kart var; ciro/marj/alacak/verecek aynı ağırlıkta. İlk metrik büyütülmüp sparkline'ı büyütülmeli, diğer üçü destek kartı olmalı.
3. **Hızlı işlemler liste gibi:** Beş özdeş satır, sağ kolonu bir ayar menüsü gibi gösteriyor. Tahsilat birincil geniş karo, diğerleri 2×2 karo olmalı.
4. **Orta şerit dengesiz:** "Bekleyen İşler" kartı içeriği kadar kalıyor; sağda büyük boş alan oluşuyor. Kart `height: 100%`, liste `flex: 1`, alt CTA dibe sabitlenmeli.
5. **Detay kartları farklı yükseklikte:** Özellikle "Kâr & Marj" boş durumda kısa kalıyor; yanındaki "Fatura / Sipariş" uzun. Grid çocukları `height: 100%` olmalı, `FinansKarti` gövdesi `flex: 1` ile footer'ı dibe itmeli.
6. **Hardcoded yüzey renkleri:** `FinansKarti`, `KarKoprusu`, `KokpitIcgoruler`, `KokpitNakitTakvimi` içinde `rgba(20,184,166,…)` ve `rgba(255,255,255,…)` var; açık temada doğru görünmüyor. Token'a taşınmalı.
7. **Anlamsız durum rengi:** Alacak/Verecek kartındaki tahsilat oranı eşikten bağımsız kırmızı görünüyor. `≥80` yeşil, `50–79` amber, `<50` kırmızı olmalı.
8. **Tekrarlanan "Tümünü gör" kutuları:** Her kartın altında tam genişlik buton var; dört kez tekrar edince gürültü oluşturuyor. Sessiz sağa yaslı metin bağlantısına dönüşmeli.
9. **Küçük dokunma hedefleri:** Üst bardaki ikon butonlar 28 px; mobilde 40 px'e çıkıyor ama 44 px hedefinin altında.
10. **Sayfa genişliği:** `max-width: none`; 1920 px'te kartlar aşırı yayılıyor. 1760 px üst sınır ve ortalama iyi bir çerçeve sağlar.

## Dosya yapısı

**Değişecek:**
- `frontend/src/assets/app.css` — `--kokpit-*` token'ları, global `.sr-only`, tema override'ları
- `frontend/src/views/Dashboard.vue` — sayfa ritmi, grid, bölüm başlığı, kart yüksekliği
- `frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue` — odaklı KPI bandı
- `frontend/src/components/dashboard/kokpit/MetrikKutu.vue` — `boyut`/sparkline yüksekliği
- `frontend/src/components/dashboard/kokpit/KokpitHizliIslemler.vue` — birincil karo + 2×2
- `frontend/src/components/dashboard/kokpit/KokpitAkis.vue` — gün grupları ve zaman çizgisi
- `frontend/src/components/dashboard/kokpit/KokpitBekleyenIsler.vue` — ikonlu satır, dibe sabit CTA
- `frontend/src/components/dashboard/kokpit/FinansKarti.vue` — ortak yüzey, başlık, footer
- `frontend/src/components/dashboard/kokpit/KarKoprusu.vue` — yüzey token'ı
- `frontend/src/components/dashboard/kokpit/KokpitIcgoruler.vue` — yüzey token'ı
- `frontend/src/components/dashboard/kokpit/KokpitNakitTakvimi.vue` — yüzey token'ı
- `frontend/src/components/dashboard/kokpit/AlacakVerecekBolumu.vue` — tahsilat eşiği rengi
- `frontend/src/components/dashboard/kokpit/FaturaSiparisBolumu.vue` — footer hizalaması
- `frontend/src/components/dashboard/kokpit/MikroGrafik.vue` — `tip="alan"`, son nokta
- `frontend/src/components/dashboard/kokpit/NakitBolumu.vue` — odak değer, alan grafiği
- `frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue` — boş durum ve trend grafiği
- `frontend/src/components/dashboard/kokpit/KokpitGlobalFiltre.vue` — üst bar hiyerarşisi
- `frontend/src/locales/tr.json`, `frontend/src/locales/en.json` — gün grubu anahtarları

**Silinecek:**
- `frontend/src/assets/dashboard.css` — sıfır import; `.sr-only` taşındıktan sonra ölü kod

**Testler:**
- `frontend/src/components/__tests__/FinansOzetBandi.spec.js`
- `frontend/src/components/__tests__/KokpitHizliIslemler.spec.js`
- `frontend/src/components/__tests__/KokpitAkis.spec.js`
- `frontend/src/components/__tests__/KokpitBekleyenIsler.spec.js`
- `frontend/src/components/__tests__/NakitAlacakBolumleri.spec.js`
- `frontend/src/components/__tests__/KarFaturaBolumleri.spec.js`
- `frontend/src/views/__tests__/Dashboard.spec.js`
- `frontend/cypress/e2e/dashboard.cy.js`

---

### Task 1: Görsel temel — `.sr-only`, ölü CSS ve yüzey token'ları

**Files:**
- Modify: `frontend/src/assets/app.css`
- Delete: `frontend/src/assets/dashboard.css`
- Modify: `frontend/src/components/dashboard/kokpit/FinansKarti.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KarKoprusu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KokpitIcgoruler.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KokpitNakitTakvimi.vue`
- Test: `frontend/cypress/e2e/dashboard.cy.js`

**Interfaces:**
- Produces: `:root` ve `[data-theme='light']` altında `--kokpit-glow`, `--kokpit-parlaklik`, `--kokpit-ust-cizgi`, `--kokpit-kart-golge`, `--kokpit-deger-buyuk`, `--kokpit-deger`, `--kokpit-etiket`, `--kokpit-kart-pad`.
- Produces: global `.sr-only` utility (position absolute, 1×1, clip).
- Consumes: mevcut `--accent`, `--bg-card`, `--border`, `--elev-1` token'ları.

- [ ] **Step 1: Başarısız Cypress testini yaz**

`dashboard.cy.js` içindeki ilk testin sonuna ekle:

```js
// Canlı duyuru alanı görünür olmamalı; yoksa "Son güncelleme" iki kez görünür.
cy.get('[data-test="dashboard-kokpit"] [role="status"]').should('not.be.visible')
```

- [ ] **Step 2: Testi çalıştır ve kırmızı olduğunu doğrula**

```bash
cd frontend
npm run build
npx vite preview --port 5173 --strictPort   # ayrı terminal
npx cypress run --browser chrome --spec cypress/e2e/dashboard.cy.js
```

Beklenen: `expected ... to not be visible` hatası.

- [ ] **Step 3: `.sr-only` utility'sini ve token'ları ekle**

`app.css` içindeki `:root` bloğuna ekle:

```css
/* Kokpit yüzey dili */
--kokpit-glow: radial-gradient(520px 180px at 100% 0%, rgba(20, 184, 166, 0.10), transparent 72%);
--kokpit-parlaklik: linear-gradient(180deg, rgba(255, 255, 255, 0.028), transparent 38%);
--kokpit-ust-cizgi: linear-gradient(90deg, var(--accent), transparent 72%);
--kokpit-kart-golge: var(--elev-1), inset 0 1px 0 rgba(255, 255, 255, 0.025);
--kokpit-deger-buyuk: clamp(26px, 2.2vw, 34px);
--kokpit-deger: clamp(19px, 1.5vw, 23px);
--kokpit-etiket: 11.5px;
--kokpit-kart-pad: 16px;
```

`[data-theme='light']` bloğuna ekle:

```css
--kokpit-glow: radial-gradient(520px 180px at 100% 0%, rgba(13, 148, 136, 0.08), transparent 72%);
--kokpit-parlaklik: linear-gradient(180deg, rgba(15, 23, 42, 0.02), transparent 38%);
--kokpit-kart-golge: var(--elev-1), inset 0 1px 0 rgba(255, 255, 255, 0.7);
```

Global utility olarak `.sr-only` sınıfını `app.css`'in yardımcı sınıflar bölümüne taşı (dashboard.css'teki tanımın aynısı).

- [ ] **Step 4: Ölü dosyayı sil ve `FinansKarti` yüzeyini token'a çevir**

```bash
git grep -n "dashboard.css" -- frontend/src || true
git rm frontend/src/assets/dashboard.css
```

`FinansKarti.vue`, `KarKoprusu.vue`, `KokpitIcgoruler.vue` ve `KokpitNakitTakvimi.vue` içindeki `rgba(255, 255, 255, …)` / `rgba(20, 184, 166, …)` yüzey değerlerini token'a çevir. `FinansKarti.vue` için:

```css
background:
  var(--kokpit-glow),
  var(--kokpit-parlaklik),
  var(--bg-card);
box-shadow: var(--kokpit-kart-golge);
```

`::before` çizgisini `background: var(--kokpit-ust-cizgi);` yap. Diğer üç dosyada `linear-gradient(180deg, rgba(255,255,255,…), transparent)` yerine `var(--kokpit-parlaklik)`; `inset 0 1px 0 rgba(255,255,255,…)` yerine `var(--kokpit-kart-golge)` kullan. Böylece açık temada beyaz parlama oluşmaz.

- [ ] **Step 5: Testleri ve kalite kapılarını çalıştır**

```bash
cd frontend
npx cypress run --browser chrome --spec cypress/e2e/dashboard.cy.js
npm run lint
npm run build
```

Beklenen: Cypress yeşil, lint 0 uyarı, build başarılı.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/assets/app.css frontend/src/assets/dashboard.css frontend/src/components/dashboard/kokpit/FinansKarti.vue frontend/src/components/dashboard/kokpit/KarKoprusu.vue frontend/src/components/dashboard/kokpit/KokpitIcgoruler.vue frontend/src/components/dashboard/kokpit/KokpitNakitTakvimi.vue frontend/cypress/e2e/dashboard.cy.js
git commit -m "refactor(dashboard): global sr-only ve kokpit yuzey tokenlari"
```

---

### Task 2: Hero KPI bandı — tek odak metriği + üç destek

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/MetrikKutu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue`
- Test: `frontend/src/components/__tests__/FinansOzetBandi.spec.js`

**Interfaces:**
- `MetrikKutu` consumes: `boyut?: 'normal' | 'buyuk'` (default `'normal'`), `sparklineYukseklik?: number` (default `32`).
- `MetrikKutu` produces: `.metrik-kutu--buyuk`, değer font-size `var(--kokpit-deger-buyuk)`, sparkline yüksekliği verilen değer.
- `FinansOzetBandi` produces: `data-test="finans-ust"`, `.finans-ust--odakli`; ilk birincil metrikte `data-test="kpi-<anahtar>"` korunur ve `.metrik-kutu--buyuk` bulunur.

- [ ] **Step 1: Başarısız testleri yaz**

`FinansOzetBandi.spec.js` içine ekle:

```js
it('ilk metrigi buyuk odak karti olarak isaretler', () => {
  const wrapper = mountBant({ kpi })
  const ciro = wrapper.find('[data-test="kpi-ciro"]')
  expect(ciro.classes()).toContain('metrik-kutu--buyuk')
  expect(wrapper.find('[data-test="finans-ust"]').classes()).toContain('finans-ust--odakli')
})

it('dort buyuk metrik ve kompakt serit korunur', () => {
  const wrapper = mountBant({ kpi })
  expect(wrapper.findAll('.finans-ust > *')).toHaveLength(4)
  expect(wrapper.find('[data-test="finans-ikincil"]').exists()).toBe(true)
})

it('degisim yuzdesi yoksa sahte rozet gostermez', () => {
  const wrapper = mountBant({ kpi: [{ anahtar: 'ciro', deger: 0 }] })
  expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('0,00')
  expect(wrapper.find('[data-test="kpi-ciro"] .metrik-kutu-degisim--notr').exists()).toBe(true)
  expect(wrapper.find('[data-test="kpi-ciro"]').text()).not.toContain('-100')
})
```

- [ ] **Step 2: Testleri çalıştır ve kırmızı olduğunu doğrula**

```bash
npx vitest run src/components/__tests__/FinansOzetBandi.spec.js
```

Beklenen: `metrik-kutu--buyuk` / `finans-ust` bulunamadı.

- [ ] **Step 3: `MetrikKutu` varyantını uygula**

Şablon köküne sınıf bağla:

```vue
:class="{
  'metrik-kutu--kritik': kritik,
  'metrik-kutu--link': !!to,
  'metrik-kutu--buyuk': boyut === 'buyuk'
}"
```

`MikroGrafik` çağrısındaki sabit `:yukseklik="32"` yerine `:yukseklik="sparklineYukseklik"` kullan. CSS'te:

```css
.metrik-kutu-deger { font-size: var(--kokpit-deger); }
.metrik-kutu-etiket { font-size: var(--kokpit-etiket); }
.metrik-kutu--buyuk .metrik-kutu-deger { font-size: var(--kokpit-deger-buyuk); }
.metrik-kutu--buyuk { padding: calc(var(--kokpit-kart-pad) + 2px); }
.metrik-kutu--buyuk .metrik-kutu-etiket { font-size: 12px; }
```

- [ ] **Step 4: `FinansOzetBandi` grid'ini odaklı hale getir**

`<div class="finans-ust">` öğesine `data-test="finans-ust"` ve `:class="{ 'finans-ust--odakli': birincil.length > 0 }"` ekle. `v-for`'u indeksli yap:

```vue
<MetrikKutu
  v-for="(oge, index) in birincil"
  :key="oge.anahtar"
  :boyut="index === 0 ? 'buyuk' : 'normal'"
  :sparkline-yukseklik="index === 0 ? 44 : 28"
  ...
/>
```

CSS:

```css
.finans-ust {
  grid-template-columns: minmax(240px, 1.45fr) repeat(3, minmax(0, 1fr));
}
.finans-ikincil {
  gap: 0;
  padding-top: 6px;
  border-top: 1px solid var(--border);
}
.ikincil-cip {
  border-color: transparent;
  background: transparent;
  padding: 4px 10px;
}
.ikincil-cip + .ikincil-cip { border-left: 1px solid var(--border); border-radius: 0; }
@media (max-width: 1100px) { .finans-ust { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 520px) { .finans-ust { grid-template-columns: minmax(0, 1fr); } }
```

Mevcut `@media` kurallarını bu değerlerle değiştir; `.finans-ust > *` sayısının 4 kalması korunur.

- [ ] **Step 5: Testleri çalıştır**

```bash
npx vitest run src/components/__tests__/FinansOzetBandi.spec.js
```

Beklenen: tüm testler yeşil.

- [ ] **Step 6: Görsel kontrol ve commit**

1440 ve 390 px'te ekran görüntüsü al; ciro kartının diğerlerinden belirgin büyük, destek kartlarının eşit yükseklikte olduğunu doğrula.

```bash
git add frontend/src/components/dashboard/kokpit/MetrikKutu.vue frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue frontend/src/components/__tests__/FinansOzetBandi.spec.js
git commit -m "feat(dashboard): odakli KPI bandi ve sessiz ikincil metrik seridi"
```

---

### Task 3: Hızlı işlemler — birincil karo + 2×2 ızgara

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/KokpitHizliIslemler.vue`
- Test: `frontend/src/components/__tests__/KokpitHizliIslemler.spec.js`

**Interfaces:**
- Produces: ilk aksiyonda `.hizli-islem--birincil`, her aksiyonda `.hizli-ikon` sarmalayıcısı; `data-test` kancaları aynı.
- Produces: masaüstünde `.hizli-liste` 2 sütun; birincil karo `grid-column: span 2`.

- [ ] **Step 1: Başarısız testleri yaz**

```js
it('tahsilati birincil karo olarak isaretler', () => {
  const wrapper = mountBar()
  expect(wrapper.find('[data-test="hizli-tahsilat"]').classes()).toContain('hizli-islem--birincil')
})

it('her aksiyonda ikon sarmalayicisi vardir', () => {
  const wrapper = mountBar()
  expect(wrapper.findAll('.hizli-ikon')).toHaveLength(5)
})
```

- [ ] **Step 2: Testleri çalıştır ve kırmızı olduğunu doğrula**

```bash
npx vitest run src/components/__tests__/KokpitHizliIslemler.spec.js
```

- [ ] **Step 3: Şablonu ve CSS'i uygula**

Her `<i>` öğesini sar:

```vue
<span class="hizli-ikon" aria-hidden="true"><i class="pi pi-wallet" /></span>
```

Tahsilat butonuna `hizli-islem--birincil` sınıfını ekle. CSS:

```css
.hizli-liste {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}
.hizli-islem--birincil { grid-column: span 2; min-height: 58px; }
.hizli-ikon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 9px;
  background: var(--bg-muted); color: var(--text-secondary);
}
.hizli-islem--birincil .hizli-ikon { background: var(--accent-soft); color: var(--accent); }
.hizli-islem--birincil .hizli-etiket { font-size: 13px; font-weight: 700; }
@media (max-width: 640px) { .hizli-liste { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (pointer: coarse) { .hizli-islem { min-height: 48px; } }
```

Mevcut mobil 2 sütun kuralını yukarıdakiyle birleştir; 5 aksiyonun 6 hücreye oturmasını `hizli-islem--birincil` sağlar.

- [ ] **Step 4: Testleri çalıştır**

```bash
npx vitest run src/components/__tests__/KokpitHizliIslemler.spec.js
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KokpitHizliIslemler.vue frontend/src/components/__tests__/KokpitHizliIslemler.spec.js
git commit -m "feat(dashboard): hizli islemlerde birincil karo ve 2x2 duzen"
```

---

### Task 4: Orta şerit — gün gruplu akış ve dengeli bekleyen işler

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/KokpitAkis.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KokpitBekleyenIsler.vue`
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/locales/tr.json`, `frontend/src/locales/en.json`
- Test: `frontend/src/components/__tests__/KokpitAkis.spec.js`, `frontend/src/components/__tests__/KokpitBekleyenIsler.spec.js`

**Interfaces:**
- `KokpitAkis` produces: `data-test="akis-grup-<BUGUN|DUN|BU_HAFTA|DAHA_ESKI>"`, `.akis-grup-baslik`; `data-test="akis-oge-<globalIndex>"` indeksleri korunur.
- `KokpitBekleyenIsler` produces: `data-test="bekleyen-ikon-<anahtar>"`, `.bekleyen-ikon--{kritik|uyari|bilgi}`; `.bekleyen-nokta` sınıfı kaldırılır (test güncellenir).
- i18n keys: `dashboard.gunBugun`, `dashboard.gunDun`, `dashboard.gunBuHafta`, `dashboard.gunDahaEski`.

- [ ] **Step 1: Başarısız testleri yaz**

`KokpitAkis.spec.js`:

```js
it('olaylari gune gore gruplar ve global indeksi korur', () => {
  const bugun = new Date()
  const dun = new Date(Date.now() - 86400000)
  const eski = new Date(Date.now() - 10 * 86400000)
  const wrapper = mountAkis({
    olaylar: [
      olay({ tarih: bugun.toISOString() }),
      olay({ tarih: dun.toISOString(), tur: 'FATURA' }),
      olay({ tarih: eski.toISOString(), tur: 'ODEME' })
    ]
  })
  expect(wrapper.find('[data-test="akis-grup-BUGUN"]').exists()).toBe(true)
  expect(wrapper.find('[data-test="akis-grup-DUN"]').exists()).toBe(true)
  expect(wrapper.find('[data-test="akis-grup-DAHA_ESKI"]').exists()).toBe(true)
  expect(wrapper.find('[data-test="akis-oge-2"]').exists()).toBe(true)
})
```

`KokpitBekleyenIsler.spec.js` mevcut `.bekleyen-nokta--kritik` assert'ini şu şekilde değiştir:

```js
expect(wrapper.find('[data-test="bekleyen-ikon-vadesiGecen"]').exists()).toBe(true)
expect(wrapper.find('.bekleyen-ikon--kritik').exists()).toBe(true)
```

- [ ] **Step 2: Testleri çalıştır ve kırmızı olduğunu doğrula**

```bash
npx vitest run src/components/__tests__/KokpitAkis.spec.js src/components/__tests__/KokpitBekleyenIsler.spec.js
```

- [ ] **Step 3: i18n anahtarlarını ekle**

`tr.json`:

```json
"gunBugun": "Bugün",
"gunDun": "Dün",
"gunBuHafta": "Bu hafta",
"gunDahaEski": "Daha eski"
```

`en.json`:

```json
"gunBugun": "Today",
"gunDun": "Yesterday",
"gunBuHafta": "This week",
"gunDahaEski": "Older"
```

- [ ] **Step 4: Akışı grupla**

`KokpitAkis.vue` içinde `gruplar` computed'ı ekle; **`gorunen` listesi üzerinden** hesapla (filtre ve "Tümünü gör" davranışı korunur). Her grupta `{ anahtar, etiket, ogeler: [{ olay, index }] }` tut. Gün farkı: 0 → `BUGUN`, 1 → `DUN`, 2–7 → `BU_HAFTA`, aksi → `DAHA_ESKI`; geçersiz tarih `DAHA_ESKI`. Şablonda her grup için:

```vue
<div class="akis-grup" :data-test="`akis-grup-${grup.anahtar}`">
  <span class="akis-grup-baslik">{{ grup.etiket }}</span>
  <ul class="akis-liste">
    <li v-for="{ olay, index } in grup.ogeler" :key="`${olay.tur}-${index}`">
      <button :data-test="`akis-oge-${index}`" ...>
```

CSS: `.akis-grup-baslik { font-size: 10px; letter-spacing: .06em; text-transform: uppercase; color: var(--text-muted); padding: 6px 6px 2px; }` ve gruplar arası `margin-top: 6px`.

- [ ] **Step 5: Bekleyen işler kartını dengele**

`.bekleyen-nokta` yerine:

```vue
<span
  class="bekleyen-ikon"
  :class="seviyeSinifi(oge.seviye)"
  :data-test="`bekleyen-ikon-${oge.anahtar}`"
  aria-hidden="true"
>
  <i :class="seviyeIkonu(oge.seviye)" />
</span>
```

`seviyeIkonu`: KRITIK → `pi pi-exclamation-circle`, WARN → `pi pi-exclamation-triangle`, diğer → `pi pi-info-circle`. `seviyeSinifi` artık `bekleyen-ikon--kritik`, `bekleyen-ikon--uyari`, `bekleyen-ikon--bilgi` döndürür (eski `bekleyen-nokta--*` sınıfları kaldırılır). CSS:

```css
.kokpit-bekleyen { height: 100%; }
.bekleyen-liste { flex: 1 1 auto; }
.bekleyen-ikon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; flex-shrink: 0; border-radius: 9px; font-size: 12px;
}
.bekleyen-ikon--kritik { background: var(--danger-soft); color: var(--danger); }
.bekleyen-ikon--uyari { background: var(--warning-soft); color: var(--warning); }
.bekleyen-ikon--bilgi { background: var(--info-soft); color: var(--info); }
```

`Dashboard.vue` içinde `.kokpit-orta` sütunlarını `minmax(0, 2.2fr) minmax(280px, 1fr)` yap.

- [ ] **Step 6: Testleri ve i18n kontrolünü çalıştır**

```bash
npx vitest run src/components/__tests__/KokpitAkis.spec.js src/components/__tests__/KokpitBekleyenIsler.spec.js
npm run i18n:check
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KokpitAkis.vue frontend/src/components/dashboard/kokpit/KokpitBekleyenIsler.vue frontend/src/views/Dashboard.vue frontend/src/locales/tr.json frontend/src/locales/en.json frontend/src/components/__tests__/KokpitAkis.spec.js frontend/src/components/__tests__/KokpitBekleyenIsler.spec.js
git commit -m "feat(dashboard): gun gruplu akis ve dengeli bekleyen isler karti"
```

---

### Task 5: Detay kartları — eşit yükseklik, ortak başlık ve doğru durum rengi

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/FinansKarti.vue`
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/components/dashboard/kokpit/AlacakVerecekBolumu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/FaturaSiparisBolumu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/NakitBolumu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue`
- Test: `frontend/src/components/__tests__/NakitAlacakBolumleri.spec.js`, `frontend/src/components/__tests__/KarFaturaBolumleri.spec.js`

**Interfaces:**
- `FinansKarti` produces: `height: 100%`, `.fin-kart-govde { flex: 1 1 auto }`, footer dibe sabit.
- `AlacakVerecekBolumu` produces: `.alacak-performans--iyi|uyari|kotu`, `.alacak-stat-deger--iyi|uyari|kotu`.
- Eşikler: `>= 80` iyi, `>= 50` uyarı, `< 50` kötü.

- [ ] **Step 1: Başarısız testleri yaz**

`NakitAlacakBolumleri.spec.js` içine ekle:

```js
it('tahsilat oranini esige gore renklendirir', () => {
  const iyi = mount(AlacakVerecekBolumu, {
    ...mountOptions,
    props: { veri: { ...alacakVeri, tahsilatOrani: 90 }, paraBirimi: 'TRY' }
  })
  const kotu = mount(AlacakVerecekBolumu, {
    ...mountOptions,
    props: { veri: { ...alacakVeri, tahsilatOrani: 30 }, paraBirimi: 'TRY' }
  })
  expect(iyi.find('[data-test="alacak-tahsilat-performansi"]').classes()).toContain('alacak-performans--iyi')
  expect(kotu.find('[data-test="alacak-tahsilat-performansi"]').classes()).toContain('alacak-performans--kotu')
})
```

- [ ] **Step 2: Testi çalıştır ve kırmızı olduğunu doğrula**

```bash
npx vitest run src/components/__tests__/NakitAlacakBolumleri.spec.js
```

- [ ] **Step 3: Kart yüksekliğini ve footer'ı düzelt**

`FinansKarti.vue`:

```css
.fin-kart { height: 100%; padding: var(--kokpit-kart-pad); }
.fin-kart-govde { flex: 1 1 auto; }
.fin-kart-baslik { font-size: 16px; }
.fin-kart-alt-baslik { font-size: 11.5px; }
.fin-kart-alt { margin-top: auto; }
```

`Dashboard.vue`:

```css
.kokpit-bolum > * { height: 100%; }
.kokpit-bolum-baslik {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; letter-spacing: 0.04em;
}
.kokpit-bolum-baslik::before {
  content: ''; width: 16px; height: 2px; border-radius: 2px; background: var(--accent);
}
```

- [ ] **Step 4: Tahsilat eşiğini uygula**

`AlacakVerecekBolumu.vue` içinde:

```js
const tahsilatSeviyesi = computed(() => {
  const oran = tahsilatOraniDegeri.value
  if (oran >= 80) return 'iyi'
  if (oran >= 50) return 'uyari'
  return 'kotu'
})
```

Performans kapsayıcısına `:class="'alacak-performans--' + tahsilatSeviyesi"` ekle; CSS'te bar ve değer rengini `--success`, `--warning`, `--danger` üzerinden ver. Aynı sınıfı `alacak-stat-deger` için de kullan.

- [ ] **Step 5: "Tümünü gör" footer'ını sessizleştir**

Çocuk bileşenlerdeki `.finans-detay-buton` tanımlarını (`NakitBolumu.vue`, `AlacakVerecekBolumu.vue`, `KarMarjBolumu.vue`, `FaturaSiparisBolumu.vue`) şu ortak davranışa getir:

```css
.finans-detay-buton {
  align-self: flex-end;
  width: auto;
  padding: 4px 2px;
  border: 0;
  background: transparent;
  color: var(--accent);
  font-size: 11.5px;
  font-weight: 700;
}
```

`FinansKarti` footer'ı `display: flex; justify-content: flex-end;` yap; mobilde (`max-width: 600px`) buton tam genişlik ve 44 px yüksekliğe döner.

- [ ] **Step 6: Testleri çalıştır**

```bash
npx vitest run src/components/__tests__/NakitAlacakBolumleri.spec.js src/components/__tests__/KarFaturaBolumleri.spec.js
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/FinansKarti.vue frontend/src/views/Dashboard.vue frontend/src/components/dashboard/kokpit/AlacakVerecekBolumu.vue frontend/src/components/dashboard/kokpit/FaturaSiparisBolumu.vue frontend/src/components/dashboard/kokpit/NakitBolumu.vue frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue frontend/src/components/__tests__/NakitAlacakBolumleri.spec.js
git commit -m "feat(dashboard): esit yukseklikli detay kartlari ve tahsilat esigi rengi"
```

---

### Task 6: Grafikler — alan grafiği ve tutarlı palet

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/MikroGrafik.vue`
- Modify: `frontend/src/components/dashboard/kokpit/MetrikKutu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue`
- Modify: `frontend/src/components/dashboard/kokpit/NakitBolumu.vue`
- Modify: `frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue`
- Test: yeni `frontend/src/components/__tests__/MikroGrafik.spec.js`

**Interfaces:**
- `MikroGrafik` consumes: `tip` artık `'sparkline' | 'line' | 'bar' | 'alan'`; `renk` mevcut.
- `MikroGrafik` produces: `tip="alan"` için `<linearGradient>` dolgu ve son değerde `<circle>`; gradient id'si modül düzeyinde artan sayaçla benzersiz.
- `NakitBolumu` produces: toplam likidite için `tip="alan"` sparkline; `KarMarjBolumu` trend için alan grafiği.

- [ ] **Step 1: Başarısız testi yaz**

`MikroGrafik.spec.js`:

```js
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MikroGrafik from '../dashboard/kokpit/MikroGrafik.vue'

describe('MikroGrafik', () => {
  it('alan tipinde gradyan ve son nokta cizer', () => {
    const wrapper = mount(MikroGrafik, {
      props: { tip: 'alan', seriler: [{ degerler: [1, 3, 2, 5], renk: 'var(--accent)' }] }
    })
    const svg = wrapper.find('svg').html()
    expect(svg).toContain('linearGradient')
    expect(svg).toContain('<circle')
  })
})
```

- [ ] **Step 2: Testi çalıştır ve kırmızı olduğunu doğrula**

```bash
npx vitest run src/components/__tests__/MikroGrafik.spec.js
```

- [ ] **Step 3: Alan varyantını uygula**

`serilerDuz` aynı kalır. Şablonda `tip === 'alan'` için:

```vue
<defs>
  <linearGradient :id="gradyanId" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%" stop-color="currentColor" stop-opacity="0.28" />
    <stop offset="100%" stop-color="currentColor" stop-opacity="0" />
  </linearGradient>
</defs>
<polygon :points="alanNoktalari(seri.degerler)" :fill="`url(#${gradyanId})`" />
<polyline ... />
<circle :cx="100" :cy="yKoordinati(seri.degerler[seri.degerler.length - 1])" r="2.2" fill="currentColor" />
```

`gradyanId` için modül düzeyinde `let sayac = 0` ve `const gradyanId = 'mikro-alan-' + (++sayac)` kullan; Vue 3.3'te `useId` yok. `tip` validator'ına `'alan'` ekle.

- [ ] **Step 4: Kullanım yerlerini güncelle**

- `MetrikKutu`: birincil kartta `tip="alan"` (prop olarak `grafikTip` ekle, varsayılan `'sparkline'`); `FinansOzetBandi` birincil metriğe `:grafik-tip="index === 0 ? 'alan' : 'sparkline'"` geçsin.
- `NakitBolumu`: toplam likidite `MikroGrafik` çağrısına `tip="alan"`, yükseklik 40.
- `KarMarjBolumu`: trend grafiğini `tip="alan"` yap; seri renkleri `frontend/src/utils/grafikRenkleri.js` içindeki `GRAFIK.yesil`, `GRAFIK.kirmizi`, `GRAFIK.mavi` sabitlerinden gelsin (hex tekrarı yok).

- [ ] **Step 5: Testleri çalıştır**

```bash
npx vitest run src/components/__tests__/MikroGrafik.spec.js src/components/__tests__/FinansOzetBandi.spec.js src/components/__tests__/NakitAlacakBolumleri.spec.js
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/MikroGrafik.vue frontend/src/components/dashboard/kokpit/MetrikKutu.vue frontend/src/components/dashboard/kokpit/FinansOzetBandi.vue frontend/src/components/dashboard/kokpit/NakitBolumu.vue frontend/src/components/dashboard/kokpit/KarMarjBolumu.vue frontend/src/components/__tests__/MikroGrafik.spec.js
git commit -m "feat(dashboard): alan grafigi ve tutarli grafik paleti"
```

---

### Task 7: Üst bar ve sayfa ritmi

**Files:**
- Modify: `frontend/src/components/dashboard/kokpit/KokpitGlobalFiltre.vue`
- Modify: `frontend/src/views/Dashboard.vue`
- Test: `frontend/cypress/e2e/dashboard.cy.js`

**Interfaces:**
- `KokpitGlobalFiltre` produces: 22 px başlık, accent noktalı eyebrow, 30 px segment kontrol, mobilde 44 px ikon butonlar.
- `Dashboard.vue` produces: `max-width: 1760px`, masaüstü `padding: 16px 18px 28px`, mobil `10px 12px 22px`.
- `dashboard.cy.js` produces: 390 px'te yatay taşma yok assert'i.

- [ ] **Step 1: Başarısız Cypress testini yaz**

`dashboard.cy.js` içine ekle:

```js
it('390px viewportta yatay tasma olmaz', () => {
  cy.viewport(390, 844)
  cy.visit('/')
  cy.get('[data-test="dashboard-kokpit"]', { timeout: 20000 }).should('exist')
  cy.document().then((doc) => {
    expect(doc.documentElement.scrollWidth).to.be.at.most(390)
  })
})
```

Mevcut spec oturumu localStorage ile kuruyorsa aynı `beforeEach` kullanılır; ek kurulum gerekmez.

- [ ] **Step 2: Testi çalıştır ve kırmızı olduğunu doğrula**

```bash
npx cypress run --browser chrome --spec cypress/e2e/dashboard.cy.js
```

Beklenen: taşma varsa `scrollWidth` assert'i kırmızı. Taşma yoksa test yeşil başlar; Task 7'nin kalanı görsel iyileştirme olarak ilerler, testi yeşil tut.

- [ ] **Step 3: Üst bar hiyerarşisini uygula**

`KokpitGlobalFiltre.vue` CSS:

```css
.kokpit-filtre { padding: 12px 14px; border-radius: var(--radius-lg); }
.kokpit-baslik { font-size: 22px; letter-spacing: -0.025em; }
.kokpit-eyebrow { display: inline-flex; align-items: center; gap: 6px; font-size: 10.5px; }
.kokpit-eyebrow::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--accent); }
.kokpit-donem { padding: 3px; }
.kokpit-donem-buton { min-height: 30px; padding: 0 12px; font-size: 11.5px; }
.kokpit-donem-buton--aktif { background: var(--accent-soft); color: var(--accent); box-shadow: none; }
@media (max-width: 760px) {
  .kokpit-ikon-buton { width: 44px; height: 44px; }
  .kokpit-donem { overflow-x: auto; }
}
```

`sonGuncelleme` metnini 11 px yap; 1200 px altında gizleme kuralı korunur.

- [ ] **Step 4: Sayfa ritmini uygula**

`Dashboard.vue`:

```css
.kokpit-sayfa {
  max-width: 1760px;
  margin: 0 auto;
  padding: 16px 18px 28px;
  gap: var(--kokpit-bosluk);
}
.kokpit-sayfa--ferah { --kokpit-bosluk: 24px; }
@media (max-width: 600px) {
  .kokpit-sayfa { padding: 10px 12px 22px; }
}
```

`--kokpit-bosluk` token'ını `.kokpit-sayfa` içinde tanımla; `app.css`'teki genel token ile çakışmasın.

- [ ] **Step 5: Cypress ve build'i çalıştır**

```bash
npx cypress run --browser chrome --spec cypress/e2e/dashboard.cy.js
npm run build
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/dashboard/kokpit/KokpitGlobalFiltre.vue frontend/src/views/Dashboard.vue frontend/cypress/e2e/dashboard.cy.js
git commit -m "feat(dashboard): premium ust bar ve sayfa ritmi"
```

---

### Task 8: Erişilebilirlik, responsive ve final doğrulama

**Files:**
- Modify: gerektiğinde `frontend/src/assets/app.css`, ilgili kokpit bileşenleri
- Modify: `docs/superpowers/plans/2026-11-22-anasayfa-premium-cila.md` (checkbox'lar)
- Test: tüm mevcut testler + Cypress `dashboard.cy.js`

**Interfaces:**
- Consumes: önceki task'ların tüm sınıf ve prop'ları.
- Produces: 390/768/1024/1440/1920 doğrulanmış, klavye ve reduced-motion uyumlu ekran.

- [ ] **Step 1: Odak ve dokunma hedeflerini kontrol et**

Tüm yeni butonlarda `:focus-visible` var mı bak. Eksikse `outline: 2px solid var(--accent); outline-offset: 2px;` ekle. `@media (pointer: coarse)` altında hızlı işlem, bekleyen satır ve footer butonlarının en az 44 px olduğunu doğrula.

- [ ] **Step 2: Reduced-motion kontrolü**

Yeni gradyan/geçişler için:

```css
@media (prefers-reduced-motion: reduce) {
  .metrik-kutu, .hizli-islem, .akis-satir, .bekleyen-satir,
  .fin-kart, .finans-detay-buton { transition: none; }
}
```

Grafik çizimi statik SVG olduğu için ek animasyon yok; varsa kaldır.

- [ ] **Step 3: Açık/koyu tema ve kırılım kontrolü**

Sırayla 1920, 1440, 1264, 1024, 768, 390 px ve `light`/`dark` temada sayfayı aç. Şunları doğrula:

- Yatay kaydırma yok.
- KPI değerleri taşmıyor; uzun etiketler ellipsis.
- Bekleyen işler kartı akış kartıyla aynı yükseklikte.
- Detay kartları satırda eşit yükseklikte; footer'lar hizalı.
- Açık temada beyaz parlama/teal leke yok.

- [ ] **Step 4: Tam kalite kapılarını çalıştır**

```bash
cd frontend
npm run lint
npm run i18n:check
npm run test
npm run build
```

Beklenen: lint 0 uyarı, i18n temiz, 3000+ test yeşil, build başarılı.

- [ ] **Step 5: Cypress dashboard spec'ini çalıştır**

```bash
npx vite preview --port 5173 --strictPort   # ayrı terminal
npx cypress run --browser chrome --spec cypress/e2e/dashboard.cy.js
```

- [ ] **Step 6: Öncesi/sonrası ekran görüntüsünü al**

Gerçek uygulamada (Traefik `http://localhost`) geçici bir Cypress spec'i ile 1440 px viewport ve tam sayfa görüntüsü al; `docs/superpowers/specs/assets/` altına `2026-11-22-anasayfa-sonra-ust.png` ve `-tam.png` olarak kaydet. Geçici spec'i commit etme.

- [ ] **Step 7: Planı işaretle ve commit**

```bash
git add docs/superpowers/plans/2026-11-22-anasayfa-premium-cila.md docs/superpowers/specs/assets/2026-11-22-anasayfa-sonra-ust.png docs/superpowers/specs/assets/2026-11-22-anasayfa-sonra-tam.png
git commit -m "docs(dashboard): premium cila plani ve oncesi/sonrasi goruntuleri"
```

---

## Doğrulama özeti

| Kontrol | Komut / yöntem | Beklenen |
|---|---|---|
| Lint | `npm run lint` | 0 uyarı |
| i18n | `npm run i18n:check` | Temiz |
| Birim testleri | `npm run test` | Tümü yeşil |
| Build | `npm run build` | Başarılı |
| E2E dashboard | `npx cypress run --spec cypress/e2e/dashboard.cy.js` | Yeşil |
| 390 px taşma | Cypress `scrollWidth` assert'i | ≤ 390 |
| Tema | Manuel dark/light | Token'lar doğru |
| Erişilebilirlik | Klavye Tab + ekran okuyucu | Odak görünür, `sr-only` gizli |
| Görsel | 1440/390 ekran görüntüsü | Odak, hizalama, boşluklar |

## Execution Handoff

Plan tamamlandıktan sonra uygulama için iki seçenek var:

- **Subagent-driven:** Her task ayrı subagent tarafından uygulanır, ardından taze bir reviewer kontrol eder. Görsel tutarlılık ve çok dosyalı değişiklik nedeniyle en güvenli yol.
- **Native:** Tüm task'lar bu oturumda sırayla uygulanır, sonunda tek bir reviewer tüm branch'i kontrol eder. Daha hızlı; planın taşıdığı tasarım kararlarına güveniyorsanız uygun.

Öneri: **Subagent-driven**, çünkü Task 2–6 birbirinin sınıf/prop sözleşmelerini kullanıyor ve her task sonunda görsel doğrulama gerekiyor.
