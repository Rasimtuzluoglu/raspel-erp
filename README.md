# RasPel ERP

Kucuk ve orta olcekli isletmeler icin gelistirilmis, Turkce oncelikli ERP yazilimi.

**Teknik altyapi:** Java 21 + Spring Boot 3.2 | Vue 3 + PrimeVue 4 | PostgreSQL 16 + Redis + RabbitMQ | Docker

---

## Ozellikler

### Finans
- Cari hesap takibi, bakiye, kredi limiti, vade
- Alis/satis faturasi, iskonto, KDV, otomatik seri no
- Banka ve kasa yonetimi, IBAN kopyalama
- Cek/senet, butce, masraf takibi
- Doviz kurlari (TCMB'den otomatik)

### Ticaret
- Hizli satis (barkod okuyucu, termal fis)
- Siparis, irsaliye, iade yonetimi
- E-Fatura (UBL-TR 2.1)
- Fiyat listesi, satinalma, CRM firsat takibi

### Stok
- Stok karti, kritik seviye uyarisi
- Depo ve sube yonetimi
- Seri/lot takibi, sayim

### Personel
- Personel kayitlari, izin takibi
- Puantaj, maas bordro, vardiya

### Muhasebe
- Hesap plani (otomatik olusur)
- Yevmiye fisleri, mizan, defter-i kebir

### Ekstralar
- Dashboard (istatistikler, grafikler, hizli aksiyonlar)
- Raporlar (KDV, BA/BS, cari ekstre, yaslandirma)
- Denetim loglari ve anomali tespiti
- Not defteri, belge yukleme
- Coklu sirket destegi, rol ve yetki yonetimi
- Iki faktorlu dogrulama (TOTP)
- PWA destegi (masaustune kur, cevrimdisi calis)
- Yerlesik araclar: hesap makinesi, doviz cevirici, KDV/taksit/kar marji hesaplayici

---

## Hizli Baslangic

```bash
# Altyapi (PostgreSQL + Redis + RabbitMQ + Adminer)
docker-compose -f docker-compose.dev.yml up -d

# Backend (http://localhost:8081)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:5173)
cd frontend && npm run dev
```

Ilk giris: `admin` / `admin123`

---

## Production

```bash
# .env dosyasini olusturup tum degiskenleri doldurun
cp .env.example .env

# Tum servisleri baslat (Traefik SSL, Prometheus, Grafana dahil)
docker-compose up -d
```

Production oncesi `docs/GO-LIVE.md` kontrol listesini gozden gecirin.

---

## Proje Yapisi

```
raspel-erp/
├── backend/                  # Spring Boot API
│   ├── controller/           # 7 alt paket (finans, ticaret, envanter, ik, muhasebe, sistem, sube)
│   ├── service/              # Is mantigi
│   ├── repository/           # JPA + Redis cache
│   ├── entity/               # JPA entity'ler
│   └── dto/                  # Veri transfer objeleri
├── frontend/              # Vue 3 SPA
│   ├── views/             # 52 sayfa
│   ├── components/        # 25+ paylasimli bilesen
│   ├── composables/        # 12 composable
│   ├── stores/            # 11 Pinia store
│   └── api/               # Axios client + 7 domain modul
├── config/               # Traefik, Prometheus, Grafana, Alertmanager
├── scripts/              # Yedekleme, geri yukleme, test
└── docs/                 # API, kullanim, kurulum
```

---

## Scripts

| Script | Amac |
|--------|------|
| `backup.ps1` | Veritabani yedekleme |
| `restore.ps1` | Yedekten geri yukleme |
| `schedule-backup.ps1` | Windows zamanlanmis gorev |
| `load-test.mjs` | Yuk testi |
| `e2e-workflow-test.mjs` | Uctan uca is akisi testi |

---

## Lisans

MIT — (c) 2026 Rasim Tuzluoglu
