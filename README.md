# <img src="https://img.icons8.com/fluency/48/calculator.png" width="32" /> RasPel — Yeni Nesil ERP

> **Ras**im **Tuz**luoğlu tarafından geliştirilmiş, modern ve kapsamlı Kurumsal Kaynak Planlama sistemi.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.12-6DB33F?style=flat&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Vue.js-3.3-4FC08D?style=flat&logo=vuedotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Tests-551-green?style=flat" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/SSL-Let's_Encrypt-0b7d3d?style=flat" />
</p>

<p align="center">
  <b>Finans · Ticaret · Envanter · İK · Rapor — Tek platformda</b>
</p>

---

## ✨ Öne Çıkanlar

- 🧠 **Akıllı Anomali Tespiti** — Mükerrer fatura ve çift ödemeleri otomatik bulur
- 🔔 **Anlık Bildirimler** — WebSocket + masaüstü bildirimleri, kritik stok/fatura/sipariş/ödeme olaylarında tetiklenir
- 🖨️ **Termal Fiş** — Fiyatlı/fiyatsız yazdırma, önizleme penceresi, kameralı barkod okuyucu
- 🔍 **Evrensel Arama** — Ctrl+K ile 9 modülde anlık arama + son aramalar
- 📎 **Belge Yönetimi** — Fatura/siparişe dosya iliştirme
- 🧾 **Genel Muhasebe** — Hesap planı, yevmiye fişleri, mizan, defter-i kebir
- 💳 **Banka Mutabakatı** — Hesap özeti yükleme + otomatik fatura eşleştirme
- 📊 **KDV Beyanname & BA/BS** — Dönem bazlı beyannameye hazırlık raporları
- ✉️ **E-posta Entegrasyonu** — Fatura PDF e-postayla gönderme + vadesi geçen tahsilat hatırlatıcıları
- 🔐 **2FA (TOTP)** — Google Authenticator uyumlu iki faktörlü doğrulama
- 📱 **PWA** — Kurulabilir web uygulaması, mobil uyum
- 🔐 **Production Hazır** — Let's Encrypt SSL, SMTP, otomatik yedekleme, felaket kurtarma testi

---

## 🚀 Özellikler

### 💰 Finans
| Modül | Açıklama |
|-------|----------|
| **Genel Muhasebe** | Hesap planı (varsayılan plan otomatik), dengeli yevmiye fişleri (`MUH-YYYY-000001`), mizan, defter-i kebir |
| **Cari Hesap** | Müşteri/tedarikçi yönetimi, bakiye takibi, kredi limiti, ödeme vadesi, tahsilat/ödeme |
| **Fatura** | Alış/satış faturası, iskonto, KDV, e-fatura, **otomatik fatura no** (`FTR-2026-000001`), PDF (fiyatlı/fiyatsız), **e-posta ile gönderme** |
| **Banka & Kasa** | Hesap takibi, IBAN tıkla-kopyala, **banka mutabakatı** (CSV/Excel özeti + otomatik eşleştirme) |
| **Çek/Senet** | Portföy takibi, vade yönetimi |
| **Bütçe & Masraf** | Aylık/yıllık planlama ve gider takibi |

### 🛒 Ticaret
| Modül | Açıklama |
|-------|----------|
| **Hızlı Satış (POS)** | Barkod arama (kameralı okuyucu dahil), anlık sepet, **termal fiş yazdırma (fiyatlı/fiyatsız + önizleme)** |
| **E-Fatura** | UBL-TR 2.1 taslağı, alıcı/satıcı bilgisi, GİB gönderimi (entegratör uç noktası tanımlıysa), XML indirme |
| **CRM** | Fırsat takibi (Yeni→Temas→Teklif→Kazanıldı), değer/kapanış tahmini, durum filtreleri |
| **Sipariş** | Otomatik sipariş no (`SIP-2026-000001`), durum takibi, durum değişiminde e-posta |
| **Satın Alma** | Talep → Sipariş akışı |
| **İrsaliye & İade** | Sevk ve iade süreçleri, stok hareketi, durum yönetimi |
| **Fiyat Listesi** | Ürün bazlı alış/satış fiyatı |

### 📦 Envanter
| Modül | Açıklama |
|-------|----------|
| **Stok Yönetimi** | Ürün kartı, barkod, marka, çoklu birim, kritik stok uyarısı, tablo/kart görünümü |
| **Kritik Stok & Yeniden Sipariş** | Min. seviye altındaki ürünler, önerilen sipariş miktarı, tedarikçi bilgisi |
| **Depo** | Çoklu depo, depo bazlı stok, transfer |
| **Seri/Lot & Sayım** | Seri takibi, sayım fişi (tamamlanınca stok düzeltme) |
| **Veri Aktar** | CSV ile toplu stok/cari (batch insert) |

