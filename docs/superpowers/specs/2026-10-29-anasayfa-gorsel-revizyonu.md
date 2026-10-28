# Anasayfa Görsel Revizyonu — Tasarım Sözleşmesi (v2)

**Tarih:** 2026-10-29
**Stil:** A — Sessiz Kurumsal
**Durum:** Onay bekliyor (kod YAZILMADI)

## 0. Çalışma şekli (bağlayıcı)
- Her aşamada **1280px ve 390px ekran görüntüsü** üretilir ve Review paneline gösterilir.
- Kullanıcı **onay vermeden** bir sonraki aşamaya geçilmez.
- Kullanıcı isterse kendi ekran görüntüsünü paylaşır ve birebir hedeflenir.

## 1. Tasarım ilkeleri
- Sakin, okunabilir, kurumsal. "Admin paneli" değil "işletme yönetim ürünü".
- Tek kart dili, tek boşluk ölçeği, tek tipografi ölçeği.
- Renk yalnız anlam taşır; her renk metin/ikon ile desteklenir.
- Yapay trend/yüzde/sahte veri yok.

## 2. Tasarım tokenları (dashboard kapsamlı; mevcut tema değişkenleri üzerinden)
| Token | Açık | Koyu |
|---|---|---|
| Sayfa zemini | #f8fafc | #0b1220 |
| Kart zemini | #ffffff | #111a2c |
| Kart sınırı | #e5e7eb | #1e293b |
| Metin | #0f172a | #e2e8f0 |
| İkincil metin | #64748b | #94a3b8 |
| Vurgu (marka) | #2563eb | #3b82f6 |
| Başarı / Uyarı / Hata | #16a34a / #d97706 / #dc2626 | #22c55e / #f59e0b / #f87171 |

- Köşe yarıçapı: kart 12px, çip 8px.
- Gölge: açıkta yok; koyuda çok hafif (1px sınır yeterli).
- Boşluk: sayfa 24px, kart 20px, kartlar arası 16px.

## 3. Tipografi ölçeği
- KPI değeri: 26px/700, tabular-nums.
- Bölüm başlığı: 15px/600.
- Gövde: 14px. İkincil/etiket: 13px. Alt bilgi: 12px.
- Finansal değerler tek biçim (`formatCurrency`); tarihler tek biçim.

## 4. Yerleşim (12 kolonlu ızgara)
1. **Üst bar** (~56px): sol "Günaydın, Ad" + alt "Şirket · Tarih · Son güncelleme · Canlı"; sağ "Ara (Ctrl K)" + yenile + tema/ayar.
2. **Piyasa şeridi** (ince, 4 kur: USD/EUR/GBP/BTC; kapatılabilir).
3. **KPI satırı (5 kart):** Likidite, Alacak, Borç, Bu Ay Ciro, Bu Ay Net Kâr.
4. **Ana ızgara:**
   - Görev merkezi (8) · İçgörüler (4)
   - Nakit akışı (8) · Hızlı işlemler (4)
   - 12 ay gelir/gider (8) · Kritik stok (4)
   - Kategori (4) · En çok satan (4) · Canlı akış (4)
5. Detay analizler (yaşlandırma, borçlu/alacaklı, kasa-banka, pareto) anasayfada yok → Raporlar/Kokpit.

**Kırılımlar:** ≥1280 → 5 KPI; <1280 → 3+2; <1024 → kartlar 12/6; <760 → tek kolon; <640 → KPI tek kolon. Yatay taşma olmayacak (ölçülür).

## 5. Bileşen davranışları
- **DashboardKart:** ortak başlık (ikon çipi 26px + başlık + sağ bağlantı), gövde, alt slot.
- **Görev merkezi:** 6 satır; ikon + başlık + açıklama (hover'da tam metin) + adet + tutar + aksiyon. Boş kolon yok.
- **Nakit akışı:** 7/30 geçiş; veri varsa 200px grafik, yoksa 120px boş durum + "Kasa'ya git"; tahmini projeksiyon "Tahmini" etiketli.
- **Grafikler (Chart.js):** mat grid, tek vurgu rengi, degrade yok, tooltip para formatı, eksen ≤8 etiket. Boş veri sahte veri değil.
- **Hızlı işlemler:** 3×3, 44px hedef, yalnız mevcut rotalar.
- **İçgörü / Kritik stok / Canlı akış:** kompakt, en fazla 3/4/5 satır.

## 6. Durumlar
- **Loading:** bölüm bazlı skeleton (layout shift yok).
- **Error:** bölüm bazlı "yüklenemedi" + "Yeniden dene"; tüm sayfa kilitlenmez.
- **Empty:** açıklama + yönlendirme + ilgili eylem; sahte veri yok.

## 7. Finansal kurallar (değişmez)
- Hesaplamalar backend'deki gibidir; gösterim dışında değişiklik yapılmaz.
- Ciro ≠ kâr, alacak ≠ nakit, kasa/banka ≠ tahsilat toplamı karıştırılmaz.
- Projeksiyon açıkça "Tahmini" etiketli.

## 8. Kapsam dışı
- Backend/migration, yeni bağımlılık, tema motoru değişimi, kişiselleştirme mantığı değişimi (sırala+gizle korunur).

## 9. Uygulama aşamaları (her biri onaylı)
1. Üst bar + piyasa şeridi + KPI satırı.
2. Görev merkezi + içgörüler + hızlı işlemler.
3. Nakit akışı + 12 ay grafik + kategori/ürün + kritik stok + canlı akış.
4. Koyu tema + mobil (390) + boş/hata durumları.
5. Tam testler (lint/i18n/vitest/build) + Docker + push.
