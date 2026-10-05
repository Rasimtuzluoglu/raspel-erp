# RasPel ERP — Kurulum Kılavuzu

## Gereksinimler
- Docker + Docker Compose (v2)
- En az 8GB RAM ayrılmış Docker (compose bellek limitleri toplamı ~6GB)
- Üretimde: alan adı (SSL için), e-posta SMTP hesabı

## 1. Hızlı Kurulum (Geliştirme)

```bash
# 1. .env dosyasını oluştur
cp .env.example .env
# .env içindeki parolaları değiştirin

# 2. Stack'i başlat
docker-compose up -d --build

# 3. Servisler hazır olana kadar bekle (~2-3 dk)
```

| Servis | Adres |
|---|---|
| **Frontend (uygulamaya giriş)** | **http://localhost** |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Traefik Dashboard | http://localhost:8080/dashboard |
| Adminer (DB) | http://localhost:8082 |
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |

### Erişim nasıl çalışır (yerel)

Tüm giriş **Traefik üzerinden `http://localhost` (port 80)** adresinden yapılır. Bu adres
doğrudan frontend konteynerine değil, Traefik'e gider; Traefik isteği konteyner
ağında `raspel-frontend:80` servisine yönlendirir. `/api/...` istekleri ise aynı
adres üzerinden `raspel-backend:8081` servisine gider. Yani **tek adres** yeterlidir;
frontend ve API aynı origin'den geldiği için tarayıcı CORS kurallarına takılmaz.

`https://localhost` da çalışır (tarayıcıda sertifika uyarısı çıkar): `security-headers`
içindeki HSTS, tarayıcıyı localhost'u HTTPS'e zorlar. Yerelde Let's Encrypt
`localhost` için sertifika veremediğinden Traefik kendi self-signed varsayılan
sertifikasını sunar — bu **kasıtlıdır** ve logda ACME hatası üretmez. Gerçek
sertifika yalnızca `APP_DOMAIN` bir alan adı olduğunda alınır.

