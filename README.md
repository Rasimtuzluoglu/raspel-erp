# <img src="https://img.icons8.com/fluency/48/calculator.png" width="32" /> RasPel - Yeni Nesil ERP

> **Ras**im **Tuz**luoğlu tarafından geliştirilmiş, modern ve kapsamlı Kurumsal Kaynak Planlama sistemi.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Vue.js-3.3-4FC08D?style=flat&logo=vuedotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" />
</p>

---

## 🚀 Özellikler

### 💰 Finans
| Modül | Açıklama |
|-------|----------|
| **Cari Hesap** | Müşteri/tedarikçi yönetimi, bakiye takibi, kredi limiti, ödeme vadesi, tahsilat/ödeme |
| **Fatura** | Alış/satış faturası, iskonto, ödeme, KDV hesaplama, e-fatura entegrasyonu |
| **Banka & Kasa** | Hesap takibi, havale/EFT, kasa hareketleri |
| **Çek/Senet** | Portföy takibi, vade yönetimi, cirolama |
| **Bütçe** | Gelir/gider bütçesi, aylık/yıllık planlama |
| **Masraf** | Gider takibi, belge no ile kayıt |

### 🛒 Ticaret
| Modül | Açıklama |
|-------|----------|
| **Satış (POS)** | Barkod okuyuculu hızlı satış, anlık sepet, seri no arama, termal yazıcı fiş, para üstü |
| **Sipariş** | Teklif → Sipariş dönüşümü, durum takibi |
| **Satın Alma** | Talep → Sipariş akışı, tedarikçi yönetimi |
| **İrsaliye** | Sevk irsaliyesi, irsaliye bazlı fatura, stok hareketi |
| **İade** | İade yönetimi, iade kalemleri, durum takibi |
| **Fiyat Listesi** | Ürün bazlı alış/satış fiyatı, geçerlilik tarihi |

### 📦 Envanter
| Modül | Açıklama |
|-------|----------|
| **Stok Yönetimi** | Ürün kartı, barkod, marka, KDV, raf no, ağırlık, kategori, çoklu birim, tedarikçi bilgisi, maliyet yöntemi |
| **Depo** | Çoklu depo desteği, depo bazlı stok takibi |
| **Seri/Lot** | Seri no, lot no, son kullanma tarihi |
| **Stok Sayım** | Sayım fişi, beklenen/sayılan farkı, otomatik hesaplama |
| **Toplu Stok İmport** | CSV'den toplu ürün yükleme |
| **Depolar Arası Transfer** | Depodan depoya stok taşıma |

### 👥 İnsan Kaynakları
| Modül | Açıklama |
|-------|----------|
| **Personel** | Künye, iletişim, maaş bilgisi |
| **Puantaj** | Günlük devam takibi, raporlama |
| **İzin** | İzin talebi, onay/ret mekanizması, durum filtresi |
| **Maaş Bordro** | Brüt/kesinti/net, dönemsel hesaplama |
| **Vardiya** | Sabah/Akşam/Gece vardiya planlaması |

### 📊 Rapor & Analiz
- Anlık dashboard (cari sayısı, bakiye, tahsilat/ödeme, bekleyen izin, aylık gelir/gider grafiği)
- Satış/sipariş istatistikleri, en çok satan ürünler
- İK özet kartları (aktif çalışan, bugün izinli, bu ay işe başlayacak)
- Bakiye grafiği (pasta), son hareketler (çizgi grafik + tablo)
- Vade yaşlandırma, KDV raporu
- Excel export (Cari, Fatura, Hareket, Stok, Banka, Kasa, Personel)
- PDF rapor

### ⚙️ Sistem
| Özellik | Açıklama |
|---------|----------|
| **Yetkilendirme** | Admin/User rolleri, sayfa bazlı erişim, `requiresAdmin` route guard |
| **Çoklu Şirket** | Tenant izolasyonu, girişte firma seçimi |
| **Denetim Log** | Tüm işlemler kayıt altına alınır |
| **Yedekleme** | Otomatik/manuel, Günlük/Haftalık/Aylık/Yıllık rotasyon |
| **Hızlı Arama** | Ctrl+K ile evrensel arama |
| **Tema** | Açık/Koyu mod desteği, light mode tema fix'leri |
| **Avatar Yükleme** | Kullanıcı profil fotoğrafı yükleme |
| **Dil** | i18n altyapısı (şu an TR, EN eklenebilir) |

---

## 🏗️ Mimari

