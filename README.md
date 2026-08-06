# RasPel ERP

Küçük ve orta ölçekli işletmeler için geliştirilmiş, Türkçe öncelikli ERP yazılımı. Finans, ticaret, stok, personel ve muhasebe modüllerini tek platformda sunar.

| Katman | Teknoloji |
|--------|-----------|
| Backend | Java 21, Spring Boot 3.2, PostgreSQL 16, Redis 7, RabbitMQ |
| Frontend | Vue 3, Vite, PrimeVue 4, Pinia, Chart.js |
| Altyapı | Docker Compose, Traefik, Prometheus, Grafana |
| Güvenlik | JWT, BCrypt, 2FA (TOTP), tenant izolasyonu |
| Test | 510 backend (JUnit 5), 84 frontend (Vitest), Cypress E2E |

---

## Modüller

### Finans
**Cari Hesaplar:** Müşteri ve tedarikçi kayıtları, bakiye takibi, kredi limiti, ödeme vadesi. Her cari hesap için tahsilat ve ödeme geçmişi, IBAN doğrulama, toplu Excel çıktısı.

**Fatura:** Alış ve satış faturası. Otomatik seri numarası (şirket bazlı `FTR-1-2026-000001`), iskonto, KDV hesaplama, PDF çıktısı, e-posta ile gönderim. Fatura çoğaltma (aynı kalemlerle yeni fatura), toplu silme.

**Banka & Kasa:** Hesap bakiyeleri, para giriş/çıkış takibi. Banka mutabakatı için hesap özeti yükleme ve otomatik fatura eşleştirme. IBAN kopyalama.

**Çek/Senet:** Portföy takibi, vade yönetimi, durum güncelleme.

**Bütçe & Masraf:** Aylık/yıllık bütçe planlama, gider kaydı ve karşılaştırma.

**Döviz:** TCMB'den otomatik kur çekme, döviz çevirici araç.

### Ticaret
**Hızlı Satış:** Barkod okuyucu destekli POS ekranı. Sepet, indirim, termal fiş yazdırma.

**Sipariş & İrsaliye:** Satış siparişi, irsaliye düzenleme, durum takibi. Siparişten faturaya dönüşüm.

**E-Fatura:** UBL-TR 2.1 formatında e-fatura oluşturma ve GİB'e gönderim.

**Satınalma:** Talep ve sipariş yönetimi, tedarikçi bazlı takip.

**CRM:** Satış fırsatı takibi, aşama yönetimi.

**İade & Fiyat Listesi:** Ürün iade yönetimi, müşteri bazlı özel fiyat listeleri.

### Stok
**Stok Kartı:** Ürün kodu, barkod, alış/satış fiyatı, KDV oranı, birim, kritik stok seviyesi. Stok hareket geçmişi (giriş/çıkış detayı), toplu CSV içe/dışa aktarım.

**Depo & Şube:** Çoklu depo ve şube yönetimi, depolar arası transfer.

**Seri/Lot & Sayım:** Ürün seri numarası takibi, stok sayım fişi.

### Personel
**Personel Kaydı:** TC kimlik doğrulama, iletişim bilgileri, departman ve pozisyon.

**İzin & Puantaj:** Yıllık izin takibi, günlük puantaj girişi.

**Maaş & Vardiya:** Bordro hesaplama, vardiya planlaması.

### Muhasebe
**Hesap Planı:** Varsayılan hesap planı otomatik oluşturulur, şirket bazlı özelleştirilebilir.

**Fiş, Mizan, Defter-i Kebir:** Dengeli yevmiye fişleri, dönem sonu mizan ve defter-i kebir raporları.

### Sistem
**Dashboard:** Özet istatistikler, son 6 aylık gelir-gider grafiği, kritik stok uyarıları, hızlı işlem butonları.

**Raporlar:** KDV beyannamesi, BA/BS formu, cari hesap ekstresi, yaşlandırma raporu. Tüm listelerden Excel çıktısı.

