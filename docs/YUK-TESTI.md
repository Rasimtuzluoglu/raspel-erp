# Yük ve Stres Testi

RasPel ERP'nin sürekli ve yoğun kullanım (3 kasa + 2 depo) altındaki davranışını
ölçmek için k6 tabanlı yük testi altyapısı. Araç ve senaryolar **Docker** içindedir;
ayrı kurulum gerekmez.

## Araç ve Çalıştırma

```bash
# Veri üret (izole "YUK TESTI" şirketi; demo veriye dokunmaz)
powershell -File scripts/generate-test-data.ps1

# Senaryolar (--profile loadtest)
docker compose --profile loadtest run --rm k6 run smoke.js
docker compose --profile loadtest run --rm k6 run load.js
docker compose --profile loadtest run --rm k6 run stress.js
docker compose --profile loadtest run --rm k6 run spike.js
docker compose --profile loadtest run --rm k6 run write-flow.js
docker compose --profile loadtest run --rm k6 run correctness.js
# Soak (uzun): DURATION ile
docker compose --profile loadtest run --rm -e DURATION=30m k6 run soak.js
```

Parametreler (ortam değişkeni): `BASE_URL` (varsayılan `http://raspel-backend:8081`),
`TEST_USER` (varsayılan `admin_test`), `TEST_PASSWORD`, `TEST_SIRKET_ID` (varsayılan `5`).

