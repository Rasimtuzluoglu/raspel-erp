# Hızlı Satış (POS) — Yeniden Tasarım Spesifikasyonu

Bu belge, Hızlı Satış ekranının yeniden tasarım programını tanımlar. Fazlar
sırayla sevk edilir; her faz `posYeniAkis` bayrağının (ve gerektiğinde alt
bayrakların) arkasında gelir. Eski akış bir sürüm daha korunur.

## Kilitli kararlar
- **Masaüstü-öncelikli:** klavye + barkod ana akış; mobil nadir (≤1280px tek
  sütun davranışı korunur, ekstra yatırım yok).
- **Tasarım dili korunur:** PrimeVue 4 + teal (#14b8a6); yeni görsel sistem
  yok. Mevcut `--pos-*` token katmanı ve PrimeVue bileşenleri kullanılır.
- **Bağımlılık serbest:** `@vueuse/core` (zaten mevcut) kullanılır; IndexedDB
  için tek küçük paket (`idb-keyval`) Faz F'de eklenir.
- **Kademeli sevk:** feature-flag + aşamalı; her faz bağımsız geri alınabilir.

## Hedefler
| Hedef | Ölçüt |
|---|---|
Kasa hızı | 10 kalemlik satış < 25 sn; barkod→sepete < 150 ms |
Ödeme | Ödeme açılışı→tamam < 8 sn, fare kullanmadan |
Yoğun katalog | 5.000 üründe arama < 200 ms; kaydırma 60 fps |
Çevrimdışı | Ağ yokken barkod+arama+satış; online'da otomatik senkron |
Öğrenilebilirlik | İlk gün 6 kısayol; 1 haftada tam klavye akışı |

## Yeni bilgi mimarisi
```
ÜST ŞERİT  [Komut Girişi] [Müşteri çipi] [Fis modu] [☰ sekmeler]
ÜRÜN ALANI (sol)                 SİPARİŞ RAYI (sağ)
  kategori rayı                    kalem listesi (sanal)
  sonuçlar (sanal ızgara)          toplam bloğu (sticky)
                                   [ÖDEME] tek birincil aksiyon
ÖDEME → tam ekran odaklı katman (overlay)
```
- Müşteri/teslimat sağ panelde değil, üst şeritte çip + açılır panel.
- Ödeme panel içinde değil, overlay.

## Fazlar
### Faz A — Zemin, ölçüm, flag  ▸ TAMAMLANDI
- Cypress POS sürüklenmesi onarıldı; paylaşılan `cy.posKatalogMockla(urunler)`
  komutu eklendi (filtreli/ara/gruplama-dagilimi/en-cok-satanlar/satis-onerileri
  + legacy `stoklar` tek yerden).
- `posYeniAkis` bayrağı `usePosTercih`'e eklendi (varsayılan kapalı).
- **Arama kutusu hatası BULUNDU ve DÜZELTİLDİ:** `HizliSatis.vue` arama kutusu
  `@input` ile bağlıydı; AutoComplete `input` olayı **yaymaz** (PrimeVue 4
  `emits` listesinde yok), bu yüzden binding bir DOM fallthrough dinleyicisine
  dönüşüp arama ref'ine **event nesnesi** yazıyordu. `usePosKatalog` computed'ı
  `aramaMetni.value?.trim()` çağırınca `TypeError` atıyor, güncelleme çöküyor
  (ErrorBoundary: "component update") ve **ne öneri ne ızgara araması**
  çalışıyordu. Doğru olay `@update:model-value` ile değiştirildi (yalnızca
  string metin aktarılır). Sonuç: `satis-onerileri?q=` ve `filtreli?q=`
  istekleri artık çalışıyor; `redteam-faz2` 2.5/2.6 yeniden etkin.
- Not (Faz D): ızgara araması artık her tuşta `filtreli?q=` atıyor; debounce +
  prefetch Faz D'de eklenecek.

### Faz B — Ödeme overlay (`posOdemeOverlay`)  ▸ TAMAMLANDI
`PosOdemeOverlay.vue` (PrimeVue `Dialog`) eklendi: NAKIT'te sayısal tuş takımı
(0-9/00/C/⌫ → `alinanNakit`), mevcut `PosOdemePaneli` (başlıksız, `baslikGizle`)
yöntem/taksit/kasa/banka/POS/hızlı-nakit/para-üstü alanlarıyla içeride,
footer'da "Satışı Tamamla". `posYeniAkis` açıkken sağ paneldeki katlanır ödeme
bloğu yerine **tek birincil "Ödeme Al"** aksiyonu gösterilir; satış başarılı
olunca overlay kapanır. Bayrak kapalıyken mevcut satır içi panel AYNEN çalışır.
Ödeme durumu prop'ları `$attrs` ile overlay'e ve oradan panele aktarılır
(kebab/camel her iki anahtar biçimi de desteklenir).

### Faz C — Komut yüzeyi (`posKomutKutusu`)  ▸ TAMAMLANDI
`usePosKomut.js` (saf, tam test edilmiş çözümleyici): `@ad` → müşteri,
`N*terim` / `terim*N` / `Nx terim` → miktar, aksi halde barkod/ürün arama;
miktar 1..9999'a normalize edilir.
Barkod kutusu (`globalBarkodEkle`) bu grameri destekler: `3*1234` ürünü 3 adet
ekler (`barkodTarandi(terim, adet)`), `@Ali` müşteri moduna geçip aramayı
tetikler. Böylece kasiyer barkod/ürün/miktar/müşteri için tek kutuyu kullanır.
Not: İki ayrı kutu (barkod + canlı öneri) tam olarak TEK kutuya indirilmedi;
gramer barkod kutusunda destekleniyor. Tam birleştirme istenirse ayrı iş.

### Faz D — Katalog hızı
`useVirtualList` ile sanal ızgara; `useDebounceFn` arama; prefetch; barkodda
optimistic satır. (`barkodTarandi` yerel araması POS kataloğunu da tarar.)

### Faz E — Çoklu sepet / askıya al
`PosSepetSekmeleri`; `Ctrl+H` park/aç; `usePosSepet` sepet koleksiyonu.

### Faz F — Çevrimdışı dayanıklılık
IndexedDB katalog önbelleği (`idb-keyval`); imzalı/idempotent kuyruk senkronu
(mevcut `raspel_offline_satis_kuyrugu` + Wave 0 idempotency).

### Faz G — Görsel sistem
Kart modları (Vitrin/Yoğun); varsayılan büyük tipografi; motion +
`prefers-reduced-motion`. PrimeVue+teal korunur.

### Faz H — Erişilebilirlik & rötuş
Klavye/aria tutarlılığı, i18n, dokümantasyon, tam E2E.

## Doğrulama çerçevesi
- Birim (Vitest): yeni/değişen composable'lar için kırmızı-doğrulamalı test.
- Cypress: POS spec'leri + her fazın akış spec'i.
- Kapılar: `lint` · `i18n:check` · `vitest run` · `build` · `docs-sync --check` ·
  Docker rebuild + sağlık.

## Riskler
- Eğitim yükü → ilk kullanım coach + kısayol şeridi.
- Regresyon → feature-flag + zengin Cypress.
- Backend → offline katalog için indeks/önek ucu (mevcut `filtreli` üzerine).
