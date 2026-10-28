/**
 * Anasayfa widget duzeni (kisisellestirme) yardimcilari.
 *
 * Duzen sozlesmesi: `{ versiyon: 1, sira: [{ key, gorunur: boolean }] }`
 * - Sira duz bir listedir; tum kartlar tek bir 12 kolonlu izgarada, bu sirayla
 *   yerlestirilir. Kart genisligi (span) anahtara gore sabittir
 *   (`WIDGET_SPANLARI`); kullanici yalnizca siralamayi ve gorunurlugu degistirir.
 * - `kpi` ve `bugunSerit` sabittir: gizlenebilir ama tasinamaz. Bu iki bolum
 *   izgaranin ustunde ayri bantlar olarak cizilir.
 * - `boyut` alani yoktur; eski kayitlardaki `boyut`, `hedefler` ve `vade`
 *   alanlari normallestirme sirasinda temizlenir.
 *
 * Bu modul saf fonksiyonlardir; view ve testler ayni kaynagi kullanir.
 */

export const DUZEN_VERSIYON = 1
export const DUZEN_STORAGE_ANAHTAR = 'raspel_dashboard_duzen'

/** Widget anahtarlari (registry) — cizim sirasinin varsayilani. */
export const DASHBOARD_WIDGET_ANAHTARLARI = [
  'kpi',
  'bugunSerit',
  'gorevler',
  'icgoruler',
  'nakitAkisi',
  'hizliIslemler',
  'aylikGelirGider',
  'kritikStok',
  'kategoriSatislari',
  'enCokSatanlar',
  'akis'
]

/** Izgaranin ustunde ayri bant olarak cizilen sabit bolumler. */
export const SABIT_ANAHTARLARI = ['kpi', 'bugunSerit']

/**
 * Kart genislikleri (12 kolonlu izgara).
 * Masaustu yerlesimi: 8+4, 8+4, 8+4, 4+4+4.
 */
export const WIDGET_SPANLARI = {
  gorevler: 8,
  icgoruler: 4,
  nakitAkisi: 8,
  hizliIslemler: 4,
  aylikGelirGider: 8,
  kritikStok: 4,
  kategoriSatislari: 4,
  enCokSatanlar: 4,
  akis: 4
}

/** Widget etiketleri icin i18n anahtarlari (dashboard blokunda). */
export const WIDGET_ETIKET_ANAHTARLARI = {
  kpi: 'dashboard.widgetKpi',
  bugunSerit: 'dashboard.bugunSeridi',
  gorevler: 'dashboard.widgetGorevler',
  icgoruler: 'dashboard.widgetIcgoruler',
  nakitAkisi: 'dashboard.widgetNakitAkisi',
  aylikGelirGider: 'dashboard.widgetAylikGelirGider',
  kategoriSatislari: 'dashboard.widgetKategoriSatislari',
  enCokSatanlar: 'dashboard.widgetEnCokSatanlar',
  akis: 'dashboard.widgetAkis',
  kritikStok: 'dashboard.widgetKritikStok',
  hizliIslemler: 'dashboard.widgetHizliIslemler'
}

/** Widget'in 12 kolonlu izgaradaki sabit genisligi. */
export function widgetSpan(key) {
  return WIDGET_SPANLARI[key] || 4
}

/** Sabit mi? (gizlenebilir, tasinamaz) */
export function sabitMi(key) {
  return SABIT_ANAHTARLARI.includes(key)
}

/**
 * Rol bazli varsayilan sira.
 *
 * Yerlesim spec'te sabit oldugu icin tum roller ayni sirayi kullanir; rol
 * farki yalnizca gorunurluk tercihinde (kullanici bazli duzen) ortaya cikar.
 */
const VARSAYILAN_SIRA = [
  'kpi',
  'bugunSerit',
  'gorevler',
  'icgoruler',
  'nakitAkisi',
  'hizliIslemler',
  'aylikGelirGider',
  'kritikStok',
  'kategoriSatislari',
  'enCokSatanlar',
  'akis'
]

export const ROL_SIRALARI = {
  ADMIN: [...VARSAYILAN_SIRA],
  MUHASEBE: [...VARSAYILAN_SIRA],
  USER: [...VARSAYILAN_SIRA]
}

export const VARSAYILAN_ROL = 'ADMIN'

/** Rolun sira listesi; bilinmeyen rolde ADMIN'e duser. */
export function rolSirasi(rol) {
  return ROL_SIRALARI[rol] || ROL_SIRALARI[VARSAYILAN_ROL]
}

