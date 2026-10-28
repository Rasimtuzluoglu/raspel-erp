# Tahsilat Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-28  
**Kapsam:** Tahsilat ekranı, işlem bazlı tahsilat/iptal, hedef–performans, geçmiş filtreleri, toplu e-posta  
**Birincil kullanıcı:** Tahsilat / finans ekibi  
**Durum:** Tasarım onaylandı

---

## 1. Amaç

- Tahsilatı tek bir **işlem** olarak izlemek ve gerekçeli geri almak.
- Geçmişi sunucu taraflı filtre/sayfalamayla yönetmek ve Excel’e aktarmak.
- Aylık **şirket tahsilat hedefi** koyup gerçekleşmeyi ölçmek.
- Yaşlandırma listesini sunucu filtreleri, cari drawer’ı ve toplu e-posta ile güçlendirmek.
- Mobil + erişilebilirlik desteği sağlamak.

**Kapsam dışı:** Personel bazlı hedef, otomatik/şablonlu hatırlatma zamanlaması, Kanban.

---

## 2. İşlem (batch) modeli

Mevcut tahsilat girişi tek çağrıda birden çok `Hareket` (fatura başına + fazla/avans) ve kasa/banka hareketi üretir. Bunlar `finans.tahsilat_islem` kaydıyla birleştirilir.

### `finans.tahsilat_islem`
| Alan | Tip |
|---|---|
| `id` | BIGSERIAL |
| `sirket_id` | BIGINT |
| `cari_id` | BIGINT |
| `tarih` | DATE |
| `tutar` | NUMERIC(19,2) |
| `odeme_yontemi` | VARCHAR(20) |
| `kasa_id`, `banka_id` | BIGINT |
| `durum` | `AKTIF / IPTAL` |
| `iptal_gerekce` | VARCHAR(500) |
| `iptal_eden_kullanici_id`, `iptal_tarihi` | BIGINT / TIMESTAMP |
| `olusturan_kullanici_id` | BIGINT |
| `legacy_hareket_id` | BIGINT (yalnız backfill eşlemesi, unique) |
| `olusturma_tarihi` | TIMESTAMP |

- `cari.hareket.tahsilat_islem_id` (FK, nullable) eklenir; yeni tahsilatlarda tüm satırlar aynı işleme bağlanır.
- **Legacy backfill:** her mevcut TAHSILAT hareketi için tek satırlık işlem üretilir (`durum=AKTIF`, `legacy_hareket_id=hareket.id`), hareket bağlanır. Böylece geçmiş görünür ve tek tek geri alınabilir.

### Hedef: `finans.tahsilat_hedef`
`id, sirket_id, yil, ay, tutar, olusturan_kullanici_id, olusturma_tarihi, guncelleme_tarihi`; `(sirket_id, yil, ay)` unique.

---

## 3. Tahsilat girişi

- Mevcut akış korunur (FEFO/vade sırası, fatura dağıtımı, fazla/avans, kasa/banka, taksit, muhasebe entegrasyonu, idempotency).
- İşlem başında `tahsilat_islem` oluşturulur; her `Hareket` bu id ile yazılır.
- Kasa/banka hareketleri mevcut `kaynakId=hareketId` bağıyla kalır.
- Tek transaction: herhangi bir adım hata verirse işlem ve hareketler yazılmaz.

---

## 4. Geri alma

- `POST /api/tahsilat/islem/{id}/iptal` — gövde `{ gerekce }`.
- Yetki: ADMIN veya MUHASEBE (`FINANS_WRITE`/rol).
- Kurallar:
  - İşlem `AKTIF` olmalı; ikinci iptal idempotent hata verir.
  - Gerekçe zorunlu (min 3 karakter).
  - Orijinal tarih dönem kilidindeyse reddedilir.
  - İşlemdeki her Hareket için `HareketService.hareketIptal(id)` çağrılır: cari bakiye, fatura `odenenTutar/kalanTutar/odemeDurumu`, kasa/banka bakiyesi ve ters hareketler düzeltilir.
  - İşlem `IPTAL`, gerekçe/eden/tarih yazılır.
- Kısmi iptal yoktur; işlem bütünlüğü korunur.
- Geçmişte iptal işlemi `IPTAL` rozeti ve gerekçesiyle görünür; orijinal dağıtımlar salt-okunur gösterilir.

---

## 5. Geçmiş

- `GET /api/tahsilat/gecmis` parametreleri: `q`, `cariId`, `odemeYontemi`, `durum` (`AKTIF/IPTAL`), `kasaId`, `bankaId`, `baslangic`, `bitis`, `page`, `size`, `sort`.
- Dönüş: `Page<TahsilatIslemDTO>`:
  - `id, tarih, cariId, cariAd, tutar, odemeYontemi, kasaAd, bankaAd, durum, iptalGerekce, olusturanKullaniciId, satirSayisi`
  - `dagilimlar[]`: `{ hareketId, faturaId, faturaNumarasi, tutar, aciklama, iptal }` (fatura/avans).
- Legacy tek satırlık işlemler de aynı DTO ile döner (`id` gerçek işlem id’si).
- Excel: işlem satırı + dağıtım detayı (`/exports/tahsilat-gecmis`).

---

## 6. Yaşlandırma / KPI / sekmeler

### Sekmeler
1. **Yaşlandırma** (varsayılan) — cari listesi, açık fatura detayı, seçim.
2. **Tahsilat Geçmişi** — işlem listesi + filtreler + Excel.
3. **Hedef & Performans** — ay/yıl, hedef, tahsil edilen, gerçekleşme, 6 aylık trend.

Sekme hafızası: `raspel_tahsilat_tab`.

