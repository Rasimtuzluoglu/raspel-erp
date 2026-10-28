# Kategoriler Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-28
**Kapsam:** Tüm kategori türlerinin tek merkezde yönetimi (Kasa gelir/gider, stok, masraf, bütçe), hiyerarşi, arşiv, birleştirme
**Birincil kullanıcı:** Yönetici / muhasebe
**Durum:** Tasarım onaylandı

## 1. Amaç

Dağınık kategori tanımlarını (kasa gelir/gider, stok, masraf, bütçe) tek bir registry'de toplamak; arama, filtre, sayfalama, 2 seviyeli hiyerarşi, renk/ikon, arşivleme, kullanım sayısı ve güvenli birleştirme sağlamak. Mevcut kayıtlar ve raporlar bozulmadan çalışmaya devam eder.

## 2. Veri modeli (V173)

### 2.1 `sistem.kategori`
| Kolon | Tip | Not |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `sirket_id` | BIGINT NOT NULL | tenant |
| `kapsam` | VARCHAR(20) NOT NULL | `KASA_GELIR, KASA_GIDER, STOK, MASRAF, BUTCE` |
| `ad` | VARCHAR(100) NOT NULL | `MASRAF`/`BUTCE` kapsamında büyük harfe normalize |
| `ust_kategori_id` | BIGINT NULL | self-FK, `ON DELETE SET NULL`, **en fazla 2 seviye** |
| `renk` | VARCHAR(20) NULL | sabit paletten hex |
| `ikon` | VARCHAR(60) NULL | `pi pi-*` |
| `sira` | INT NOT NULL DEFAULT 0 | |
| `aktif` | BOOLEAN NOT NULL DEFAULT TRUE | arşiv = false |
| `aciklama` | VARCHAR(255) NULL | |
| `olusturma_tarihi` | TIMESTAMP NOT NULL DEFAULT NOW() | |
| `guncelleme_tarihi` | TIMESTAMP NULL | |

İndeksler:
- `UNIQUE (sirket_id, kapsam, lower(ad))`
- `(sirket_id, kapsam, aktif, sira)`
- `(ust_kategori_id)`

