<div align="center">

# ⚡ RasPel ERP

### Modern & Akıllı Kurumsal Kaynak Planlama Sistemi

**Fatura, stok, cari, saha operasyonları, finans ve personel yönetimi — hepsi tek platformda.**

İnternet kesilse de çalışan PWA desteği · Yapay zeka ile talep tahmini · Çok kiracılı (multi-tenant) veri izolasyonu · Çoklu kullanıcı güvenliği · Gelişmiş yetkilendirme

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![PrimeVue](https://img.shields.io/badge/PrimeVue-4-41B883)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

![Tests](https://img.shields.io/badge/Tests-1047%20backend%20%7C%20687%20frontend-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

</div>

---

## 🚀 Öne Çıkan Yenilikler & Akıllı Özellikler

| Modül | Açıklama |
|---|---|
| **🔐 Çok Kiracılı (Multi-Tenant) İzolasyon** | Her servis katmanında şirket sahiplik doğrulaması (`TenantChecker`, fail-closed). Tenant bilgisi olmayan kayıtlar erişilemez; referans ID'ler (personel, stok, cari, depo, dönem) tenant bazında doğrulanır. Cache anahtarları dahi şirket bazında izole edilir. |
| **🔑 Şifre Sıfırlama Akışı** | E-posta ile tek kullanımlık, 1 saat geçerli sıfırlama bağlantısı (SHA-256 hash'li token) + yönetici panelinden hızlı sıfırlama. Güvenlik gereği kullanıcı/e-posta varlığı sızdırılmaz. |
| **📄 Sunucu-Taraflı Fatura Tasarımı & POS Fiş Ayarları** | Fatura tasarım şablonu (başlık, renk, kağıt boyutu, logo, imza) ve POS fiş ayarları sunucuda şirket bazında saklanır; PDF üretiminde otomatik uygulanır. |
| **✅ Gerçek Gönderim Durumu (Sahte Başarı Yok)** | E-posta servisi gönderim sonucunu döndürür; SMTP yapılandırılmamışsa "GÖNDERİLDİ" raporlanmaz. e-Fatura yalnızca gerçek GİB/entegratör yanıtıyla onaylanır — simülasyon üretilmez. |
| **🛡️ Yedekleme & Geri Yükleme Güvenliği** | Geri yükleme öncesi zorunlu otomatik ön-snapshot, tekil işlem kilidi ve şifre ile yeniden kimlik doğrulaması. Redis kalıcılığı (`appendonly`) ile iptal edilen oturumlar restart'ta kaybolmaz. |
| **Dashboard (Kişiselleştirilebilir)** | 6 widget: Bugünün Özeti & Hedefler, İstatistik Kartları, Kritik Stok Uyarıları, Grafikler, Son Hareketler, Ödeme Vadeleri. Her kullanıcı kendi dashboard'unu açıp kapatabilir. |
| **Tahsilat Merkezi** | Vadesi geçmiş/alacak bekleyen faturalar yaşlandırma tablosu, otomatik hatırlatma e-postası, WhatsApp ve arama aksiyonları. Alacak/borç toplamları anlık kartlarda. |
| **Saha Personeli Portalı** | Saha çalışanları için özel mobil uyumlu arayüz. Sipariş teslimatı, dijital müşteri imzası (Canvas), masraf fişi yükleme ve anlık izin talebi oluşturma. |
| **Onay Merkezi** | İzin, masraf, satınalma ve saha sipariş taleplerinin yöneticiler tarafından tek tıkla incelenip onaylanmasını sağlayan merkezi iş akışı. |
| **Yönetici Kokpiti** | Sadece yöneticilere özel ciro, net kâr, bütçe hedefleri ve şirket likidite analizlerini gösteren üst yönetim panosu. |
| **Gelişmiş Kârlılık Analizi** | Ağırlıklı ortalama maliyet (COGS) motoru üzerinden aylık trend, kategori/ürün/cari kırılımı, negatif marj uyarıları ve iade düzeltmeli net kâr. |
| **Fatura İşlem Geçmişi & Yazdırma İzi** | Her fatura için oluşturma/düzenleme/durum/silme ve yazdırma olayları; alan bazlı önce/sonra değişiklikleriyle zaman çizelgesinde. |
| **Yapay Zeka Destekli ERP Asistanı** | OpenAI, Google Gemini ve Anthropic Claude API anahtarlarını AES-256 ile şifreleyerek entegre eden, doğal dilde şirket verilerini sorgulayan akıllı sohbet asistanı. |
| **Akıllı Stok & Talep Tahmini** | Son 90 günlük tüketim hızına göre tükenme süresi, emniyet stoku ve tedarik süresini hesaba katarak proaktif satınalma önerileri üretir. |
| **Eşzamanlı Çoklu Kullanıcı Koruması** | Pessimistic Locking (`SELECT FOR UPDATE`) ile stok ve kasa çakışmalarını, eksiye düşmeyi ve mükerrer belge numaralandırmayı engeller. |
| **Nakit Akışı Projeksiyonu** | Kasa/banka bakiyesi üzerine gelecek 30/60/90 gündeki tahsilat ve ödemeleri kümülatif ekleyerek finansal geleceği grafiklerle simüle eder. |
| **Otomatik Bulut Yedekleme** | Yedekleri otomatik/manuel olarak bulut deposuna (MinIO / S3 uyumlu) senkronize eder; kopyalar istendiğinde AES-256-GCM ile şifrelenir (`.enc`). |
| **BTC & Döviz Kurları** | USD, EUR, GBP, Altın ve Bitcoin kurları TCMB ve Binance/CoinGecko API'lerinden canlı çekilir, anlık çevirici ile dönüşüm yapılır. |
| **Kapsamlı Performans Optimizasyonu** | Rapor ve liste uçlarında N+1 sorguları ortadan kaldırıldı; toplu (batch) stok/kalem/cari yüklemesi, sayfa bazlı tek sorgu ile DTO dönüşümü ve export üst sınırı. |

---

## 🧩 Temel ERP Yetenekleri

| Özellik | Açıklama |
|---|---|
| **Çoklu Şirket & Şube** | Kullanıcı birden fazla firmaya atanabilir, girişte seçim yapar. Şirketler arası tam veri izolasyonu. |
| **3 Adımlı Güvenli Giriş** | Kullanıcı/Şifre → TOTP 2FA (opsiyonel) → Firma Seçimi → Dashboard. Brute-force koruması ve IP kısıtlaması. |
| **Şifre Sıfırlama** | E-posta ile tek kullanımlık bağlantı (1 saat) veya yönetici panelinden anında sıfırlama; tüm oturumlar geçersiz kılınır. |
| **Dinamik RBAC & Yetki Matrisi** | Roller ve modül bazlı okuma, yazma, silme ve dışa aktarım izinleri (`v-permission`). |
| **Mobil Uyumlu (PWA)** | Alt navigasyon menüsü, 44px dokunmatik hedefler, safe-area desteği, masaüstü ve mobilde yerel uygulama gibi kurulabilir. |
| **Karanlık / Aydınlık Tema** | PrimeVue Lara tema + token katmanı ile zümrüt aksanlı, gece ve gündüze uygun modern arayüz. |
| **CRM Kanban & Zaman Çizelgesi** | Müşteri fırsatlarını sürükle-bırak yöntemiyle yönetin, kayıt tarihçelerini Timeline ile inceleyin. |
| **Klavye Kısayolları** | `Ctrl+K` Omnibar hızlı arama, `Ctrl+Shift+T` tema değiştirme, `Esc` kapatma ile fareye ihtiyaç duymadan hızlı operasyon. |
| **Adres Defteri** | Elektrikçi, tesisatçı, marangoz gibi hizmet kişileri; ad, telefon, e-posta, adres ve etiketlerle kayıtlı. Arama + meslek/etiket filtresi ve hızlı aksiyonlar. |
| **Oturum & Anomali Güvenliği** | Aktif oturumları listeleme/uzaktan sonlandırma, mükerrer fatura/ödeme ve yüksek tutar anomalilerini otomatik bildirme. |
| **Yedek Doğrulama & Denetim İzi** | Yedeklerin bütünlük/güncellik health-check'i; denetim loglarında değişiklik öncesi/sonrası değer (diff) kaydı. |

---

## 🏗️ Modül Mimarisi

### 💰 Finans & Muhasebe
- **Cari Hesaplar**: Müşteri, tedarikçi, bakiye, kredi limiti, vade takibi, IBAN doğrulama, toplu Excel aktarımı ve ekstre. Cariye özel geçmiş ürünler ve ürün bazlı fiyat geçmişi.
- **Fatura Yönetimi**: Alış/Satış faturası, otomatik seri no (`FTR-1-2026-000001`), iskonto, KDV, PDF, e-posta gönderimi ve çoğaltma. İşlem geçmişi (diff) ve yazdırma izi. Sunucu-taraflı tasarım şablonu PDF'e uygulanır.
- **Banka & Kasa**: Hesap bakiyeleri, para giriş/çıkışı, CSV/Excel/OFX hesap özeti yükleme, otomatik mutabakat ve kasalar arası para aktarımı.
- **Çek/Senet, Bütçe & Masraflar**: Portföy takibi, departman bütçeleri, masraf fişleri ve nakit akışı projeksiyonu.
- **Genel Muhasebe**: Otomatik tek düzen hesap planı, dengeli yevmiye fişi, mizan, defter-i kebir, bilanço ve kâr/zarar.
- **Döviz Kurları & BTC**: Canlı döviz/kripto kurları, otomatik çevirici, manuel kur girişi.
- **Tahsilat Merkezi**: Vade takibi, yaşlandırma raporu, hatırlatma e-postası (gerçek gönderim durumu), WhatsApp/arama aksiyonları.

### 🛒 Ticaret & Satış
- **Hızlı Satış (POS)**: Barkod okuyucu destekli hızlı satış, sepet, indirim ve termal fiş yazdırma. Global barkod girişi, kameralı sürekli okuma, çok satanlar paneli, offline satış kuyruğu ve ESC/POS termal yazıcı desteği. Fiş ayarları sunucuda saklanır.
- **Sipariş & İrsaliye**: Siparişten irsaliyeye, irsaliyeden faturaya tek tıkla kontrollü iş akışı. Saha siparişleri onay akışı.
- **E-Fatura**: UBL-TR 2.1 standardında GİB uyumlu e-fatura ve e-arşiv entegrasyonu. GİB gönderim ve durum sorgulaması yalnızca gerçek entegratör üzerinden; sahte onay üretilmez.
- **CRM Kanban**: Satış hunisi, teklif yönetimi, aşama takibi ve müşteri bazlı özel fiyat listeleri. Müşteri kayıp (churn) riski skorlama.
- **Adres Defteri**: Hizmet kişilerinin kaydı; isim/telefon/adres arama, meslek ve etiket filtresi, tek tıkla arama/WhatsApp/e-posta ve haritada açma.

### 📦 Stok & Envanter
- **Stok Kartları & Barkod**: Kritik seviye alarmı, akıllı AI talep tahmini, hareket geçmişi. Ağırlıklı ortalama maliyet (COGS) motoru ile güncel birim maliyet ve satış anı maliyet anlık görüntüsü.
- **Çoklu Depo & Şube**: Şubeler arası transfer, seri/lot/SKT takibi (FEFO tüketim) ve periyodik stok sayım modülü.
- **Üretim (Reçete & Emir)**: Ürün ağacı tanımı, Taslak→Üretimde→Tamamlandı/İptal akışı, kısmi üretim ve fire, otomatik hammadde düşümü + mamul girişi, hammadde/işçilik/toplam maliyet. Hammadde ihtiyaç analizi ve siparişten otomatik üretim emri.

### 📊 Raporlama & Analitik
- **Rapor Merkezi**: Cari ekstre, gelir/gider, KDV, yaşlandırma, cari & ürün kârlılığı, nakit akışı projeksiyonu ve pivot tablo; grafiklerle zenginleştirilmiş sekmeler, PDF/Excel dışa aktarım ve e-posta ile paylaşım.
- **Fatura İşlem & Yazdırma Geçmişi Raporu**: Tarih/tür/olay/kullanıcı filtreli, olay grafiği ve PDF/Excel çıktısı.
- **Merkezi Rapor Arama & Favoriler**: Tüm raporlar arasında tek arama, sık kullanılan raporları yıldızlayıp hızlı erişim.

### 👥 İnsan Kaynakları (İK)
- **Personel Kartları**: TC Kimlik doğrulama, departman/pozisyon atamaları, rol tanımı (şoför, depocu vb.) ve acil durum bilgileri.
- **İzin & Vardiya**: İzin talepleri, hakediş hesaplama, haftalık vardiya planlama ve puantaj cetveli.
- **Maaş Bordro**: SGK, gelir vergisi ve damga vergisi kesintileriyle otomatik bordro hesaplama.

---

## ⚡ Hızlı Başlangıç

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

> **Not:** İlk açılışta veritabanı boşsa sistem otomatik olarak İlk Kurulum Sihirbazı ekranına yönlendirir; şirket bilgilerinizi ve ilk yönetici hesabınızı tanımlayarak hemen başlayabilirsiniz.

---

## 🐳 Üretim (Production / Docker)

```bash
# .env dosyasını yapılandırın
cp .env.example .env

# Tüm servisleri tek komutla başlatın (Traefik, SSL, Prometheus, Grafana dahil)
docker-compose up -d
```

Detaylı canlıya geçiş kontrol listesi için [`docs/GO-LIVE.md`](docs/GO-LIVE.md) dosyasını inceleyebilirsiniz.

---

## ✅ Test ve Kalite Güvencesi

Proje uçtan uca kapsamlı birim ve entegrasyon testleriyle korunmaktadır:

```bash
# Backend Testleri (JUnit 5 + H2 + Mockito)
cd backend
mvn -B test -q          # 1047 Test (0 Hata)

# Frontend Testleri (Vitest)
cd frontend
npm run test            # 687 Test (0 Hata)

# Kod Standartları & Linting
npm run lint            # Sıfır ESLint Uyarısı
npm run i18n:check      # i18n bütünlük kontrolü

# Üretim Derlemesi (PWA Build)
npm run build           # Optimize edilmiş üretim paketleri
```

---

## 📁 Proje Dizin Mimarisi

```
raspel-erp/
├── backend/                 # Spring Boot 3.2 REST API
│   └── src/main/java/com/raspel/erp/
│       ├── controller/      # REST Denetleyicileri (envanter, finans, ik, muhasebe, sistem, ticaret)
│       ├── service/         # İş mantığı, AI motorları, Redis cache, SeriNo servisi, TahsilatService
│       ├── repository/      # JPA Repository katmanı (Pessimistic Lock destekli)
│       ├── entity/          # JPA Veritabanı Varlıkları
│       ├── dto/             # Data Transfer Objects
│       └── config/          # Spring Security, JWT, TenantChecker, WebSocket, Cache
│
├── frontend/                # Vue 3 SPA + Vite + PrimeVue 4 + Tailwind CSS
│   └── src/
│       ├── views/           # 73 Görünüm (Dashboard, Tahsilat, SahaPortali, Onaylar, YoneticiKokpiti vb.)
│       ├── components/      # 51 Paylaşılan Bileşen
│       ├── stores/          # 13 Pinia Durum Yönetimi (auth, dashboard, doviz, fatura, stok vb.)
│       ├── composables/     # 19 Composable Hook (Tema, Yetki, Oturum, Kısayol)
│       └── api/             # Modüler Axios İstemcisi
│
├── config/                  # Traefik Reverse Proxy, Prometheus, Grafana
├── scripts/                 # Otomatik yedekleme ve bakım betikleri
└── docs/                    # Mimari ve kullanım dökümanları
```

---

<div align="center">

## 📜 Lisans

Bu proje [MIT](LICENSE) lisansı ile lisanslanmıştır.

© 2026 RasPel ERP

</div>
