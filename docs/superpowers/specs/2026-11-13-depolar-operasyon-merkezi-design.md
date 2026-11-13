# Depolar Operasyon Merkezi — Tasarım

Tarih: 2026-11-13
Durum: Onaylandı (uygulama planı bekliyor)

## 1. Amaç

Depolar ekranını; depo sorumlusu, yönetici ve saha/satış ekiplerinin ortak
kullandığı bir **depo operasyon merkezine** dönüştürmek. Ekran; depo durumunu,
kritik stokları, transfer akışını ve hareket geçmişini tek yerden görünür
kılmalı; stok giriş/çıkışı ve transfer onayını hızlı ve denetlenebilir hale
getirmelidir.

### Başarı ölçütleri

- 500 depo / 50.000 depo-stok satırında liste ve arama **< 2 sn** (sayfalı).
- Depo sorumlusu stok giriş/çıkışını **3 tıkta** tamamlar; negatif, stok aşımı
  ve aynı depo transferi **sunucuda da** reddedilir.
- Geciken transfer talepleri tek ekranda görünür; onay/red **kim tarafından,
  ne zaman** kaydedilir.
- Mobilde kritik aksiyonlar 44 px dokunma hedefiyle çalışır.

## 2. Kapsam

### Dahil

- Depo listesinde sunucu taraflı arama/filtre/sayfalama; kart ve tablo görünümü.
- Tıklanabilir KPI şeridi (depo, kalem, sıfır/kritik stok, geciken talep).
- Depo stok paneli: filtre/sayfalama, kritik/sıfır görünümü, stok değeri.
- Stok ekle/çıkar: yetki kontrolü, doğrulama, işlem sonrası önizleme.
- Transfer talepleri: durum sekmeleri, SLA yaşı, onay/red (gerekçeli), denetim.
- Stok hareket geçmişi (depo ve ürün bazlı).
- Depo detay (360) diyaloğunun zenginleştirilmesi.
- CSV dışa aktarma (liste / stok / transfer).
- Mobil uyum ve erişilebilirlik.

### Hariç

- Barkodlu stok sayımı akışının yeniden yazımı.
- Rota optimizasyonu / sevkiyat planlama.
- Maliyet muhasebesi motoru değişikliği.
- Depo kapasitesinin hacim/ağırlık olarak ölçülmesi.

## 3. Ürün kararları

1. **Kapasite = SKU (kalem) kapasitesi.** `Depo.kapasiteKalem` opsiyoneldir.
   Doluluk % = depodaki farklı stok kalemi / kapasite. Kapasite girilmemişse
   doluluk gösterilmez.
2. **SLA = 24 saat** (konfigüre edilebilir: `app.depo.transfer-sla-saat`).
   Kovalar: `NORMAL < 4s`, `YAKLASIYOR 4–24s`, `GECIKTI > 24s`.
3. **Yetki matrisi**
   | İşlem | ADMIN | Depo sorumlusu | Diğer |
   |---|---|---|---|
   | Görüntüleme | ✓ | ✓ | ✓ |
   | Stok ekle/çıkar | ✓ | ✓ (yalnız kendi deposu) | ✗ |
   | Transfer talebi oluştur | ✓ | ✓ | ✓ |
   | Transfer onay/red | ✓ | ✗ | ✗ |
   | Depo oluştur/düzenle/sil | ✓ | ✗ | ✗ |
   | CSV indir | ✓ | ✓ | ✓ |
4. **Denetim:** transferde onaylayan/reddeden kullanıcı, onay/red tarihi ve red
   sebebi saklanır. Stok hareketinde manuel depo işlemi/transfer için kullanıcı
   yazılır; diğer kaynaklarda `NULL` kalır ve arayüzde “—” gösterilir.
5. **Geriye dönük uyumluluk:** Mevcut `GET /api/depolar` ve
   `GET /api/depolar/{id}/stoklar` **dizi** dönmeye devam eder. Sayfalı uçlar
   ayrı eklenir; Stoklar, İrsaliyeler, Fatura formu ve StokSeriler etkilenmez.

## 4. Veri modeli (Flyway V183)

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

Kurallar:
- `kullanici_id` yalnız manuel depo işlemi/transferde doldurulur; diğer
  kaynaklarda `NULL` kalır ve arayüzde “—” gösterilir.
- `sorumlu_kullanici_id` yalnız ADMIN tarafından atanır; atanmamışsa işlem
  yetkisi ADMIN’e aittir.
- Yeni kolonlar nullable; mevcut kayıtlar etkilenmez.

## 5. API sözleşmesi

### 5.1 Yeni uçlar