### 2.2 Bağlantı kolonları
`kategori_id BIGINT NULL` + indeks:
- `stok.stok` → kapsam `STOK`
- `finans.masraf` → kapsam `MASRAF`
- `finans.butce` → kapsam `BUTCE`
- `ticaret.iskonto_kurali` → kapsam `STOK`
- `muhasebe.kasa_hareket.kategori_id` mevcut kolon **yeni tabloya** yönlendirilir (id'ler korunur).

Legacy string kolonlar (`stok.kategori`, `finans.masraf.kategori`, `finans.butce.kategori`, `ticaret.iskonto_kurali.kategori`) **korunur** ve registry ile senkron tutulur.

### 2.3 Backfill
1. `sistem.gelir_gider_kategori` satırları **id'leri korunarak** `sistem.kategori`ya taşınır (`GELIR → KASA_GELIR`, `GIDER → KASA_GIDER`), sequence `setval` ile hizalanır, eski tablo düşürülür.
2. `stok.stok` distinct adlar → `STOK`; `finans.masraf` → `MASRAF`; `finans.butce` → `BUTCE`; `ticaret.iskonto_kurali` → `STOK`.
3. Boş/null adlar atlanır. Aynı kapsamda case-insensitive aynı ad tek registry satırına bağlanır.
4. `MASRAF` ve `BUTCE` adları `UPPER(TRIM(...))` ile normalize edilir (mevcut masraf davranışı ve bütçe–masraf eşleşmesi korunur); legacy kolonlar da normalize edilir.
5. Backfill idempotent; `ON CONFLICT DO NOTHING` + koşullu `UPDATE ... WHERE kategori_id IS NULL`.

## 3. API (`/api/kategoriler`)

| Uç | Yetki | Açıklama |
|---|---|---|
| `GET /?kapsam&q&aktif&ustKategoriId&page&size&sort` | ADMIN/USER | sayfalı düz liste |
| `GET /agac?kapsam&q&aktif` | ADMIN/USER | 2 seviyeli ağaç + kullanım sayıları |
| `GET /ozet?kapsam` | ADMIN/USER | `{toplam, aktif, pasif, altKategori}` |
| `GET /secenekler?kapsam` | ADMIN/USER | aktif, üst kategoriye göre gruplu dropdown |
| `GET /{id}` | ADMIN/USER | detay |
| `GET /{id}/kullanim` | ADMIN/USER | `{kategoriId, ad, toplam, detaylar[{modul, adet}]}` |
| `POST /` | ADMIN/USER | oluştur (ad tekrarı, 2 seviye, kapsam) |
| `PUT /{id}` | ADMIN/USER | güncelle; ad değişimi legacy kolonlara yayılır |
| `DELETE /{id}` | ADMIN | yalnız kullanım 0 ve alt kategori yoksa |
| `POST /{id}/arsivle` | ADMIN | `aktif=false`; aktif alt kategori varsa 400 |
| `POST /{id}/aktiflestir` | ADMIN | `aktif=true`; pasif üst kategori varsa 400 |
| `POST /birlestir` | ADMIN | `{kaynakId, hedefId}` → bağlar hedefe, kaynak arşiv |
| `GET /sistem-kategorileri` | ADMIN/USER | personel masraf kodları (salt-okunur) |

**DTO'lar**
- `KategoriDTO`: `id, kapsam, ad, ustKategoriId, ustKategoriAd, renk, ikon, sira, aktif, aciklama, altKategoriSayisi, kullanimSayisi, olusturmaTarihi, guncellemeTarihi`
- `KategoriAgacDTO`: `KategoriDTO` + `altKategoriler: []`
- `KategoriOzetDTO`: `toplam, aktif, pasif, altKategori`
- `KategoriKullanimDTO`: `kategoriId, ad, toplam, detaylar[{modul, adet}]`
- `KategoriBirlestirRequest`: `kaynakId, hedefId`
- `SistemKategoriDTO`: `kod, ad, kapsam`

**Doğrulama**
- Aynı `sirket + kapsam + ad (case-insensitive)` tekrarı → 400.
- Üst kategori aynı kapsamda ve üstünün üstü olmayacak (2 seviye); kendine/çocuğuna bağlama yasak.
- `renk` sabit palette; `ikon` `pi pi-[a-z0-9-]+`; `sira >= 0`.
- Silme: kullanım > 0 veya alt kategori varsa 400 (mesajda adet).
- Merge: aynı kapsam zorunlu; kaynak ≠ hedef; hedef aktif olmalı; kaynağın alt kategorileri hedefe taşınır.

## 4. Domain entegrasyonu

| Modül | Davranış |
|---|---|
| Stok | kayıt ekle/güncelle: `kategori` adı registry'de get-or-create (`STOK`), `kategoriId` + legacy ad yazılır |
| Masraf (finans) | aynı (`MASRAF`, uppercase); legacy ad uppercase |
| Bütçe | aynı (`BUTCE`, uppercase) |
| İskonto kuralı | aynı (`STOK`) |
| Kasa hareketi | `kategoriId` doğrudan registry'ye; yalnız aktif kategori seçilebilir; tür uyumu (`GELIR → KASA_GELIR`) korunur |
| Personel masraf/avans | sabit kodlar (`YAKIT, YEMEK, KONAKLAMA, ULASIM, MALZEME, DIGER`) — değişmez, salt-okunur listelenir |

**Senkron kuralları**
- Kategori yeniden adlandırılırsa bağlı tüm legacy string kolonlar tek transaction'da güncellenir.
- Merge'de bağlı tüm `kategori_id` ve legacy adlar hedefe çekilir; kaynak `aktif=false`.
- Arşivlenen kategori yeni seçimlerde görünmez; geçmiş kayıtlar korunur.
- Tenant kontrolü her işlemde (`TenantChecker`).

## 5. Frontend

`views/Kategoriler.vue` tam revizyon:
- `PageHeader` + 4 KPI: Toplam, Aktif, Pasif, Alt kategori.
- **Kapsam sekmeleri:** Kasa (Gelir/Gider) · Stok · Masraf · Bütçe; hafıza `raspel_kategoriler_kapsam`.
- **Ağaç tablo:** üst kategoriler ve alt kategoriler (aç/kapa), `AppDataTable` tabanlı; arama, aktif/pasif filtresi, sıralama (sıra/ad), sayfalama; mobil kart.
- Satır aksiyonları: düzenle, alt kategori ekle, yukarı/aşağı sırala, arşivle/aktifleştir, birleştir, sil (kullanımda disabled + neden).
- **Kullanım drawer'ı:** modül bazlı adetler (stok, kasa, masraf, bütçe, iskonto) ve "ilgili kayda git" bağlantıları.
- **Form dialog:** ad, üst kategori, renk paleti (12 renk), PrimeIcons seçici, sıra, açıklama, aktif; canlı önizleme.
- **Merge dialog:** hedef kategori seçimi, kaynak/hedef kullanım özeti, uyarı metni.
- Masraf sekmesinde **Sistem Kategorileri** salt-okunur kartı (personel masraf kodları).
- Kasa ekranı dropdown'u `/secenekler?kapsam=KASA_GELIR|KASA_GIDER` ile gruplu ve yalnız aktif.
- A11y: `aria-live`, klavye/odak, `aria-pressed`, `role=dialog`; responsive `92vw`; `prefers-reduced-motion`.
- i18n: `kategoriler` bloğu genişletilir (`tr`/`en` simetrik).

## 6. Test

**Backend**
- V173/backfill: id korunumu, kapsam eşlemesi, distinct adlar, uppercase normalizasyonu, idempotency.
- CRUD + doğrulama: tekrar ad, 2 seviye, döngü, kapsam uyuşmazlığı, palet/ikon.
- Kullanım sayımı: 5 modül.
- Silme engeli, arşiv/aktifleştirme kuralları.
- Merge: bağ taşıma, legacy ad güncelleme, kaynak arşiv, farklı kapsam reddi.
- Domain: stok/masraf/bütçe/iskonto kaydında get-or-create + senkron; kasa tür uyumu.
- Tenant/yetki.
- `PostgresEntegrasyonTest`: V173 gerçek PostgreSQL'de.

**Frontend**
- KPI, kapsam sekmeleri + hafıza, arama/filtre, ağaç aç/kapa, sayfalama.
- Form doğrulama, renk/ikon seçimi, düzenleme.
- Kullanım drawer, arşiv/aktifleştir, silme engeli, merge.
- Sistem kategorileri salt-okunur.
- Kasa dropdown gruplu/aktif.
- i18n simetrisi, a11y, mobil.

**Kapılar:** ESLint 0, `npm run i18n:check`, Vitest + kapsam, `mvn -B clean verify`, Docker build/deploy, V173 doğrulama.

## 7. Riskler ve kararlar

| Risk | Karar |
|---|---|
| Rename/merge'in legacy string alanlara yayılmaması | Tek transaction'da bulk update; testle doğrulanır |
| Bütçe geçmiş dönem adlarının değişmesi | Merge/rename geçmişi de günceller; işlem öncesi kullanım özeti gösterilir |
| Masraf uppercase davranışının bozulması | `MASRAF`/`BUTCE` registry adları uppercase tutulur |
| Kasa kategori id çakışması | Eski id'ler korunarak taşınır; sequence hizalanır |
| Personel masraf kodlarının değişmesi | Sabit kalır, salt-okunur gösterilir |
| 2 seviye ihlali | Servis guard'ı + DB self-FK |
| Kullanımdaki kategori silinmesi | 400 + adet; arşivleme önerilir |