### 👥 İnsan Kaynakları
| Modül | Açıklama |
|-------|----------|
| **Personel, Puantaj, İzin** | Künye, devam takibi, onay mekanizması |
| **Maaş Bordro & Vardiya** | Brüt/kesinti/net hesaplama, vardiya planlama |

### 📊 Rapor & Analiz
- Dashboard: nakit akışı, son görüntülenenler, kritik stok, gelir/gider grafikleri (widget ayarları)
- Cari ekstre, KDV, yaşlandırma, **favori raporlar**
- **KDV Beyanname** — 1-2 ve 19-20 no.lu tablolar, oran bazlı matrah/KDV, ödenecek/devreden
- **BA/BS** — Eşik üstü alış/satış bildirim formu listeleri
- Excel export + PDF rapor (şirket logolu)
- **Denetim Log** — filtreleme, kayıtlı filtreler, Excel export
- **Anomaliler** — mükerrer kayıt taraması

### ⚙️ Sistem
| Özellik | Açıklama |
|---------|----------|
| **Güvenlik** | JWT + Admin/User rolleri + login rate limit (2FA yolları dahil) + oturum zaman aşımı uyarısı |
| **İki Faktörlü Doğrulama** | RFC 6238 TOTP (Google Authenticator/Authy uyumlu), kur/kapat, girişte ikinci adım |
| **Çoklu Şirket** | Tenant izolasyonu, dönem yönetimi |
| **Notlar** | Renkli etiketler, önem derecesi, silme geri alma |
| **Yedekleme** | Otomatik rotasyon (Günlük/Haftalık/Aylık/Yıllık) + felaket kurtarma |
| **Bildirimler** | WebSocket + masaüstü, tür bazlı tercihler, kritik stok/fatura/sipariş/ödeme tetikleyicileri |
| **E-posta** | Fatura PDF gönderimi, vadesi geçen tahsilat hatırlatıcıları (günlük 08:00), sipariş/fatura bildirimleri |
| **Tema & Dil** | Açık/Koyu tema + vurgu rengi, TR/EN dil seçici |
| **Klavye Kısayolları** | Ctrl+K arama, Ctrl+S kaydet, F2 yeni kayıt, Ctrl+P yazdır, Esc kapat, `?` rehber |
| **PWA** | Kurulabilir web uygulaması (manifest + service worker), masaüstü bildirimleri |
| **Kullanıcı Dostu** | İlk kurulum admin oluşturucu, demo veri yükleme, onboarding, taslak otomatik kayıt, "Geri Al" silme, satır hızlı eylem menüsü, ilk ziyaret ipuçları, kullanıcı başına sayfa boyutu tercihi |
| **Hesap Ayarları** | Profil, şifre, 2FA ve görünüm tercihleri tek sayfada |

---

## 🏗️ Mimari

```
raspel-erp/
├── backend/              # Spring Boot 3.2.12 + Java 21
│   ├── src/main/java/    # 240+ Java sınıfı
│   │   ├── controller/   # REST API (60+ endpoint)
│   │   ├── service/      # İş mantığı (Redis cache, batch, N+1 optimizasyonu)
│   │   ├── repository/   # Veri erişim (JPA)
│   │   ├── entity/       # JPA entity'leri (40+ tablo)
│   │   ├── dto/          # Veri transfer objeleri
│   │   ├── config/       # Security, JWT, Redis, RabbitMQ, WebSocket, Rate Limiter
│   │   └── exception/    # GlobalExceptionHandler (Türkçe hata mesajları)
│   └── src/main/resources/
│       └── db/migration/ # Flyway (V1..V26 + performans index'leri)
├── frontend/             # Vue 3 + PrimeVue 4 + Pinia + Chart.js
│   └── src/views/        # 35+ sayfa (chunk-split + lazy loading)
├── config/               # Traefik (SSL), Prometheus, Grafana
├── docs/                 # Kurulum, kullanım ve API kılavuzları
├── scripts/              # Yedekleme, felaket kurtarma, E2E ve yük testleri
└── docker-compose.yml    # 9 container, tek komutla ayağa kalkar
```

### 🛠️ Teknoloji Yığını

