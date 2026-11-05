# Anasayfa — Kâr Analizi (Faz 3)

**Tarih:** 2026-11-05
**Kapsam:** Seçili dönem için ürün ve cari bazlı kâr/marj analizi.
**Neden:** Şube filtresi mevcut şemada mümkün değil (operasyonel tablolarda `sube_id` yok); `fatura_kalem` maliyet alanları gerçek kâr analizini destekliyor.

## Backend
`GET /api/dashboard/kar-analizi?donem=BU_AY|GECEN_AY|BU_CEYREK|BU_YIL`
```
{
  donem: {kod, baslangic, bitis, etiket},
  ozet: {ciro, maliyet, kar, marj, maliyetsizKalem},
  urunler: [{stokId, ad, ciro, maliyet, kar, marj}],   // en kârlı 10
  cariler: [{cariId, ad, ciro, maliyet, kar, marj}]    // en kârlı 10
}
```
- Yalnız `SATIS + KESILDI` faturalar; `k.tutar` ciro, maliyet = `k.maliyetTutar` (yoksa `birimMaliyet*adet`).
- Maliyeti olmayan kalemler `maliyetsizKalem` sayacında; kâr hesabına 0 maliyetle girer ve UI’da uyarı gösterilir.
- Dönem aralığı `donemAraligiCoz` ile aynı; tenant zorunlu; cache anahtarında dönem.
- TVF/yeni tablo yok.

## Frontend
- Üst barda “Kâr Analizi” ikonu → drawer (`DashboardKarAnalizi.vue`).
- Özet: Ciro, Maliyet, Kâr, Marj + maliyetsiz kalem uyarısı.
- En kârlı 10 ürün ve 10 cari: yatay bar + tablo (ciro/maliyet/kâr/marj).
- Seçili dönemi kullanır; drawer açıkken dönem değişirse yenilenir.
- Boş/hata/yükleniyor; mobil 92vw; a11y.