> Not: Testler backend'e doğrudan (Docker ağı) gider, Traefik rate-limit'ini atlar.
> Giriş tek kez `setup()` içinde yapılır (login rate-limit'e takılmamak için).

## Test Verisi

`scripts/test-data-generator.sql` (idempotent): 5.000 cari, 2.000 stok,
100.000 fatura + 300.000 kalem, 150.000 cari hareket, 200.000 stok hareket
(izole `sirket_id`). Yaklaşık 20 saniyede üretilir.

Temizlemek için:

```sql
DELETE FROM fatura.fatura_kalem k USING fatura.fatura f, sistem.sirket s
  WHERE k.fatura_id=f.id AND f.sirket_id=s.id AND s.ad='YUK TESTI';
DELETE FROM fatura.fatura f USING sistem.sirket s WHERE f.sirket_id=s.id AND s.ad='YUK TESTI';
DELETE FROM cari.hareket h USING sistem.sirket s WHERE h.sirket_id=s.id AND s.ad='YUK TESTI';
DELETE FROM stok.stok_hareket sh USING stok.stok st, sistem.sirket s
  WHERE sh.stok_id=st.id AND st.sirket_id=s.id AND s.ad='YUK TESTI';
DELETE FROM cari.cari_hesap c USING sistem.sirket s WHERE c.sirket_id=s.id AND s.ad='YUK TESTI';
DELETE FROM stok.stok st USING sistem.sirket s WHERE st.sirket_id=s.id AND s.ad='YUK TESTI';
```

## Ölçülen Sonuçlar (tek makine, Docker Desktop)

| Senaryo | Eşzamanlılık | Sonuç |
|---|---|---|
| Smoke | 1 VU / 30 sn | p95 **90 ms**, hata %0 |
| Load | 0→20 VU / 6 dk | 9.348 istek, p95 **28 ms**, hata %0 |
| Write flow (POS satış) | 0→15 VU / 4,5 dk | 2.207 satış, p95 **48 ms**, hata **%0** |
| Spike | 5→100 VU ani / 3,5 dk | 24.062 istek, p95 **98 ms**, hata **%0,1**, restart 0 |
| Soak | 20 VU / 30 dk | 186.698 istek, p95 **30 ms**, hata **%0**, restart 0, bellek stabil |
| Stress (kırılma noktası) | 0→200 VU / 15 dk | 150–200 VU üzerinde kapasite sınırı; bkz. aşağıda |
| Correctness | 1 iterasyon | oversell yok (10/10), idempotency tek kayıt |

> Ölçümler aynı makinede (uygulama + yük üreticisi) yapıldığı için görecelidir;
> gerçek kapasite için ayrı bir yük üreticisi önerilir.

### Kapasite Sınırı

- **20 VU (gerçekçi üretim: 3 kasa + 2 depo)**: %0 hata, p95 < 30 ms — rahat.
- **100 VU (ani yoğunluk)**: %0,1 hata, restart 0 — kabul edilebilir.
- **150–200 VU**: HikariCP pool (50) doygunluğa ulaşır (`waiting` artar, 5 sn
  connection-timeout aşılır) ve heap 1,28 GB'a dayanır. Bu, tek backend
  instance'ının **bilinen kapasite sınırıdır**; yatay ölçekleme (replica) veya
  daha büyük instance gerekir. Stress senaryosu bu sınırı bulmak için tasarlanmıştır.

## Yük Testinin Ortaya Çıkardığı ve Giderilen Kritik Sorunlar

1. **Sayfalama bellekte yapılıyordu (çökme)**: `FaturaRepository` sayfalı sorgularında
   `kalemler` koleksiyonu fetch ediliyordu → Hibernate tüm sonucu belleğe yükleyip
   sayfalamayı bellekte yapıyordu (`HHH90003004`). 100k fatura ile backend çöküyordu.
   **Çözüm:** koleksiyon fetch kaldırıldı, `Fatura.kalemler` için `@BatchSize(50)`.
   Faturalar listesi 1.489 ms → **263 ms**.
2. **Eşzamanlı fatura numarası mükerrerliği**: `max+1` üretimi yarış koşuluna açıktı
   → `uk_fatura_no_sirket` ihlali (%13,8 hata). **Çözüm:** `sistem.seri_sayac` (V113)
   üzerinden atomik `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` + kontrolörde
   mükerrer-numara retry'ı. Hata **%0**.
3. **Cari bakiyesinde optimistic-lock çakışması**: oku-değiştir-yaz (%10,8 hata).
   **Çözüm:** `UPDATE CariHesap SET bakiye = bakiye + :tutar` atomik güncelleme.
   Hata **%0**.
4. **`/api/dashboard` sınırsız fatura yüklemesi (bellek şişmesi / OOM)**: dashboard
   her açılışta vadesi yaklaşan **6.293 faturayı** entity olarak yüklüyordu (~1 MB
   yanıt) → yük altında heap tükeniyordu. **Çözüm:** `findVadesiGecen`/`findVadesiYaklasan`
   `Pageable` ile sınırlandı (en çok 5 bildirim), alacak yaşlandırma toplamları DB
   aggregate'e çevrildi. Yanıt 999.475 → **5.821 byte** (171× küçülme).
5. **Diğer sınırsız listeleme sorguları**: `GunlukOzetService`, `AjandaService`,
   `YoneticiKokpitService`, `RaporService` (yaşlandırma) ve `TahsilatService`
   (tahsis + hatırlatma, 2 kullanım) tüm şirket faturalarını belleğe yükleyip
   filtrelıyordu. **Çözüm:** cari'ye özel sorgular (`findTahsilatEdilecekByCari`) ve
   grup bazında aggregate (`cariBazindaMaksGecikme`, native `MAX(:bugun - vade_tarihi)`).
6. **JVM cgroup limitini görmüyordu**: 1 GiB konteynerde JVM heap'i **host** RAM'ine
   göre (4,15 GiB) hesaplıyordu; stress/spike altında konteyner tekrar tekrar
   OOM-kill yiyordu (`oom` → exit 137). **Çözüm:** Dockerfile `ENTRYPOINT`'te açık
   `-Xms256m -Xmx1280m -Xss512k -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m`;
   `mem_limit` **2 GB** (`.env` `BACKEND_MEM_LIMIT`), Tomcat thread havuzu 100'e
   sınırlandı. Restart **%88 hata → %0**.

## Doğruluk Testi Kapsamı (`correctness.js`)

- **Stok aşırı satış (oversell)**: 10 adet stok iken 30 eşzamanlı satış → yalnızca 10
  başarılı, kalan miktar 0, negatife düşme yok (pessimistic kilit).
- **Fatura idempotency**: aynı `X-Idempotency-Key` ile 10 eşzamanlı satış → **tek** fatura.

## Eşikler

- Okuma p95 < 500–1000 ms, satış p95 < 1500 ms, hata oranı < %0,5.
- `hikaricp_connections_pending` uzun süre > 0 olmamalı (bkz. Prometheus kuralı
  `DatabasePoolSaturation`).
- Soak boyunca JVM heap ve DB bağlantı sayısı sabit kalmalı (sızıntı yok).

