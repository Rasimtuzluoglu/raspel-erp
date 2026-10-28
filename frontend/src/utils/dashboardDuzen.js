/**
 * Anasayfa widget duzeni (kisisellestirme) yardimcilari.
 *
 * Duzen sozlesmesi: `{ versiyon: 1, sira: [{ key, gorunur: boolean }] }`
 * - Sira duz bir listedir; gorsel yerlesim (ana icerik / sag ray) anahtardan
 *   turetilir (`widgetBolgesi`). Boylece ayni kayit hem tek kolonlu mobilde hem
 *   iki bolgeli masaustunde ayni anlami tasir.
 * - Siralama yalnizca ayni bolge icinde yapilir; bolgeler arasi tasima yoktur.
 * - `kpi` sabittir: gizlenebilir ama tasinamaz (her zaman panonun ust bandi).
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
  'nakitAkisi',
  'aylikGelirGider',
  'kategoriSatislari',
  'enCokSatanlar',
  'akis',
  'kritikStok',
  'hizliIslemler'
]

/** Sag rayda yasayan bolumler; digerleri ana iceriktedir. */
export const RAY_ANAHTARLARI = ['icgoruler', 'hizliIslemler', 'kritikStok']

/** Konumu degismeyen bolumler (gizlenebilir, tasinamaz). */
export const SABIT_ANAHTARLARI = ['kpi']

/** Widget etiketleri icin i18n anahtarlari (dashboard blokunda). */
export const WIDGET_ETIKET_ANAHTARLARI = {
  kpi: 'dashboard.widgetKpi',
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

/** Widget'in gorsel bolgesi: 'ana' (sol, esnek) veya 'ray' (sag, sabit). */
export function widgetBolgesi(key) {
  return RAY_ANAHTARLARI.includes(key) ? 'ray' : 'ana'
}

/**
 * Rol bazli varsayilan sira. ADMIN tum bolumleri, MUHASEBE finans odakli,
 * USER ise operasyonel bolumleri gorur.
 */
export const ROL_SIRALARI = {
  ADMIN: [
    'kpi',
    'gorevler',
    'nakitAkisi',
    'aylikGelirGider',
    'kategoriSatislari',
    'enCokSatanlar',
    'akis',
    'icgoruler',
    'hizliIslemler',
    'kritikStok'
  ],
  MUHASEBE: [
    'kpi',
    'gorevler',
    'nakitAkisi',
    'aylikGelirGider',
    'kategoriSatislari',
    'akis',
    'icgoruler',
    'kritikStok',
    'enCokSatanlar',
    'hizliIslemler'
  ],
  USER: ['kpi', 'gorevler', 'akis', 'hizliIslemler', 'kritikStok', 'enCokSatanlar', 'icgoruler', 'nakitAkisi']
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
 * - `kpi` her zaman basa alinir (sabit ust band).
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

/** Sabit bolumleri listenin basina tasir (kpi ust band). */
function sabitBasa(sira) {
  const sabit = sira.filter((w) => SABIT_ANAHTARLARI.includes(w.key))
  const digerleri = sira.filter((w) => !SABIT_ANAHTARLARI.includes(w.key))
  return [...sabit, ...digerleri]
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
 * Widget'i ayni bolge icinde bir adim yukari/asagi tasir.
 * Bolge disina cikilmaz; sabit bolumler tasinmaz.
 */
export function widgetTasi(sira, key, yon) {
  if (SABIT_ANAHTARLARI.includes(key)) return sira
  const mevcut = sira.findIndex((w) => w.key === key)
  if (mevcut === -1) return sira
  const bolge = widgetBolgesi(key)
  const adim = yon < 0 ? -1 : 1
  let hedef = mevcut + adim
  while (hedef >= 0 && hedef < sira.length) {
    const aday = sira[hedef]
    // Sabit bolumler (kpi) siralama hedefi olamaz.
    if (aday && !SABIT_ANAHTARLARI.includes(aday.key) && widgetBolgesi(aday.key) === bolge) break
    hedef += adim
  }
  if (hedef < 0 || hedef >= sira.length) return sira
  return tasiIndex(sira, mevcut, hedef)
}

/**
 * Surukle-birak: `kaynakKey` widget'ini `hedefKey` widget'inin konumuna tasir.
 * Yalnizca ayni bolge icinde ve sabit olmayan bolumler icin calisir.
 */
export function widgetBirak(sira, kaynakKey, hedefKey) {
  if (!kaynakKey || !hedefKey || kaynakKey === hedefKey) return sira
  if (SABIT_ANAHTARLARI.includes(kaynakKey) || SABIT_ANAHTARLARI.includes(hedefKey)) return sira
  if (widgetBolgesi(kaynakKey) !== widgetBolgesi(hedefKey)) return sira
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
