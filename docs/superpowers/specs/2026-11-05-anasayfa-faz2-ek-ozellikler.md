# Anasayfa — Ek Özellikler (Faz 2)

**Tarih:** 2026-11-05
**Kapsam:** Kişisel not/todo, kısayol özelleştirme, Excel ve PDF/yazdırma.

## 1. Kişisel not / todo
- Üst barda not ikonu + açık not sayısı rozeti → drawer.
- `localStorage` anahtarı kullanıcı bazlı (`raspel_dashboard_notlar_<kullaniciId>`).
- Öğe: `{id, metin, tamam, tarih}`; ekle, tamamla, sil, tamamlananları temizle.
- En fazla 50; boş/uzun metin doğrulaması; boş durum; klavye/aria; mobil 92vw.

## 2. Kısayol özelleştirme
- Üst kısayol şeridinde “Düzenle” → modal.
- Kullanılabilir tüm kısayollar (mevcut 8 + satınalma, onaylar, raporlar, kokpit, ajanda, yedekler).
- En fazla 8 seçim; sıra katalog sırasıdır. `localStorage` `raspel_dashboard_kisayollar`.
- Seçim yoksa/bozuksa varsayılan 8’e düşer. ADMIN olmayan kokpit/yedekler seçemez.

## 3. Excel dışa aktarma
- `GET /api/exports/dashboard?donem=BU_AY|GECEN_AY|BU_CEYREK|BU_YIL` → `.xlsx`.
- Sayfa 1 “Ozet”: dönem, ciro, net kâr, likidite, alacak, borç, stok değeri, kritik stok, bekleyen onay, vadesi geçen.
- Sayfa 2 “Grafikler”: 12 ay gelir/gider/net, 30 gün nakit, kategori satışları, en çok satanlar, yaşlandırma, borçlu/alacaklı, sipariş durumları, bütçe/gerçekleşen, stok değeri.
- Yetki: mevcut export guard’ı (ADMIN/USER/MUHASEBE); tenant zorunlu.

## 4. PDF / yazdırma
- Üst barda yazdır ikonu → `window.print()`.
- `@media print`: kenar çubuğu/üst bar/butonlar gizlenir; kartlar tam genişlik; sayfa kırılması kart ortasında olmaz; beyaz zemin/siyah metin.
- Ayrı PDF kütüphanesi eklenmez.

## Doğrulama
Backend Excel testleri + `clean verify`; frontend lint/i18n/vitest/build; Docker; 1280/390.
