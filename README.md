<div align="center">

<img src="frontend/public/logo-full.png" alt="RasPel ERP" width="260">

# ⚡ RasPel ERP

### İşletmenizi tek ekrandan yönetin.

**Fatura · Stok · Cari · Kasa & Banka · Saha · İK · Muhasebe — hepsi tek platformda, Türkçe, bulutta veya kendi sunucunuzda.**

İnternet kesilse de satış yapan PWA · Yapay zeka destekli stok tahmini · Çok kiracılı (multi-tenant) veri izolasyonu · Kurumsal düzeyde güvenlik

[![CI](https://github.com/Rasimtuzluoglu/raspel-erp/actions/workflows/ci.yml/badge.svg)](https://github.com/Rasimtuzluoglu/raspel-erp/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/version-1.35.0-6366f1)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![PrimeVue](https://img.shields.io/badge/PrimeVue-4-41B883)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

</div>

---

## 🎯 Neden RasPel ERP?

Çoğu ERP ya çok karmaşık, ya çok pahalı, ya da Türkiye gerçeklerine uzak. **RasPel**, gerçek bir KOBİ'nin gününü düşünerek tasarlandı: sabah açtığınızda **ne yapmanız gerektiğini** söyler, satışı **saniyeler içinde** tamamlatır ve tüm finansal tabloyu **tek doğru kaynaktan** gösterir.

| | RasPel ERP |
|---|---|
| 🇹🇷 **Türkçe öncelikli** | Tüm arayüz ve belgeler Türkçe; İngilizce desteği de hazır |
| 🧾 **e-Fatura uyumlu** | UBL-TR 2.1 standardında GİB entegrasyonu |
| 🚀 **Kurulumdan 5 dk** | Docker ile tek komut; ilk kurulum sihirbazı |
| 💸 **Şeffaf maliyet** | Kendi sunucunuzda çalıştırın, lisans ücreti yok |
| 🔒 **Veriniz sizde** | Tam veri sahipliği, multi-tenant izolasyon |

---

## ✨ Öne Çıkan Yetenekler

### 🛒 Satışı kolaylaştıranlar
- **Hızlı Satış (POS)** — Barkod okuyucu desteği, kameralı sürekli okuma, çok satanlar paneli, indirim, termal fiş yazdırma (58/80 mm) ve **internet kesilse bile** çalışan offline satış kuyruğu.
- **Teklif → Sipariş → İrsaliye → Fatura** — Tek tıkla, kontrollü iş akışı. KDV dahil fiyatlama, kademeli iskonto motoru.
- **Tahsilat Merkezi** — Vadesi geçen alacaklar yaşlandırma tablosu, otomatik hatırlatma e-postası, WhatsApp ve arama aksiyonları.
- **CRM Merkezi** — Lead yönetimi, aktivite takibi ve kampanyalar; potansiyel müşteriyi cari hesaba dönüştürme.

### 🧠 Sizi öne geçiren akıllı özellikler
- **Yapay Zeka ERP Asistanı** — OpenAI, Gemini ve Claude desteği; doğal dille şirket verinizi sorgular, API anahtarları AES-256 ile şifrelenir.
- **AI Stok & Talep Tahmini** — Son 90 günlük tüketime göre tükenme süresi, emniyet stoku ve proaktif satınalma önerisi üretir.
- **Nakit Akışı Projeksiyonu** — Gelecek 30/60/90 günün tahsilat/ödemelerini kümülatif simüle eder.
- **Gelişmiş Kârlılık Analizi** — Ağırlıklı ortalama maliyet (COGS) motoru, ürün/kategori/cari kırılımı, negatif marj uyarıları.

### 🔐 Kurumsal güvenlik ve kontrol
- **3 Adımlı Güvenli Giriş** — Kullanıcı/Şifre → TOTP 2FA → Firma seçimi. Brute-force koruması ve IP kısıtlaması.
- **Çok Kiracılı (Multi-Tenant) İzolasyon** — Servis katmanında şirket sahiplik doğrulaması, fail-closed; cache dahi şirket bazında izole.
- **Dönem Kilidi & Yıl Sonu Kapanışı** — Kilitli dönemlerde belgeler değiştirilemez; mali dönem bütünlüğü garanti.
- **Denetim İzi & Anomali Tespiti** — Alan bazlı önce/sonra değişiklik geçmişi; mükerrer/yüksek tutar anomalilerinde otomatik bildirim.

### 📊 İşletmeyi bir arada tutanlar
- **Dashboard** — Kişiselleştirilebilir widget'lar: istatistikler, kritik stok, grafikler, son hareketler, ödeme vadeleri.
- **Rapor Merkezi** — Cari ekstre, KDV, yaşlandırma, kârlılık, nakit akışı, pivot tablo; PDF/Excel dışa aktarım.
- **Genel Muhasebe** — Otomatik tek düzen hesap planı, yevmiye, mizan, defter-i kebir, bilanço, kâr/zarar.
- **Üretim** — Reçete (ürün ağacı), üretim emri, kısmi üretim/fire, otomatik hammadde düşümü ve maliyet.
- **İnsan Kaynakları** — Personel kartları, izin, vardiya, puantaj ve bordro (SGK/vergi kesintileri).

> **Tam özellik listesi:** Aşağıdaki [Modül Mimarisi](#-modül-mimarisi) bölümüne bakın.

---

## 🏗️ Modül Mimarisi

### 💰 Finans & Muhasebe
- **Cari Hesaplar** — Müşteri/tedarikçi, bakiye, kredi limiti, vade takibi, IBAN doğrulama, toplu Excel aktarımı, ekstre.
- **Fatura Yönetimi** — Alış/Satış, otomatik seri no (`FTR-1-2026-000001`), iskonto, KDV, PDF, e-posta, çoğaltma; işlem geçmişi ve yazdırma izi.
- **Kasa & Banka** — Bakiye, para giriş/çıkış, CSV/Excel/OFX özet yükleme, otomatik mutabakat, kasalar arası aktarım.
- **Çek/Senet, Bütçe & Masraf** — Portföy takibi, departman bütçeleri, masraf fişleri, nakit akışı projeksiyonu.
- **Genel Muhasebe & Döviz** — Yevmiye, mizan, bilanço, kâr/zarar; canlı döviz/altın/BTC kurları.

### 🛒 Ticaret & Satış
- **Hızlı Satış (POS)** · **Sipariş & İrsaliye** · **E-Fatura (UBL-TR 2.1)** · **CRM Kanban & Merkezi** · **İskonto Kuralları** · **Adres Defteri**

### 📦 Stok & Envanter
- **Stok Kartları & Barkod** — Kritik seviye alarmı, AI talep tahmini, hareket geçmişi, COGS maliyet motoru.
- **Çoklu Depo & Şube** — Şubeler arası transfer, seri/lot/SKT takibi (FEFO), periyodik stok sayım.
- **Üretim** — Reçete, üretim emri, otomatik hammadde düşümü ve mamul girişi.

### 📊 Raporlama & İK
- **Rapor Merkezi** — Grafikli sekmeler, PDF/Excel, e-posta paylaşımı, merkezi rapor arama ve favoriler.
- **İK** — Personel kartları, izin/vardiya/puantaj, otomatik bordro.

---

## ⚡ Hızlı Başlangıç

### Geliştirme Ortamı

```bash
# 1) Altyapı servisleri (PostgreSQL, Redis, RabbitMQ)
docker-compose up -d postgres redis rabbitmq

# 2) Backend (Java 21 + Maven)  →  http://localhost:8081
cd backend
mvn spring-boot:run

# 3) Frontend (Node.js)         →  http://localhost:5173
cd frontend
npm ci
npm run dev
```

> **İlk açılış:** Veritabanı boşsa sistem sizi otomatik **İlk Kurulum Sihirbazı**'na yönlendirir; şirket bilgilerinizi ve ilk yönetici hesabınızı tanımlayarak hemen başlarsınız.

---

## 🐳 Üretim (Docker)

```bash
cp .env.example .env      # JWT, DB, Redis şifrelerini düzenleyin
docker-compose up -d      # Traefik + SSL + Prometheus + Grafana dahil 11 servis
```

| Servis | Görev |
|---|---|
| `traefik` | Reverse proxy + otomatik SSL |
| `backend` / `frontend` | Uygulama |
| `postgres` / `redis` / `rabbitmq` | Veri, cache, kuyruk |
| `minio` | S3 uyumlu yedek deposu |
| `prometheus` / `grafana` / `alertmanager` | İzleme & alarm |
| `adminer` | Veritabanı yönetimi |

Canlıya geçiş için [`docs/GO-LIVE.md`](docs/GO-LIVE.md), günlük operasyon/yedekleme için [`docs/OPERASYON.md`](docs/OPERASYON.md).

---

## ✅ Test ve Kalite

| Katman | Kapsam |
|---|---|
| Backend | **1.236** test (JUnit 5 · H2 · Mockito) + JaCoCo kapsam eşiği |
| Frontend | **763** test (Vitest) + kapsam eşiği |
| Uçtan uca | **42** Cypress E2E senaryosu |
| Kalite | Sıfır ESLint uyarısı · i18n bütünlük kontrolü · Trivy & Gitleaks güvenlik taraması |

```bash
cd backend  && mvn -B clean verify        # Derle + test + kapsam
cd frontend && npm run lint && npm run i18n:check && npm run test && npm run build
cd frontend && npm run cypress:run        # E2E (dev sunucusu üzerinde)
```

Her `push` ve `PR`'da GitHub Actions ile backend, frontend, e2e, docs ve güvenlik işleri otomatik çalışır.

---

## 📁 Proje Yapısı

```
raspel-erp/
├── backend/                 # Spring Boot 3.5 REST API (540 Java dosyası, 75 controller)
│   └── src/main/
│       ├── java/com/raspel/erp/   # controller · service · repository · entity · dto · config
│       └── resources/db/migration # Flyway (111 migration)
│
├── frontend/                # Vue 3 SPA + Vite + PrimeVue 4
│   └── src/
│       ├── views/           # 75 görünüm (lazy-loaded)
│       ├── components/      # 53 paylaşılan bileşen
│       ├── stores/          # 13 Pinia store
│       ├── composables/     # 20 composable
│       └── locales/         # tr.json / en.json
│
├── config/                  # Traefik · Prometheus · Grafana
├── scripts/                 # Yedekleme, kurtarma, yük testi
└── docs/                    # API, kurulum, mimari, operasyon
```

---

## 🤝 Katkı & Destek

- 🐛 Hata bildirimi / öneri: [Issues](https://github.com/Rasimtuzluoglu/raspel-erp/issues)
- 📚 Dokümantasyon: [`docs/`](docs) — [Kurulum](docs/KURULUM.md) · [Kullanım](docs/KULLANIM.md) · [Mimari](docs/MIMARI.md) · [API](docs/API.md)

---

<div align="center">

## 📜 Lisans

Bu proje [MIT](LICENSE) lisansı ile lisanslanmıştır.

**RasPel ERP** — İşletmenizi tek ekrandan yönetin.

© 2026 RasPel ERP

</div>
