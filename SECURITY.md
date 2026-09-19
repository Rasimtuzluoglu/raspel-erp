# Güvenlik Politikası (SECURITY)

## Güvenlik Açığı Bildirimi

Güvenlik açığı bulduysanız lütfen **public issue açmayın**. Şu adresten iletin:

- E-posta: `security@raspel-erp.com` (varsa)
- Ya da GitHub Security Advisory: https://github.com/Rasimtuzluoglu/raspel-erp/security/advisories

Yanıt süresi: 72 saat içinde ilk geri bildirim hedeflenir.

## Sorumlu Açıklama

- Açığı özel olarak bildirin, yayınlamayın
- Düzeltme yayınlanana kadar bekleyin
- Açığı kötüye kullanmayın

## Üretim Ortamı Kontrol Listesi

### Zorunlu Ortam Değişkenleri (prod)

Aşağıdakiler **zorunludur**; tanımlanmazsa uygulama fail-fast yapar:

```bash
JWT_SECRET=            # en az 256-bit (32 byte) base64 değer
POSTGRES_PASSWORD=
REDIS_PASSWORD=
RABBITMQ_PASSWORD=
GRAFANA_PASSWORD=
```

### Güvenlik Ayarları (prod otomatik)

- JWT cookie: `Secure` + `HttpOnly` + `SameSite=Strict`
- HSTS aktif
- Actuator: yalnızca `health` ve `prometheus` kimliksiz erişilebilir; diğer uçlar ADMIN
- `/api/sirketler/aktif` public değil
- Şifre politikası: min 8, max 72, büyük/küçük harf + rakam + özel karakter
- Giriş rate limit: 5 deneme / 60 sn (IP **ve** kullanıcı adı bazında; şifre sıfırlama uçları dahil)
- Zayıf/varsayılan parolalar (`postgres`, `raspel`, `raspelRedis2026`, `admin`, `123456` vb.) prod'da reddedilir (fail-fast)
- CORS joker (`*`) origin prod'da reddedilir
- CSV export: formula injection koruması (`=`, `+`, `-`, `@` önekleri)
- Container'lar non-root kullanıcıyla çalışır
- Ağ izolasyonu: frontend-net / backend-net / db-net

### Yedekleme / Kurtarma (K10)

- Günlük otomatik `pg_dump` yedeği alınır (uygulama içi `BackupService`, 03:00) ve şifrelenmiş bulut kopyası MinIO'ya yüklenir.
- MinIO aynı sunucuda çalıştığı için **offsite kopya** için MinIO bucket'ı S3 uyumlu harici depoya replike edilmeli veya `scripts/backup.ps1` çıktısı harici ortama kopyalanmalıdır.
- PITR (point-in-time recovery) gereksinimi için PostgreSQL WAL arşivleme (`archive_mode=on`) ayrıca yapılandırılmalıdır; mevcut kurulum günlük snapshot seviyesindedir.
- Kurtarma tatbikatı: `scripts/disaster-recovery-test.ps1` (üç ayda bir çalıştırılması önerilir).

### Anahtar Döndürme

- `JWT_SECRET` değerini düzenli döndürün (öneri: 90 günde bir)
- Döndürme sırasında tüm oturumlar geçersiz olur (kullanıcılar yeniden giriş yapar)

## Kapsam Dışı

- Local dev ortamındaki varsayılan şifreler (`admin/admin123` dev seed) üretimde geçerli değildir; `APP_ADMIN_PASSWORD` ile değiştirilmelidir
