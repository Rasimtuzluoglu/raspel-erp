# Anasayfa — Finans Masası Revizyonu (v3)

**Tarih:** 2026-11-03
**Durum:** Uygulanıyor
**Hedef:** Yoğun, grafik ağırlıklı, kısayollu "finans masası" anasayfa.

## 1. Düzen
```
Üst bar (selamlama, şirket/tarih, Ara Ctrl+K, yenile, düzenle)
Piyasa şeridi (USD/EUR/GBP/BTC, ince)
Kısayol şeridi (8 işlem, tek satır, yatay kaydırma)
KPI: 9 kart (5 + 4)
Gövde: 12 kolon
  Sol (8): grafik duvarı (2 kolon alt ızgara)
    Nakit akışı (2 kolon)  ·  Ciro hedef (1) + 12 ay gelir/gider (1)
    Yaşlandırma (1) + Kasa/Banka (1)
    Kategori (1) + En çok satan (1)
    Borçlu (1) + Alacaklı (1)
    Sipariş durumları (1) + Bütçe/gerçekleşen (1)
    Stok değeri (2)
  Sağ (4): tek kolon
    Bugün yapılacaklar · Hızlı işlemler · Kritik stok · Canlı akış · İçgörüler
```
- Kart padding 14px, grid gap 12px, grafik yüksekliği 160px (donut 150px), KPI yüksekliği ~96px.
- Sol/sağ bölge içinde sırala + gizle; bölgeler arası taşıma yok.
- 1200 altı: sağ bölge alta iner, sol tek kolon; 760 altı: her şey tek kolon.

## 2. KPI (9)
Likidite · Alacak · Borç · Bu Ay Ciro · Bu Ay Net Kâr · Bugün Tahsilat · Bugün Ödeme · Bekleyen Sipariş · Stok Değeri.
- Bekleyen Sipariş: `gorevler` içindeki `ONAY_SIPARIS` adedi; yüklenene kadar 0.
- Yapay trend yok; eksik veride "—".

## 3. Kısayollar
- **Üst şerit (8):** Yeni Fatura, Tahsilat, Ödeme, Hızlı Satış, Yeni Cari, Kasa, Stoklar, Teklifler.
- **Sağ ızgara (6):** Satınalma, Onaylar, Raporlar, Yönetici Kokpiti, Ajanda, Yedekler.
- Yalnız mevcut rotalar; ADMIN olmayan kokpiti görmez.

## 4. Grafikler (11)
1. Nakit akışı (7/30) — mevcut.
2. Ciro hedef gerçekleşme — mevcut `hedef`.
3. 12 ay gelir/gider + net — mevcut.
4. Alacak yaşlandırma — mevcut.
5. Kasa/Banka donut — mevcut.
6. Kategori donut — mevcut.
7. En çok satan ürünler — mevcut.
8. En borçlu cariler — mevcut.
9. En alacaklı cariler — mevcut.
10. **Sipariş durumları** — yeni veri.
11. **Bütçe / gerçekleşen** — yeni veri.
12. **Stok değeri** (kategori dağılımı + toplam) — yeni veri.

Boş veri: sahte veri yok, 120px kompakt boş durum + ilgili ekrana bağlantı.

## 5. Backend sözleşmesi (`GET /api/dashboard/grafikler` genişler)
```
siparisDurumlari: [{durum, adet}]
butceGerceklesen: [{kategori, butce, gerceklesen}]     // bu ay, GIDER, en çok 6
stokDegeri: {
  toplamDeger, kritikAdet,
  kategoriler: [{kategori, deger, adet}]               // en çok 6
}
```
- Mevcut alanlar korunur (geriye uyum).
- Sipariş: `GROUP BY durum`.
- Bütçe: bu yıl/ay GIDER bütçeleri; gerçekleşen = bu ay masraflar; kategori eşleşmesi.
- Stok: `SUM(miktar*fiyat)` kategori bazında.
- Migration gerekmez (şema değişmiyor).

## 6. Finansal kurallar
Mevcut hesaplamalar korunur; bütçe/gerçekleşen yalnızca mevcut bütçe+masraf kayıtlarından türetilir. Uydurma veri yok.

## 7. Doğrulama
- Backend: `mvn -B test` + `clean verify`.
- Frontend: lint + i18n + Vitest + build.
- 1280/390 ölçüm: yatay taşma yok, 9 KPI, 11 grafik, kısayollar.
- Docker build/deploy + push.
