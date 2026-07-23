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
| **Cari Hesap** | Müşteri/tedarikçi yönetimi, bakiye takibi, tahsilat/ödeme |
| **Fatura** | Alış/satış faturası, KDV hesaplama, e-fatura entegrasyonu |
| **Banka & Kasa** | Hesap takibi, havale/EFT, kasa hareketleri |
| **Çek/Senet** | Portföy takibi, vade yönetimi, cirolama |
| **Bütçe** | Gelir/gider bütçesi, aylık/yıllık planlama |
| **Masraf** | Gider takibi, belge no ile kayıt |

### 🛒 Ticaret
| Modül | Açıklama |
|-------|----------|
| **Satış (POS)** | Barkod okuyuculu hızlı satış, anlık sepet, para üstü |
| **Sipariş** | Teklif → Sipariş dönüşümü, durum takibi |
| **Satın Alma** | Talep → Sipariş akışı, tedarikçi yönetimi |
| **İrsaliye** | Sevk irsaliyesi, irsaliye bazlı fatura |
| **İade** | İade yönetimi, durum takibi |
| **Fiyat Listesi** | Ürün bazlı alış/satış fiyatı, geçerlilik tarihi |

### 📦 Envanter
| Modül | Açıklama |
|-------|----------|
| **Stok Yönetimi** | Ürün kartı, barkod, marka, KDV, raf no, ağırlık, kategori |
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
| **İzin** | İzin talebi, onay mekanizması |
| **Maaş Bordro** | Brüt/kesinti/net, dönemsel hesaplama |
| **Vardiya** | Sabah/Akşam/Gece vardiya planlaması |

### 📊 Rapor & Analiz
- Anlık dashboard (cari, bakiye, fatura, stok)
- Satış/sipariş istatistikleri
- İK özet kartları
- Bakiye grafiği (pasta)
- En çok satan ürünler (bar grafik)
- Son hareketler (çizgi grafik + tablo)
- Vade yaşlandırma, KDV raporu

### ⚙️ Sistem
| Özellik | Açıklama |
|---------|----------|
| **Yetkilendirme** | Admin/User rolleri, sayfa bazlı erişim |
| **Çoklu Şirket** | Tenant izolasyonu, her şirket kendi verisini görür |
| **Denetim Log** | Tüm işlemler kayıt altına alınır |
| **Hızlı Arama** | Ctrl+K ile evrensel arama |
| **Tema** | Açık/Koyu mod desteği |
| **Dil** | i18n altyapısı (şu an TR, EN eklenebilir) |

---

## 🏗️ Mimari

```
raspel-erp/
├── backend/              # Spring Boot 3.2 + Java 21
│   ├── src/main/java/    # 170+ Java sınıfı
│   │   ├── controller/   # REST API (50+ endpoint)
│   │   ├── service/      # İş mantığı
│   │   ├── repository/   # Veri erişim (JPA)
│   │   ├── entity/       # JPA entity'leri (40+ tablo)
│   │   ├── dto/          # Veri transfer objeleri
│   │   ├── config/       # Security, JWT, Redis, RabbitMQ
│   │   └── exception/    # Custom exception'lar
│   └── src/main/resources/
│       └── db/migration/ # Flyway (12 migration)
├── frontend/             # Vue 3 + PrimeVue 4 + Pinia
│   └── src/views/        # 30+ sayfa
├── config/               # Traefik, Prometheus, Grafana
├── scripts/              # Yedekleme script'leri
└── docker-compose.yml    # Tek komutla ayağa kalkar
```

### 🛠️ Teknoloji Yığını

| Katman | Teknoloji |
|--------|-----------|
| **Backend** | Java 21, Spring Boot 3.2, Spring Security, JPA/Hibernate, Flyway |
| **Frontend** | Vue 3, Vite, PrimeVue 4, Pinia, Chart.js, Axios |
| **Veritabanı** | PostgreSQL 16 |
| **Cache** | Redis 7 |
| **Message** | RabbitMQ 3 |
| **Auth** | JWT (jjwt 0.12) + httpOnly cookie |
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

## 📸 Ekran Görüntüleri

| Ana Sayfa | Hızlı Satış | Stok Yönetimi |
|-----------|-------------|---------------|
| Dashboard | Barkod okut, sepete ekle, ödeme | Ürün kartı, depo, sayım |

*(Ekran görüntüleri eklenecek)*

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
