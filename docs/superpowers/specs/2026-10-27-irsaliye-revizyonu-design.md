# İrsaliye Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-27  
**Kapsam:** İrsaliye (sevk irsaliyesi) ekranı, durum akışı, stok etkisi, teslimat entegrasyonu, toplu fatura ve Excel  
**Birincil kullanıcı:** Sevkiyat / depo  
**Durum:** Tasarım onaylandı; uygulama planı bekliyor

---

## 1. Amaç ve başarı kriterleri

İrsaliye ekranını sevkiyat operasyonunun günlük akışına uygun hale getirmek:

- Hazırlık → sevk → teslim adımlarını ayrı durumlarla izlemek.
- Stok hareketini fiili sevk anında (mal araçtan çıkarken) işlemek.
- Sevk anında teslimat kaydını otomatik oluşturmak ve teslimatta kapatmak.
- Taslak/hazırlık irsaliyelerini düzenleyebilmek, detaylı görüntülemek.
- Aynı cariye ait birden çok irsaliyeyi tek işlemle faturaya dönüştürmek.
- Listeyi KPI + sekmeler + sunucu filtreleriyle yönetilebilir kılmak.
- Excel çıktısı, mobil kartlar ve erişilebilirlik desteği sağlamak.

**Başarı:** Sevkiyat kullanıcısı, hazırlanan irsaliyeleri tek ekrandan sevk edebilmeli; stok yalnızca sevk anında değişmeli; teslimat kaydı otomatik oluşmalı; mevcut veriler ve stok bakiyeleri bozulmamalı.

**Kapsam dışı:** Kanban panosu, sevk takvimi, satır bazında kısmi sevk/teslim, iade modülü değişiklikleri, çoklu depo bölme.

---

## 2. Durum modeli

| Durum | Anlam | Stok etkisi | Teslimat |
|---|---|---|---|
| `TASLAK` | Yeni oluşturulmuş, hazırlanmamış | Yok | Yok |
| `HAZIRLANDI` | Depoda hazırlanmış/toplanmış | Yok | Henüz yok |
| `SEVK_EDILDI` | Araçtan çıkmış / yolda | **Düşer** (SATIS) / **girer** (ALIS) | Oluşturulur, `YOLDA` |
| `TESLIM_EDILDI` | Alıcıya teslim edilmiş | Değişmez | `TESLIM_EDILDI` |
| `IPTAL` | İptal edilmiş | Sevk edilmişse geri alınır | `IPTAL` |

### İzin verilen geçişler

| Geçiş | Kural |
|---|---|
| `TASLAK → HAZIRLANDI` | Serbest; stok yok |
| `HAZIRLANDI → SEVK_EDILDI` | Şoför + teslimat adresi zorunlu; stok hareketi + teslimat oluşturma |
| `SEVK_EDILDI → TESLIM_EDILDI` | Teslim alan/tarih kaydı; stok değişmez; teslimat kapanır |
| `SEVK_EDILDI → HAZIRLANDI` | “Sevki geri al”; stok iadesi; teslimat `IPTAL`; canlı fatura varsa engel |
| `TASLAK/HAZIRLANDI → IPTAL` | Stok etkisi yok |
| `SEVK_EDILDI/TESLIM_EDILDI → IPTAL` | Stok iadesi + teslimat iptali; canlı fatura varsa engel |

- `TESLIM_EDILDI` geri alınamaz (yalnızca iptal; o da stok iadesi ve fatura kontrolüyle).
- Durum yalnızca açık aksiyon uçlarından değişir; genel `PUT /{id}` durum değiştiremez (mevcut güvenlik kuralı korunur).
- Eski `/durum` ucu geriye uyum için çalışır: `KESILDI` isteği `TASLAK` ise otomatik hazırla+sevk, `HAZIRLANDI` ise sevk, `SEVK_EDILDI` ise idempotent kabul; `IPTAL/TASLAK` istekleri geçiş matrisine uyar.

---

## 3. Stok ve fatura kuralları

### Sevk (stok)
- SATIS: stok azalır; ALIS: stok artar. Mevcut `maliyetService`, `stokSeriService` (FEFO), `depoStokService` ve `StokHareket` kaydı aynı şekilde kullanılır.
- Stok yetersizliği, depo çözümü ve tenant kontrolleri mevcut davranışla korunur.
- Eşzamanlı çift sevkü önlemek için geçişlerde irsaliye **pessimistic lock** ile okunur.
- Sevk geri alma/iptal: stok ve maliyet hareketi ters kayıtla düzeltilir; stok hareketi `IRSALIYE` kaynağıyla izlenir.

