/**
 * Anasayfa widget duzeni (kisisellestirme) yardimcilari.
 *
 * Duzen sozlesmesi: `{ versiyon: 1, sira: [{ key, gorunur: boolean }] }`
 * - Iki bolge vardir: `SOL` (grafik duvari) ve `SAG` (aksiyon paneli).
 *   Kullanici yalnizca **bolge icinde** siralar; bolgeler arasi tasima yoktur.
 * - `kpi` ve `bugunSerit` sabittir: gizlenebilir ama tasinamaz. Bu iki bolum
 *   izgaranin/govdenin ustunde ayri bantlar olarak cizilir.
 * - Kart genisligi (span) anahtara gore sabittir; kullanici boyut degistiremez.
 *   `boyut` eski kayitlarda varsa normallestirme sirasinda temizlenir.
 *
 * Bu modul saf fonksiyonlardir; view ve testler ayni kaynagi kullanir.
 */

export const DUZEN_VERSIYON = 1
export const DUZEN_STORAGE_ANAHTAR = 'raspel_dashboard_duzen'

/** Bolge kimlikleri. */
export const BOLGE_SOL = 'SOL'
export const BOLGE_SAG = 'SAG'

/** Sol bolge (grafik duvari) varsayilan sirasi — spec'teki 2 kolonlu dizilim. */
export const SOL_WIDGET_ANAHTARLARI = [
  'nakitAkisi',
  'ciroHedef',
  'aylikGelirGider',
  'alacakYaslandirma',
  'kasaBanka',
  'kategoriSatislari',
  'enCokSatanlar',
  'enCokBorcCariler',
  'enCokAlacakCariler',
  'siparisDurumlari',
  'butceGerceklesen',
  'stokDegeri'
]

/** Sag bolge (aksiyon paneli) varsayilan sirasi. */
export const SAG_WIDGET_ANAHTARLARI = [
  'gorevler',
  'hizliIslemler',
  'kritikStok',
  'akis',
  'icgoruler'
]

/** Widget anahtarlari (registry) — sabit bantlar + iki bolge. */
export const DASHBOARD_WIDGET_ANAHTARLARI = [
  'kpi',
  'bugunSerit',
  ...SOL_WIDGET_ANAHTARLARI,
  ...SAG_WIDGET_ANAHTARLARI
]

/** Govdenin ustunde ayri bant olarak cizilen sabit bolumler. */
export const SABIT_ANAHTARLARI = ['kpi', 'bugunSerit']

/**
 * Kart genislikleri (sol bolgenin 2 kolonlu izgarasi).
 * `nakitAkisi` ve `stokDegeri` tam satir; diger grafikler tek kolon.
 */
export const WIDGET_SPANLARI = {
  nakitAkisi: 2,
  stokDegeri: 2,
  ciroHedef: 1,
  aylikGelirGider: 1,
  alacakYaslandirma: 1,
  kasaBanka: 1,
  kategoriSatislari: 1,
  enCokSatanlar: 1,
  enCokBorcCariler: 1,
  enCokAlacakCariler: 1,
  siparisDurumlari: 1,
  butceGerceklesen: 1,
  gorevler: 1,
  hizliIslemler: 1,
  kritikStok: 1,
  akis: 1,
  icgoruler: 1
}

/** Widget etiketleri icin i18n anahtarlari (dashboard blokunda). */
export const WIDGET_ETIKET_ANAHTARLARI = {
  kpi: 'dashboard.widgetKpi',
  bugunSerit: 'dashboard.bugunSeridi',
  nakitAkisi: 'dashboard.widgetNakitAkisi',
  ciroHedef: 'dashboard.widgetCiroHedef',
  aylikGelirGider: 'dashboard.widgetAylikGelirGider',
  alacakYaslandirma: 'dashboard.widgetYaslandirma',
  kasaBanka: 'dashboard.widgetKasaBanka',
  kategoriSatislari: 'dashboard.widgetKategoriSatislari',
  enCokSatanlar: 'dashboard.widgetEnCokSatanlar',
  enCokBorcCariler: 'dashboard.widgetBorcCariler',
  enCokAlacakCariler: 'dashboard.widgetAlacakCariler',
  siparisDurumlari: 'dashboard.widgetSiparisDurumlari',
  butceGerceklesen: 'dashboard.widgetButce',
  stokDegeri: 'dashboard.widgetStokDegeri',
  gorevler: 'dashboard.widgetGorevler',
  hizliIslemler: 'dashboard.widgetHizliIslemler',
  kritikStok: 'dashboard.widgetKritikStok',
  akis: 'dashboard.widgetAkis',
  icgoruler: 'dashboard.widgetIcgoruler'
}

