# Saha Portalı Revizyonu — Tasarım

Tarih: 2026-11-17
Durum: Onay bekliyor
Kapsam: `frontend/src/views/SahaPortali.vue`, yeni saha bileşenleri, tek yeni backend ucu (`/api/saha/ozet`), idempotency TTL iyileştirmesi.

## 1. Amaç ve başarı ölçütleri

Saha personelinin (telefon ağırlıklı, bağlantısı kopabilen) günlük işini tek ekranda, az dokunuşla ve veri kaybetmeden yapması.

Başarı ölçütleri:

- Açılışta **en fazla 2 API isteği** (özet + aktif sekme); bugünkü 8 istek/`size=500` kalkar.
- İlk anlamlı içerik (özet kartları) ≤ 1,5 sn (yerel ağ, cache sıcak).
- Çevrimdışıyken teslimat (imza), ziyaret, masraf, izin ve tahsilat kaydedilebilir; bağlantı gelince **tek dokunuşla veya otomatik** gönderilir.
- Aynı tahsilat iki kez yazılmaz (`X-Idempotency-Key`, uzun TTL).
- Telefonda (390 px) yatay taşma yok; tüm birincil eylem hedefleri ≥ 44 px.
- Mevcut uçlar ve diğer ekranlar bozulmaz (yalnızca ek uç ve opsiyonel header).

## 2. Kapsam dışı

- Güzergâh optimizasyonu (TSP), canlı filo takibi, çevrimdışı harita karoları.
- Çoklu sürücü atama/planlama ekranı.
- Saha portalı dışındaki ekranların yeniden tasarımı.

## 3. Kararlar (kullanıcı ile netleşen)

| Konu | Karar |
|---|---|
| Cihaz | Telefon ağırlıklı; çevrimdışı **kritik** |
| “Bugün” özeti | Teslimat durumu, Ziyaretler, Taleplerim |
| İşlevsel öncelik | Çevrimdışı kuyruk, Rota/navigasyon, İmza/foto akışı |
| Navigasyon | Adres → harita uygulaması (`geo:` denenir, yoksa Google Maps dir) |
| Kuyruk deposu | IndexedDB (localStorage fallback) |
| Para güvenliği | Tahsilat yalnız idempotency anahtarıyla kuyruğa girer |

## 4. Backend

### 4.1 `GET /api/saha/ozet`

Yetki: `ADMIN, USER, MUHASEBE, DRIVER, SAHA` (sınıf düzeyi); veri **oturum kullanıcısına** göre daraltılır.

Yanıt (`SahaOzetDTO`):

```json
{
  "tarih": "2026-11-17",
  "teslimat": {
    "bekleyen": 3,
    "yolda": 1,
    "bugunTeslimEdilen": 2,
    "geciken": 1
  },
  "ziyaret": {
    "bugun": 2,
    "sonZiyaretTarihi": "2026-11-17T14:12:00"
  },
  "talepler": {
    "bekleyenIzin": 1,
    "bekleyenMasraf": 2
  }
}
```

Kurallar:

- `teslimat`: `Teslimat.driverId = kullaniciId`, `bekleyen = durum in (BEKLEMEDE)`, `yolda = YOLDA`, `bugunTeslimEdilen = TESLIM_EDILDI ve teslimTarihi bugün`, `geciken = beklenenTeslimTarihi < bugün ve durum not in (TESLIM_EDILDI, IPTAL)`.
- Sürücü kapsamı: `driverId` **istekten alınmaz**, oturumdaki `kullaniciId` kullanılır. ADMIN/USER saha portalını açarsa kendi üzerine atanmış teslimatlar görünür.
- `ziyaret`: `sistem.notlar` içinde `kullaniciId = oturum`, `olusturmaTarihi` bugün (00:00–23:59); `sonZiyaretTarihi` aynı kümenin en yenisi.
- `talepler`: `BEKLEMEDE` durumundaki izin ve masraf/avans talepleri (kullanıcıya ait).
- Cache: `@Cacheable("sahaOzet", key = "#sirketId + ':' + #kullaniciId")`, TTL 60 sn. Mutasyonlardan sonra frontend özeti yeniden çağırır (ek `@CacheEvict` karmaşasına girilmez; 60 sn TTL yeterli).
- Repository eklemeleri (türetilmiş sorgular):
  - `TeslimatRepository`: `countBySirketIdAndDriverIdAndDurum`, `countBySirketIdAndDriverIdAndTeslimTarihiBetweenAndDurum` (LocalDateTime aralığı), `countBySirketIdAndDriverIdAndBeklenenTeslimTarihiBeforeAndDurumNotIn`.
  - `NotRepository`: `countBySirketIdAndKullaniciIdAndOlusturmaTarihiBetween`, `findTop1BySirketIdAndKullaniciIdOrderByOlusturmaTarihiDesc`.