### KPI (global)
- Toplam Alacak
- Vadesi Geçmiş
- 30 Gün İçinde
- **Bu Ay Tahsilat** (iptal hariç TAHSILAT toplamı)
- **Hedef Gerçekleşme %** = bu ay tahsilat / bu ay hedef (hedef yoksa `-`)

### Yaşlandırma filtreleri (sunucu)
`q` (cari adı/telefon), `aralik` (Vadesi Gelmemiş / 0-30 / 31-60 / 61-90 / 90+), `sadeceGecikmis`, `minTutar`.
`GET /api/tahsilat` bu parametrelerle genişler; özet toplamları global kalır, `cariler` filtrelenir.

### Performans
- `GET /api/tahsilat/performans?yil&ay`:
  `hedef, tahsilEdilen, gerceklesmeYuzde, tahsilatSayisi, vadesiGecmisToplam, trend[{ay, tahsilat, hedef}]` (son 6 ay).
- `PUT /api/tahsilat/hedef` gövde `{yil, ay, tutar}` (ADMIN/MUHASEBE); aynı ay için upsert.
- Hedef para birimi TRY.

---

## 7. Cari drawer + toplu e-posta

- `TahsilatCariDrawer`:
  - İletişim (telefon/e-posta), toplam/gecikmiş alacak, yaş aralığı.
  - Açık faturalar (vade, gecikme, kalan).
  - Son 5 tahsilat (tarih, tutar, yöntem, durum).
  - Aksiyonlar: Tahsilat Al, WhatsApp, E-posta hatırlat.
- Yaşlandırma listesinde çoklu seçim; “Toplu E-posta Hatırlat”:
  - Yalnız seçili cariler; e-postası olmayanlar atlanır.
  - Onay diyaloğu: alıcı sayısı + atlanacak sayısı.
  - `POST /api/tahsilat/toplu-hatirlat` gövde `{ cariIds: [] }`; yanıt `{ gonderilen, atlanan, hatalar[] }`.
  - İşlem sonucu modal/toast.

---

## 8. Frontend

- `PageHeader` + 5 KPI + TabView.
- Yaşlandırma: `AppDataTable` (genişleyen fatura satırları), seçim kolonu, sunucu filtreleri, mobil kartlar.
- Geçmiş: işlem tablosu, genişleyen dağıtım, filtre çubuğu, Excel, iptal aksiyonu (yetki varsa).
- Performans: hedef kartı + düzenleme, gerçekleşme çubuğu, 6 ay trend (CSS bar).
- Drawer, toplu e-posta modalı, iptal gerekçe modalı.
- Skeleton, `aria-live`, `aria-pressed`, klavye/odak, `prefers-reduced-motion`, responsive diyaloglar (`92vw` breakpoint).
- Filtre hafızası: `raspel_tahsilat_filtre`.

---

## 9. Hata senaryoları

- İşlem yok/başka şirket → 404/403.
- İşlem zaten `IPTAL` → 400, idempotent mesaj.
- Gerekçe boş → 400.
- Dönem kilidi → 400, açıklayıcı.
- Kasa/banka hareketi ters kaydı sırasında hata → tüm iptal transaction’ı geri alınır.
- Hedef negatif → 400; ay 1–12, yıl 2000–2100.
- Toplu e-posta: seçim boş → 400; e-postasız cariler atlanır.
- Yetkisiz iptal/hedef → 403.

---

## 10. Test stratejisi

**Backend**
- Tahsilat girişinde tek işlem + N hareket bağı; legacy işlem dönüşümü.
- İşlem iptali: cari/fatura/kasa/banka bakiyeleri, ters hareketler, dönem kilidi, idempotentlik, yetki.
- Geçmiş filtre/sayfalama, işlem DTO dağıtımları, Excel.
- Hedef upsert + performans hesabı (hedef yok, iptal hariç).
- Yaşlandırma filtreleri.
- Toplu hatırlatma: e-postalı/e-postasız, kısmi hata.
- Mevcut TahsilatService/Controller testleri korunur.

**Frontend**
- KPI/sekme/filtre hafızası; sunucu parametreleri.
- Geçmiş filtreleri/iptal akışı; performans hedef düzenleme.
- Drawer, seçim + toplu e-posta, mobil/a11y.
- Mevcut testler.

**Kapılar:** ESLint 0, i18n tr/en, Vitest + kapsam, `mvn clean verify` + JaCoCo, Docker, V170 doğrulama.

---

## 11. Fazlar

1. V170 + işlem/hedef modeli + tahsilat girişine batch bağlama + testler.
2. İşlem iptali + geçmiş filtreleri + Excel.
3. Hedef/performans + yaşlandırma filtreleri + toplu hatırlatma backend.
4. Frontend KPI/sekme/liste + drawer + toplu e-posta + mobil/a11y.
5. Tam kapılar + Docker + rapor.

---

## 12. Riskler

| Risk | Azaltma |
|---|---|
| Legacy kayıtların yanlış gruplanması | Her legacy hareket tek işlem; unique `legacy_hareket_id` |
| İptalde çift ters kayıt | İşlem durumu + Hareket iptal bayrağı + tek transaction |
| Dönem kilidi | İptal öncesi kontrol; hata mesajı |
| Geçmiş performansı | Sunucu sayfalama + indeks `(sirket_id,tarih,durum)` |
| Hedefin geçmiş ayları değiştirmesi | Yalnız seçili ay upsert; trend hedefleri okuyarak gösterir |
| Toplu e-posta SMTP yok | Kısmi sonuç + anlaşılır hata |
| Fatura bakiyesi tutarsızlığı | Mevcut `hareketIptal` mantığı yeniden kullanılır |