### Faturalama
- İptal dışındaki **her durumda** fatura kesilebilir.
- `TASLAK`/`HAZIRLANDI` için fatura isteği önce otomatik **SEVK_EDILDI** geçişini çalıştırır (şoför/adres yoksa anlaşılır doğrulama hatası döner), ardından fatura oluşturur. Aynı transaction: fatura başarısızsa sevk ve stok geri alınır.
- `SEVK_EDILDI`/`TESLIM_EDILDI`: doğrudan fatura.
- Mevcut çift düşüm koruması korunur: irsaliyeye bağlı canlı fatura varsa yeniden faturalama engellenir.
- Fatura oluşunca teslimat kaydına `faturaId`/`faturaNumarasi` bağlanır.

### Toplu fatura
- Seçimde tüm kayıtlar **aynı cari** olmalı; farklı cari seçilirse işlem reddedilir.
- `IPTAL` ve canlı faturası olan kayıtlar atlanır.
- Her irsaliye için **ayrı fatura** üretilir; işlemler kayıt bazında bağımsızdır.
- Sonuç: `basarili[]` (irsaliye no, fatura id/no) ve `atlanan[]` (sebep) özeti döner.

---

## 4. Veri modeli ve migration (V169)

### `muhasebe.irsaliye` yeni alanlar
| Alan | Tip | Not |
|---|---|---|
| `plaka` | VARCHAR(20) | Araç plakası |
| `sofor_id` | BIGINT | DRIVER rolü / şoför personeli kullanıcı id |
| `teslim_alan` | VARCHAR(150) | Teslim edilen kişi |
| `teslim_tarihi` | TIMESTAMP | Gerçekleşen teslim |
| `teslimat_adresi` | VARCHAR(500) | Boşsa cari adresi kullanılır |
| `koli_adeti` | INTEGER | |
| `desi` | NUMERIC(10,2) | |
| `brut_kg` | NUMERIC(10,2) | |
| `tasiyici_firma` | VARCHAR(150) | Dış taşıyıcı |
| `sevk_tarihi` | TIMESTAMP | Sevk anı |
| `teslimat_id` | BIGINT | FK → `ticaret.teslimat(id)` ON DELETE SET NULL |

### `ticaret.teslimat`
- `irsaliye_id BIGINT` + kısmi unique indeks (`WHERE irsaliye_id IS NOT NULL`).
- `fatura_id` nullable kalır; sevk anında fatura yoksa teslimat yine oluşturulabilir.

### Yeni tablo: `muhasebe.irsaliye_durum_log`
- `id`, `irsaliye_id` (FK cascade), `sirket_id`, `eski_durum`, `yeni_durum`, `kullanici_id`, `aciklama`, `olusturma_tarihi`.
- Her oluşturma ve durum geçişinde kayıt yazılır; detay drawer zaman çizelgesini besler.

### Mevcut veriler
- `KESILDI → SEVK_EDILDI` güncellenir; `sevk_tarihi` boşsa `tarih` ile doldurulur.
- Mevcut kayıtlar için geriye dönük teslimat oluşturulmaz (`teslimat_id` null).
- Stok bakiyeleri değiştirilmez (KESILDI zaten stok işlemişti).

---

## 5. Backend API

### Listeleme / özet
- `GET /api/irsaliyeler/filtreli`
  - Parametreler: `q`, `durum`, `tur`, `cariId`, `baslangic`, `bitis`, `faturali` (boolean), `page`, `size`, `sort`.
  - Dönüş: `Page<IrsaliyeDTO>`.
- `GET /api/irsaliyeler/ozet?baslangic&bitis`
  - `taslak`, `hazirlanan`, `sevkEdilen`, `teslimEdilenBuAy`, `faturasiz`, `iptal`.
- Cari filtresi için mevcut cari listesi ucu kullanılır; ayrı kategori ucu eklenmez.

### Detay
- `GET /api/irsaliyeler/{id}` genişletilir:
  - `kalemler`, `cariHesapAdi`, `siparisNo`, `faturaNumarasi`, `teslimatId`, `teslimatDurum`, `gecmis[]` (durum logları), sevk/teslim alanları.

