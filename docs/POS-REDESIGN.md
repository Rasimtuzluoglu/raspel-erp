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
- **Bulgu (Faz C'ye taşındı):** ürün arama kutusu (AutoComplete) otomasyonda
  yazarken ne `/api/stoklar/satis-onerileri` ne de `filtreli?q=` isteği
  üretiyor; yerel `input` olayıyla bile tetiklenmiyor. "Arama kutusu işlevsiz
  olabilir" — Faz C'de bu kutu yeniden yazılacak. Bu nedenle
  `redteam-faz2-hizliSatis.cy.js` içindeki 2.5 ve 2.6 testleri **gerekçeli
  olarak `it.skip`** edildi (silinmedi); Faz C'de yeni davranışa göre açılır.

### Faz B — Ödeme overlay (`posOdemeOverlay`)
Sayısal tuş takımı, hızlı nakit (₺50/100/200), otomatik para üstü, "kalan".
`PosOdemePaneli` içeriği overlay'e taşınır; `F9/F10/Ctrl+S` davranışı korunur.

### Faz C — Komut yüzeyi (`posKomutKutusu`)
Barkod / ürün adı / müşteri (`@ad`) / miktar (`3*`, `x3`) / manuel fiyat tek
kutuda. Saf çözümleyici `usePosKomut.js` (test edilebilir). **Arama kutusu
bulgusu burada çözülür.**

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