/** Widget'in bolgesi: 'SOL' | 'SAG'; sabit bantlarda null. */
export function widgetBolgesi(key) {
  if (SOL_WIDGET_ANAHTARLARI.includes(key)) return BOLGE_SOL
  if (SAG_WIDGET_ANAHTARLARI.includes(key)) return BOLGE_SAG
  return null
}

/** Widget'in sol bolge izgarasindaki sabit genisligi. */
export function widgetSpan(key) {
  return WIDGET_SPANLARI[key] || 1
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
const VARSAYILAN_SIRA = [...DASHBOARD_WIDGET_ANAHTARLARI]

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
 * - Sira bolgelere gore toplanir: once SOL (grafikler), sonra SAG (aksiyon).
 *   Boylece eski tek liste kayitlari iki bolgeli yerlesime sorunsuz tasinir.
 * - Eksik bolumler rol varsayilanindan gizli olarak eklenir; yeni bir bolum
 *   eklendiginde eski tercihler bozulmaz.
 */
export function duzenNormallestir(duzen, rol = VARSAYILAN_ROL) {
  const varsayilan = rolDuzeni(rol)
  const sira = []
  const gorulen = new Set()
  const kaynak = duzenGecerliMi(duzen) ? duzen.sira : varsayilan.sira

  for (const w of kaynak) {
    if (!w || !DASHBOARD_WIDGET_ANAHTARLARI.includes(w.key) || gorulen.has(w.key)) continue
    gorulen.add(w.key)
    sira.push({ key: w.key, gorunur: w.gorunur !== false })
  }
  for (const w of varsayilan.sira) {
    if (gorulen.has(w.key)) continue
    gorulen.add(w.key)
    // Yeni eklenen bolumler (kullanicinin kaydettigi duzende hic yoktu)
    // varsayilan olarak GORUNUR olmali; aksi halde yeni grafikler eski
    // tercihler yuzunden gizli kalir ve anasayfa seyrek gorunur.
    sira.push({ key: w.key, gorunur: true })
  }
  return { versiyon: DUZEN_VERSIYON, sira: bolgelereGore(sira) }
}

/** Sabitler basta; kalanlar SOL ve SAG olarak, kendi ic sirasi korunarak. */
function bolgelereGore(sira) {
  const sabitler = SABIT_ANAHTARLARI.map((key) => sira.find((w) => w.key === key)).filter(Boolean)
  const sol = sira.filter((w) => widgetBolgesi(w.key) === BOLGE_SOL)
  const sag = sira.filter((w) => widgetBolgesi(w.key) === BOLGE_SAG)
  return [...sabitler, ...sol, ...sag]
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
 * Widget'i kendi bolgesi icinde bir adim yukari/asagi tasir.
 * Sabit bantlar ve diger bolgedeki widget'lar atlanir; tasinamaz.
 */
export function widgetTasi(sira, key, yon) {
  if (sabitMi(key)) return sira
  const bolge = widgetBolgesi(key)
  if (!bolge) return sira
  const mevcut = sira.findIndex((w) => w.key === key)
  if (mevcut === -1) return sira
  const adim = yon < 0 ? -1 : 1
  let hedef = mevcut + adim
  while (hedef >= 0 && hedef < sira.length) {
    const aday = sira[hedef]
    if (aday && !sabitMi(aday.key) && widgetBolgesi(aday.key) === bolge) break
    hedef += adim
  }
  if (hedef < 0 || hedef >= sira.length) return sira
  return tasiIndex(sira, mevcut, hedef)
}

/**
 * Surukle-birak: `kaynakKey` widget'ini `hedefKey` widget'inin konumuna tasir.
 * Yalnizca ayni bolge icinde calisir; sabit bantlar ne tasinir ne de hedef olur.
 */
export function widgetBirak(sira, kaynakKey, hedefKey) {
  if (!kaynakKey || !hedefKey || kaynakKey === hedefKey) return sira
  if (sabitMi(kaynakKey) || sabitMi(hedefKey)) return sira
  const kaynakBolge = widgetBolgesi(kaynakKey)
  if (!kaynakBolge || kaynakBolge !== widgetBolgesi(hedefKey)) return sira
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