```
raspel-erp/
├── backend/              # Spring Boot 3.2 + Java 21
│   ├── src/main/java/    # 210+ Java sınıfı
│   │   ├── controller/   # REST API (50+ endpoint)
│   │   ├── service/      # İş mantığı
│   │   ├── repository/   # Veri erişim (JPA)
│   │   ├── entity/       # JPA entity'leri (40+ tablo)
│   │   ├── dto/          # Veri transfer objeleri
│   │   ├── config/       # Security, JWT, Redis, RabbitMQ, Rate Limiter
│   │   └── exception/    # Custom exception + GlobalExceptionHandler
│   └── src/main/resources/
│       └── db/migration/ # Flyway (17 migration)
├── frontend/             # Vue 3 + PrimeVue 4 + Pinia + Chart.js
│   └── src/views/        # 35+ sayfa (Dashboard, POS, Tüm CRUD sayfaları)
├── config/               # Traefik, Prometheus, Grafana
├── scripts/              # Yedekleme script'leri
└── docker-compose.yml    # 9 container, tek komutla ayağa kalkar
```

### 🛠️ Teknoloji Yığını

| Katman | Teknoloji |
|--------|-----------|
| **Backend** | Java 21, Spring Boot 3.2, Spring Security, JPA/Hibernate, Flyway |
| **Frontend** | Vue 3, Vite, PrimeVue 4, Pinia, Chart.js, Axios |
| **Veritabanı** | PostgreSQL 16 |
| **Cache** | Redis 7 |
| **Message** | RabbitMQ 3 |
| **Auth** | JWT (jjwt 0.12) + httpOnly cookie + Login rate limit |
| **Excel** | Apache POI 5.2.5 (.xlsx export) |
| **E2E Test** | Cypress 15 |
| **Monitoring** | Prometheus + Grafana |
| **Reverse Proxy** | Traefik 3 |
| **Container** | Docker + Docker Compose |
| **CI/CD** | GitHub Actions (+ Trivy security scan) |

---

## 🚀 Hızlı Başlangıç

```bash
# Projeyi klonla
git clone https://github.com/Rasimtuzluoglu/raspel-erp.git
cd raspel-erp

# .env dosyasını oluştur
cp .env.example .env

# Tüm servisleri ayağa kaldır
docker compose up -d --build

# Tarayıcıda aç
open http://localhost
```

### Giriş Bilgileri

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| `admin` | `admin123` | ADMIN (tüm sayfalar) |
| `muhasebe` | `123456` | USER (kısıtlı erişim) |

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

---

## 📦 Demo Verisi

İlk çalıştırmada otomatik olarak yüklenen veriler:
- 👤 4 kullanıcı (admin, muhasebe, ali, zeynep)
- 🏢 2 şirket
- 📅 3 dönem (2024-2026)
- 🏪 4 şube + 4 depo
- 📦 20 örnek ürün (yazıcı, monitör, aksesuar...)
- 👥 8 cari hesap (müşteri/tedarikçi)
- 🏦 3 banka hesabı
- 💵 3 kasa

---

## 📜 Migration'lar

| Versiyon | Açıklama |
|----------|----------|
| V1 | İlk şema (28 tablo, 7 schema) |
| V4 | Şube & Depo yapısı |
| V5 | Tenant izolasyonu (`sirket_id`) |
| V6 | `Double` → `BigDecimal` dönüşümü |
| V10 | Finans, Ticaret, Envanter, İK modülleri |
| V11 | Cari hesap detay alanları |
| V12 | Stok detay alanları (marka, kategori, satış fiyatı) |
| V13 | Cari hesap ID null izni, entity fix'leri |
| V14 | Fatura kalem `olusturma_tarihi` default |
| V15 | Siparis/ÇekSenet/Irsaliye `cari_hesap_id` DROP NOT NULL |
| V16 | Fatura iskonto/ödeme, hareket `odeme_sekli`, iade kalemleri |
| V17 | Cari kredi limiti/ödeme vadesi, Stok çoklu birim/tedarikçi/maliyet yöntemi |

---

## 🧪 Test

```bash
# Backend testleri (374 test)
cd backend
./mvnw test

# Frontend E2E testleri (Cypress)
cd frontend
npm run cypress:open   # interaktif
npm run cypress:run    # headless
```

---

## 🤝 Katkı

Geliştirme sürecinde katkıda bulunmak için:
1. Fork'la
2. Feature branch oluştur (`git checkout -b feature/yeni-ozellik`)
3. Değişiklikleri commit'le (`git commit -m 'feat: yeni özellik'`)
4. Branch'i push'la (`git push origin feature/yeni-ozellik`)
5. Pull Request aç

---

## 📄 Lisans

**© 2026 Rasim Tuzluoğlu** - Tüm hakları saklıdır.

---

<p align="center">
  <sub>Built with ❤️ by <strong>Rasim Tuzluoğlu</strong></sub>
  <br>
  <sub>Spring Boot · Vue.js · PostgreSQL · Docker</sub>
</p>
