# RasPel ERP — Kurulum Kılavuzu

## Gereksinimler
- Docker + Docker Compose (v2)
- En az 4GB RAM ayrılmış Docker
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
| Frontend | http://localhost |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Traefik Dashboard | http://localhost:8080/dashboard |
| Adminer (DB) | http://localhost:8082 |
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |

**Varsayılan kullanıcılar (dev):**
- `admin` / `admin123` (ADMIN)
- `muhasebe` / `123456` (USER)

> **Uyarı:** Varsayılan parolaları ilk girişte mutlaka değiştirin!

## 2. Üretim Kurulumu (SSL ile)

### 2.1 DNS Ayarı
Alan adınızı (ör: `erp.sirketiniz.com`) sunucunuzun IP'sine yönlendirin.

### 2.2 .env Yapılandırması

```env
# Güvenlik
POSTGRES_PASSWORD=<güçlü-parola>
REDIS_PASSWORD=<güçlü-parola>
JWT_SECRET=<uzun-rastgele-base64>

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

### 2.4 Güvenlik Kontrol Listesi
- [ ] Tüm parolalar değiştirildi
- [ ] JWT_SECRET uzun ve rastgele
- [ ] HTTPS çalışıyor (https://alan-adiniz)
- [ ] SMTP e-postası test edildi
- [ ] Grafana varsayılan parolası değiştirildi
- [ ] Traefik dashboard parolası değiştirildi (config/traefik/dynamic.yml)

## 3. Yedekleme

### Otomatik Yedekleme
- Her gece 03:00'te DAILY yedek (30 gün saklama)
- Haftalık (180 gün), Aylık (365 gün), Yıllık (sınırsız)
- UI: **Yedekler** sayfası → yedek al/indir/sil

### Manuel Yedek
```bash
docker exec raspel-backend curl -X POST "http://localhost:8081/api/backups/manual?type=DAILY" \
  -H "Authorization: Bearer <token>"
```

### Geri Yükleme (Felaket Kurtarma)
```bash
# Yedek dosyasını bulun (backend container'ında)
docker exec raspel-backend ls /app/backups

# Yedeği postgres'e geri yükleyin
docker exec raspel-postgres sh -c "gunzip -c /app/backups/raspelerp-DAILY-<tarih>.sql.gz | psql -U postgres -d raspelerp"
```

Felaket kurtarma testi: `powershell -File scripts/disaster-recovery-test.ps1`

## 4. Güncelleme

```bash
git pull origin main
docker-compose up -d --build
# Flyway migration'ları otomatik uygulanır (V2..V26)
```

## 5. Testler

```bash
# Backend (477 test)
cd backend && mvn test

# Frontend (74 test)
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