### Geçişler
- `POST /{id}/hazirla` — TASLAK → HAZIRLANDI
- `POST /{id}/sevk` — gövde: `plaka`, `soforId`, `teslimatAdresi`, `tasiyiciFirma`, `koliAdedi`, `desi`, `brutKg`
- `POST /{id}/teslim` — gövde: `teslimAlan`, `teslimTarihi` (opsiyonel, varsayılan şimdi)
- `POST /{id}/sevki-geri-al`
- `POST /{id}/iptal` — gövde: `aciklama` (opsiyonel)
- Tümü `IRSALIYE_WRITE` yetkisi; dönem kilidi kontrolü korunur.

### Toplu fatura
- `POST /api/irsaliyeler/toplu-fatura` — gövde `{ "ids": [1,2,3] }`
- Aynı cari kontrolü; her kayıt bağımsız transaction; özet dönüş.

### Excel
- `GET /api/exports/irsaliyeler?q&durum&tur&cariId&baslangic&bitis&faturali`
- Kolonlar: No, Tarih, Cari, Tür, Durum, Plaka, Şoför, Sevk Tarihi, Teslim Tarihi, Fatura No, Kalem Sayısı, Açıklama.

### DTO alanları
`IrsaliyeDTO` ek alanlar: `plaka`, `soforId`, `soforAd`, `teslimAlan`, `teslimTarihi`, `teslimatAdresi`, `koliAdedi`, `desi`, `brutKg`, `tasiyiciFirma`, `sevkTarihi`, `teslimatId`, `teslimatDurum`, `siparisNo`, `faturaNumarasi`, `gecmis`.

---

## 6. Teslimat entegrasyonu

- Sevk anında `TeslimatService.irsaliyedenOlustur` çağrılır:
  - `sirketId`, `irsaliyeId`, `siparisId` (varsa), `driverId = soforId`, `musteriAdi` (cari), `teslimatAdresi` (irsaliye veya cari adresi), `beklenenTeslimTarihi = sevk günü`, `durum = YOLDA`, `faturaId` varsa bağlanır.
  - Şoför, mevcut `gecerliSurucu` kuralıyla doğrulanır (DRIVER rolü veya aktif şoför personeli).
  - Adres zorunludur; irsaliyede boşsa cari adresi kullanılır, ikisi de boşsa hata.
- Teslim anında teslimat `TESLIM_EDILDI`; `teslimTarihi`, `teslimAlanAd` senkronlanır.
- Sevk geri alma/iptal: teslimat `IPTAL`.
- Fatura oluşunca teslimat `faturaId` + `faturaNumarasi` ile bağlanır.
- İdempotentlik: `irsaliye_id` unique; aynı irsaliye için ikinci teslimat oluşturulmaz.

---

## 7. Frontend

### Yapı
- `PageHeader` (başlık, alt başlık, ikon) + aksiyonlar: Excel, Yeni İrsaliye.
- **4 KPI:** Hazırlanan, Sevk Edilen, Teslim Edilen (bu ay), Faturasız. KPI tıklaması ilgili sekmeye/filtreye gider.
- **Sekmeler:** Tümü / Hazırlanan / Sevk Edilen / Teslim Edilen / Faturalanan. Varsayılan **Hazırlanan**; sekme hafızası `raspel_irsaliye_tab`.
- Sunucu filtreleri: arama, cari, tür, durum, tarih aralığı, faturasız; `raspel_irsaliye_filtre` ile kalıcı.
- `AppDataTable` (lazy, sıralama/sayfalama) + mobil kart görünümü (≤768px).
- Seçim kolonu + toplu işlem çubuğu (“Toplu Faturaya Dönüştür”); farklı cari seçilirse buton devre dışı ve uyarı.

### Detay drawer
- Üst: irsaliye no, cari, tür, durum, fatura/sipariş bağlantıları.
- Sevk/teslim bilgileri: plaka, şoför, taşıyıcı, koli/desi/kg, sevk tarihi, teslim alan/tarihi, teslimat durumu.
- Kalem tablosu (stok, miktar, birim, açıklama).
- Durum zaman çizelgesi (log).
- Aksiyonlar: duruma göre Hazırla / Sevk Et / Teslim Et / Sevki Geri Al / İptal / Faturaya Dönüştür / Düzenle / PDF.

