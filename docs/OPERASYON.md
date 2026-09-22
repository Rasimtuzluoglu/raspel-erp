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
- Günlük yedek doğrulama uygulama içinde otomatik çalışır (`BackupService.gunlukYedekDogrulama`).

### 4a. PITR (Point-in-Time Recovery / WAL arşivleme)

PostgreSQL artık `archive_mode=on` ile çalışır; WAL dosyaları `./backups/wal-archive`
(container içinde `/wal-archive`) altına arşivlenir. Böylece bir felakette en son
base backup'tan sonra **istenen ana** kadar geri dönülebilir.

- **Base backup** al (öneri: haftada bir, Retention 14 gün):

  ```powershell
  powershell -File scripts/pitr-basebackup.ps1 -KeepDays 14
  ```

  Çıktı: `backups/wal-archive/base/<YYYYMMDD_HHmmss>/` (tar.gz + pg_wal).

- **Arşiv kontrolü**:

  ```bash
  docker exec raspel-postgres sh -c "ls -1 /wal-archive | head; echo '---base---'; ls -1 /wal-archive/base"
  docker exec raspel-postgres psql -U postgres -c "SELECT * FROM pg_stat_archiver;"
  ```

  `failed_count` artıyorsa `archive_command` başarısız demektir; WAL silinmez, disk büyür.

- **Kurtarma adımları** (felaket anında, dikkatli uygulayın):

  1. Uygulamayı durdurun: `docker compose stop backend frontend`.
  2. Mevcut veriyi koruma altına alın (üzerine yazmadan önce):
     `docker run --rm -v raspel-erp_postgres_data:/data -v "%CD%/backups:/backup" alpine sh -c "cd /data && tar czf /backup/pre-restore-$(date +%s).tar.gz ."`
  3. Data dizinini boşaltıp base backup'ı açın:
     `docker run --rm -v raspel-erp_postgres_data:/var/lib/postgresql/data -v "%CD%/backups/wal-archive:/wal-archive" alpine sh -c "rm -rf /var/lib/postgresql/data/* && tar xzf /wal-archive/base/<STAMP>/base.tar.gz -C /var/lib/postgresql/data && mkdir -p /var/lib/postgresql/data/pg_wal && tar xzf /wal-archive/base/<STAMP>/pg_wal.tar.gz -C /var/lib/postgresql/data/pg_wal"`
  4. Kurtarma hedefini yazın (`/var/lib/postgresql/data/postgresql.auto.conf`):
     ```
     restore_command = 'cp /wal-archive/%f %p'
     recovery_target_time = '2026-09-20 16:00:00+03'
     recovery_target_action = 'promote'
     ```
     ve data dizinine `recovery.signal` adlı boş dosya koyun.
  5. `docker compose up -d postgres`; loglarda `recovery stopping before ...` ve
     `database system is ready to accept connections` görülene kadar bekleyin.
  6. Doğrulayın: `docker exec raspel-postgres psql -U postgres -d raspelerp -c "select count(*) from fatura.fatura;"` ve
     `docker compose up -d backend frontend`.

  > Not: `recovery_target_time` istediğiniz ana göre ayarlanır; belirtilmezse tüm
  > arşivlenmiş WAL uygulanır ve mevcut son ana kadar gelinir.

### 4b. Offsite (harici) yedek

MinIO aynı sunucuda olduğundan offsite kopya ayrıca alınmalıdır. Script üç hedefi
destekler (birini ortam değişkeniyle seçin):

```powershell
# S3/B2/Drive (rclone kurulu olmalı)
$env:OFFSITE_RCLONE_REMOTE="b2:raspel-backups"; powershell -File scripts/offsite-replicate.ps1
# Uzak sunucu
$env:OFFSITE_SCP_TARGET="user@server:/backups/raspel"; powershell -File scripts/offsite-replicate.ps1
# Harici disk / NAS
$env:OFFSITE_DIR="E:\RasPelBackups"; powershell -File scripts/offsite-replicate.ps1
```

Bu komutu günlük yedek sonrasına zamanlayın (Windows Task Scheduler veya cron).

## 5. Sağlık İzleme ve Uyarılar

- `GET /actuator/health` (kimliksiz) — container healthcheck.
- `GET /actuator/prometheus` — **kimliksiz değil**: ya `ROLE_ADMIN` JWT ya da paylaşılan
  scrape token'ı (`APP_METRICS_SCRAPE_TOKEN`, `X-Metrics-Token` başlığı veya `Authorization: Bearer`)
  gerekir. Prometheus bu token'ı `docker secret` üzerinden okur; token boşsa scrape devre dışıdır.
- Diğer `/actuator/**` uçları **yalnızca ADMIN**.
- **Alertmanager** bildirim kanalları (ortam değişkeniyle):
  - Slack: `SLACK_WEBHOOK_URL` (kanal: `#raspel-alerts`).
  - E-posta: `ALERT_EMAIL_TO` + `SMTP_SMARTHOST`, `SMTP_FROM`, `SMTP_USERNAME`, `SMTP_PASSWORD`.
  - Tanımlı olmayan kanalın bloğu başlangıçta otomatik kaldırılır (htpasswd/güvenli).
- Kural seti (`config/prometheus/alert.rules.yml`): ServiceDown, yüksek heap,
  yüksek 5xx oranı (uyarı %5 / kritik %2), DB bağlantı havuzu doygunluğu,
  yüksek HTTP gecikmesi (p95 uyarı / p99 kritik), yüksek disk doluluk (%85),
  yüksek JVM GC duraklaması ve RabbitMQ kuyruk birikmesi. SLO eşikleri:
  p99 gecikme 3 sn, 5xx oranı %2 (kritik).

## 6. Sürüm Yükseltme

```bash
git pull origin main
docker compose build backend frontend
docker compose up -d --no-deps --force-recreate backend frontend
# Flyway migration'ları backend başlangıcında otomatik uygulanır (VNNN__*.sql)
```

Yeni sürümde `main.js` sürüm anahtarı değiştiği için istemci önbelleği ve eski
form taslakları otomatik temizlenir.
