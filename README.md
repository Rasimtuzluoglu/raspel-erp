# RasPel ERP — Modern & Akıllı Kurumsal Kaynak Planlama Sistemi

> Fatura, stok, cari, saha operasyonları, finans ve personel yönetimi... Hepsi tek ekranda. İnternet kesilse de çalışan PWA desteği, yapay zeka ile talep tahmini, eşzamanlı çoklu kullanıcı güvenliği ve gelişmiş yetkilendirme.

**Java 21 · Spring Boot 3.2 · Vue 3 · PrimeVue 4 · PostgreSQL 16 · Redis · RabbitMQ · Docker · Yapay Zeka (AI)**

---

## 🌟 Öne Çıkan Yenilikler & Akıllı Özellikler

| Modül | Açıklama |
|---|---|
| 🚚 **Saha Personeli Portalı** | Saha çalışanları için özel tasarlanmış mobil uyumlu arayüz. Sipariş teslimatı, **dijital müşteri imzası (Canvas)**, masraf fişi yükleme ve anlık izin talebi oluşturma. |
| 🛡️ **Onay Merkezi (İzin, Masraf, Satınalma)** | Saha ve ofis personellerinden gelen izin, masraf ve satınalma taleplerinin yöneticiler tarafından tek tıkla incelenip onaylanmasını sağlayan merkezi iş akışı. |
| 📊 **Yönetici Kokpiti (Admin Cockpit)** | Sadece yöneticilere özel ciro, net kâr, bütçe hedefleri ve şirket likidite analizlerini gösteren sadeleştirilmiş üst yönetim panosu. |
| 🤖 **Yapay Zeka Destekli ERP Asistanı** | OpenAI, Google Gemini ve Anthropic Claude API anahtarlarını AES-256 ile şifreleyerek entegre eden, doğal dilde şirket verilerini sorgulayan akıllı sohbet asistanı. |
| 🧠 **Akıllı Stok & Talep Tahmini (Predictive AI)** | Son 90 günlük tüketim hızına göre ürünlerin tükenme süresini hesaplar, emniyet stoku ve tedarik süresini hesaba katarak proaktif satınalma önerileri üretir. |
| 🔒 **Eşzamanlı Çoklu Kullanıcı Koruması (Concurrency)** | Aynı anda yüzlerce çalışan işlem yaparken **Pessimistic Locking (SELECT FOR UPDATE)** ile stok ve kasa çakışmalarını, eksiye düşmeyi ve mükerrer fatura numaralandırmayı engeller. |
| 📈 **Nakit Akışı Projeksiyonu** | Kasa/banka bakiyesi üzerine gelecek 30/60/90 gündeki tahsilat ve ödemeleri kümülatif ekleyerek finansal geleceği grafiklerle simüle eder. |
| ☁️ **Otomatik Bulut Yedekleme** | Veritabanı yedeklerini uçtan uca **AES-256** ile şifreleyerek otomatik veya manuel olarak **AWS S3, Google Drive veya Dropbox** bulut depolarına senkronize eder. |

---

## 🏢 Temel ERP Yetenekleri

| Özellik | Açıklama |
|-----------|----------|
| 🏢 **Çoklu Şirket & Şube** | Kullanıcı birden fazla firmaya atanabilir, girişte seçim yapar. Şirketler arası tam veri izolasyonu. |
| 🔐 **3 Adımlı Güvenli Giriş** | Kullanıcı/Şifre → TOTP 2FA (opsiyonel) → Firma Seçimi → Dashboard. Brute-force koruması ve IP kısıtlaması. |
| 🎭 **Dinamik RBAC & Yetki Matrisi** | Roller ve modül bazlı (Cari, Fatura, Stok vb.) okuma, yazma, silme ve dışa aktarım izinleri (`v-permission`). |
| 📱 **PWA (Progressive Web App)** | Masaüstü ve mobile yerel uygulama gibi kurulabilir, çevrimdışı önbellek desteğiyle çalışır. |
| 📊 **CRM Kanban & Zaman Çizelgesi** | Müşteri fırsatlarını sürükle-bırak yöntemiyle yönetin, kayıt tarihçelerini Timeline ile inceleyin. |
| ⚡ **Yüksek Performans & Redis Cache** | Tekil önbellek sorgusu ile anında açılan Dashboard ve optimize edilmiş veri transferi. |
| ⌨️ **Klavye Kısayolları** | `Ctrl+K` Omnibar hızlı arama, `Esc` kapatma ile fareye ihtiyaç duymadan hızlı operasyon. |
| 🌙 **Karanlık / Aydınlık Tema** | Gece ve gündüz kullanımına uygun modern tema ve renk paleti desteği. |

---

## 📦 Modül Mimarisi

### 💰 Finans & Muhasebe
- **Cari Hesaplar**: Müşteri, tedarikçi, bakiye, kredi limiti, vade takibi, IBAN doğrulama, toplu Excel aktarımı ve ekstre.
- **Fatura Yönetimi**: Alış/Satış faturası, otomatik seri no (`FTR-1-2026-000001`), iskonto, KDV, PDF, e-posta gönderimi ve çoğaltma.
- **Banka & Kasa**: Hesap bakiyeleri, para giriş/çıkışı, MT940 hesap özeti yükleme ve otomatik mutabakat.
- **Çek/Senet, Bütçe & Masraflar**: Portföy takibi, departman bütçeleri, masraf fişleri ve nakit akışı projeksiyonu.
- **Genel Muhasebe**: Otomatik tek düzen hesap planı, dengeli yevmiye fişi, mizan ve defter-i kebir.

