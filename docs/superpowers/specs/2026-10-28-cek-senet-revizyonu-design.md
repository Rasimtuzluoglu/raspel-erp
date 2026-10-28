# Çek/Senet Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-28  
**Kapsam:** Çek/senet portföyü, teminat/ciro/protesto akışı, tahsil ve iptali, vade takvimi, KPI, drawer, Excel  
**Birincil kullanıcı:** Finans / muhasebe  
**Durum:** Tasarım onaylandı

---

## 1. Amaç

- Çek/senet yaşam döngüsünü (portföy → teminat/ciro → tahsil/protesto) izlenebilir kılmak.
- Tahsil edilen çek/senedi gerekçeyle geri alıp cari/fatura/kasa-banka etkilerini düzeltmek.
- Vade riskini yaşlandırma panosu + aylık takvimle göstermek.
- Sunucu filtreleri, detay geçmişi, Excel ve mobil/erişilebilirlik sağlamak.

**Kapsam dışı:** Banka teminat mektubu modülü, kısmi tahsil, çek tedarikçi portföyü ayrımı.

---

## 2. Veri modeli (V171)

### `muhasebe.cek_senet` yeni alanlar
| Alan | Tip | Not |
|---|---|---|
| `tahsilat_hareket_id` | BIGINT | Tahsilde oluşan cari hareket |
| `kasa_hareket_id` | BIGINT | Tahsilde oluşan kasa hareketi |
| `banka_hareketi_id` | BIGINT | Tahsilde oluşan banka hareketi |
| `ciro_cari_id` | BIGINT | Ciro edilen cari |
| `ciro_tarihi` | TIMESTAMP | |
| `ciro_notu` | VARCHAR(500) | |
| `teminat_tarihi` | TIMESTAMP | |
| `teminat_aciklama` | VARCHAR(500) | Banka/üçüncü taraf serbest metin |
| `protesto_tarihi` | TIMESTAMP | |

### Yeni `muhasebe.cek_senet_durum_log`
`id, cek_senet_id (FK cascade), sirket_id, eski_durum, yeni_durum, islem, kullanici_id, aciklama, olusturma_tarihi`.
İşlemler: `OLUSTUR, GUNCELLE, TEMINAT, TEMINAT_IADE, CIRO, CIRO_IADE, IADE_AL, TAHSIL, TAHSIL_IPTAL, PROTESTO`.

### Durumlar
`PORTFOY, TEMINATTA, CIRO_EDILDI, TAHSIL_EDILDI, PROTESTO, IADE`.
Legacy `ODENDI` okunurken `TAHSIL_EDILDI` kabul edilir (migration gerekmez; servis eşlemesi).

---

## 3. Durum geçişleri

| Geçiş | Kural | Finansal etki |
|---|---|---|
| PORTFOY → TEMINATTA | Açıklama opsiyonel | Yok |
| TEMINATTA → PORTFOY | Teminat iadesi | Yok |
| PORTFOY → CIRO_EDILDI | Hedef cari zorunlu | Yok (belge takibi) |
| CIRO_EDILDI → IADE | Ciro iadesi | Yok |
| IADE → PORTFOY | Portföye geri al | Yok |
| PORTFOY/TEMINATTA → TAHSIL_EDILDI | Kasa/banka opsiyonel, fatura opsiyonel | Hareket + kasa/banka |
| TAHSIL_EDILDI → PORTFOY | Yalnız `tahsil-iptal`, gerekçe zorunlu | Ters kayıt |
| PORTFOY/TEMINATTA → PROTESTO | Açıklama | Yok; terminal |

> **Not:** `PROTESTO` terminaldir; tahsil iptali uygulanmaz. `IPTAL` durumu bu revizyonda eklenmez; silme kuralları mevcut gibi kalır (tahsil/ödendi silinemez).

Tüm geçişlerde tenant + dönem kilidi (vade tarihi) + log zorunlu.

---

## 4. Tahsilat

- Tutar = çek/senet tutarı (kısmi yok).
- `faturaId` opsiyonel: verilirse cari hareket o faturaya bağlanır (fatura kalan/ödeme durumu güncellenir); verilmezse cariye tahsilat/avans yazılır.
- Kasa veya banka (aynı anda ikisi olamaz) seçilebilir; seçilmezse yalnız cari hareket.
- Oluşan `Hareket`, `KasaHareket`, `BankaHareketi` id’leri çek/senet kaydına yazılır.
- Kasa/banka hareketlerinde `kaynakTip='CEK_SENET'`, `kaynakId=cek_senet.id`.
- İşlem tek transaction; hata olursa hiçbir kayıt kalmaz.
- Muhasebe entegrasyonu mevcut `otomatikMuhasebeService.cekSenetTahsilIsle` ile korunur.

### Tahsil iptali
- `POST /api/cek-senet/{id}/tahsil-iptal` gövde `{gerekce}` (min 3).
- Yalnız `TAHSIL_EDILDI`; gerekçe zorunlu; vade dönemi kilitliyse red.
- `tahsilatHareketId` varsa `HareketService.hareketIptal` (cari + fatura düzeltmesi).
- `kasaHareketId` / `bankaHareketiId` varsa ters hareket + bakiye düzeltmesi (kaynak bağıyla).
- Durum `PORTFOY`; tahsil alanları temizlenir; log `TAHSIL_IPTAL`.
- İkinci iptal engellenir.

---

## 5. Ciro / teminat / protesto

