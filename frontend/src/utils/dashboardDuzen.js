/**
 * Anasayfa widget duzeni (kisisellestirme) yardimcilari.
 *
 * Duzen sozlesmesi: `{ versiyon: 1, sira: [{ key, boyut: 1|2, gorunur: boolean }] }`
 * - `boyut: 1` yarim genislik, `boyut: 2` tam genislik.
 * - Sira, kullanicinin gordugu siradir; `gorunur: false` olanlar duzen panelinde
 *   listelenir ama sayfada cizilmez.
 *
 * Bu modul saf fonksiyonlardir; view ve testler ayni kaynagi kullanir.
 */

export const DUZEN_VERSIYON = 1
export const DUZEN_STORAGE_ANAHTAR = 'raspel_dashboard_duzen'

/** Widget anahtarlari (registry). */
export const DASHBOARD_WIDGET_ANAHTARLARI = [
  'kpi',
  'gorevler',
  'icgoruler',
  'hedefler',
  'nakitAkisi',
  'aylikGelirGider',
  'kategoriSatislari',
  'enCokSatanlar',
  'akis',
  'vade',
  'kritikStok',
  'hizliIslemler'
]

/** Widget etiketleri icin i18n anahtarlari (dashboard blokunda). */
export const WIDGET_ETIKET_ANAHTARLARI = {
  kpi: 'dashboard.widgetKpi',
  gorevler: 'dashboard.widgetGorevler',
  icgoruler: 'dashboard.widgetIcgoruler',
  hedefler: 'dashboard.widgetHedefler',
  nakitAkisi: 'dashboard.widgetNakitAkisi',
  aylikGelirGider: 'dashboard.widgetAylikGelirGider',
  kategoriSatislari: 'dashboard.widgetKategoriSatislari',
  enCokSatanlar: 'dashboard.widgetEnCokSatanlar',
  akis: 'dashboard.widgetAkis',
  vade: 'dashboard.widgetVade',
  kritikStok: 'dashboard.widgetKritikStok',
  hizliIslemler: 'dashboard.widgetHizliIslemler'
}

/** Rol varsayilan boyutlari (1 = yarim, 2 = tam genislik). */
export const VARSAYILAN_BOYUTLAR = {
  kpi: 2,
  gorevler: 1,
  icgoruler: 1,
  hedefler: 2,
  nakitAkisi: 1,
  aylikGelirGider: 1,
  kategoriSatislari: 1,
  enCokSatanlar: 1,
  akis: 1,
  vade: 1,
  kritikStok: 1,
  hizliIslemler: 2
}

/**
 * Rol bazli varsayilan sira. ADMIN tum bolumleri, MUHASEBE finans odakli,
 * USER ise operasyonel bolumleri gorur (hedefler/vade gizli).
 */
export const ROL_SIRALARI = {
  ADMIN: [
    'kpi',
    'gorevler',
    'icgoruler',
    'hedefler',
    'nakitAkisi',
    'aylikGelirGider',
    'akis',
    'kategoriSatislari',
    'enCokSatanlar',
    'vade',
    'kritikStok',
    'hizliIslemler'
  ],
  MUHASEBE: [
    'kpi',
    'gorevler',
    'hedefler',
    'nakitAkisi',
    'aylikGelirGider',
    'vade',
    'akis',
    'kategoriSatislari',
    'icgoruler',
    'kritikStok',
    'hizliIslemler',
    'enCokSatanlar'
  ],
  USER: ['kpi', 'gorevler', 'akis', 'hizliIslemler', 'kritikStok', 'enCokSatanlar', 'icgoruler', 'nakitAkisi']
}

export const VARSAYILAN_ROL = 'ADMIN'

/** Rolun sira listesi; bilinmeyen rolde ADMIN'e duser. */
export function rolSirasi(rol) {
  return ROL_SIRALARI[rol] || ROL_SIRALARI[VARSAYILAN_ROL]
}

/** Rol icin tam duzen nesnesi uretir (tum gorunur, varsayilan boyutlarla). */
export function rolDuzeni(rol) {
  return {
    versiyon: DUZEN_VERSIYON,
    sira: rolSirasi(rol).map((key) => ({
      key,
      boyut: VARSAYILAN_BOYUTLAR[key] || 1,
      gorunur: true
    }))
  }
}

