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
- 🔔 **Anlık Bildirimler** — WebSocket + masaüstü bildirimleri, kullanıcı tercihleri
- 🖨️ **Termal Fiş** — Fiyatlı/fiyatsız yazdırma, önizleme penceresi
- 🔍 **Evrensel Arama** — Ctrl+K ile 9 modülde anlık arama + son aramalar
- 📎 **Belge Yönetimi** — Fatura/siparişe dosya iliştirme
- 🔐 **Production Hazır** — Let's Encrypt SSL, SMTP, otomatik yedekleme, felaket kurtarma testi

---

## 🚀 Özellikler

### 💰 Finans
| Modül | Açıklama |
|-------|----------|
| **Cari Hesap** | Müşteri/tedarikçi yönetimi, bakiye takibi, kredi limiti, ödeme vadesi, tahsilat/ödeme |
| **Fatura** | Alış/satış faturası, iskonto, KDV, e-fatura, **otomatik fatura no** (`FTR-2026-000001`), PDF (fiyatlı/fiyatsız) |
| **Banka & Kasa** | Hesap takibi, IBAN tıkla-kopyala |
| **Çek/Senet** | Portföy takibi, vade yönetimi |
| **Bütçe & Masraf** | Aylık/yıllık planlama ve gider takibi |

### 🛒 Ticaret
| Modül | Açıklama |
|-------|----------|
| **Hızlı Satış (POS)** | Barkod arama, anlık sepet, **termal fiş yazdırma (fiyatlı/fiyatsız + önizleme)** |
| **Sipariş** | Otomatik sipariş no (`SIP-2026-000001`), durum takibi |
| **Satın Alma** | Talep → Sipariş akışı |
| **İrsaliye & İade** | Sevk ve iade süreçleri, stok hareketi |
| **Fiyat Listesi** | Ürün bazlı alış/satış fiyatı |

### 📦 Envanter
| Modül | Açıklama |
|-------|----------|
| **Stok Yönetimi** | Ürün kartı, barkod, marka, çoklu birim, kritik stok uyarısı, tablo/kart görünümü |
| **Depo** | Çoklu depo, depo bazlı stok, transfer |
| **Seri/Lot & Sayım** | Seri takibi, sayım fişi |
| **Veri Aktar** | CSV ile toplu stok/cari (batch insert) |

### 👥 İnsan Kaynakları
| Modül | Açıklama |
|-------|----------|
| **Personel, Puantaj, İzin** | Künye, devam takibi, onay mekanizması |
| **Maaş Bordro & Vardiya** | Brüt/kesinti/net hesaplama, vardiya planlama |

### 📊 Rapor & Analiz
- Dashboard: nakit akışı, son görüntülenenler, kritik stok, gelir/gider grafikleri
- Cari ekstre, KDV, yaşlandırma, **favori raporlar**
- Excel export + PDF rapor (şirket logolu)
- **Denetim Log** — filtreleme, kayıtlı filtreler, Excel export
- **Anomaliler** — mükerrer kayıt taraması

### ⚙️ Sistem
| Özellik | Açıklama |
|---------|----------|
| **Güvenlik** | JWT + Admin/User rolleri + login rate limit + oturum zaman aşımı uyarısı |
| **Çoklu Şirket** | Tenant izolasyonu, dönem yönetimi |
| **Notlar** | Renkli etiketler, önem derecesi, silme geri alma |
| **Yedekleme** | Otomatik rotasyon (Günlük/Haftalık/Aylık/Yıllık) + felaket kurtarma |
| **Bildirimler** | WebSocket + masaüstü, tür bazlı tercihler |
| **Tema & Dil** | Açık/Koyu tema, TR/EN dil seçici |
| **Klavye Kısayolları** | Ctrl+K arama, Ctrl+S kaydet, F2 yeni kayıt, Ctrl+P yazdır, Esc kapat |
| **Kullanıcı Dostu** | Taslak otomatik kayıt, boş tablo yönlendirmeleri, şifre güç göstergesi, sütun ayarları, toplu işlemler |

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

### Servisler

| Servis | Adres |
|--------|-------|
| Frontend | http://localhost |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Adminer (DB) | http://localhost:8082 |
| Grafana | http://localhost:3000 (admin/admin) |
| RabbitMQ | http://localhost:15672 (raspel/raspel) |

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
| Backend (JUnit) | **477** | `cd backend && mvn test` |
| Frontend (Vitest) | **74** | `cd frontend && npm run test` |

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
