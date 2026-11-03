# Anasayfa — Ek Özellikler (Faz 1)

**Tarih:** 2026-11-03
**Kapsam:** Dönem seçici + KPI karşılaştırma, hızlı onay, vade takvimi, nakit projeksiyon grafiği, hedef kısayolu, bildirim zili, otomatik yenileme.
**Kural:** Kart ızgarası değişmez; yeni alanlar drawer/dropdown olarak açılır.

## Backend

### 1. `GET /api/dashboard/ozet?donem=BU_AY|GECEN_AY|BU_CEYREK|BU_YIL`
- Varsayılan `BU_AY` (geriye uyum).
- `ciro` ve `kar` seçilen dönem için hesaplanır; `onceki` = bir önceki eşdeğer dönem; `degisimYuzde` = yüzde değişim (önceki 0 ise null).
- `hedef` yalnız `BU_AY` için dolu; diğer dönemlerde null (UI “hedef yok” gösterir).
- Likidite/net cari/stok anlık değerlerdir; dönemden etkilenmez.
- DTO’ya `donem{kod,baslangic,bitis,etiket}` eklenir.
- Cache anahtarına dönem eklenir.

### 2. `GET /api/dashboard/vadeler?baslangic=YYYY-MM-DD&bitis=YYYY-MM-DD`
- Vadesi aralıkta olan, KESİLDİ ve ödeme durumu ODENDI/IPTAL olmayan faturalar.
- `{faturaId, faturaNumarasi, cariAd, vadeTarihi, kalanTutar, geciktiMi}`.
- Aralık en fazla 93 gün; tenant zorunlu; en fazla 200 kayıt.

## Frontend

### Üst bar
- Dönem seçici (Bu Ay / Geçen Ay / Bu Çeyrek / Bu Yıl); seçim localStorage’da; `ozet` yeniden yüklenir.
- Bildirim zili: `gorevSayilari` (onay, vadesi geçen, kritik stok) rozetli dropdown; ilgili ekrana link.
- Otomatik yenileme anahtarı (60 sn, yalnız sekme görünürken; localStorage).

### KPI
- Ciro ve kâr kartlarında `degisimYuzde` varsa “önceki döneme göre” rozeti.
- Hedef yoksa “Hedef belirle” → `/yonetici-kokpiti`.

### Hızlı onay
- Görev satırındaki ONAY_* kayıtlarında “Hızlı onay” aksiyonu (ADMIN/MUHASEBE).
- Drawer: `/onaylar/bekleyen?tur=...` listesi; satır bazında Onayla / Reddet (gerekçe modalı).
- `POST /onaylar/karar`; sonra görevler + özet yenilenir, toast.

### Vade takvimi
- Üst barda takvim ikonu → drawer; aylık grid; günlerde tutar/adet; güne tıklayınca liste.
- `GET /dashboard/vadeler`; önceki/sonraki ay.

### Nakit projeksiyon grafiği
- Nakit Akışı kartında 30/60/90 gün tahmini mini çizgi grafik; “Tahmini” etiketi korunur.

## Doğrulama
Backend testleri + `clean verify`; frontend lint/i18n/vitest/build; Docker; 1280/390 taşma yok.