- Dosyalar: `controller/sistem/SahaController.java`, `service/sistem/SahaOzetService.java`, `dto/sistem/SahaOzetDTO.java`.
- Testler: servis birim testleri (durum/rol/tenant), controller `@WebMvcTest` (200 + 403), boş veri senaryosu.

### 4.2 Idempotency TTL (tahsilat)

Mevcut `IdempotencyService` 10 dk TTL kullanıyor; çevrimdışı kuyruk saatler sonra gönderebilir.

- `IdempotencyService`’e `deneKilit(anahtar, Duration ttl)` ve `tamamla(anahtar, id, Duration ttl)` aşırı yüklemeleri eklenir (Redis + yerel map aynı davranış).
- `TahsilatController` tahsilat girişinde TTL = `app.saha.tahsilat-idempotency-saat:24` (varsayılan 24 saat).
- **Şema değişikliği yok.**
- Davranış: aynı anahtar 24 saat içinde tekrar gelirse ikinci kayıt oluşmaz; mevcut mesaj korunur.

## 5. Frontend mimarisi

### 5.1 Bileşenler

| Bileşen | Görev |
|---|---|
| `views/SahaPortali.vue` | Yeni düzen, sekme yönlendirme, özet/kuyruk bağlama |
| `components/saha/SahaOzetKartlari.vue` | 3 “Bugün” kartı (teslimat, ziyaret, taleplerim), tıklanınca sekme |
| `components/saha/SahaKuyrukCubugu.vue` | Bekleyen kayıt çubuğu: sayaç, tür kırılımı, “Şimdi gönder”, hata ipucu |
| `components/saha/SahaImzaSheet.vue` | Tek ekran: imza pad + teslim alan + not + foto; çevrimiçi/çevrimdışı gönderim |
| `components/saha/SahaNavigasyonButton.vue` | Adres → `geo:` / Google Maps dir; tek dokunuş |
| `components/saha/SahaTeslimatKarti.vue` | Teslimat DTO’su: durum, adres, müşteri, telefon, WhatsApp, navigasyon, imzala; “sıradaki” vurgusu |
| `composables/useSahaKuyruk.js` | IndexedDB kuyruğu: ekle/listele/sil/retry, otomatik senkron |
| `composables/useSahaOzet.js` | Özet yükleme + 60 sn tazeleme + hata durumu |
| `api/modules/saha.js` | `sahaAPI.ozet()` (index.js’e eklenir) |

Mevcut `ImzaPad` ve `GorevNotPaneli` yeniden kullanılır; `SahaSiparislerPanel` yerini `SahaTeslimatKarti` listesine bırakır (sipariş listesi tüm şirketi çektiği için kaldırılıyor; hızlı sipariş oluşturma korunur).

### 5.2 Düzen

```
[Saha başlığı: avatar · ad · çevrimiçi/çevrimdışı]      [⋮]
[Kuyruk çubuğu — yalnız kuyruk > 0]
[Bugün: Teslimat 4/7 | Ziyaret 2 | Taleplerim 3]
[Teslimatlar | Ziyaret | Talepler | Tahsilat]
[aktif sekme içeriği]
```

- Sekmeler `TabView` yerine **segmented control + `v-if`**; her sekme ilk açılışta verisini çeker, sonra 60 sn önbellek.
- Aktif sekme `localStorage`’da saklanır (mevcut davranış korunur).
- “Bugün” kartına tıklama ilgili sekmeyi açar (teslimat → Teslimatlar, ziyaret → Ziyaret, talepler → Talepler).
- Mobilde sekmeler yatay kaydırılabilir; masaüstünde içerik maksimum 1100 px ortalanır.

### 5.3 Veri yükleme

- Açılış: `sahaAPI.ozet()` + aktif sekmenin ucu (paralel).
- Teslimat sekmesi: `/api/deliveries?driverId=<oturum kullanıcısı>` (`teslimatAPI.teslimatlar(id)`); servis, sürücü rolünde yalnız kendi kayıtlarını döndürür. `size=500` tüm siparişler kalkar.
- Ziyaret sekmesi: `notAPI.getAll({ size: 30 })` (kullanıcı notları) + formdaki cari araması sunucu tarafı.
- Talepler: `personelIzinAPI.getAll`, `personelMasrafTalepAPI.getKullaniciTalepleri` (yalnız bu sekme açılınca).
- Tahsilat: `tahsilatAPI.ozet()` yalnız sekme açılınca; cari/kasa/banka seçenekleri form açılınca ve arama ile.
- Mutasyon sonrası yalnız ilgili sekme + özet tazelenir.