### Diyaloglar
- Yeni/Düzenle: temel bilgiler + kalemler + barkod; düzenleme yalnızca `TASLAK`/`HAZIRLANDI`.
- Sevk Et: plaka, şoför (sürücü listesi), taşıyıcı, teslimat adresi (cari adresi ön dolu), koli/desi/kg; onayla sevk.
- Teslim Et: teslim alan, teslim tarihi (varsayılan şimdi).
- İptal: opsiyonel açıklama; stok/fatura etkisi uyarısı.
- Tüm kritik işlemlerde onay + sonuç toast’ı; hata mesajları alan bazında gösterilir.
- Responsive: `:style="{ width: 'Npx' }"` + `:breakpoints="{ '575px': '92vw' }"`.

### Erişilebilirlik
- `aria-live` liste sonucu, `aria-pressed` sekmeler/filtre çipleri, ilerleme/rozet etiketleri, klavye ile drawer/dialog odak yönetimi, `prefers-reduced-motion`.

---

## 8. Hata senaryoları

- Şoför seçilmeden sevk → 400, alan hatası.
- Adres yok (irsaliye + cari) → 400.
- Yetersiz stok → mevcut mesaj.
- Canlı fatura varken sevk geri alma/iptal → 409/400, açıklayıcı mesaj.
- Zaten faturalanmış irsaliyeyi yeniden faturalama → engel.
- Toplu fatura: farklı cari, iptal, canlı fatura → atlanan listesi.
- Aynı anda iki sevk isteği → pessimistic lock + durum kontrolü; ikincisi reddedilir.
- Fatura oluşturulamazsa otomatik sevk geri alınır (transaction).

---

## 9. Test stratejisi

**Backend**
- Geçiş matrisi: her durum/aksiyon kombinasyonu (izinli/red).
- Stok: sevk düşümü, geri alma, iptal, ALIS yönü, FEFO/depo hareketi.
- Fatura: erken faturalamada otomatik sevk, çift düşüm engeli, canlı fatura kontrolü, toplu fatura aynı cari + kısmi başarı.
- Teslimat: oluşturma, idempotentlik, teslim kapanışı, iptal, fatura bağlama.
- Log kaydı; filtre/özet/Excel.
- Mevcut irsaliye testleri korunur.

**Frontend**
- KPI/sekme/filtre hafızası; sunucu parametreleri.
- Drawer verisi ve aksiyonlar; sevk/teslim form doğrulaması.
- Toplu seçim aynı cari kuralı ve sonuç modalı.
- Mobil kart, a11y, iskelet.
- Mevcut testler ve i18n anahtarları.

**Kapılar**
- ESLint 0, `npm run i18n:check`, Vitest + kapsam, `mvn clean verify` + JaCoCo, Docker build/deploy, migration doğrulama.

---

## 10. Uygulama fazları (öneri)

1. **Faz 1 — Veri + servis temeli:** V169 migration/entity/DTO/log; durum geçişleri, stok, fatura otomatik sevk, teslimat servisi, backend testleri.
2. **Faz 2 — Liste + KPI + filtreler:** `/filtreli`, `/ozet`, `/exports`; frontend PageHeader, KPI, sekmeler, sunucu liste, hafıza, iskelet.
3. **Faz 3 — Detay + düzenleme + aksiyonlar:** drawer, yeni/düzenle, hazırla/sevk/teslim/geri al/iptal diyalogları, PDF.
4. **Faz 4 — Teslimat + toplu fatura + mobil/a11y:** teslimat bağlantıları, toplu fatura UI, mobil kart, erişilebilirlik.
5. **Faz 5 — Kapanış:** tam kapılar, Docker, canlı doğrulama, rapor, commit/push.

---

## 11. Riskler ve azaltma

| Risk | Azaltma |
|---|---|
| Legacy KESILDI stokları ikinci kez işlenir | Migration yalnızca durum + sevk tarihi; stok dokunulmaz |
| Erken fatura + sonra sevk çift düşüm | Erken fatura otomatik sevk eder; `irsaliyeId` bağı korunur |
| Eşzamanlı sevk | Pessimistic lock + durum guard |
| Faturasız teslimat unique çakışması | `irsaliye_id` unique; idempotent oluşturma |
| Toplu fatura kısmi hata | Kayıt bazında transaction + sonuç özeti |
| Dönem kilidi | Tüm geçişlerde mevcut `donemService.kilitKontrol` korunur |
| Teslimat şoför doğrulaması | Mevcut `gecerliSurucu` kuralı yeniden kullanılır |
| Raporlarda durum adları | Eski `KESILDI` etiketi UI’da `SEVK_EDILDI` olarak gösterilir; API uyumluluğu korunur |
