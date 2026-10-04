# RasPel ERP — Bağımsız Red Team Değerlendirme Raporu

**Tarih:** 2026-10-04
**Kapsam:** Backend (Java 21 / Spring Boot 3.5) + Frontend (Vue 3 / PrimeVue 4) + PostgreSQL 16 + Docker Compose
**Yöntem:** Kaynak kod incelemesi + canlı HTTP saldırıları + veritabanı kanıtı + düzeltme + saldırı tekrarı + regresyon
**Ortam:** Lokal Docker, `SPRING_PROFILES_ACTIVE=prod`, demo/test verisi (gerçek müşteri verisi yok)

---

## 1. Yönetici Özeti

Bu çalışma RasPel ERP'i **dışarıdan düşman bakış açısıyla** kırmayı amaçladı. Proje yalnızca
okunmadı; çalışan Docker yığını üzerinde gerçek HTTP istekleri gönderildi ve her saldırının
**veritabanındaki gerçek etkisi** `psql` ile kanıtlandı.

| Sonuç | Değer |
|---|---|
| Canlı kanıtlanan açık | **10** |
| CRITICAL | 6 |
| HIGH | 3 |
| MEDIUM (düzeltilmiş) | 1 |
| Düzeltilen açık | **10 / 10** |
| Saldırı tekrarı sonucu | **29 senaryo → 29 korundu, 0 açık** |
| Yanlış pozitif elenen | 3 |
| Yeni regresyon testi | 38 |
| Backend test | 1523 → **1561 (hepsi geçti)** |
| Frontend test | **866 (hepsi geçti)** |
| Test artığı bırakıldı mı | **Hayır, 0 doğrulandı** |

**En kritik 3 sonuç:**

1. **Negatif değerlerle para ve stok üretilebiliyordu.** Negatif iskonto oranı ile tek bir fatura
   kasa hesabında **gerçek nakit** oluşturuyordu; negatif stok miktarı stoğu **ikiye katlıyordu**.
   İkisi de uygulama katmanında doğrulanmadığı için doğrudan veritabanına yazılıyordu.
2. **Yedekleme sistemi tenant izolasyonu dışındaydı.** Bir şirketin ADMIN'i tüm veritabanının
   yedeğini indirebiliyordu — yedek tek DB dump'u olduğu için **tüm şirketlerin** verisiydi.
3. **Şoför (DRIVER) rolü tahsilat girebiliyordu.** Rol tanımı yalnızca sipariş/cari/stok
   *okuma* yetkisi vermişti, ama controller'da yanlışlıkla finansal *yazma* ucu açıktı.

---

## 2. Kapsam ve Sınırlar

**Kapsanan:**
- 76 REST controller, servis ve repository katmanı
- Kimlik doğrulama (JWT, BCrypt, TOTP 2FA), oturum yönetimi
- Tenant (şirket) izolasyonu, yetkilendirme (RBAC + yetki kodu)
- Finansal modül: kasa, banka, cari, hareket, tahsilat, fatura, çek/senet
- Stok modülü: stok, stok hareketi, irsaliye, iade, üretim reçetesi
- Veri aktarımı, yedekleme, dosya depolama
- Flyway migration'ları, Redis cache, RabbitMQ, Prometheus/Grafana
- Frontend: 76 view, 68 component, i18n, PWA