### 5.4 Çevrimdışı kuyruk (IndexedDB)

DB: `raspel-saha`, store `kuyruk`, `keyPath: 'id'` (autoIncrement), indeksler: `tur`, `durum`, `olusturma`.

Kayıt:

```js
{
  id: 1,
  clientKey: "uuid",          // idempotency anahtarı (tahsilat için zorunlu)
  tur: "TESLIMAT" | "ZIYARET" | "MASRAF" | "IZIN" | "TAHSILAT",
  payload: { ... },           // ekran formunun JSON hali
  dosyalar: [{ ad, mime, dataUrl }], // imza/foto (IndexedDB)
  durum: "BEKLIYOR" | "DENENIYOR" | "HATA",
  deneme: 0,
  sonHata: null,
  olusturma: "2026-11-17T15:00:00Z"
}
```

Senkron kuralları:

- Tetikleyiciler: `online` olayı, uygulama açılışı (kuyruk doluysa), 60 sn’lik zamanlayıcı (yalnız kuyruk varken), kullanıcı “Şimdi gönder”.
- Sıralı gönderim; başarılı kayıt silinir, hata `deneme++` ve `sonHata` ile kalır.
- 4xx (doğrulama) → `HATA`, otomatik denenmez; kullanıcı düzenleyip yeniden gönderebilir veya siler.
- Ağ/timeout → `BEKLIYOR` (sonraki turda yeniden denenir).
- Eşzamanlı senkron engeli: tek `senkronizeEdiliyor` bayrağı + `navigator.locks` varsa kilit.
- Kota/IndexedDB yoksa localStorage fallback (maks 10 kayıt; aşılırsa kullanıcıya “cihazda yer yok” uyarısı).
- **Eski kuyruk taşıma:** mevcut `raspel_saha_kuyrugu` (localStorage, `NOT`/`TESLIMAT` kayıtları) ilk açılışta yeni şemaya eşlenip IndexedDB’ye aktarılır ve eski anahtar silinir. Eşleme: `NOT → ZIYARET`, `TESLIMAT → TESLIMAT`; `imzaDataUrl` `dosyalar[0]`’a taşınır.
- Tahsilat kaydı `clientKey`’i `X-Idempotency-Key` olarak gönderir (mevcut `tahsilatAPI.gir(data, key)`).
- Teslimat: `teslimatAPI.teslimEt(teslimatId, govde, imzaDosyasi)` (teslimat kaydı üzerinden; sipariş durumu sunucuda senkronlanır); imza dataURL’den `File` üretilir. Kuyrukta `payload.teslimatId` saklanır.
- Masraf: önce `create`, sonra `belgeAPI.yukle('MasrafTalep', id, foto)`; iki adımdan biri başarısızsa kayıt “kısmi” işaretlenir ve yeniden denenir (belge zaten yüklüyse atlanır).
- Ziyaret: `notAPI.create` + (`cariHesapId` varsa) `belgeAPI.yukle`.
- İzin: `personelIzinAPI.create`.

Veri kaybı riski: aynı ziyaret notu, yanıt kaybı + yeniden deneme durumunda iki kez oluşabilir. Kabul: notlar idempotent değil; kuyruk yalnız ağ hatasında yeniden dener ve kullanıcıya “tekrar gönderildi” bilgisi verilir. Para hareketlerinde bu risk idempotency ile kapalıdır.

### 5.5 İmza/foto akışı

- Teslimat kartı → “İmzala ve teslim et” → `SahaImzaSheet` (Dialog, mobilde tam ekran).
- Alanlar: teslim alan adı (zorunlu), not (opsiyonel), imza (zorunlu), foto (opsiyonel).
- Aksiyonlar: **Gönder** (çevrimiçi) / **Kuyruğa al** (çevrimdışı veya gönderim hatası). Tek onay; ardından kart durumu iyimser güncellenir, hata olursa geri alınır.
- Erişilebilirlik: `aria-live` durum mesajları, 44 px hedefler, imza pad klavye alternatifi (ad-soyad + onay kutusu) — mevcut `ImzaPad` davranışı korunur.

### 5.6 Navigasyon ve rota

- Her teslimat kartında “Yol tarifi”: `https://www.google.com/maps/dir/?api=1&destination=<adres>`; mobilde önce `geo:0,0?q=<adres>` denenir (harita uygulaması yoksa web’e düşer).
- Kullanıcı kartları sürükleyerek sıralar; sıra `localStorage` (`raspel_saha_teslimat_sirasi`) ile cihazda saklanır.
- İlk kart “Sıradaki teslimat” olarak vurgulanır; teslim edilince sıradaki otomatik öne geçer.
- Adres yoksa navigasyon düğmesi gizlenir.

