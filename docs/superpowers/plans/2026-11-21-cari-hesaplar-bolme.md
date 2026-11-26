# CariHesaplar.vue Bölme Planı

**Durum:** Taslak (uygulama ayrı oturumda, dilim dilim)
**Hedef dosya:** `frontend/src/views/CariHesaplar.vue` — 3859 satır (şablon 1–1690, script 1691–3023, stil 3024–3859)
**Amaç:** Davranış ve DOM sözleşmesi **birebir korunarak** bakımı kolay bileşenlere ayırmak. Hedef: view ≤ ~1200 satır.

## Kısıtlar

- Mevcut 6 test dosyası (`CariHesaplar-faz0/1/2/4/5.spec.js`, `CariHesaplar-menu.spec.js`) view’ı mount edip iç DOM’u arıyor:
  `.ph`, `.cari-filtreler`, `.cari-arama`, `.islem-menu`, `.dialog-form`, `.ekstre-hizli`, `.hareket-baslik`, `.adres-form`, `.bakiye-rozet`.
  → Bileşenlere taşınan DOM **aynı sınıflarla** render edilmeli; testler mount noktasını değiştirmemeli.
- Scoped CSS: taşınan markup’ın stilleri de ilgili bileşene taşınmalı (parent scoped stil child’a işlemez).
- Her dilim ayrı commit; dilim sonunda: hedefli spec’ler + tam Vitest + lint + i18n + build + Cypress `cari-hesaplar.cy.js`.

## Dilimler

> **Durum:** 1a tamamlandı (2026-11-21): başlık + bakiye özeti + ekstre aracı
> `components/cari/CariHareketBasligi.vue` bileşenine taşındı (parent 3859 → 3792 satır).
> Not: Otomatik/metin tabanlı taşıma denemeleri mojibake yorumlar ve çapraz bağlar
> yüzünden kırıldı; bundan sonraki dilimler **elle** ve küçük adımlarla yapılmalı.
>
> **Dilim 2 tamamlandı (2026-11-26):** Form dialogu
> `components/cari/CariHesapFormDialog.vue` bileşenine taşındı; parent
> 3342 → 2694 satır. `.dialog-form`, `ad/iban/krediLimiti/temsilciId` DOM
> sözleşmesi korundu. Yeni/duzenle/kopya akışları `cariId` + `baslangicVerisi`
> prop'larıyla sürüyor; 6 yeni bileşen testi eklendi.
>
> **Dilim 3 tamamlandı (2026-11-26):** Arama + filtre paneli + aktif filtre
> çipleri `components/cari/CariFiltreCubugu.vue` bileşenine taşındı; parent
> 2694 → 2543 satır. `.cari-filtreler`, `.cari-arama` ve
> `data-test="cari-aktif-filtreler"` korundu; state `v-model`/emit ile bağlı.
> 7 yeni bileşen testi eklendi; kaynak-tabanlı iki denetim yeni bileşene
> yönlendirildi.

### 1. `CariKartDialog.vue` (en büyük kazanç, ~405 şablon + ~500 script)
Mevcut 896–1300 arası “Hareketler” diyaloğu aslında **360 kart**: özet + ekstre aracı + hareket tablosu + 4 sekme (faturalar, özel fiyatlar, ürün geçmişi, notlar).

- **Props:** `visible`, `cari` (`{ id, ad }`), `hareketler`, `hareketlerYukleniyor`, `bakiyeOzeti` (`{ tahsilat, odeme, borclandirma, guncelBakiye }`)
- **Emits:** `update:visible`, `ekstre-yazdir({ baslangic, bitis })`, `fatura-detay(fatura)`, `tahsilat(cari)`, `yenilendi`
- **İçeride kalacak:** ekstre tarih aralığı + hızlı aralık, sekmeler, özel fiyat ekleme/silme, not ekleme/silme, ürün fiyat geçmişi görünümü, `hareketBadgeSinifi`/`hareketTuruEtiketi`
- **Parent’ta kalacak:** `cariHareketler` yüklemesi (tek uç), `cariEkstreYazdir` (yazdırma servisi), `faturaDetayAc`, `tahsilatAc`, `seciliCari`
- **Taşınacak CSS:** `.hareket-*`, `.ekstre-*`, `.cari-not*`, `.cari-ozel-fiyat*`, `.cari-urun*`, `.fg-*`, `.ozel-fiyat-*`, `.badge`, `.fatura-link`, `.positive/.negative` (bu son ikisi paylaşılıyorsa `ui-kit.css`’e genel yardımcı olarak alınır)

### 2. `CariHesapFormDialog.vue` (~368 şablon + form script’i)
- **Props:** `visible`, `cariId` (null → yeni)
- **Emits:** `update:visible`, `kaydedildi(cari)`
- **İçeride:** form state + doğrulama + kaydetme + adres/yetkili/kredi bölümleri + temsilci seçimi
- **DOM sözleşmesi:** `.dialog-form`, `id="ad|iban|krediLimiti|temsilciId"` aynen korunur (faz2 spec’i bunları arıyor)

### 3. `CariFiltreCubugu.vue` (~120 şablon)
Arama + filtre paneli + görünüm/density seçici. `.cari-filtreler`, `.cari-arama` sınıfları korunur; state `v-model` ile.

### 4. `CariKpiBandi.vue` (~90 şablon)
KPI kartları + bakiye rozet mantığı. `.bakiye-rozet.borc/.alacak` sınıfları korunur (faz0 spec’i).

### 5. Kalan view
Liste tablosu (kolon görünürlüğü, seçim, satır eylemleri) + mobil kart listesi + batch bar + diğer diyalogların ince kabukları.

## Test stratejisi

- **Karakterizasyon:** Taşımadan önce mevcut spec’ler yeşil olduğu doğrulanır (davranış kilidi).
- **Yeni bileşen testleri:** `CariKartDialog` için: sekmelerin render’ı, ekstre hızlı aralık emit’i, not/özel fiyat emit’leri, boş durum.
- **Regresyon:** Her dilim sonunda tam paket (Vitest 2978+) ve Cypress `cari-hesaplar.cy.js` + `fatura.cy.js`.
- **Görsel:** 1440 px ve 390 px, açık/koyu tema; kart diyaloğunda taşma kontrolü.

## Riskler ve önlemler

| Risk | Önlem |
|---|---|
| Scoped CSS taşınmaması → görsel bozulma | Dilim başına sınıf listesi çıkarılıp CSS taşınır; dilim sonunda görsel kontrol |
| Source-based testlerin kırılması | `.ph`, `.cari-*`, `.dialog-form` sınıfları ve `id`’ler korunur |
| Parent-child bağlantı sayısı (20+ prop) | Önce props/emits sözleşmesi yazılır, sonra taşınır; veri yükleme parent’ta kalır |
| `faturaDetayAc` / yazdırma bağımlılığı | Emit ile parent’a delege; child API çağırmaz |
| Aynı anda çok dilim | Her dilim ayrı commit; kırmızıysa geri alınabilir |

## Sıra

1 → 2 → 3 → 4 → 5. İlk dilim en büyük kazanç; ikinci dilim en riskli (form) olduğu için testleri genişletilir.