/** Gelen duzen sozlesmeye uygun mu? (en az bir bilinen widget icermeli) */
export function duzenGecerliMi(duzen) {
  if (!duzen || typeof duzen !== 'object' || !Array.isArray(duzen.sira)) return false
  return duzen.sira.some((w) => w && DASHBOARD_WIDGET_ANAHTARLARI.includes(w.key))
}

/**
 * Duzeni guvenli hale getirir: bilinmeyen/cift kayitlari atar, boyutu
 * 1|2'ye sabitler, eksik widget'lari (rol varsayilanindan) gizli olarak ekler.
 * Boylece yeni bir widget eklendiginde eski tercihler bozulmaz.
 */
export function duzenNormallestir(duzen, rol = VARSAYILAN_ROL) {
  if (!duzenGecerliMi(duzen)) return rolDuzeni(rol)
  const sira = []
  const gorulen = new Set()
  const kaynak = duzen.sira
  for (const w of kaynak) {
    if (!w || !DASHBOARD_WIDGET_ANAHTARLARI.includes(w.key) || gorulen.has(w.key)) continue
    gorulen.add(w.key)
    sira.push({
      key: w.key,
      boyut: Number(w.boyut) === 2 ? 2 : 1,
      gorunur: w.gorunur !== false
    })
  }
  for (const w of rolDuzeni(rol).sira) {
    if (gorulen.has(w.key)) continue
    sira.push({ ...w, gorunur: false })
  }
  return { versiyon: DUZEN_VERSIYON, sira }
}

/** Tek bir widget'in gorunurlugunu degistirir (yeni dizi doner). */
export function widgetGorunurlukDegistir(sira, key, gorunur) {
  return sira.map((w) => (w.key === key ? { ...w, gorunur: Boolean(gorunur) } : w))
}

/** Tek bir widget'in boyutunu 1 <-> 2 arasinda degistirir. */
export function widgetBoyutDegistir(sira, key) {
  return sira.map((w) => (w.key === key ? { ...w, boyut: w.boyut === 2 ? 1 : 2 } : w))
}

/** Widget'i bir adim yukari/asagi tasir (klavye ve panel butonlari icin). */
export function widgetTasi(sira, key, yon) {
  const mevcut = sira.findIndex((w) => w.key === key)
  if (mevcut === -1) return sira
  const hedef = mevcut + (yon < 0 ? -1 : 1)
  if (hedef < 0 || hedef >= sira.length) return sira
  const kopya = [...sira]
  const [tasinan] = kopya.splice(mevcut, 1)
  kopya.splice(hedef, 0, tasinan)
  return kopya
}

/** Sürükle-birak: `kaynakKey` widget'ini `hedefKey` widget'inin konumuna tasir. */
export function widgetBirak(sira, kaynakKey, hedefKey) {
  if (!kaynakKey || !hedefKey || kaynakKey === hedefKey) return sira
  const kaynak = sira.findIndex((w) => w.key === kaynakKey)
  const hedef = sira.findIndex((w) => w.key === hedefKey)
  if (kaynak === -1 || hedef === -1) return sira
  const kopya = [...sira]
  const [tasinan] = kopya.splice(kaynak, 1)
  kopya.splice(hedef, 0, tasinan)
  return kopya
}

// --- localStorage fallback -------------------------------------------------
// Backend kaydi basarisiz oldugunda duzen cihazda tutulur; bir sonraki acilista
// backend bos donerse bu kayit kullanilir.

export function yerelDuzenOku() {
  try {
    const ham = localStorage.getItem(DUZEN_STORAGE_ANAHTAR)
    if (!ham) return null
    const veri = JSON.parse(ham)
    const duzen = veri && veri.duzen ? veri.duzen : veri
    if (!duzenGecerliMi(duzen)) return null
    return { duzen, kaydedilemedi: Boolean(veri && veri.kaydedilemedi) }
  } catch {
    return null
  }
}

export function yerelDuzenYaz(duzen, kaydedilemedi = false) {
  try {
    localStorage.setItem(DUZEN_STORAGE_ANAHTAR, JSON.stringify({ duzen, kaydedilemedi }))
  } catch {
    /* depo dolu/kapali: kisisellestirme yalnizca oturumluk kalir */
  }
}