## 6. Hata yönetimi ve bildirimler

- Özet alınamazsa: kartlar iskelet + “Tekrar dene”; sekmeler çalışmaya devam eder.
- Sekme verisi alınamazsa: ilgili sekmede boş/hata durumu, diğer sekmeler etkilenmez.
- Kuyruk gönderiminde kısmi başarı: “2 gönderildi, 1 hata” tek toast.
- Tüm hata mesajları `GlobalExceptionHandler` gövdesinden okunur; ham hata gösterilmez.

## 7. i18n

Yeni anahtarlar (tr/en): `sahaPortali.bugun`, `bugunTeslimat`, `bugunZiyaret`, `bugunTalepler`, `kuyrukBaslik`, `kuyrukSimdiGonder`, `kuyrukHata`, `kuyrukSil`, `kuyrukYenidenDene`, `imzaTeslimBaslik`, `imzaFotoOpsiyonel`, `navigasyon`, `siralamaIpucu`, `cevrimdisiKaydedildi`, `gorevYuklenemedi`, `ozetYuklenemedi`.
Mevcut `sahaPortali.*` anahtarları korunur.

## 8. Test planı

Backend:

- `SahaOzetServiceTest`: driver kapsamı, durum sayıları, bugün filtresi, boş veri, tenant.
- `SahaControllerTest`: 200 + JSON alanları; USER/SAHA erişimi; yetkisiz 403.
- `TahsilatController`/`IdempotencyService`: aynı anahtar 24 saat içinde ikinci kez → çift kayıt yok; TTL aşımı sonrası yeni kayıt.

Frontend (Vitest):

- `useSahaKuyruk`: ekle/listele, başarılı senkron siler, hata kaydı korur, 4xx → HATA, fallback localStorage.
- `SahaOzetKartlari`: 3 kart, tıklama emit, yükleniyor/hata.
- `SahaKuyrukCubugu`: sayaç/tür kırılımı, gönder tetikleme.
- `SahaImzaSheet`: zorunlu alan doğrulaması, çevrimdışı → kuyruk emit.
- `SahaPortali`: açılışta 2 istek, sekme lazy, özet tıklama yönlendirmesi, mobil düzen.

Cypress (dashboard benzeri mock):

- 390 px: sekmeler, özet kartları, kuyruk çubuğu, yatay taşma yok.
- Çevrimdışı: `cy.intercept` teslimat/tahsilat hata → kuyruk oluşur; `cy.window().trigger('online')` → kuyruk gönderilir ve çubuk kaybolur.
- Navigasyon düğmesi doğru URL’yi üretir (tıklama stub’lanır).
- İmza sheet: tek ekranda gönderim.

## 9. Yayınlama / geri alma

- Backend önce deploy edilir (yalnız ek uç + TTL değişimi; mevcut davranış bozulmaz).
- Frontend arkasında feature flag yok; eski sayfa tek dosya olduğu için geri alma `git revert` ile yapılır.
- Kuyruk verisi cihazda kalır; geri alma sonrası eski sürüm localStorage kuyruğunu okumaya devam eder (IndexedDB kuyruğu yeni sürümde işlenir; eski sürüm görmez — geri almada bilinen sınır).

## 10. Riskler

| Risk | Etki | Önlem |
|---|---|---|
| Ziyaret notu mükerrer gönderimi | Düşük (veri kirliliği) | Ağ hatasında yeniden dene + kullanıcıya bilgi; para dışı kabul |
| IndexedDB kotası | Orta | dataURL boyut sınırı (imza ≤ 300 KB, foto ≤ 1,5 MB), kota hatasında kullanıcı uyarısı |
| Sürücü özetinin şirket geneliyle karışması | Yüksek (yanlış sayı) | Uç yalnız `driverId = oturum kullanıcısı`; testte başka sürücü verisiyle doğrulanır |
| Idempotency TTL sonrası tekrar | Orta | TTL 24 saat; kuyruk aynı anahtarı korur; 24 saatten eski kayıt için kullanıcıya “manuel kontrol” uyarısı |
| Eski tarayıcı (IndexedDB yok) | Düşük | localStorage fallback |

## 11. Onay kriterleri

- [ ] Özet ucu ve testleri yeşil.
- [ ] Açılışta ≤ 2 istek (ağ panelinde doğrulandı).
- [ ] Çevrimdışı 5 tür kayıt kuyruğa girer; online olunca gönderilir.
- [ ] Tahsilat çift kayıt üretmez.
- [ ] Mobil 390 px’te taşma yok; tüm birincil eylemler ≥ 44 px.
- [ ] Mevcut Vitest/Cypress paketleri yeşil; yeni testler eklendi.