| Katman | Teknoloji |
|--------|-----------|
| **Backend** | Java 21, Spring Boot 3.2.12, Spring Security, JPA/Hibernate, Flyway, Resilience4j |
| **Frontend** | Vue 3, Vite, PrimeVue 4, Pinia, Chart.js, Axios |
| **Veritabanı** | PostgreSQL 16 (24 performans index'i) |
| **Cache** | Redis 7 (Jackson serializer, 5 cache bölgesi) |
| **Message** | RabbitMQ 3 + STOMP WebSocket |
| **Auth** | JWT + login rate limit |
| **E-posta** | SMTP HTML şablonları (fatura, sipariş, şifre, stok uyarısı) |
| **Monitoring** | Prometheus + Grafana |
| **Reverse Proxy** | Traefik 3 + Let's Encrypt SSL |
| **CI/CD** | GitHub Actions (build + test + Trivy) |

---

## 🚀 Hızlı Başlangıç

```bash
git clone https://github.com/Rasimtuzluoglu/raspel-erp.git
cd raspel-erp

cp .env.example .env   # parolaları değiştirin

docker compose up -d --build

open http://localhost
```

### Giriş Bilgileri (dev)

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| `admin` | `admin123` | ADMIN (tüm sayfalar) |
| `muhasebe` | `123456` | USER (kısıtlı erişim) |

> ⚠️ Varsayılan parolaları ilk girişte değiştirin!

> 💡 **İlk kurulum:** Boş bir veritabanında uygulama ilk başlatıldığında otomatik olarak `admin` kullanıcısı oluşturulur (kullanıcı adı/şifre `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` ortam değişkenleriyle özelleştirilebilir). `dev` profilinde `DataSeeder` ek demo kullanıcıları ve şirketler yükler.

### Servisler

| Servis | Adres |
|--------|-------|
| Frontend | http://localhost |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Adminer (DB) | http://localhost:8082 |
| Grafana | http://localhost:3000 (admin/admin) |

### 🔑 Önemli Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `JWT_SECRET` | (güçlü dev anahtarı) | **Üretimde mutlaka ayarlayın.** JWT imzalama anahtarı (en az 32 bayt). |
| `SPRING_MAIL_USERNAME/PASSWORD` | boş | SMTP kimlik bilgileri — dolu değilse e-postalar log'a yazılır |
| `SPRING_MAIL_FROM` | `noreply@raspel-erp.com` | Gönderen e-posta adresi |
| `APP_EFATURA_GIB_ENDPOINT` | boş | GİB/entegratör uç noktası — boşsa e-fatura gönderimi yerel onay (simülasyon) |
| `APP_ADMIN_USERNAME/PASSWORD` | `admin`/`admin123` | İlk kurulumda oluşturulacak admin |
| `APP_BACKUP_DIR` | `/app/backups` | Yedek dizini |

---

## 🔐 Production Kurulumu (SSL)

1. Alan adınızı sunucuya yönlendirin
2. `.env` içinde `ACME_EMAIL` ve `APP_DOMAIN` ayarlayın
3. `docker compose up -d traefik` — Let's Encrypt sertifikası otomatik alınır
4. Detaylı kılavuz: **[docs/KURULUM.md](docs/KURULUM.md)**

---

## 🧪 Testler

| Katman | Adet | Komut |
|--------|------|-------|
| Backend (JUnit) | **512** | `cd backend && mvn test` |
| Frontend (Vitest) | **84** | `cd frontend && npm run test` |

### Operasyonel Testler (`scripts/`)
- **Felaket Kurtarma** — `disaster-recovery-test.ps1` (yedek → sil → geri yükle → doğrula)
- **Uçtan Uca İş Akışı** — `e2e-workflow-test.mjs` (giriş→cari→stok→sipariş→fatura→tahsilat→PDF)
- **Yük Testi** — `load-test.mjs 50 5` (50 eşzamanlı: %100 başarı, ~600 istek/sn)

---

## 📚 Dokümantasyon

- **[KURULUM.md](docs/KURULUM.md)** — Kurulum, SSL, yedekleme, sorun giderme
- **[KULLANIM.md](docs/KULLANIM.md)** — Modül rehberi, kısayollar, iş akışları
- **[API.md](docs/API.md)** — Endpoint listesi, kimlik doğrulama, hata formatları

---

## 📜 Migration'lar (Flyway)

| Versiyon | Açıklama |
|----------|----------|
| V1-V21 | Temel şema, modüller, tenant izolasyonu, e-fatura |
| V22 | Notlar tablosu |
| V23 | Not renk alanı |
| V24 | `sistem.not` → `sistem.notlar` (H2 uyumu) |
| V25 | Belge yönetimi tablosu |
| V26 | **24 performans index'i** |
| V27 | Genel muhasebe (hesap planı, fiş, kalem) + CRM fırsatları |
| V28 | Banka mutabakatı (hesap özeti hareketleri) |
| V29 | 2FA kolonları + döviz kuru efektif kur kolonları |

---

## 🤝 Katkı

1. Fork'la
2. Feature branch oluştur (`git checkout -b feature/yeni-ozellik`)
3. Değişiklikleri commit'le
4. Branch'i push'la
5. Pull Request aç

---

## 📄 Lisans

**© 2026 Rasim Tuzluoğlu** — Tüm hakları saklıdır.

---

<p align="center">
  <sub>Built with ❤️ by <strong>Rasim Tuzluoğlu</strong></sub>
  <br>
  <sub>Spring Boot · Vue.js · PostgreSQL · Docker</sub>
</p>