- `POST /{id}/ciro` `{cariId, not}`: PORTFOY, hedef cari zorunlu; `CIRO_EDILDI`, tarih/not/log. Finansal hareket yok.
- `POST /{id}/ciro-iade`: CIRO_EDILDI → IADE; log.
- `POST /{id}/iade-al`: IADE → PORTFOY; log.
- `POST /{id}/teminat` `{aciklama}`: PORTFOY → TEMINATTA; tarih/açıklama/log.
- `POST /{id}/teminat-iade`: TEMINATTA → PORTFOY; log.
- `POST /{id}/protesto` `{aciklama}`: PORTFOY/TEMINATTA → PROTESTO; tarih/açıklama/log.

---

## 6. Backend API

- `GET /api/cek-senet/filtreli?q&tur&durum&cariId&baslangic&bitis&vadeAralik&minTutar&protesto&page&size&sort`
- `GET /api/cek-senet/ozet?baslangic&bitis`
  - `portfoy, teminatta, ciroEdilen, tahsilEdilen, protesto, vadesiGecen, buAyVadesiGelen, toplamTutar` (+ tür kırılımı).
- `GET /api/cek-senet/{id}`: cariAd, faturaNo, ciroCariAd, kasa/banka adları, `gecmis[]`.
- `GET /api/cek-senet/takvim?baslangic&bitis`: gün bazlı `{tarih, adet, tutar, kayitlar[]}` (aylık grid).
- `GET /api/exports/cek-senet` Excel (liste + durum/vade/ciro/tahsil bilgileri).
- Mevcut `PUT /{id}/durum` geriye uyum için korunur; yeni aksiyon uçları tercih edilir.

---

## 7. Frontend

- `PageHeader` + 5 KPI: Portföy, Teminatta, Vadesi Geçen, Bu Ay Vadesi Gelen, Protesto.
- Sekmeler: **Portföy / Vade Takvimi / Geçmiş**; varsayılan Portföy; `raspel_cek_senet_tab`.
- Portföy: sunucu filtreleri (tür, durum, cari, vade aralığı, min tutar, arama), sıralama/sayfalama, Excel, mobil kart.
- Vade Takvimi: yaşlandırma kartları (Bu Hafta / Bu Ay / Geciken / 30+ / 60+ / 90+) + **aylık takvim grid’i**; görünüm geçişi; güne tıklayınca liste filtrelenir.
- Geçmiş: durum logları / tahsil-ciro-protesto işlemleri, filtrelenebilir.
- Detay drawer: bilgiler, cari/fatura, ciro/teminat, tahsil bağları, zaman çizelgesi, duruma göre aksiyonlar.
- Aksiyonlar: Tahsil Et (fatura opsiyonel, kasa/banka), Ciro Et, Teminat Ver/İade, Protesto, Tahsil İptali, Düzenle, Sil.
- Modallar: tahsil, iptal gerekçesi, ciro hedefi, teminat açıklaması, protesto açıklaması; onay + toast.
- Skeleton, `aria-live`, `aria-pressed`, klavye/odak, `prefers-reduced-motion`, responsive diyalog (`92vw`).
- Filtre hafızası: `raspel_cek_senet_filtre`.

---

## 8. Hata senaryoları

- Geçersiz geçiş → 400, mevcut/yeni durum mesajı.
- Tahsil iptalinde gerekçe yok / dönem kilitli / ikinci iptal → 400.
- Ciro hedefi yok veya başka şirket → 400/403.
- Kasa+banka birlikte seçilmesi → 400.
- Fatura başka cariye ait → 400.
- Yetkisiz işlem (USER) → 403 (ADMIN/MUHASEBE).
- Takvim aralığı > 62 gün → 400.

---

## 9. Test stratejisi

**Backend:** durum geçiş matrisi; tahsil + hareket/kasa/banka bağları; fatura opsiyonel; tahsil iptali (cari/fatura/kasa/banka, dönem kilidi, idempotent); ciro/teminat/protesto + log; filtre/özet/takvim; Excel; legacy ODENDI.
**Frontend:** KPI/sekme/filtre hafızası; portföy listesi; takvim/aging; drawer; tahsil + fatura seçimi; iptal/ciro/teminat/protesto; Excel; mobil/a11y.
**Kapılar:** ESLint 0, i18n tr/en, Vitest + kapsam, `mvn clean verify` + JaCoCo, Docker, V171 doğrulama.

---

## 10. Fazlar

1. V171 + durum log + tahsil bağlantı alanları + testler.
2. Tahsil iptali + ciro/teminat/protesto uçları + testler.
3. Filtreli liste/özet/takvim/Excel.
4. Frontend KPI/sekme/panolar/takvim/drawer/aksiyonlar/mobil.
5. Tam kapılar + Docker + rapor.

---

## 11. Riskler

| Risk | Azaltma |
|---|---|
| Kasa/banka ters kaydının bulunamaması | Hareket id’leri çek/senet kaydında saklanır |
| Çift tahsil iptali | Durum guard + hareket iptal bayrağı |
| Legacy ODENDI uyumsuzluğu | Servis eşlemesi (`ODENDI → TAHSIL_EDILDI`) |
| Takvim büyük aralık | 62 gün sınırı |
| Ciro finansal etkisi | Bu sürümde belge takibi; muhasebe fişi oluşturulmaz (dokümante) |
| Yetersiz test | Geçiş matrisi tablo-testi |
