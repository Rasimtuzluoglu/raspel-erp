# RasPel ERP — KOBİ'nin Tek Panosu

> Fatura, stok, cari, personel... Hepsi tek ekranda. İnternet kesilse de çalışır, masaüstüne kurulur, yapay zeka ile geleceği tahmin eder, her yerden erişilir.

**Java 21 · Spring Boot 3.2 · Vue 3 · PrimeVue 4 · PostgreSQL 16 · Redis · Docker · Yapay Zeka (AI)**

---

## 🤖 Akıllı ERP & Yapay Zeka Özellikleri

| Modül | Açıklama |
|---|---|
| 🧠 **Akıllı Stok & Talep Tahmini (Predictive AI)** | Son 90 günlük tüketim hızına göre ürünlerin kaç gün sonra tükeneceğini hesaplar. Emniyet stoku ve tedarik süresini hesaba katarak proaktif sipariş önerir, 1 tıkla satınalma talebine dönüştürür. |
| 💬 **Doğal Dilde Veri Sorgulama (AI Chat Assistant)** | *"Bu ay en çok ciro yapan 3 müşteri kim?"*, *"Gelecek hafta vadesi gelen ödemelerim neler?"*, *"Kasa ve banka toplam bakiyemiz nedir?"* gibi doğal dilde soruları anında analiz eder, özet grafik ve tablolar üretir. |
| 📈 **Nakit Akışı Projeksiyonu (Cash Flow Forecast)** | Kasa/banka bakiyesi üzerine gelecek 30/60/90 gündeki beklenen tahsilat ve ödemeleri kümülatif ekleyerek gelecek kasa durumunu grafik ve tablolarla tahmin eder. |
| ☁️ **Otomatik Cloud Yedekleme** | Veritabanı yedeklerini uçtan uca **AES-256** ile şifreleyerek otomatik veya manuel olarak **AWS S3, Google Drive veya Dropbox** bulut depolarına senkronize eder. |
| 🛡️ **IP Kısıtlaması & Güvenlik Anomalileri** | Tanımlı IP Beyaz Listesi (Whitelist) dışındaki giriş denemelerini, mesai dışı toplu veri değişikliklerini ve şüpheli hareketleri yapay zeka ile otomatik tespit eder. |

---

## Neden RasPel?

KOBİ'lerin muhasebeciye bağımlı kalmadan günlük işlerini yönetebilmesi için yapıldı. Karmaşık ERP'lerin aksine menüsü sade, öğrenmesi kolay, ihtiyacın kadarını gösteriyor.

| Öne çıkan | Açıklama |
|-----------|----------|
| 🏢 Çoklu şirket | Kullanıcı birden fazla firmaya atanabilir, girişte seçer. Admin tüm firmaları görür |
| 🔐 3 adımlı giriş | Kullanıcı/şifre → 2FA (opsiyonel) → Firma seçimi → Dashboard. Firmalar public değil |
| 📱 PWA (Progressive Web App) | Tarayıcıdan masaüstüne/mobile yerel uygulama olarak kurun, önbellek desteğiyle çalışın |
| 📊 CRM Kanban & Zaman Çizelgesi | Fırsatları sürükle-bırak yöntemiyle yönetin, kayıt tarihçelerini Timeline ile inceleyin |
| 📤 Esnek Export & Yetkilendirme | Excel/CSV/Print entegre menüsü, `v-permission` yetki direktifi |
| 🧮 Yerleşik araçlar | Hesap makinesi, döviz çevirici, KDV/taksit/kar marjı hesabı |
| ⌨️ Klavye kısayolları | `Ctrl+K` Omnibar arama, `Esc` kapatma... fareye gerek yok |
| 🌙 Karanlık mod | Gece geç saatlere kadar çalışanlar için modern tema |

---

## Modüller

### 💰 Finans

**Cari Hesaplar** — Müşteri, tedarikçi, bakiye, kredi limiti, vade takibi. IBAN doğrulama, toplu Excel, cari ekstre.

**Fatura** — Alış/satış faturası. Otomatik seri no (`FTR-1-2026-000001`), iskonto, KDV, PDF, e-posta gönderimi. Faturayı çoğalt, toplu sil.

**Banka & Kasa** — Hesap bakiyeleri, para giriş/çıkışı. Hesap özeti yükleyip otomatik fatura eşleştirme (mutabakat).

