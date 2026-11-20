/**
 * Grafik ve KPI renk paleti (tek otorite).
 *
 * Chart.js ve KpiKart renk değerleri CSS değişkeni okuyamaz (dataset'e somut
 * renk gerekir). Bu yüzden token'ların hex karşılıkları burada tutulur; her
 * değer `app.css` semantik token'ı ile birebir aynıdır (test bunu doğrular).
 * Yeni bir grafik rengi gerekiyorsa önce app.css token'ı, sonra buradaki kayıt
 * eklenir.
 */

export const GRAFIK = {
  mavi: '#3b82f6',
  yesil: '#10b981',
  amber: '#f59e0b',
  amberAcik: '#fbbf24',
  kirmizi: '#ef4444',
  mor: '#8b5cf6',
  teal: '#14b8a6',
  gri: '#64748b',
  camgobegi: '#06b6d4',
  lime: '#a3e635'
}

/** Çok serili grafiklerde sabit sıra (renk körlüğü için ayrışan tonlar). */
export const SERI = [
  GRAFIK.mavi,
  GRAFIK.yesil,
  GRAFIK.amber,
  GRAFIK.mor,
  GRAFIK.kirmizi,
  GRAFIK.camgobegi,
  GRAFIK.lime
]

/** Durum → renk eşlemesi (KPI/rozet/grafik ortak). */
export const DURUM_RENK = {
  basarili: GRAFIK.yesil,
  uyari: GRAFIK.amber,
  hata: GRAFIK.kirmizi,
  bilgi: GRAFIK.mavi,
  notr: GRAFIK.gri
}