/** Rol icin tam duzen nesnesi uretir (tum bolumler gorunur). */
export function rolDuzeni(rol) {
  return {
    versiyon: DUZEN_VERSIYON,
    sira: rolSirasi(rol).map((key) => ({ key, gorunur: true }))
  }
}

/** Gelen duzen sozlesmeye uygun mu? (en az bir bilinen widget icermeli) */
export function duzenGecerliMi(duzen) {
  if (!duzen || typeof duzen !== 'object' || !Array.isArray(duzen.sira)) return false
  return duzen.sira.some((w) => w && DASHBOARD_WIDGET_ANAHTARLARI.includes(w.key))
}

/**
 * Duzeni guvenli hale getirir.
 *
 * - Bilinmeyen/cift kayitlar ve eski `boyut` alani atilir (`hedefler`/`vade`
 *   gibi kaldirilan anahtarlar bu sayede sessizce temizlenir).
 * - `kpi` ve `bugunSerit` her zaman basa alinir (sabit ust bantlar).
 * - Eksik bolumler rol varsayilanindan gizli olarak eklenir; boylece yeni bir
 *   bolum eklendiginde eski tercihler bozulmaz.
 */
export function duzenNormallestir(duzen, rol = VARSAYILAN_ROL) {
  if (!duzenGecerliMi(duzen)) {
    const varsayilan = rolDuzeni(rol)
    return { versiyon: DUZEN_VERSIYON, sira: sabitBasa(varsayilan.sira) }
  }
  const sira = []
  const gorulen = new Set()
  for (const w of duzen.sira) {
    if (!w || !DASHBOARD_WIDGET_ANAHTARLARI.includes(w.key) || gorulen.has(w.key)) continue
    gorulen.add(w.key)
    sira.push({ key: w.key, gorunur: w.gorunur !== false })
  }
  for (const w of rolDuzeni(rol).sira) {
    if (gorulen.has(w.key)) continue
    sira.push({ key: w.key, gorunur: false })
  }
  return { versiyon: DUZEN_VERSIYON, sira: sabitBasa(sira) }
}

/** Sabit bolumleri (kpi, bugunSerit) listenin basina tasir. */
function sabitBasa(sira) {
  const sabitler = SABIT_ANAHTARLARI.map((key) => sira.find((w) => w.key === key)).filter(Boolean)
  const digerleri = sira.filter((w) => !SABIT_ANAHTARLARI.includes(w.key))
  return [...sabitler, ...digerleri]
}

/** Tek bir widget'in gorunurlugunu degistirir (yeni dizi doner). */
export function widgetGorunurlukDegistir(sira, key, gorunur) {
  return sira.map((w) => (w.key === key ? { ...w, gorunur: Boolean(gorunur) } : w))
}

/** Iki konumu yer degistirir (yeni dizi doner). */
function tasiIndex(sira, kaynak, hedef) {
  const kopya = [...sira]
  const [tasinan] = kopya.splice(kaynak, 1)
  kopya.splice(hedef, 0, tasinan)
  return kopya
}

/**
 * Widget'i duz listede bir adim yukari/asagi tasir.
 * Sabit bolumlerin (kpi, bugunSerit) uzerinden atlanir; onlar tasinmaz.
 */
export function widgetTasi(sira, key, yon) {
  if (sabitMi(key)) return sira
  const mevcut = sira.findIndex((w) => w.key === key)
  if (mevcut === -1) return sira
  const adim = yon < 0 ? -1 : 1
  let hedef = mevcut + adim
  while (hedef >= 0 && hedef < sira.length) {
    const aday = sira[hedef]
    if (aday && !sabitMi(aday.key)) break
    hedef += adim
  }
  if (hedef < 0 || hedef >= sira.length) return sira
  return tasiIndex(sira, mevcut, hedef)
}

/**
 * Surukle-birak: `kaynakKey` widget'ini `hedefKey` widget'inin konumuna tasir.
 * Sabit bolumler ne tasinir ne de hedef olur.
 */
export function widgetBirak(sira, kaynakKey, hedefKey) {
  if (!kaynakKey || !hedefKey || kaynakKey === hedefKey) return sira
  if (sabitMi(kaynakKey) || sabitMi(hedefKey)) return sira
  const kaynak = sira.findIndex((w) => w.key === kaynakKey)
  const hedef = sira.findIndex((w) => w.key === hedefKey)
  if (kaynak === -1 || hedef === -1) return sira
  return tasiIndex(sira, kaynak, hedef)
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