### 🛒 Ticaret & Satış
- **Hızlı Satış (POS)**: Barkod okuyucu destekli hızlı satış, sepet, indirim ve termal fiş yazdırma.
- **Sipariş & İrsaliye**: Siparişten irsaliyeye, irsaliyeden faturaya tek tıkla kontrollü iş akışı.
- **E-Fatura**: UBL-TR 2.1 standardında GİB uyumlu e-fatura ve e-arşiv entegrasyonu.
- **CRM Kanban**: Satış hunisi, teklif yönetimi, aşama takibi ve müşteri bazlı özel fiyat listeleri.

### 📦 Stok & Envanter
- **Stok Kartları & Barkod**: Kritik seviye alarmı, akıllı AI talep tahmini, hareket geçmişi.
- **Çoklu Depo & Şube**: Şubeler arası transfer, seri/lot/SKT takibi ve periyodik stok sayım modülü.

### 👥 İnsan Kaynakları (İK)
- **Personel Kartları**: TC Kimlik doğrulama, departman/pozisyon atamaları, acil durum bilgileri.
- **İzin & Vardiya**: İzin talepleri, hakediş hesaplama, haftalık vardiya planlama ve puantaj cetveli.
- **Maaş Bordro**: SGK, gelir vergisi ve damga vergisi kesintileriyle otomatik bordro hesaplama.

---

## 🚀 Hızlı Başlangıç

### Geliştirme Ortamı (Minimal Setup)

```bash
# 1. Altyapı Servislerini Başlatın
docker-compose up -d postgres redis rabbitmq

# 2. Backend Sunucusunu Başlatın (Java 21 + Maven)
cd backend
mvn spring-boot:run     # API -> http://localhost:8081

# 3. Frontend Geliştirme Sunucusunu Başlatın (Node.js)
cd frontend
npm ci
npm run dev            # UI -> http://localhost:5173
```

> **Not:** İlk açılışta veritabanı boşsa sistem otomatik olarak **İlk Kurulum Sihirbazı** ekranına yönlendirir; şirket bilgilerinizi ve ilk yönetici hesabınızı tanımlayarak hemen başlayabilirsiniz.

---

## 🐳 Üretim (Production / Docker)

```bash
# .env dosyasını yapılandırın
cp .env.example .env

# Tüm servisleri tek komutla başlatın (Traefik, SSL, Prometheus, Grafana dahil)
docker-compose up -d
```

Detaylı canlıya geçiş kontrol listesi için `docs/GO-LIVE.md` dosyasını inceleyebilirsiniz.

---

## 🧪 Test ve Kalite Güvencesi

Proje uçtan uca kapsamlı birim ve entegrasyon testleriyle korunmaktadır:

```bash
# Backend Testleri (JUnit 5 + H2 + Mockito)
cd backend
mvn -B test -q          # 600+ Test (0 Hata)

# Frontend Testleri (Vitest)
cd frontend
npm run test           # 153 Test (0 Hata)

# Kod Standartları & Linting
cd frontend
npm run lint           # Sıfır ESLint Uyarısı

# Üretim Derlemesi (PWA Build)
npm run build          # Optimize edilmiş üretim paketleri
```

---

## 📂 Proje Dizin Mimarisi

```
raspel-erp/
├── backend/                 # Spring Boot 3.2 REST API
│   └── src/main/java/com/raspel/erp/
│       ├── controller/      # REST Denetleyicileri (envanter, finans, ik, muhasebe, sistem, ticaret)
│       ├── service/         # İş mantığı, AI motorları, Redis cache, SeriNo servisi
│       ├── repository/      # JPA Repository katmanı (Pessimistic Lock destekli)
│       ├── entity/          # JPA Veritabanı Varlıkları
│       ├── dto/             # Data Transfer Objects
│       └── config/          # Spring Security, JWT, WebSocket, Cache konfigürasyonu
│
├── frontend/                # Vue 3 SPA + Vite + PrimeVue 4
│   └── src/
│       ├── views/           # 57 Görünüm (SahaPortali, Onaylar, YoneticiKokpiti, Dashboard vb.)
│       ├── components/      # Paylaşılan Bileşenler (Timeline, ExportMenu, Omnibar, İmza)
│       ├── stores/          # Pinia Durum Yönetimi (auth, dashboard, fatura, stok vb.)
│       ├── composables/     # 12+ Composable Hook (Tema, Yetki, Oturum, Kısayol)
│       └── api/             # Modüler Axios İstemcisi
│
├── config/                  # Traefik Reverse Proxy, Prometheus, Grafana
├── scripts/                 # Otomatik yedekleme ve bakım betikleri
└── docs/                    # Mimari ve kullanım dökümanları
```

---

## 📄 Lisans

Bu proje [MIT](LICENSE) lisansı ile lisanslanmıştır. © 2026 RasPel ERP.