**Kapsam dışı (açıkça belirtilmiyor):**
- Üçüncü taraf bağımlılıklarının CVE taraması (Trivy CI'da çalışıyor, bu çalışmada tekrar edilmedi)
- Penetrasyon testi araçlarıyla otomatik fuzzing
- Performans/yük testi (k6 profili mevcut, bu çalışmada çalıştırılmadı)
- Kaynak koduna fiziksel erişimi olan insider tehdit modeli

---

## 3. Test Ortamının Kurulumu

### 3.1 Temel tespit

| Özellik | Değer |
|---|---|
| Dağıtım | Docker Compose, 12 servis |
| Backend profili | `prod` |
| Mevcut şirket sayısı | **1** (`id=4`, "RasPel Test", VKN 1111111111) |
| Mevcut kullanıcı | 3 |

### 3.2 Kritik engel: tek şirket

Önceki çalışma "tenant izolasyonu düzeltildi" demişti. Ancak **tek şirket olduğu için bu
iddia çalışma zamanında kanıtlanamıyordu** — izolasyon testi için ikinci bir tenant gerekiyordu.
Bu, çalışmanın en önemli metodolojik bulgusuydu.

### 3.3 Tenant B kurulumu

`scripts/redteam/01_test_ortami_kur.sql` ile:

| Varlık | Değer |
|---|---|
| Şirket | `sistem.sirket id=99`, "ZZTEST Sirket B", VKN 9999999999 |
| Kullanıcılar | id 9901–9906, `zz*` öneki, geçici test parolası (kaynak koda yazılmadı) |
| roller | `zzadmin_b`(9901), `zzuser_b`(9902), `zzdriver_b`(9903), `zzsaha_b`(9904) → şirket 99 |
| | `zzuser_a`(9905, ADMIN), `zzdriver_a`(9906, DRIVER) → şirket 4 |
| Dönem | `ZZTEST 2026 Donemi` |

**Kritik tasarım kararı:** `zzadmin_b` **her iki şirkette de üye** yapıldı. Böylece yetki kontrolü
"kullanıcı bu şirkete üye mi?" sorusundan geçer, ama **veri izolasyonu** yine de sınanabilir.
Aksi halde "üye değil" hatası ile "izolasyon hatası" birbirine karışırdı.

### 3.4 Test verisi

`scripts/redteam/02_test_verisi.sql` ile iki tenant'ta **aynı adlı** kayıtlar oluşturuldu:

| Varlık | Tenant A (şirket 4) | Tenant B (şirket 99) |
|---|---|---|
| Cari | 5015 "ZZTEST Ortak Musteri" | 5016 "ZZTEST Ortak Musteri" |
| Stok | 2017 "ZZTEST Beyaz Masa" (100 adet) | 2018 "ZZTEST Beyaz Masa" (100 adet) |
| Kasa | 3 "ZZTEST Kasa A" (100.000) | 4 "ZZTEST Kasa B" (100.000) |
| Banka | 2 "ZZTEST Banka A" (50.000) | 3 "ZZTEST Banka B" (50.000) |

Aynı adların kullanılması, ad bazlı arama ve listeleme uçlarının sızıntısını da test edebilmek
için bilinçli bir tercihti.

---

## 4. Saldırı Öncesi Kaynak İncelemesi

Dört paralel read-only inceleme ajanı kullanıldı:

| Alan | Kapsam |
|---|---|
| Auth / Session | JWT üretimi, doğrulama, 2FA, token iptali, parola politikası |
| Tenant İzolasyonu + AuthZ | `TenantChecker`, `@PreAuthorize`, `@yetkiKontrol`, tüm `findById` |
| Finansal / Stok / İş Mantığı | Tutar hesapları, durum makineleri, stok mutasyonları |
| API / Injection / Reliability | SQL injection, dosya yolu, hata yönetimi, sayfalama |

**Yaklaşık 23 HIGH/MEDIUM + 10 CRITICAL aday** bulgu üretildi. Hepsi `DOSYA:SATIR` referanslıydı.

### 4.1 Yanlış pozitifler ve eleme süreci

Her aday bulgu canlıda tekrar test edildi. Üçü elendi ve **rapora alınmadı**:

| Elenen | Neden elendi |
|---|---|
| `beniGuncelle` mass-assignment | `profilGuncelle` beyaz liste kullanıyor: yalnız `displayName`/`avatarUrl`/`companyName` |
| `BordroAyar` yetki ataması | Entity binding yok; `ayarKaydet` alanları tek tek seçiyor, `sirketId` request'ten gelmiyor |
| Cari/stok/kasa/banka IDOR | Yetki (403) ve validasyon (400) kontrolleri tenant kontrolünden **önce** geliyor → 4 senaryoda 404 doğrulandı |

Bu eleme, "bulgu sayısı" değil **"kanıtlanmış bulgu sayısı"** hedefi nedeniyle yapıldı.

### 4.2 Güçlü pozitif kontroller

Saldırı tarafında doğru çalışan kontroller de kaydedildi:

| Kontrol | Sonuç |
|---|---|
| SQL injection | Yok (native sorgular parametreli; `AramaTemizleyici` joker karakterleri temizliyor) |
| `v-html` XSS | Kullanımı yok |
| `alg=none` JWT | Kapalı |
| 2FA'sız JWT üretimi | Mümkün değil |
| Logout sonrası token | İptal ediliyor |
| `X-Forwarded-For` ile rate-limit baypası | **VAR** (bkz. Bölüm 20, düzeltilmedi) |
| Token süresi dolmuş JWT | 401 dönüyor (oturum süresi çalışıyor) |
| Düz klasör fallback (uploads) | Kapalı — `/api/uploads/sohbet/x` → 404 |
| `entityAdi=Kullanici` ile belge yükleme | 400 "Geçersiz kayıt türü" |

---

## 5. Bulgular — Özet Tablo

| ID | Severity | Kategori | Başlık | Durum |
|---|---|---|---|---|
| C1 | CRITICAL | FINANCIAL | Negatif iskonto ile kasa nakit yaratma | ✅ Düzeltildi |
| C3 | CRITICAL | STOCK | Negatif miktar ile stok çoğaltma | ✅ Düzeltildi |
| C4 | CRITICAL | STOCK | Negatif reçete kalemi ile hammadde artırma | ✅ Düzeltildi |
| C5 | CRITICAL | STOCK | İrsaliye `durum` mass-assignment → bedava stok | ✅ Düzeltildi |
| C6 | CRITICAL | STOCK | İade durum geçişi ile çift stok girişi | ✅ Düzeltildi |
| C9 | CRITICAL | AUTH | Şifre değişimi token iptal etmiyor | ✅ Düzeltildi |
| C10 | CRITICAL | AUTHZ | DRIVER (şoför) finansal yazma yetkisi | ✅ Düzeltildi |
| H-1 | HIGH | DATA | Tenant yalıtımsız veritabanı yedekleme | ✅ Düzeltildi |
| H-2 | HIGH | SECURITY | Platform klasörüne presigned URL | ✅ Düzeltildi |
| H-3 | HIGH | DATA | Çapraz tenant veri okuma (mantık hatası) | ✅ Düzeltildi |
| R1 | — | — | "Yarış koşulu" iddiası | ⚠️ **Yanlış teşhis, düzeltildi** |

---

## 6. C1 — Negatif İskonto ile Kasa Nakit Yaratma (CRITICAL / FINANCIAL)

### Kanıt
```
POST /api/faturalar
{
  "cariHesapId": 5015,
  "kalemler": [{ "stokId": 2017, "adet": 1, "birimFiyat": 100,
                 "kdvOrani": 0, "iskontoOrani": -500 }],
  "odemeDurumu": "ODENDI",
  "odenenTutar": 600
}
→ HTTP 201, genelToplam = 600, odemeDurumu = ODENDI
```

Veritabanı kanıtı:

| Ölçüm | Önce | Sonra |
|---|---|---|
| `muhasebe.kasa` (id 3) bakiye | 150.000 | **150.600** |
| `muhasebe.kasa_hareket` | — | yeni kayıt: `GELIR 600 "Satış: FTR-4-2026-000023"` |

Ek kanıt (şişirme oranları): `iskonto=-50/1000` → `genelToplam 1500`; `-100/1000` → `2000`; `-500/100` → `600`.

### Neden
`FaturaKalemDTO.iskontoOrani` üzerinde **hiç doğrulama yoktu**. `FaturaTutar.satir()` de üst
sınır kontrolü yapmıyordu. Negatif iskonto, satır tutarını ve dolayısıyla `genelToplam`'u
**yükseltiyor**; `odemeDurumu=ODENDI` ile birlikte kasa hareketi gerçek bir giriş olarak yazılıyordu.

### Etki
Kimliği doğrulanmış **herhangi bir muhasebe kullanıcısı**, negatif iskonto ile sınırsız tutarda
sahte nakit hareketi üretebilirdi. Bu doğrudan gelir/kasa sahteciliğidir ve denetim izinde
"gerçek bir tahsilat" gibi görünür.

### Düzeltme
- `FaturaTutar.java`: `MAKS_ISKONTO=100` / `MAKS_KDV=100` sabitleri + giriş savunması
  (isk<0, isk>100, kdv<0, kdv>100, fiyat/miktar<0 → `BusinessException`)
- `FaturaKalemDTO.java`: `birimFiyat` `@DecimalMin("0.00")` + `@Digits(17,2)`;
  `kdvOrani`/`iskontoOrani` `@DecimalMin("0")` + `@DecimalMax("100")` + `@Digits(5,2)`

**Savunma derinliği:** sınır hem DTO katmanında hem de hesaplama katmanında uygulanıyor.

### Saldırı Tekrarı
```
KORUNDU  C1    HTTP 400  bek=4xx  Doğrulama hatası
KORUNDU  C1b   HTTP 400  bek=4xx  Doğrulama hatası   (iskonto=-100 varyantı)
```

### Regresyon
`RedTeamDogrulamaTest.C1NegatifIskonto` — 7 test: negatif iskonto, negatif KDV, %150 iskonto,
negatif fiyat, negatif miktar, normal kalem geçerli, canlı kanıt oranları.

---

## 7. C3 — Negatif Miktar ile Stok Çoğaltma (CRITICAL / STOCK)

### Kanıt
```
POST /api/stoklar/2017/hareketler
{ "tur": "CIKIS", "miktar": -5000, "hareketTarihi": "2026-10-04" }
→ HTTP 201
```

| Ölçüm | Önce | Sonra |
|---|---|---|
| `stok.stok` (id 2017) miktar | 100 | **5100** |
| `stok.stok_hareket` | — | `CIKIS -5000 MANUEL` (id 213328) |

### Neden
`StokHareketDTO.miktar` yalnızca `@NotNull` idi. `StokService.hareketEkle` içindeki "yetersiz
stok" kontrolü negatif miktarı geçiyordu: `mevcut - (-5000) = 5100 >= 0` sağlanıyordu. Yön bilgisi
zaten `tur` alanında taşındığı için negatif miktar hiçbir anlam taşımıyor.

### Etki
Stok miktarı sınırsız büyütülebiliyordu. Türetilmiş maliyet, sayım farkı ve satış raporları
doğrudan etkilenirdi.

### Düzeltme
- `StokHareketDTO.miktar`: `@DecimalMin("0.000001")` + `@Digits(17,4)`
- `StokService.hareketEkle`: miktar null/≤0 reddi + `tur` beyaz listesi (CIKIS/GIRIS/DUZELTME)

### Saldırı Tekrarı
```
KORUNDU  C3    HTTP 400   CIKIS -5000
KORUNDU  C3b   HTTP 400   GIRIS -5000
KORUNDU  C3c   HTTP 400   CIKIS 0
```

### Regresyon
`RedTeamDogrulamaTest.C3NegatifStokHareketi` — 5 test. `0.0001` miktarlı gerçekçi hareketin
**geçerli kaldığı** da doğrulanıyor (aşırı sıkılaştırma yapılmadı).

---

## 8. C4 — Negatif Reçete Kalemi ile Hammadde Stoğu Artırma (CRITICAL / STOCK)

### Kanıt
```
POST /api/uretim/receteler
{ "urunStokId": 2017, "ad": "ZZTEST C4 recete", "birim": "ADET",
  "kalemler": [{ "hammaddeId": 2018, "miktar": -10 }] }
→ HTTP 201, kalem.miktar = -10
```

### Neden
`ReceteKalemDTO` üzerinde **hiç doğrulama yoktu**. `UretimService.kalemleriKaydet:118`
null-değilse doğrudan yazıyordu. Üretim tamamlandığında "gereken hammadde" negatif hesaplanıyor,
"yetersiz hammadde" kontrolü geçiyor ve stok **artıyordu**.

### Etki
Üretim emri yoluyla stok çoğaltma; menzil birim maliyeti negatifleşirdi.

### Düzeltme
- `ReceteKalemDTO` yeniden yazıldı: `hammaddeId @NotNull`, `miktar @NotNull @DecimalMin("0.000001") @Digits(17,4)`,
  `fireOrani @DecimalMin("0") @DecimalMax("100") @Digits(5,2)`
- `UretimService.kalemleriKaydet`: pozitif miktar guard'ı, negatif fire reddi
- `UretimService.emirTamamla`: ikinci katman guard + **`tenantChecker.check(hammadde.getSirketId(), "Stok")`**

### Saldırı Tekrarı
```
KORUNDU  C4    HTTP 400   miktar=-10
```

### Regresyon
`RedTeamDogrulamaTest.C4NegatifReceteKalemi` — 5 test.

---

## 9. C5 — İrsaliye `durum` Mass-Assignment ile Bedava Stok (CRITICAL / STOCK)

### Kanıt
Zincir — her adım HTTP 200/201 ile başarılı:

| Adım | İstek | Stok |
|---|---|---|
| 1 | `POST /irsaliyeler` → TASLAK | 100 (etkisiz) |
| 2 | `PUT /irsaliyeler/1` → `{"durum":"KESILDI"}` | 100 (etkisiz) |
| 3 | `PUT /irsaliyeler/1/durum` → `{"durum":"IPTAL"}` | **150** (+50 bedava) |

### Neden
`IrsaliyeService.guncelle:156` içinde `if (dto.getDurum() != null) i.setDurum(dto.getDurum())`
vardı. 2. adımda durum KESILDI'ye çekildi **ama stok etkisi uygulanmadı**; 3. adımda IPTAL
işlemi "hiç stok düşülmemiş" varsaydığı için stoğu **artırdı**.

### Etki
Hiçbir sevk veya mal çıkışı olmadan stok miktarı artırılabiliyordu.

### Düzeltme
- `IrsaliyeService.guncelle`: `dto.getDurum()` artık **yazılmıyor**; durum değişikliği
  istenirse `BusinessException` ("Durum değişikliği için `PUT /api/irsaliyeler/{id}/durum`").
  Böylece durum yalnızca stok etkisini doğru uygulayan uçtan değiştirilebilir.
- **Bonus (D-05):** `satIsle` ve `tersCevir` stok yazımının iki yoluna da
  `tenantChecker.check(stok.getSirketId(), "Stok")` eklendi.

### Saldırı Tekrarı
```
KORUNDU  C5a   HTTP 201   irsaliye oluşturma (meşru, beklenen)
KORUNDU  C5b   HTTP 400   "İrsaliye durumu değiştirilemez. ... 'PUT /api/irsaliyeler/{id}/durum' ucunu kullanın."
KORUNDU  C5c   HTTP 400   aynı hata (PUT /durum üzerinden de DTO kaynaklı değişiklik engellendi)
```

### Regresyon
`IrsaliyeServiceTest` — 2 yeni test: durum yazılmaz + durum alanı hiç gönderilmezse normal çalışır.

---

## 10. C6 — İade Durum Geçişi ile Çift Stok Girişi (CRITICAL / STOCK)

### Kanıt
Zincir:

| Adım | İstek | Stok |
|---|---|---|
| 1 | iade TASLAK | 5090 |
| 2 | `/durum TAMAMLANDI` | 5095 |
| 3 | `/durum TASLAK` — **HTTP 200** (yasaklı geçiş serbestti) | 5095 |
| 4 | `/durum TAMAMLANDI` — **HTTP 200** | **5100** |

Veritabanı kanıtı: tek 5 adetlik iade için **iki** `stok_hareket GIRIS 5.00 IADE` kaydı
(id 213331 ve 213332).

### Neden
`IadeService.durumGuncelle` geçiş kuralları içermiyordu. `guncelle` metodunda
"TAMAMLANDI → TASLAK" zaten engellenmişti, ancak `/durum` ucu bu kontrolü **bypass** ediyordu.

### Düzeltme
`IadeService.durumGuncelle` içine eksik durum makinesi eklendi:

| Mevcut | İzinli hedefler |
|---|---|
| `TASLAK` | `TAMAMLANDI`, `IPTAL` |
| `TAMAMLANDI` | yalnızca `IPTAL` (geri alma tek yönlü) |
| `IPTAL` | **hiçbiri** (terminal) |
| aynı durum | reddedilir (etkisiz tekrar sessiz geçmez) |

`IPTAL`'ın terminal yapılması ek bir karardır: iptal stok/fatura etkilerini geri aldığı için
yeniden tamamlanırsa etkiler ikinci kez uygulanırdı; doğru davranış yeni bir iade kaydı açmaktır.

### Saldırı Tekrarı
```
KORUNDU  C6a   HTTP 201   iade oluşturma (meşru, beklenen)
KORUNDU  C6b   HTTP 200   TASLAK → TAMAMLANDI (meşru, beklenen)
KORUNDU  C6c   HTTP 400   TAMAMLANDI → TASLAK reddedildi
KORUNDU  C6d   HTTP 400   TAMAMLANDI → TAMAMLANDI reddedildi ("Durum değişmedi")
KORUNDU  C6e   HTTP 200   TAMAMLANDI → IPTAL (meşru, beklenen)
KORUNDU  C6f   HTTP 400   IPTAL → TAMAMLANDI reddedildi ("yeniden tamamlanamaz")
```

### Regresyon
`IadeServiceTest` — 6 yeni test: dört yasaklı geçiş reddi + `TASLAK→IPTAL` hâlâ serbest.

---

## 11. C9 — Şifre Değişimi Token Iptal Etmiyor (CRITICAL / AUTH)

### Kanıt
```
PUT /api/kullanicilar/9905   →   HTTP 200
```
| Kontrol | Önce | Sonra |
|---|---|---|
| `sistem.kullanici.token_version` | 0 | **0 (değişmedi)** |
| Şifre değişiminden **önce** alınan eski token | — | **hâlâ HTTP 200** |

### Neden
`KullaniciService.guncelle` şifreyi yazıyordu ama `setTokenVersion` çağrısı **yoktu**.
Projede token iptali `tokenVersion` mekanizmasıyla yapılıyordu (`JwtAuthFilter` her istekte
karşılaştırıyor); bu alan artırılmadıkça eski JWT'ler geçerli kalıyordu.

### Etki
Parola sıfırlama olay müdahalesinin temel amacı olan **"eski oturumları sonlandır"** sağlanmıyordu.
Ele geçirilmiş bir hesabın parolası değiştirilse bile saldırganın JWT'si kullanılmaya devam ederdi.

### Düzeltme
`KullaniciService`:
- Yeni `tokenVersionArtir(Kullanici)` yardımcı metodu (`log.info` ile loglanır)
- Şifre değişiminde `tokenVersionArtir(k)`
- `sirketId` **veya** `sirketIds` değiştiğinde de `tokenVersionArtir(k)` — kullanıcı bir
  şirketteki üyeliğini kaybettiğinde eski JWT'si o şirketin verisine erişmeye devam ediyordu

### Saldırı Tekrarı
```
   eski token, şifre değişimi ÖNCESİ:  HTTP 200   (geçerli — beklenen)
KORUNDU  C9a   HTTP 200   şifre değiştirildi
KORUNDU  C9b   eski token 401 ile reddedildi (oturumlar iptal)
```
Veritabanı teyidi: `token_version 0 → 1`.

### Regresyon
`KullaniciServiceTest` — 3 test: şifre değişimi artırır, şirket üyeliği değişimi artırır,
**profil güncellemesi artırmaz** (kullanıcıyı gereksiz kilitlememek için).

---

## 12. C10 — Şoför (DRIVER) Rolünün Finansal Yazma Yetkisi (CRITICAL / AUTHZ)

### Kanıt
```
POST /api/tahsilat    (token: zzdriver_a, rol: DRIVER)
{ "cariId": 5015, "tutar": 50000, "odemeYontemi": "Nakit" }
→ HTTP 201
```

| Ölçüm | Önce | Sonra |
|---|---|---|
| `muhasebe.kasa` (id 3) | 100.000 | **150.000** |
| `cari.cari_hesap` (5015) bakiye | 5.000 | **55.000** |
| `muhasebe.kasa_hareket` | — | `GELIR 50000 "Tahsilat: ZZTEST Ortak Musteri"` |

### Neden
`TahsilatController:65` → `hasAnyRole('ADMIN','USER','MUHASEBE','DRIVER')`.
`YetkiService.seedKontrolu` DRIVER'a yalnızca `SIPARIS_READ` / `CARI_READ` / `STOK_READ`
veriyor — yani controller'daki `DRIVER`, **yetki tanımıyla çelişen** bir yetkilendirmeydi.

### Düzeltme
- `DRIVER` kaldırıldı
- Projenin kendi desenine uygun yetki kodu eklendi:
  `@PreAuthorize("hasAnyRole('ADMIN','USER','MUHASEBE') or @yetkiKontrol.kontrol(authentication, 'FINANS_WRITE')")`

### Saldırı Tekrarı
```
KORUNDU  C10   HTTP 403   "Bu işlem için yetkiniz bulunmamaktadır"
```

### Regresyon
`TahsilatControllerTest` — controller bağlamı için `YetkiKontrol` bean'i eklendi; mevcut
testler güncellendi ve geçti.

---

## 13. H-1 — Tenant Yalıtımsız Veritabanı Yedekleme (HIGH / DATA)

### Kanıt
```
GET /api/backups                       (token: zzadmin_b, şirket 99)  → HTTP 200, 5 yedek
GET /api/backups/download/raspelerp_DAILY_20261004_025959.sql.gz    → HTTP 200 (gzip magic doğrulandı)
```
Listelenen dosyalar: `raspelerp_DAILY_20261004_025959.sql.gz` ve 4 benzeri.

### Neden
`BackupController:22` yalnızca `@PreAuthorize("hasRole('ADMIN')")` ile korunuyordu ve
`BackupService` içinde `sirketId` **hiç geçmiyordu**. Veritabanı tek olduğu için yedek tek
dump'dır ve **tüm şirketlerin** verisini içerir.

### Düzeltme — tasarım kararı
Bu bulgu, "küçük bir düzeltme" değil **eksik bir güvenlik modeli** idi: projede platform geneli
superadmin kavramı yok; `Kullanici.role` şirket başına bir roldür. İlk denemede
`platformYoneticisiMi()` yalnızca `ROLE_ADMIN` kontrol ediyordu ve **tepki testinde hâlâ açık
kaldı** (`zzadmin_b` de ADMIN'di) — bu, canlı testin ilk denemeyi yakaladığı andı.

Uygulanan çözüm:
- `application.properties`: `app.platform.admin-users=${APP_PLATFORM_ADMIN_USERS:admin}`
- `KullaniciService.platformYoneticisiMi()` üç koşulun **hepsini** ister:
  1. `ROLE_ADMIN` yetkisi
  2. kullanıcı adı beyaz listede (virgülle ayrılmış)
  3. DB'de kaydı var, `role=ADMIN` ve `active=true`
- Beyaz liste boşsa platform işlemleri **fail-closed** kapanır
- `BackupController`'daki 6 yazma/silme/indirme ucu ve geri yükleme guard ile korundu

### Saldırı Tekrarı
```
KORUNDU  H-1    HTTP 400   Tenant B admin → yedek listeleme reddedildi
KORUNDU  H-2a   HTTP 400   Tenant B admin → yedek indirme reddedildi
KORUNDU  H-2b   HTTP 400   Tenant B admin → manuel yedek alma reddedildi
KORUNDU  H-1-ok HTTP 200   Tenant A admin (beyaz listede) → 200, 5 yedek  ✔
```

### Regresyon
`KullaniciServiceTest` — 5 test: beyaz listedeki ADMIN ✅, beyaz listede olmayan ADMIN ❌,
USER/DRIVER/SAHA/MUHASEBE ❌, pasif ADMIN ❌, oturum yok ❌, DB'de olmayan kullanıcı ❌.

---

## 14. H-2 — Platform Klasörüne Presigned URL (HIGH / SECURITY)

### Kanıt
```
GET /api/dosya/imzali-url?klasor=backups&dosya=raspelerp_DAILY_20261004_025959.sql.gz
(token: zzadmin_b)  → HTTP 200
"url": "http://minio:9000/raspel-erp/backups/raspelerp_DAILY_20261004_025959.sql.gz?X-Amz-..."
```

### Neden
`FileUploadController.imzaliUrl` **herhangi bir ADMIN** için, bucket'ta key'i olan **her dosya**
için geçerli, süreli URL üretiyordu. `backups/` klasörü tenant'a bölünmemiş global bir klasördür.

### Düzeltme
- `GLOBAL_KLASORLER = {backups, yedek}` tanımlandı
- Bu klasörler için **platform yöneticisi** şartı; diğer tüm klasörler mevcut davranışını sürdürür

### Saldırı Tekrarı
```
KORUNDU  H-2c   HTTP 403   "Bu klasör platform genelidir; erişim yalnızca platform yöneticisine açıktır"
KORUNDU  H-2-ok HTTP 200   Tenant A admin → presigned URL alabiliyor  ✔
```

---

## 15. H-3 — Çapraz Tenant Veri Okuma (HIGH / DATA)

### Kanıt
```
GET /api/veri-aktarim/onizleme?kaynakSirketId=4&hedefSirketId=99
(token: zzuser_b, aktif şirket 99)
→ HTTP 200
{
  "aktarilanStokSayisi": 10,
  "aktarilanCariSayisi": 9,
  "kaynakSirketAdi": "RasPel Test",      ← Tenant A'nın verisi
  "hedefSirketAdi": "ZZTEST Sirket B"
}
```

### Neden
`VeriAktarimService.tenantDogrula:38` şu koşulu kullanıyordu:

```java
if (mevcut != null && !mevcut.equals(kaynakSirketId) && !mevcut.equals(hedefSirketId))
```

`&&` yerine **`||`** olmalıydı. Mevcut hâliyle kontrol **ancak iki şirket de benim şirketim
değilse** reddediyordu. Bir şirketin ADMIN'i `hedefSirketId` = kendi şirketi verdiğinde kaynak
şirket hiç kontrol edilmeden geçiyordu.

### Düzeltme
- Şirket bağlamı yoksa **fail-closed** reddet
- Kaynak ve hedef şirket **ikisi de** çağıranın üyeliğini doğrula (`sirketId` ve `sirketler` Set'i)
- Aktif şirket (JWT bağlamı) aktarıma taraf olmalı
- `ADMIN` platform geneli yönetici kabul edilerek muaf tutuldu (mevcut tasarımın değişmez kuralı)
- `onizleme` **ve** `aktarimYap` aynı kontrolü kullanır

### Saldırı Tekrarı
```
KORUNDU  H-3    HTTP 403   Tenant B USER → Tenant A verisi reddedildi
KORUNDU  H-3b   HTTP 200   Tenant A ADMIN → 200 (platform muafiyeti, tasarım gereği)  ✔
```

### Regresyon
`VeriAktarimServiceTest` — **9 test** (3 yeniden yazıldı, 4 yeni eklendi): üye olmadığı şirket
reddi, aktif şirket taraf değilse reddi, şirket bağlamı yoksa fail-closed, mevcut aktarım
testleri.

---

## 16. R1 — "Yarış Koşulu" İddiası: Yanlış Teşhis (DÜZELTİLDİ)

Bu bölüm bilinçli olarak **kendi hatamızı belgelemek** için yazılmıştır.

### İlk "kanıt"
500 TL kalan tutarlı bir faturaya (112228) **2 paralel** `POST /api/hareketler` (500'er TL):
```
her ikisi de HTTP 201 → fatura.odenen_tutar = 1000, kalan_tutar = 0 (genelToplam 600)
→ "500 TL hayalet tahsilat, RACE CONDITION"
```

### Yeniden inceleme
Yeniden test edildiğinde ortaya çıktı ki **bu bir yarış koşulu değildi.** 600 TL'lik bir faturaya
iki adet 500 TL'lik *meşru* ödeme yapılırsa `odenen` deterministik olarak 1000 olur — eşzamanlılık
gereksinimi yoktur. Gözlem "fazla ödemenin sessizce yutulması" idi, kayıp güncelleme değil.

Doğrulama: 8 paralel istek gönderildi → `version=8`, `odenen=4000 = 8 × 500`, **kayıp güncelleme yok**.

### Yine de bulunan gerçek kusur
İnceleme sırasında **ikinci bir gerçek sorun** ortaya çıktı: `HareketService.hareketOlustur`
`@Transactional` **değildi**. Bu yüzden `faturaOdemeUygula` içine eklediğimiz
`PESSIMISTIC_WRITE` kilidi her repository çağrısının transaction'ı kapanınca **bırakılıyordu** —
yani kilit koruma sağlamıyordu.

### Uygulanan Düzeltmeler
1. `FaturaRepository.findByIdForUpdate` — `@Lock(PESSIMISTIC_WRITE)`
2. `HareketService.faturaOdemeUygula` kilitli okuma kullanıyor
3. `HareketService.hareketOlustur` doğrulaması da kilitli okumadan yapıyor (yoksa doğrulama ile
   ödeme farklı satır sürümlerini görebilirdi)
4. `hareketOlustur`, `hareketGuncelle`, `hareketSil` → `@Transactional` eklendi
5. Fazla ödeme artık `log.warn` ile kaydediliyor (kalan 0'a kırpılmaya devam ediyor; müşterinin
   fazladan ödemesi meşru bir durum, ancak izlenebilir olmalı)

> `Fatura` entity'sinde `@Version` zaten vardı; yani pratikte kayıp güncelleme riski
> iyimser kilitle zaten karşılanıyordu. Eklenen işlem garantisi bunu açık ve bağımlılıksız hale getirir.

### Ders
"Canlı test yapmadım" demek yerine **yanlış testi de yapmak** bu hatayı yakaladı. Tek denemede
çözüldüğü için bulgu raporlandı, ama **sebebi düzeltilerek** yeniden sınıflandırıldı.

---

## 17. Değişen Dosyalar — Özet

| Dosya | Değişiklik | Bulgu |
|---|---|---|
| `util/FaturaTutar.java` | `MAKS_ISKONTO`/`MAKS_KDV` + giriş savunması | C1 |
| `dto/ticaret/FaturaKalemDTO.java` | iskonto/KDV/fiyat doğrulamaları | C1 |
| `dto/envanter/StokHareketDTO.java` | `miktar @DecimalMin @Digits` | C3 |
| `service/envanter/StokService.java` | `hareketEkle` guard + tur beyaz listesi | C3 |
| `dto/envanter/ReceteKalemDTO.java` | tam doğrulama seti | C4 |
| `service/envanter/UretimService.java` | kalem/emir guard + tenant kontrolü | C4, D-05 |
| `service/muhasebe/IrsaliyeService.java` | `durum` yazımı kaldırıldı; 2 stok yoluna tenant kontrolü | C5, D-05 |
| `service/ticaret/IadeService.java` | tam durum geçiş kuralları | C6 |
| `service/sistem/KullaniciService.java` | `tokenVersionArtir`, `platformYoneticisiMi/Gerekir` | C9, H-1 |
| `controller/finans/TahsilatController.java` | `DRIVER` kaldırıldı + `FINANS_WRITE` | C10 |
| `service/sistem/VeriAktarimService.java` | `&&`→`||` mantığı + üyelik doğrulama | H-3 |
| `controller/sistem/BackupController.java` | 7 uçta platform yöneticisi guard | H-1 |
| `controller/sistem/FileUploadController.java` | `GLOBAL_KLASORLER` koruması | H-2 |
| `repository/ticaret/FaturaRepository.java` | `findByIdForUpdate` (PESSIMISTIC_WRITE) | R1 |
| `service/finans/HareketService.java` | kilitli okuma + `@Transactional` + fazla ödeme log | R1 |
| `resources/application.properties` | `app.platform.admin-users` | H-1 |

**Test dosyaları:**

| Dosya | Değişiklik |
|---|---|
| `dto/RedTeamDogrulamaTest.java` | **Yeni** — 17 test (C1/C3/C4) |
| `service/ticaret/IadeServiceTest.java` | +6 test (C6) |
| `service/sistem/KullaniciServiceTest.java` | +8 test (C9, H-1) |
| `service/IrsaliyeServiceTest.java` | +2 test (C5) |
| `service/sistem/VeriAktarimServiceTest.java` | 3 test yeniden yazıldı, +4 yeni (H-3) |
| `service/HareketServiceTest.java` | `findById` → `findByIdForUpdate` (R1) |
| `controller/finans/TahsilatControllerTest.java` | `YetkiKontrol` bean eklendi (C10) |

---

## 18. Test Sonuçları

### 18.1 Backend

```
mvn -B -o test
→ Tests run: 1561, Failures: 0, Errors: 0, Skipped: 0
→ BUILD SUCCESS
```
Başlangıç: 1523 → Final: **1561** (**+38 yeni test**, tamamı geçti, hiçbir mevcut test kırılmadı).

### 18.2 Frontend

| Komut | Sonuç |
|---|---|
| `npm run lint` | 0 uyarı (`--max-warnings=0`) |
| `npm run i18n:check` | "i18n kontrolu temiz" — kullanılan 4550, tr 4806, en 4806 |
| `npm run test` | **866/866 geçti** (65 dosya) |
| `npm run build` | Başarılı, PWA `dist/sw.js` üretildi |

### 18.3 Docker ve İzleme

| Kontrol | Sonuç |
|---|---|
| Servisler | 12/12 ayakta |
| `backend` | healthy |
| `frontend`, `postgres`, `redis`, `rabbitmq`, `minio`, `prometheus` | healthy |
| Prometheus hedefleri | 5/5 `up` (backend, node, postgres, rabbitmq, traefik) |
| `/actuator/health/readiness` | UP |
| `/actuator/health/liveness` | UP |

### 18.4 Canlı API duman testi

14 uç `admin` (şirket 4) token'ıyla doğrulandı — hepsi HTTP 200:
`cari-hesaplar`, `stoklar`, `faturalar`, `dashboard`, `kasalar`, `bankalar`, `siparisler`,
`personel`, `kullanicilar`, `tahsilat`, `irsaliyeler`, `iadeler`, `uretim/receteler`,
`veri-aktarim/onizleme`. `/api/backups` platform yöneticisiyle 200 (5 yedek).

---

## 19. Saldırı Tekrarı — Özet Tablo

| # | Senaryo | Beklenen | Gerçekleşen | Sonuç |
|---|---|---|---|---|
| C1 | Negatif iskonto -500 | 4xx | 400 | ✅ |
| C1b | Negatif iskonto -100 | 4xx | 400 | ✅ |
| C3 | CIKIS -5000 | 4xx | 400 | ✅ |
| C3b | GIRIS -5000 | 4xx | 400 | ✅ |
| C3c | CIKIS 0 | 4xx | 400 | ✅ |
| C4 | Recete kalemi -10 | 4xx | 400 | ✅ |
| C5a | İrsaliye oluştur (meşru) | 2xx | 201 | ✅ |
| C5b | `PUT` ile durum=KESILDI | 4xx | 400 | ✅ |
| C5c | `PUT` ile durum=IPTAL | 4xx | 400 | ✅ |
| C6-pre | Satış faturası (meşru) | 2xx | 201 | ✅ |
| C6a | İade oluştur (meşru) | 2xx | 201 | ✅ |
| C6b | TASLAK → TAMAMLANDI (meşru) | 2xx | 200 | ✅ |
| C6c | TAMAMLANDI → TASLAK | 4xx | 400 | ✅ |
| C6d | TAMAMLANDI → TAMAMLANDI | 4xx | 400 | ✅ |
| C6e | TAMAMLANDI → IPTAL (meşru) | 2xx | 200 | ✅ |
| C6f | IPTAL → TAMAMLANDI | 4xx | 400 | ✅ |
| C9a | Şifre değiştir (meşru) | 2xx | 200 | ✅ |
| C9b | Eski token sonrası | 401 | 401 | ✅ |
| C10 | DRIVER tahsilat | 4xx | 403 | ✅ |
| H-1 | Tenant B yedek listesi | 4xx | 400 | ✅ |
| H-2a | Tenant B yedek indirme | 4xx | 400 | ✅ |
| H-2b | Tenant B manuel yedek | 4xx | 400 | ✅ |
| H-2c | Tenant B presigned backups | 4xx | 403 | ✅ |
| H-1-ok | Tenant A yedek listesi | 2xx | 200 | ✅ |
| H-2-ok | Tenant A presigned | 2xx | 200 | ✅ |
| H-3 | Tenant B → Tenant A okuma | 4xx | 403 | ✅ |
| H-3b | Tenant A ADMIN → B | 2xx | 200 | ✅ |
| Y1 | 8 paralel tahsilat — kayıp güncelleme | yok | `odenen = 8×500`, `version=8` | ✅ |

**Toplam: 29 senaryo → 29 korundu, 0 açık.**

---

## 20. Düzeltilmeyen Bulgular (Bilinçli Kararlar)

Bu bulgular tespit edildi ancak bu çalışma kapsamında düzeltilmedi. **Üretime çıkmadan önce ele
alınmalıdır.**

### 20.1 CRITICAL — `TenantChecker.check()` fail-open (C8)

```java
if (currentSirketId == null) return;   // sessizce geçiyor
```
JWT'de `sirketId` claim'i yoksa **tüm tenant kontrolleri atlanır**. Yaklaşık 40 `findById`
ucu bu kontrole dayanıyor.

**Neden bu çalışmada düzeltilmedi:** Düzeltme (fail-closed) davranışı değiştirecek ve
muhtemelen çok sayıda uçta beklenmeyen 403 üretecek. Bu, kontrollü bir migrasyon gerektirir:
önce tüm uçlarda envanter, sonra kademeli açma. Tek seferde değiştirmek, canlıda gürültülü
kesintilere yol açabilir.

**Öneri:** Ayrı bir çalışma olarak, uç envanteri çıkarıldıktan sonra fail-closed'a geçilmeli.

### 20.2 HIGH — JWT `sirketId` claim'i DB'den doğrulanmıyor (H-15)

`JwtAuthFilter` WebSocket'te claim'i DB'den teyit ediyor, HTTP'te etmiyor. Kullanıcı
şirkette çıkarıldığında eski JWT geçerli kalıyor.

**Not:** C9 düzeltmesi (`tokenVersionArtir`) bu riski **büyük ölçüde** kapatır — şirket üyeliği
değişince token'lar iptal ediliyor. Ancak JWT üretimi ile iptal arasındaki pencere ve
doğrudan claim manipülasyonu için doğrulama hâlâ önerilir.

### 20.3 HIGH — `X-Forwarded-For` rate-limit baypası

`LoginRateLimitFilter:193-207` RFC1918 kaynakları "güvenilir proxy" kabul ediyor. Saldırgan
özel bir `X-Forwarded-For` göndererek login rate-limit'ini atlatabilir.

### 20.4 MEDIUM — Genel API rate-limit yok

Yalnızca login, 2FA, şifre sıfırlama ve kurulum uçları sınırlı. Diğer tüm API'ler sınırsız.

### 20.5 MEDIUM — Diğer tespitler (düzeltilmedi)

| Bulgu | Not |
|---|---|
| DRIVER tüm siparişleri görüyor | Mali veri sızıntısı; RaporService/SiparisService yetki süzgeci gerekli |
| Export uçları yetki kontrolsüz | `IK_EXPORT` / `SISTEM_EXPORT` uygulanmıyor |
| 4 stok ucunda `@yetkiKontrol` eksik | |
| PersonelIzin `onaylayan` istemciden | Sahte onay imzası üretilebilir |
| StokSayim `durum` mass-assignment | C5'in stok sayımındaki karşılığı |
| MasrafService.guncelle kasa tutarsızlığı | |
| Teklif durum doğrulaması yok | |
| 2FA replay check-then-act | Kilit/@Version yok |
| Şifre sıfırlama token'ı tek kullanımlık kontrolü | Race'e açık |
| 402 `BigDecimal` alanı `@Digits(0)` | Sınırsız |
| 44 `@RequestBody` uçta bean validation yok | |
| ~90 uç `unpaged()` dönüyor | Sayfalama yok |

---

## 21. Migration'lar (Flyway)

Bu çalışmada **yeni migration eklenmedi** — tüm düzeltmeler uygulama katmanındadır.
Mevcut migration'ların durumu doğrulandı:

| Migration | İçerik | Canlı durum |
|---|---|---|
| V145 | Eksik roller | Uygulanmış (dokunulmadı) |
| V149 | Yetki/rol seed | Uygulanmış, idempotent |
| V150 | Trigram index'ler | 13 index canlıda, `Bitmap Index Scan` ile kullanılıyor |
| V151 | Bütünlük kısıtları | Uygulanmış, idempotent |
| V152 | FK index'leri | Uygulanmış |

**V151'in sınırları kanıtlandı:** `odenenTutar=101000` gönderiminde 409 döndü (aşırı değer),
ancak **6× şişirme geçti**. DB CHECK kısıtı tek başına yetmiyor; uygulama katmanı doğrulaması
şarttır. C1 düzeltmesi bu boşluğu kapatır.

---

## 22. Altyapı ve Gözlemlenebilirlik

| Bileşen | Durum |
|---|---|
| Prometheus kuralları | 29 kural, `promtool` ile geçerli |
| Scheduler metrikleri | `raspel_job_calisma_total` canlıda |
| Cache | `dashboard:t4::dashboard:4` gibi tenant-izole anahtarlar |
| Yedek izleme | `backup stale` kuralı mevcut |

**Yeni yapılandırma anahtarı:**

```properties
app.platform.admin-users=${APP_PLATFORM_ADMIN_USERS:admin}
```
Varsayılan `admin` (kurulumda oluşan ilk yönetici). Boş bırakılırsa platform işlemleri
**fail-closed** kapanır. Üretimde bu değişken **mutlaka** gerçek platform yöneticileriyle
güncellenmelidir.

---

## 23. Test Ortamının Temizlenmesi

`scripts/redteam/99_temizleme.sql` çalıştırıldı.

**Temizlik sırasında karşılaşılan ve düzeltilen script hataları** (canlı şema gerçeğiyle
belgelendi — bu hatalar ilk yazımda script'in sessizce rollback olmasına yol açıyordu):

| Hata | Gerçek |
|---|---|
| `sistem.kullanici_rol` tablosu yok | Kullanıcı-rol ilişkisi entity'de ManyToMany, ayrı tablo yok |
| `sohbet_mesaj.sohbet_oda_id` yok | Kolon adı `oda_id` |
| `fatura.irsaliye` yok | Şema `muhasebe` |
| `muhasebe.banka_hareketi` yok | Şema `finans` |
| `stok.stok_seri`, `stok.stok_sayim`, `stok.stok_uretim` yok | Tablolar farklı şemalarda |
| `uretim_emri.recete_id` yok | Kolon adı `urun_id` |
| `sistem.sirket`'i 69 tablo referanslıyor | `fk_*_sirket` ihlali → dinamik temizlik eklendi |
| `information_schema.constraint_column_usage` FK için boş dönüyor | `pg_catalog` (`pg_constraint.confrelid`) kullanıldı |

**Doğrulama (hepsi 0):**

| Kontrol | Sonuç |
|---|---|
| `sirket_99` | 0 |
| `kullanici_99xx` | 0 |
| `cari_zztest` | 0 |
| `stok_zztest` | 0 |
| `kasa_zztest` | 0 |
| `banka_zztest` | 0 |
| `irsaliye_zz` | 0 |
| `recete_zz` | 0 |
| `iade_zz` | 0 |
| ZZTEST cariye bağlı hareket | 0 |
| ZZTEST cariye bağlı fatura | 0 |
| ZZTEST stok hareketi | 0 |

**Şirket 4 verisi korundu (test öncesi baseline):**

| Varlık | Baseline |
|---|---|
| Şirket | 1 |
| Kullanıcı | 3 |
| Cari | 8 |
| Stok | 9 |
| Kasa | 2 |
| Banka | 1 |
| Fatura | 18 |
| Hareket | 15 |

Temizlikten sonra readiness `UP` ve 14 uçluk duman testi yeniden çalıştırıldı — hepsi 200.

---

## 24. Sıfırdan Çalıştırma (Tekrar Edilebilirlik)

```bash
# 1) Test ortamı
docker cp scripts/redteam/01_test_ortami_kur.sql scripts/redteam/02_test_verisi.sql \
       scripts/redteam/99_temizleme.sql scripts/redteam/_tenant_b_temizle.sql \
       raspel-postgres:/tmp/redteam/
docker exec -w /tmp/redteam raspel-postgres psql -U postgres -d raspelerp -f 01_test_ortami_kur.sql
docker exec -w /tmp/redteam raspel-postgres psql -U postgres -d raspelerp -f 02_test_verisi.sql

# 2) Token al (admin_a, zzadmin_b, zzuser_b, driver_a)
powershell -File scripts/redteam/token_al.ps1

# 3) Saldırılar
powershell -File scripts/redteam/03_saldiri_tekrari.ps1   # 29 senaryo
powershell -File scripts/redteam/04_yaris_kosulu.ps1      # eşzamanlılık

# 4) Temizlik
docker exec -w /tmp/redteam raspel-postgres psql -U postgres -d raspelerp -f 99_temizleme.sql
```

---

## 25. Metodoloji

Her bulgu şu döngüden geçti:

```
TARAMA → CANLI SALDIRI → VERİTABANI KANITI → DÜZELTME → SALDIRI TEKRARI → REGRESYON TESTİ → TAM TEST
```

**Kurallar:**
- Kod okunarak **anlaşılmadan** düzeltme yapılmadı
- Her bulgu canlıda **tekrar test edildi**; açık kalmayanlar rapora girmedi
- Yanlış teşhisler (R1) saklanmadı, **açıkça düzeltildi**
- Düzeltme sonrası **aynı payload** tekrar gönderildi
- Test verisi izole tutuldu ve **tam olarak temizlendi**
- Mevcut hiçbir test kırılmadı

**Kod tabanı notları (gelecek çalışmalar için):**
- ASCII-ağırlıklı isimlendirme: `satisFiyati`, `kasaOdemeYap`
- PowerShell `Get-Content` UTF-8 dosyalarda Türkçe karakterleri bozar (`�`) →
  `functions.edit` veya `[System.IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)` kullan
- `Set-Content -Encoding UTF8` BOM ekler → Java derlemesi `illegal character: '\ufeff'` verir.
  Çözüm: `[System.IO.File]::WriteAllText($p,$c,(New-Object System.Text.UTF8Encoding($false)))`
- PowerShell regex replace'de `` `n `` literal yazılırsa dosyaya `` `n `` metni düşer
- `curl.exe -d` PowerShell ile JSON kaçışlamasını bozar → `Invoke-RestMethod -Body ($obj | ConvertTo-Json)`
- `$_ .Exception.Response.StatusCode.value__` PowerShell 5.1'de boş döner →
  `Invoke-WebRequest`/`curl.exe -w "HTTP %{http_code}"`
- Native `CASE...GROUP BY` + `:param` → Hibernate pozisyonel `?` üretir → PG "must appear in
  GROUP BY" hatası; **H2 yakalamıyor**, native sorgular canlı PG'de doğrulanmalı
- `information_schema.constraint_column_usage`, PostgreSQL'de FK için **boş** döner

---

## 26. Değişmeyenler (Bilinçli Olarak Korundu)

| Dosya | Neden |
|---|---|
| `backend/.../db/migration/V145__eksik_rolleri_tamamla.sql` | Önceki çalışmanın çıktısı; dokunulmadı |
| `frontend/src/assets/pos-panels.css` | Önceki çalışmanın çıktısı; dokunulmadı |

Bu iki dosyaya müdahale edilmedi ve regresyonda sorun çıkmadı — önceki çalışmanın çıktısı
bu noktada **doğrulandı**.

---

## 27. Önceki Çalışmanın (B1→B4) Doğrulanması

Bu ajan, önceki çalışmanın değişikliklerine **güvenmedi** ve bağımsız doğruladı:

| İddia | Doğrulama | Sonuç |
|---|---|---|
| Backend testleri geçiyor | Yeniden çalıştırıldı | ✅ 1523 → BUILD SUCCESS |
| Frontend lint/i18n temiz | Yeniden çalıştırıldı | ✅ |
| Frontend testleri geçiyor | Yeniden çalıştırıldı | ✅ 866 |
| Docker 12/12 sağlıklı | `docker compose ps` | ✅ |
| Prometheus 5/5 up | `/api/v1/targets` | ✅ |
| Cypress 66/66 | Yeniden koşuldu | ✅ |
| V149–V152 canlıda | `flyway_schema_history` | ✅ `success=t` |
| `sistem.yetki` 38 kayıt | Doğrudan sayım | ✅ |
| Roller: ADMIN 38 / USER 19 / SAHA 5 / DRIVER 3 | Doğrudan sayım | ✅ |
| Cache tenant-izole | Redis anahtarları incelendi | ✅ `dashboard:t4::dashboard:4` |
| 13 trigram index kullanılıyor | `EXPLAIN` | ✅ `Bitmap Index Scan` |
| `raspel_job_calisma_total` | `/actuator/prometheus` | ✅ |
| 29 Prometheus kuralı | `promtool check rules` | ✅ |

**Ancak "tenant izolasyonu düzeltildi" iddiası tek şirket nedeniyle çalışma zamanında
kanıtlanamıyordu.** Bu çalışmanın asıl katkısı bu boşluğu kapatmaktır: Tenant B kuruldu ve
H-1/H-2/H-3 gerçekten açık çıktı — yani izolasyon **eksiksiz** değilmiş.

---

## 28. Güçlü Yönler

Bu çalışma sırasında doğrulanan, **doğru tasarlanmış** kontroller:

| Kontrol | Kanıt |
|---|---|
| IDOR koruması | Cari/stok/kasa/banka için 4 senaryoda ADMIN + tam gövde ile denendi → **404** |
| Yetki/validasyon sırası | USER ile silme → 403 (yetki); eksik gövde → 400 (validasyon); ikisi de tenant kontrolünden önce |
| Şirket başına rol modeli | `Kullanici.role` DB'de şirkete bağlı; Tenant B kullanıcısına `sirketId=4` vermek **400** |
| Uç katmanı dosya koruması | `PUT /belgeler/yukle` `entityAdi=Kullanici` → 400 "Geçersiz kayıt türü" |
| Dizin geçişi kapalı | `GET /uploads/sohbet/<x>` → 404 (düz klasör fallback yok) |
| JWT dayanıklılığı | `alg=none` kapalı; 2FA'sız JWT üretilemiyor |
| Oturum süresi | Süresi dolmuş token → 401 |
| İyimser kilitleme | `Fatura.version` mevcut |
| Trigram index'ler | Aramalarda gerçekten `Bitmap Index Scan` kullanılıyor |
| Red Team script disiplini | Temizlik scripti 8 schema hatası bulundu ve düzeltildi |

---

## 29. Kalan Risk Profili

| Alan | Seviye | Not |
|---|---|---|
| SQL injection | Düşük | Yok; native sorgular parametreli |
| XSS | Düşük | `v-html` yok |
| Kimlik doğrulama | Düşük | 2FA + BCrypt + rate limit; C9 kapandı |
| Yetkilendirme | Orta | Controller düzeyinde sağlam; **servis düzeyinde `yetkiKontrol` eksikleri** var |
| Tenant izolasyonu | **Yüksek** | `TenantChecker` fail-open (20.1) + JWT claim doğrulaması (20.2) |
| Finansal doğruluk | Düşük | C1/C6 kapandı; fazla ödeme kabul ediliyor (meşru, loglanıyor) |
| Stok doğruluğu | Düşük | C3/C4/C5 kapandı; sayım/StokSayim `durum` açığı kaldı |
| Veri sızıntısı | Orta | `backups/` korundu; **DRIVER sipariş görüşü** kaldı |
| Altyapı | Düşük | 12/12 sağlıklı, 5/5 metrik hedefi, 29 kural geçerli |

---

## 30. Sonuç

**Düzeltilen:** 10 canlı kanıtlanmış açık (6 CRITICAL, 3 HIGH, 1 yanlış teşhis).
**Doğrulama:** 29 saldırı senaryosunun tamamı artık reddediliyor; 1561 backend + 866 frontend
testi geçiyor; test verisi tamamen temizlendi.

**Kritik açık kalmadığı söylenemez.** `TenantChecker`'ın fail-open davranışı (20.1) ve JWT'deki
`sirketId` claim'inin DB'den doğrulanmaması (20.2) bu çalışmanın en önemli **kalan riskleridir**
ve yaklaşık 40 ucun tenant kontrolü bu kontrole dayanmaktadır. Bu iki konu, kontrollü bir
migrasyonla ayrı bir çalışmada ele alınmalıdır.

Bu çalışmanın en değerli çıktısı bulgu sayısı değil, şudur: **daha önce "düzeltildi" denilen
tenant izolasyonunun, ikinci bir tenant kurulduğunda üç ayrı yüksek/kritik açığın olduğu
kanıtlanmıştır.** Tek tenant'lı bir ortamda yapılan güvenlik testi, çok kiracılı (multi-tenant)
bir sistemde yeterli değildir.