| Metot | Yol | Parametreler | Dönüş |
|---|---|---|---|
| GET | `/api/depolar/sayfali` | `q, subeId, aktif, kritik, page, size` | `Page<DepoDTO>` |
| GET | `/api/depolar/{id}/stoklar/sayfali` | `q, kritik, sifir, page, size` | `Page<DepoStokDTO>` |
| GET | `/api/depolar/{id}/hareketler` | `tip, page, size` | `Page<DepoHareketDTO>` |
| GET | `/api/depolar/{id}/ozet` | — | `DepoOzetDTO` |
| GET | `/api/depo-transferler/sayfali` | `depoId, durum, baslangic, bitis, page, size` | `Page<DepoTransferDTO>` |
| POST | `/api/depo-transferler/{id}/onayla` | — | `DepoTransferDTO` |
| POST | `/api/depo-transferler/{id}/reddet` | gövde: `{ sebep }` | `DepoTransferDTO` |
| GET | `/api/depolar/export` | `tur=liste\|stok\|transfer, ...filtreler` | `text/csv` (UTF-8 BOM) |

### 5.2 Değişen uçlar

- `POST /api/depolar/{id}/stok-ekle` ve `/stok-cikar`
  - Yetki: ADMIN veya depo sorumlusu.
  - Doğrulama: `miktar > 0`, stok şirkete ait, çıkışta yeterli miktar.
  - Hareket kaydına `kullanici_id` yazılır.
- `POST /api/depo-transferler/{id}/onayla` ve `/reddet`
  - Kullanıcı kimliği servise geçirilir; onaylayan/reddeden kaydedilir.
  - Red sebebi zorunlu (en az 3 karakter).
- `POST /api/depolar` ve `PUT /api/depolar/{id}`
  - `kapasiteKalem` (>= 0) ve `sorumluKullaniciId` alanları eklenir (ADMIN).

### 5.3 Hesaplama kuralları

- `kritik` filtresi: depoda **en az bir** stokta `miktar <= minMiktar`
  (`minMiktar` null ise kritik sayılmaz).
- `sifir` filtresi: `miktar <= 0`.
- `stokDegeri` = Σ(`miktar` × (`ortalamaMaliyet` ?? `fiyat`)); ikisi de yoksa 0.
- `dolulukYuzde` = `round(kalemSayisi / kapasiteKalem * 100)`;
  `kapasiteKalem` null veya 0 ise `null` (arayüzde gizlenir).
- Sayfalama varsayılanı `size=20`, üst sınır 100; `page` 0 tabanlıdır.
- CSV: `tur=stok` için `depoId` zorunludur; `tur=liste` ve `tur=transfer`
  mevcut filtreleri kullanır. Ayırıcı `;`, kodlama UTF-8 BOM.
- `onaylayan_kullanici_id`, transferi **onaylayan veya reddeden** ADMIN’i tutar.
- DRIVER rolü kapsam dışıdır; saha kullanıcıları mevcut `USER` rolüyle çalışır.

### 5.4 DTO alanları

- `DepoDTO`: `id, ad, adres, yetkili, subeId, subeAdi, aktif, kapasiteKalem,
  sorumluKullaniciId, sorumluKullaniciAd, kalemSayisi, sifirStokSayisi,
  kritikStokSayisi, stokDegeri, dolulukYuzde, kritikMi`
  - `kritikMi`: depoda en az bir kritik stok varsa `true` (liste filtresiyle aynı).
- `DepoStokDTO`: `stokId, stokKodu, stokAd, birim, miktar, minMiktar,
  ortalamaMaliyet, deger, kritikMi`
- `DepoHareketDTO`: `id, tarih, tur, kaynakTip, stokId, stokKod, stokAd,
  miktar, kullaniciAd, aciklama`
- `DepoTransferDTO`: mevcut alanlar + `kaynakDepoAd, hedefDepoAd, stokAd,
  olusturanKullaniciAd, onaylayanKullaniciAd, yasSaat, slaDurumu, redSebebi`

## 6. Servis katmanı

- `DepoService.sayfali(filtre, pageable)`
  - Filtreler: `q` (ad/yetkili, case-insensitive), `subeId`, `aktif`, `kritik`.
  - Sayfa başına sabit sorgu: depo sayfası + tek gruplu kalem/sıfır/kritik/
    değer toplamları (N+1 yok).
- `DepoStokService`
  - `sayfali(depoId, filtre, pageable)`: `Stok` join’i ile min stok/kritik/değer.
  - `hareketler(depoId, tip, pageable)`: `stok_hareket` + `Stok` join’i.
  - `stokEkle/Cikar(depoId, stokId, miktar, kullaniciId)`: doğrulama + hareket.
- `DepoTransferService`
  - `sayfali(filtre, pageable)`, `onayla(id, kullaniciId)`,
    `reddet(id, kullaniciId, sebep)`.
  - `slaDurumu(olusturmaTarihi)` konfigürasyondan hesaplanır.
