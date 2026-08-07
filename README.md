# RasPel ERP — KOBİ'nin Tek Panosu

> Fatura, stok, cari, personel... Hepsi tek ekranda. İnternet kesilse de çalışır, masaüstüne kurulur, her yerden erişilir.

**Java 21 · Spring Boot 3.2 · Vue 3 · PrimeVue 4 · PostgreSQL 16 · Redis · Docker**

---

## Neden RasPel?

KOBİ'lerin muhasebeciye bağımlı kalmadan günlük işlerini yönetebilmesi için yapıldı. Karmaşık ERP'lerin aksine menüsü sade, öğrenmesi kolay, ihtiyacın kadarını gösteriyor.

| Öne çıkan | Açıklama |
|-----------|----------|
| 🏢 Çoklu şirket | Kullanıcı birden fazla firmaya atanabilir, girişte seçer. Admin tüm firmaları görür |
| 🔐 3 adımlı giriş | Kullanıcı/şifre → 2FA (opsiyonel) → Firma seçimi → Dashboard. Firmalar public değil |
| 📱 PWA | Tarayıcıdan masaüstüne kurun, internet kesilse de çalışın |
| 🔐 2FA | Google Authenticator ile iki faktörlü giriş |
| 🧮 Yerleşik araçlar | Hesap makinesi, döviz çevirici, KDV/taksit/kar marjı hesabı |
| ⌨️ Klavye kısayolları | `Ctrl+K` arama, `g+f` fatura, `g+c` cari... fareye gerek yok |
| 🌙 Karanlık mod | Gece geç saatlere kadar çalışanlar için |

---

## Modüller

### 💰 Finans

**Cari Hesaplar** — Müşteri, tedarikçi, bakiye, kredi limiti, vade takibi. IBAN doğrulama, toplu Excel, cari ekstre.

**Fatura** — Alış/satış faturası. Otomatik seri no (`FTR-1-2026-000001`), iskonto, KDV, PDF, e-posta gönderimi. Faturayı çoğalt, toplu sil.

**Banka & Kasa** — Hesap bakiyeleri, para giriş/çıkışı. Hesap özeti yükleyip otomatik fatura eşleştirme (mutabakat).

**Çek/Senet, Bütçe, Masraf** — Portföy takibi, aylık planlama, gider kaydı.

**Döviz** — TCMB'den günlük kur. Yerleşik çevirici ile anında hesapla.

### 🛒 Ticaret

**Hızlı Satış** — Barkod okuyuculu POS. Sepet, indirim, termal fiş.

**Sipariş → İrsaliye → Fatura** — Tek elden iş akışı.

**E-Fatura** — UBL-TR 2.1 ile GİB entegrasyonu.

**Satınalma, CRM, İade, Fiyat Listesi** — Tedarik, fırsat takibi, müşteri bazlı fiyat.

### 📦 Stok

Stok kartı, barkod, kritik seviye alarmı, hareket geçmişi, çoklu depo, şubeler arası transfer, seri/lot takibi, sayım.

### 👥 Personel

TC kimlik doğrulamalı kayıt, izin takibi, puantaj, maaş bordro, vardiya.

### 📊 Muhasebe

Otomatik hesap planı, dengeli yevmiye fişi, mizan, defter-i kebir.

### ⚙️ Sistem

Dashboard, raporlar (KDV, BA/BS, yaşlandırma), denetim logu, anomali tespiti, yedekleme, rol ve yetki yönetimi.

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

İlk başlatmada `IlkKullaniciInitializer` varsayılan admin kullanıcısını oluşturur.  
Şifreyi `APP_ADMIN_PASSWORD` ortam değişkeni ile değiştirin (varsayılan: `admin / admin123`).

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
├── backend/                 # Spring Boot API (510 test)
│   └── src/main/java/com/raspel/erp/
│       ├── controller/      # 7 alt paket
│       ├── service/         # Redis cache, tenant kontrol
│       ├── repository/      # 51 JPA repository
│       ├── entity/          # JPA entity
│       └── dto/             # Veri transfer objeleri
├── frontend/                # Vue 3 SPA (84 test)
│   └── src/
│       ├── views/           # 52 sayfa
│       ├── components/      # 25+ bileşen
│       ├── composables/     # 12 composable
│       ├── stores/          # Pinia store
│       └── api/             # Axios client + modüller
├── config/                  # Traefik, Prometheus, Grafana
├── scripts/                 # Yedekleme, test
└── docs/                    # Dökümantasyon
```

---

## Test

```bash
cd backend && mvn test       # 510 test, 0 hata
cd frontend && npm run test  # 84 test
cd frontend && npm run lint  # ESLint
```

---

## Lisans

MIT — © 2026 RasPel Co.
