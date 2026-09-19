# Operasyon Runbook'u

Bu belge üretim ortamında RasPel ERP'nin çalıştırılması, arıza müdahalesi ve
bakımı için pratik adımları içerir. Ayrıntılı kurulum için `docs/KURULUM.md`,
canlıya çıkış kontrolü için `docs/GO-LIVE.md` dosyalarına bakın.

## 1. Zamanlanmış İşler ve Kilitler (ShedLock)

Tüm zamanlanmış işler **ShedLock** ile korunur: yatay ölçekli (birden çok backend
instance) çalışmada her iş yalnızca bir düğümde tetiklenir. Kilit kayıtları
`sistem.shedlock` tablosunda tutulur.

| İş | Kilit adı | Cron | Açıklama |
|----|-----------|------|----------|
| Günlük yedek | `backupDaily` | 03:00 | `app.backup.auto-cron` |
| Bulut senkron | `backupCloudSync` | 03:30 | `app.backup.cloud-auto-cron` |
| POS gün sonu | `posGunSonu` | 23:00 | Banka bakiyesi + gün sonu kaydı |
| TCMB kurları | `tcmbKurGuncelle` | 09/12/16 | Sarkan HTTP zaman aşımı 10 sn |
| Tekrarlayan fatura | `tekrarlayanFatura` | 05:00 | Vadesi gelen tanımları işler |
| Hatırlatıcı | `hatirlatici` | 08:00 | Vadesi geçen fatura e-postası |
| Günlük özet | `gunlukOzet` | 07:00 | — |
| Teslimat gecikme | `teslimatGecikme` | 07:30 | — |
| Anomali tarama | `anomaliTarama` | 06:00 | — |
| Hata log temizliği | `eskiHataLogTemizligi` | 04:00 | Retention: `app.hata-log.retention-days` |
| Yedek doğrulama | `backupDogrulama` | 04:30 | — |

**Takılı kilit kontrolü:**

```sql
SELECT name, lock_until, locked_at, locked_by FROM sistem.shedlock;
```

`lock_until` geçmişte olmasına rağmen kayıt duruyorsa zararsızdır (sonraki tetikleme
üzerine yazılır). Acil durumda elle serbest bırakma:

```sql
DELETE FROM sistem.shedlock WHERE name = 'backupCloudSync';
```

## 2. Idempotency (Mükerrer Kayıt Koruması)

Finansal kayıt oluşturan uç noktalar (özellikle fatura) `X-Idempotency-Key`
başlığıyla korunur. Anahtar `IdempotencyService` ile yönetilir:

- Redis varsa: `SET key ISLENIYOR NX EX 600` → replikalar arası geçerli.
- Redis yoksa: JVM içi yedek map (yalnızca tek instance / dev).
- İşlem başarılıysa anahtar `OK:<kayitId>` olarak 10 dk saklanır; aynı anahtarla
  gelen tekrar isteği oluşan kaydı döner, yeni kayıt üretmez.
- İşlem hata verirse kilit bırakılır (`serbestBirak`).

**İzleme:**

```sql
-- sistem.shedlock disinda idempotency Redis'te tutulur
docker exec -it raspel-redis redis-cli -a "$REDIS_PASSWORD" KEYS 'idem:*'
```

**Önemli:** Frontend `crypto.randomUUID()` ile anahtar üretir. Mobil/entegrasyon
istemcileri de ağ tekrarına karşı aynı anahtarı yeniden kullanmalıdır.

## 3. Transaction Hijyeni

E-posta (SMTP), WebSocket/RabbitMQ bildirimleri ve kritik stok e-postaları
`AfterCommitExecutor` ile **commit sonrasında** gönderilir. Böylece:

- Veritabanı transaction'ı yavaş dış servis için açık tutulmaz (kilit süresi kısalır).
- Geri alınan (rollback) bir işlem için yanlış bildirim/e-posta çıkmaz.

Bildirim hatası iş akışını bozmaz; yalnızca loglanır (`Commit sonrası gorev
calistirilamadi`).

## 4. Yedekleme ve Kurtarma

- Günlük otomatik `pg_dump` → `APP_BACKUP_DIR`; şifreli bulut kopyası MinIO'ya.
- Elle yedek: `scripts/backup.ps1` (parola argv'de değil ortam değişkeniyle geçer).
- Kurtarma tatbikatı: `scripts/disaster-recovery-test.ps1` (üç ayda bir).
- **Offsite**: MinIO aynı sunucuda olduğundan S3 uyumlu harici depoya replikasyon
  veya `backup.ps1` çıktısının harici ortama kopyalanması gerekir.
- **PITR** için PostgreSQL WAL arşivleme (`archive_mode=on`) ayrıca yapılandırılmalıdır;
  mevcut kurulum günlük snapshot seviyesindedir.

## 5. Sağlık İzleme

- `GET /actuator/health` (kimliksiz) — container healthcheck.
- `GET /actuator/prometheus` (kimliksiz) — Prometheus scrape.
- Diğer `/actuator/**` uçları **yalnızca ADMIN**.
- Kafka/RabbitMQ ve Redis bağlantıları Prometheus/Grafana panolarından izlenir.

## 6. Sürüm Yükseltme

```bash
git pull origin main
docker compose build backend frontend
docker compose up -d --no-deps --force-recreate backend frontend
# Flyway migration'ları backend başlangıcında otomatik uygulanır (VNNN__*.sql)
```

Yeni sürümde `main.js` sürüm anahtarı değiştiği için istemci önbelleği ve eski
form taslakları otomatik temizlenir.