Dashboard (`http://localhost:8080/dashboard`) yalnızca `127.0.0.1` üzerinden erişilebilir
(`docker-compose.yml` bu portu loopback'e bağlar), dış ağdan açılamaz.

**İlk giriş (kurulum sihirbazı):**
- Hazır demo kullanıcı/veri **gelmez**. Sistemde hiç firma yokken giriş sayfası **İlk Kurulum** sihirbazını gösterir.
- Firma bilgileri + yönetici hesabı (kendi belirlediğiniz kullanıcı adı/şifre) girilir; kayıt sonrası otomatik giriş yapılır.
- Böylece teslim edilen sistemde uygulamanın eklediği hiçbir demo hesap/veri bulunmaz.

> **Uyarı:** İlk kurulumda güçlü bir yönetici şifresi belirleyin.

### 1.1 Sıfırdan Temiz Kurulum (Müşteri Teslimi)

Sistemi müşteriye tamamen boş teslim etmek için:

```bash
# 1. Varsa eski veri/hacimleri temizle (DB, MinIO, yedekler dahil)
docker-compose down -v

# 2. Temiz stack'i başlat
docker-compose up -d --build

# 3. İlk kurulumun gerekli olduğunu doğrula
curl -s http://localhost/api/kurulum/durum   # {"kurulumGerekli":true}
```

Ardından tarayıcıdan giriş yapın; **İlk Kurulum** ekranı açılır ve yalnızca müşteri firması + yönetici hesabı oluşturulur. Demo veri yüklenmez.

> **Not:** `down -v` kalıcı hacimleri (postgres, minio, prometheus, grafana, rabbitmq, redis, letsencrypt, yedekler) siler. Canlı verisi olan bir sunucuda kullanmayın.

## 2. Üretim Kurulumu (SSL ile)

### 2.1 DNS Ayarı
Alan adınızı (ör: `erp.sirketiniz.com`) sunucunuzun IP'sine yönlendirin.

### 2.2 .env Yapılandırması

```env
# Güvenlik
POSTGRES_PASSWORD=<güçlü-parola>
REDIS_PASSWORD=<güçlü-parola>
JWT_SECRET=<uzun-rastgele-base64>

# Traefik dashboard erişimi — ÜRETİMDE ZORUNLU (ikisi birlikte)
# Üretmek için: docker run --rm httpd:alpine htpasswd -nb admin GUCLU_SIFRENIZ
DASHBOARD_BASIC_AUTH=admin:<htpasswd-hash>
# Dashboard router'ına uygulanacak middleware. Boşsa dashboard korumasız çalışır.
# Yerelde boş bırakılabilir (dashboard zaten yalnızca 127.0.0.1'e bağlı).
DASHBOARD_MIDDLEWARES=dashboard-auth@file

# SSL
ACME_EMAIL=sizi@mail.com
APP_DOMAIN=erp.sirketiniz.com

# SMTP (fatura bildirimleri için)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<mail-adresiniz>
SPRING_MAIL_PASSWORD=<uygulama-şifresi>
SPRING_MAIL_FROM=<mail-adresiniz>
```

### 2.3 SSL Sertifikası (Let's Encrypt)
Traefik yapılandırması hazırdır; HTTPS otomatik etkinleşir:
1. DNS kaydının yayılmasını bekleyin
2. `docker-compose up -d traefik`
3. Traefik ilk istekte sertifikayı otomatik alır (`/letsencrypt/acme.json`)
4. HTTP → HTTPS yönlendirmesi otomatik çalışır

> **Not:** Let's Encrypt, `APP_DOMAIN` alan adına istek geldiğinde sertifika düzenler. Sertifika alımı için 80 portu dışarıdan erişilebilir olmalıdır.

`APP_DOMAIN=localhost` iken sertifika **istenmez** (Let's Encrypt `localhost` için
sertifika veremez, "Domain name needs at least one dot" hatası döner). Yerelde
`config/traefik/dynamic.yml` bunu `{{ if ne (env "APP_DOMAIN") "localhost" }}` ile
kontrol eder ve yalnızca self-signed yerel router'ları tanımlar — böylece log
ACME hatalarıyla dolmaz. `APP_DOMAIN` bir alan adı olduğunda üretim router'ları
otomatik devreye girer.

### 2.4 Güvenlik Kontrol Listesi
- [ ] Tüm parolalar değiştirildi
- [ ] JWT_SECRET uzun ve rastgele
- [ ] `APP_DOMAIN` gerçek alan adını gösteriyor (localhost **değil**)
- [ ] `ACME_EMAIL` tanımlı ve geçerli
- [ ] HTTPS çalışıyor (https://alan-adiniz) ve sertifika Let's Encrypt'ten geliyor
- [ ] SMTP e-postası test edildi
- [ ] Grafana varsayılan parolası değiştirildi
- [ ] `DASHBOARD_BASIC_AUTH` güçlü bir parola ile tanımlandı
- [ ] `DASHBOARD_MIDDLEWARES=dashboard-auth@file` tanımlandı (aksi halde dashboard korumasız)

## 3. Yedekleme

### Otomatik Yedekleme
- Her gece 03:00'te DAILY yedek (30 gün saklama — `APP_BACKUP_RETENTION_DAYS` ile değiştirilebilir)
- Haftalık (180 gün), Aylık (365 gün), Yıllık (sınırsız)
- Zamanlama: `APP_BACKUP_AUTO_CRON` (compose varsayılanı `0 0 3 * * ?`)
- Bulut (MinIO) kopyaları otomatik senkronizasyon (`autoSync`) açıksa çekilir; şifreleme açıksa AES-256-GCM ile `.enc` olarak saklanır
- UI: **Yedekler** sayfası → yedek al/indir/sil

### Manuel Yedek
```bash
docker exec raspel-backend curl -X POST "http://localhost:8081/api/backups/manual?type=DAILY" \
  -H "Authorization: Bearer <token>"
```

### Geri Yükleme (Felaket Kurtarma)
Yedek dosyaları backend container'ının `/app/backups` dizininde tutulur (`backup_data` volume).
Backend imajında `pg_dump`/`psql`/`gzip` mevcuttur; postgres alpine imajında `gunzip` güvenilir değildir ve `/app/backups` postgres container'ında yoktur. Bu yüzden geri yükleme **backend container** üzerinden yapılır:

```bash
# Yedek dosyasını listeleyin (backend container'ında)
docker exec raspel-backend ls /app/backups

# Yedeği geri yükleyin
docker exec raspel-backend sh -c "gunzip -c /app/backups/raspelerp_DAILY_<tarih>.sql.gz | psql -h postgres -U postgres -d raspelerp --set ON_ERROR_STOP=1"
```

Felaket kurtarma testi: `powershell -File scripts/disaster-recovery-test.ps1`

## 4. Güncelleme

```bash
git pull origin main
docker-compose up -d --build
# Flyway migration'ları otomatik uygulanır (V1..V144)
```

## 5. Testler

```bash
# Backend (1346 test)
cd backend && mvn test

# Frontend (814 test)
cd frontend && npm run test

# Uçtan uca iş akışı (backend çalışırken)
node scripts/e2e-workflow-test.mjs

# Yük testi (backend çalışırken)
node scripts/load-test.mjs 50 5

# Felaket kurtarma testi
powershell -File scripts/disaster-recovery-test.ps1
```

## 6. Sık Karşılaşılan Sorunlar

| Sorun | Çözüm |
|---|---|
| Backend başlamıyor | `docker-compose logs backend` — Flyway hatası varsa DB volume'ünü sıfırlayın |
| 429 login hatası | 5 deneme/60sn limiti — 60 saniye bekleyin |
| HTTPS yok | 80 portunun dışarı açık olduğunu ve DNS'in yayıldığını kontrol edin |
| E-posta gitmiyor | SMTP ayarlarını kontrol edin; Gmail'de "uygulama şifresi" gerekir |
| Yedek yok | `docker-compose logs backend | grep -i backup` |