- `DepoYetki` yardımcı sınıfı: `islemYapabilir(depo, kullanici)`.
- `DepoExportService`: CSV üretimi (UTF-8 BOM, `;` ayırıcı, başlık satırı).

## 7. Frontend mimarisi

`views/Depolar.vue` yalnız orkestrasyon ve sekmeleri tutar; iş parçaları ayrılır:

```
views/Depolar.vue
components/depo/
  DepoOzetKartlari.vue      # tıklanabilir KPI'lar
  DepoListePaneli.vue       # arama/filtre/sayfalama, tablo/kart
  DepoStokPaneli.vue        # stok listesi + ekle/çıkar + hareket kısayolu
  DepoTransferPaneli.vue    # durum sekmeleri, SLA, onay/red
  DepoHareketPaneli.vue     # hareket geçmişi
  DepoFormDialog.vue        # oluştur/düzenle (kapasite + sorumlu)
stores/depoStore.js         # liste/özet/filtre/sayfalama durumu
composables/useDepoYetki.js # rol + sorumluluk kontrolü
```

- `depoAPI` ve `depoTransferAPI`'ye sayfalı uçlar eklenir; mevcut metotlar korunur.
- Arama 300 ms debounce; sayfa boyutu 20/50/100; filtreler store’da tutulur.
- KPI tıklaması ilgili sekmeyi açar ve filtreyi uygular.
- Boş/hata/yetkisiz durumlar ayrı ayrı ele alınır; hata durumunda “Yeniden dene”.
- i18n: tüm yeni metinler `depolar.*` altında tr/en.
- Erişilebilirlik: `aria-label`, `aria-live` bildirim, klavye ile sekme/aksiyon,
  modal odak tuzağı (mevcut Dialog davranışı).

## 8. Performans

- Tüm listeler sunucu taraflı sayfalanır; istemci yalnız sayfayı tutar.
- Depo KPI’ları tek gruplu sorguyla hesaplanır.
- `q` araması indekslenebilir alanlarda (`ad`, `yetkili`) `lower(...) LIKE`.
- 50k satır için hedef: liste ucu p95 < 500 ms (yerel), arayüz < 2 sn.

## 9. Test planı

### Backend
- `DepoServiceTest`: arama/filtre/sayfalama, kritik bayrağı, doluluk hesabı.
- `DepoStokServiceTest`: min stok/kritik/değer, ekle/çıkarma doğrulamaları,
  yetki reddi, hareket kaydı.
- `DepoTransferServiceTest`: filtre, SLA kovaları, onay/red denetimi,
  red sebebi zorunluluğu, aynı depo reddi.
- Repository H2 testleri: gruplu toplamlar, sayfalama.
- `mvn verify` + JaCoCo kapısı.

### Frontend
- Vitest: `depoStore` (filtre/sayfalama), `useDepoYetki` (rol matrisi).
- Kaynak sözleşme testleri: yeni uçlar, yetki koşulları, CSV, SLA etiketleri.
- Cypress: liste filtreleme, stok giriş/çıkış engeli, transfer oluştur →
  onayla/reddet, hareket geçmişi, mobil görünüm.
- `npm run lint`, `i18n:check`, `test`, `build`.

## 10. Yayın ve geri dönüş

1. **Backend:** V183 + yeni uçlar; mevcut uçlar değişmez. Docker backend rebuild.
2. **Frontend:** yeni bileşenler + store; `Depolar.vue` geçişi. Docker frontend rebuild.
3. **Doğrulama:** tam testler, Cypress (preview), mobil manuel kontrol.
4. **Geri dönüş:** frontend commit’i geri alınır; backend eklemeleri nullable ve
   yeni uçlar olduğu için eski arayüzle uyumludur (migration geri alınmaz).

## 11. Riskler

| Risk | Önlem |
|---|---|
| Kapasite birimi belirsizliği | SKU kapasitesi, opsiyonel; girilmezse doluluk gizli |
| SLA süresi iş birimine uymaması | Konfigüre edilebilir varsayılan 24s |
| Hareketlerde kullanıcı boşluğu | Yalnız manuel/transferde doldurulur, “—” gösterilir |
| Yetki karmaşası | Matris + sunucu tarafı kontrol; sorumlu yoksa ADMIN |
| Büyük veri | Sunucu sayfalama + gruplu sorgular + indeksler |

## 12. Tamamlanma tanımı

- Tüm yeni uçlar ve ekranlar çalışıyor; mevcut ekranlar bozulmamış.
- Yetki matrisi sunucuda uygulanıyor; yetkisiz işlem 403/İş kuralı hatası veriyor.
- Transfer onay/red ve stok işlemleri denetim bilgisiyle kaydediliyor.
- tr/en i18n tam; lint/i18n/test/build temiz; Cypress senaryoları geçiyor.
- Docker imajları yeniden derlenmiş ve sağlıklı.