**Çek/Senet, Bütçe, Masraf, Nakit Akışı** — Portföy takibi, 30/60/90 günlük nakit akışı projeksiyonu, gider kaydı.

**Döviz** — TCMB'den günlük kur. Yerleşik çevirici ile anında hesapla.

### 🛒 Ticaret

**Hızlı Satış** — Barkod okuyuculu POS. Sepet, indirim, termal fiş.

**Sipariş → İrsaliye → Fatura** — Tek elden iş akışı.

**E-Fatura** — UBL-TR 2.1 ile GİB entegrasyonu.

**Satınalma, CRM Kanban, İade, Fiyat Listesi** — Tedarik, sürükle-bırak fırsat takibi, müşteri bazlı fiyat.

### 📦 Stok

Stok kartı, barkod, kritik seviye alarmı, **Akıllı AI Talep Tahmini**, hareket geçmişi, çoklu depo, şubeler arası transfer, seri/lot takibi, sayım.

### 👥 Personel

TC kimlik doğrulamalı kayıt, izin takibi, puantaj, maaş bordro, vardiya.

### 📊 Muhasebe

Otomatik hesap planı, dengeli yevmiye fişi, mizan, defter-i kebir.

### ⚙️ Sistem & Yapay Zeka

Dashboard, **AI Sohbet Asistanı**, **IP Whitelist & Güvenlik Anomalileri**, **Bulut Yedekleme (AES-256)**, raporlar (KDV, BA/BS, yaşlandırma), denetim logu, rol ve yetki yönetimi.

---

## Giriş Akışı

```
Kullanıcı adı + şifre → 2FA (varsa) → Firma seçimi → Dashboard
```

- **Admin** kullanıcılar tüm aktif firmaları görür, istediğini seçer
- **USER** kullanıcılar sadece atandıkları firmaları görür. Tek firma varsa otomatik giriş yapar
- Firma listesi public değildir, sadece giriş yapmış kullanıcıya gösterilir
- Kullanıcı yönetim panelinden bir kullanıcı birden fazla firmaya atanabilir (çoka-çok)

---

## Başlat

```bash
# Geliştirme (3 ayrı terminal)
docker-compose up -d postgres redis rabbitmq     # Altyapı
cd backend && mvn spring-boot:run                 # API → :8081
cd frontend && npm ci && npm run dev              # UI → :5173
```

İlk başlatmada sistem boşsa giriş sayfası **ilk kurulum** formunu gösterir: firma adı, vergi no ve yönetici hesabı bilgilerini girerek başlarsınız. Önceden tanımlı demo kullanıcı yoktur.

---

```bash
# Production (tek komut)
cp .env.example .env   # şifreleri ve domain'i doldur
docker-compose up -d    # 9 servis, SSL dahil
```

Production öncesi `docs/GO-LIVE.md` kontrol listesine bakın.

---

## Dizin Yapısı

```
raspel-erp/
├── backend/                 # Spring Boot 3.2 API (600+ test)
│   └── src/main/java/com/raspel/erp/
│       ├── controller/      # REST API denetleyicileri
│       ├── service/         # AI motorları, Redis cache, tenant kontrol
│       ├── repository/      # 54 JPA repository
│       ├── entity/          # JPA entity sınıfları
│       └── dto/             # Veri transfer objeleri
├── frontend/                # Vue 3 SPA + Vite + PrimeVue 4 (143 test)
│   └── src/
│       ├── views/           # 57 sayfa
│       ├── components/      # Shared bileşenler (ExportMenu, Timeline, Omnibar)
│       ├── composables/     # Composable hook'lar
│       ├── stores/          # Pinia store'lar
│       └── api/             # Axios client + modüller
├── config/                  # Traefik, Prometheus, Grafana
├── scripts/                 # Yedekleme, test
└── docs/                    # Dökümantasyon
```

---

## Test & Kalite Güvencesi

```bash
cd backend && mvn test       # 600+ Unit ve Entegrasyon Testi (0 Hata)
cd frontend && npm run test  # 143 Vitest Unit Testi (0 Hata)
cd frontend && npm run lint  # Sıfır ESLint uyarısı
cd frontend && npm run build # PWA Üretim Derlemesi
```

---

## Lisans

MIT — © 2026 RasPel Co.
