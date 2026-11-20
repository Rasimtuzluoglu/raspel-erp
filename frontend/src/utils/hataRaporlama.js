/**
 * Hafif frontend hata raporlayıcı.
 *
 * Amaç: sahada/üretimde kırılan ekranı görünür kılmak. Veritabanı yok;
 * olaylar `POST /api/telemetri/hata` üzerinden log + metriğe dönüşür.
 *
 * Kurallar:
 * - Aynı hata imzası 10 dakika boyunca tekrar gönderilmez.
 * - Dakikada en fazla 5 olay gönderilir (log spam'ini engeller).
 * - Oturum yoksa veya ağ hatası olursa kayıt localStorage kuyruğunda tutulur
 *   (en fazla 20; en eski düşer) ve `kuyruguBosalt()` ile sonra gönderilir.
 */

const VARSAYILANLAR = {
  susturmaMs: 10 * 60 * 1000,
  dakikaMs: 60 * 1000,
  dakikaSiniri: 5,
  kuyrukAnahtari: 'raspel_hata_kuyrugu',
  kuyrukSiniri: 20,
  mesajSiniri: 1000,
  stackSiniri: 4000
}

const kisalt = (deger, sinir) => {
  if (deger == null) return ''
  const metin = String(deger)
  return metin.length <= sinir ? metin : metin.slice(0, sinir)
}

const kimlikUret = () => {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  } catch {
    // yoksay
  }
  return `h-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

/**
 * @param {object} secenekler
 * @param {(govde: object) => Promise<unknown>} secenekler.gonderici zorunlu
 * @param {{getItem: Function, setItem: Function, removeItem: Function}} [secenekler.depo]
 * @param {object} [secenekler.pencere] window benzeri (test enjeksiyonu)
 * @param {() => string} [secenekler.konum] rota okuyucu
 * @param {() => boolean} [secenekler.kimlikVarMi] oturum kontrolü
 */
export function hataRaporlamayiBaslat(secenekler = {}) {
  const ayar = { ...VARSAYILANLAR, ...secenekler }
  const gonderici = ayar.gonderici
  const depo = ayar.depo || (typeof localStorage !== 'undefined' ? localStorage : null)
  const pencere = ayar.pencere || (typeof window !== 'undefined' ? window : null)
  const konum = ayar.konum || (() => (typeof location !== 'undefined' ? location.pathname : ''))
  const kimlikVarMi = ayar.kimlikVarMi || (() => true)
  const simdi = ayar.simdi || (() => Date.now())

  const sonGonderimler = []
  const susturulanlar = new Map()
  let dinliyor = false
  let hataDinleyici = null
  let promiseDinleyici = null

  const kuyruguOku = () => {
    if (!depo) return []
    try {
      const ham = depo.getItem(ayar.kuyrukAnahtari)
      const veri = ham ? JSON.parse(ham) : []
      return Array.isArray(veri) ? veri : []
    } catch {
      return []
    }
  }

  const kuyruguYaz = (kayitlar) => {
    if (!depo) return
    try {
      if (!kayitlar.length) depo.removeItem(ayar.kuyrukAnahtari)
      else depo.setItem(ayar.kuyrukAnahtari, JSON.stringify(kayitlar))
    } catch {
      // depolama dolu/kapalı: kayıt yalnız bellekte kalır
    }
  }

  const kuyrugaAl = (govde) => {
    const kuyruk = kuyruguOku()
    kuyruk.push(govde)
    while (kuyruk.length > ayar.kuyrukSiniri) kuyruk.shift()
    kuyruguYaz(kuyruk)
  }

  const govdeOlustur = ({ tur, mesaj, stack }) => ({
    hataId: kimlikUret(),
    tur: kisalt(tur || 'js', 20),
    mesaj: kisalt(mesaj || 'Bilinmeyen hata', ayar.mesajSiniri),
    stack: kisalt(stack, ayar.stackSiniri),
    rota: kisalt(konum(), 300),
    surum: kisalt(ayar.surum || '', 50),
    tarayici: kisalt(ayar.tarayici || '', 300)
  })

  const gonder = async (govde) => {
    try {
      await gonderici(govde)
      return true
    } catch {
      return false
    }
  }

  const suzgecle = ({ tur, mesaj, stack }) => {
    const simdiki = simdi()
    const imza = `${tur}|${mesaj}|${konum()}|${String(stack || '').split('\n')[0]}`
    const son = susturulanlar.get(imza)
    if (son != null && simdiki - son < ayar.susturmaMs) return false

    while (sonGonderimler.length && simdiki - sonGonderimler[0] > ayar.dakikaMs) {
      sonGonderimler.shift()
    }
    if (sonGonderimler.length >= ayar.dakikaSiniri) return false

    susturulanlar.set(imza, simdiki)
    sonGonderimler.push(simdiki)
    return true
  }

  const hataYakala = async (olay = {}) => {
    const veri = {
      tur: olay.tur || 'js',
      mesaj: olay.mesaj,
      stack: olay.stack
    }

    if (!kimlikVarMi()) {
      kuyrugaAl(govdeOlustur(veri))
      return false
    }
    if (!suzgecle(veri)) return false

    const govde = govdeOlustur(veri)
    const basarili = await gonder(govde)
    if (!basarili) {
      // Başarısız (çevrimdışı) gönderim dakikalık kotayı tüketmemeli:
      // aksi halde kuyruk dolarken sonraki hatalar hiç kaydedilmez.
      sonGonderimler.pop()
      kuyrugaAl(govde)
    }
    return basarili
  }

  const kuyruguBosalt = async () => {
    const kuyruk = kuyruguOku()
    if (!kuyruk.length) return 0
    if (!kimlikVarMi()) return 0

    const kalanlar = []
    let gonderilen = 0
    for (const govde of kuyruk) {
      // eslint-disable-next-line no-await-in-loop
      const basarili = await gonder(govde)
      if (basarili) gonderilen += 1
      else kalanlar.push(govde)
    }
    kuyruguYaz(kalanlar)
    return gonderilen
  }

  const dinleyicileriKur = () => {
    if (dinliyor || !pencere) return
    hataDinleyici = (olay) => {
      hataYakala({
        tur: 'js',
        mesaj: olay?.message || 'Bilinmeyen hata',
        stack: olay?.error?.stack
      })
    }
    promiseDinleyici = (olay) => {
      const sebep = olay?.reason
      if (sebep && (sebep.name === 'AbortError' || sebep.code === 'ERR_CANCELED')) return
      hataYakala({
        tur: 'promise',
        mesaj: sebep?.message || String(sebep || 'Yakalanmayan promise hatası'),
        stack: sebep?.stack
      })
    }
    pencere.addEventListener('error', hataDinleyici)
    pencere.addEventListener('unhandledrejection', promiseDinleyici)
    dinliyor = true
  }

  const durdur = () => {
    if (!dinliyor || !pencere) return
    pencere.removeEventListener('error', hataDinleyici)
    pencere.removeEventListener('unhandledrejection', promiseDinleyici)
    dinliyor = false
    hataDinleyici = null
    promiseDinleyici = null
  }

  return { hataYakala, kuyruguBosalt, dinleyicileriKur, durdur }
}