**Çoklu Şirket:** Her şirket kendi verisini görür, tenant izolasyonu ile veri güvenliği.

**Yetki Yönetimi:** Rol tabanlı erişim (RBAC). Admin ve kullanıcı rolleri, modül bazlı yetkilendirme.

**Denetim & Anomali:** Tüm işlemlerin kayıt altına alınması, şüpheli fatura ve mükerrer ödeme tespiti.

**Yedekleme:** Otomatik zamanlanmış veritabanı yedekleme, manuel yedek alma ve geri yükleme.

---

## Kullanıcı Deneyimi

- **Klavye kısayolları:** Ctrl+K ile evrensel arama, `g` + harf ile sayfalar arası hızlı geçiş
- **Gelişmiş/Temel mod:** Menüyü sadeleştirip sadece sık kullanılanları gösterme
- **Yerleşik araçlar:** Hesap makinesi, döviz çevirici, KDV hesaplayıcı, taksit hesaplayıcı, kar marjı hesaplayıcı
- **Form koruma:** Kaydedilmemiş değişikliklerde uyarı
- **Boş durum yönlendirmesi:** Veri yokken "ilk kaydınızı oluşturun" butonu
- **Tarih filtresi:** Liste sayfalarında hızlı tarih aralığı seçimi
- **Toplu işlem:** Fatura ve hareket listelerinde çoklu seçim ve toplu silme
- **PWA:** Tarayıcıdan masaüstüne kurulum, çevrimdışı veri görüntüleme
- **Karanlık/Aydınlık mod:** Tema değiştirme

---

## Hızlı Başlangıç (Geliştirme)

```bash
# 1. Altyapıyı başlat
docker-compose -f docker-compose.dev.yml up -d

# 2. Backend (port 8081)
cd backend
mvn spring-boot:run

# 3. Frontend (port 5173)
cd frontend
npm install
npm run dev
```

Tarayıcıda **http://localhost:5173** adresini açın.  
İlk giriş: `admin` / `admin123`

Adminer (veritabanı yönetimi): http://localhost:8082

---

## Production Kurulumu

```bash
# .env dosyasını oluşturun
cp .env.example .env
# Tüm değişkenleri doldurun (şifreler, JWT_SECRET, domain vb.)

# Tüm servisleri başlat
docker-compose up -d
```

Production öncesinde `docs/GO-LIVE.md` kontrol listesini mutlaka gözden geçirin.

Servisler: Traefik (80/443 SSL), Backend, Frontend, PostgreSQL, Redis, RabbitMQ, Prometheus, Grafana, Alertmanager

---

## Dizin Yapısı

```
raspel-erp/
├── backend/src/main/java/com/raspel/erp/
│   ├── controller/    # REST API (7 alt paket)
│   ├── service/       # İş mantığı (Redis cache, tenant kontrol)
│   ├── repository/    # JPA repository (51 adet)
│   ├── entity/        # JPA entity
│   ├── dto/           # Veri transfer objesi
│   ├── config/        # Security, Redis, WebSocket, Health
│   └── aspect/        # Audit log AOP
├── frontend/src/
│   ├── views/         # 52 sayfa (lazy-loaded)
│   ├── components/    # 25+ bileşen
│   ├── composables/   # 12 composable
│   ├── stores/        # 11 Pinia store
│   ├── api/           # Axios client + 7 domain modülü
│   └── locales/       # Türkçe + İngilizce
├── config/            # Traefik, Prometheus, Grafana
├── scripts/           # Yedekleme, geri yükleme, test
└── docs/              # API, kullanım, kurulum rehberi
```

---

## Komutlar

```bash
# Backend test
cd backend && mvn test

# Frontend test
cd frontend && npm run test

# Lint
cd frontend && npm run lint

# Build
cd frontend && npm run build
cd backend && mvn clean package -DskipTests
```

---

## Lisans

MIT — © 2026 Rasim Tuzluoğlu
