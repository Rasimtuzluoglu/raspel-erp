import { ref } from 'vue'
import { varsayilanDepo } from './sahaKuyrukDeposu.js'

const ESKI_ANAHTAR = 'raspel_saha_kuyrugu'
const YEDEK_ANAHTAR = 'raspel_saha_kuyrugu_yedek'
const DURUM = { BEKLIYOR: 'BEKLIYOR', HATA: 'HATA', GONDERILIYOR: 'GONDERILIYOR' }

function yeniId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return `k-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function yeniClientKey() {
  return `saha-${yeniId()}`
}

/** Eski localStorage kayıtlarını yeni şemaya çevirir (NOT → ZIYARET, imza → dosyalar). */
function eskidenYeni(eski) {
  if (!eski || !eski.tur) return null
  const {
    tur: eskiTur,
    clientKey,
    dosyalar,
    durum,
    deneme,
    sonHata,
    olusturma,
    id,
    payload,
    imzaDataUrl,
    fotoDataUrl,
    ...kalan
  } = eski

  const yeniPayload = payload && typeof payload === 'object' ? { ...payload } : { ...kalan }
  const yeniDosyalar = Array.isArray(dosyalar) ? [...dosyalar] : []
  const referans = yeniPayload.siparisId ?? yeniPayload.teslimatId ?? id ?? 'eski'

  // Spec §5.4: eski imza/foto alanları `dosyalar` dizisine taşınır; imza
  // kaybolursa teslim kanıtı olmadan gönderim yapılamaz.
  if (imzaDataUrl) {
    yeniDosyalar.push({ ad: `imza-${referans}.png`, mime: 'image/png', dataUrl: imzaDataUrl })
  }
  if (fotoDataUrl) {
    yeniDosyalar.push({ ad: `teslim-${referans}.jpg`, mime: 'image/jpeg', dataUrl: fotoDataUrl })
  }

  return {
    id: id || yeniId(),
    clientKey: clientKey || yeniClientKey(),
    tur: eskiTur === 'NOT' ? 'ZIYARET' : eskiTur,
    payload: yeniPayload,
    dosyalar: yeniDosyalar,
    durum: durum || DURUM.BEKLIYOR,
    deneme: deneme || 0,
    sonHata: sonHata || null,
    olusturma: olusturma || new Date().toISOString()
  }
}

/** 4xx kalıcı hatadır: tekrar denemek sunucuyu boşuna yorar. */
function kaliciHata(hata) {
  const durum = hata?.response?.status
  return Number.isFinite(durum) && durum >= 400 && durum < 500
}

function hataMesaji(hata) {
  const veri = hata?.response?.data
  if (veri && typeof veri === 'object' && veri.message) return String(veri.message)
  // Düz metin gövde proxy/HTML sayfası olabilir; ham gövde kullanıcıya gösterilmez.
  if (typeof veri === 'string') {
    const temiz = veri.trim()
    if (temiz && temiz.length <= 160 && !temiz.includes('<')) return temiz
  }
  return hata?.message ? String(hata.message) : 'Gönderilemedi'
}

function yedegeYaz(kayit) {
  if (typeof localStorage === 'undefined') return
  let mevcut = []
  try {
    const ham = localStorage.getItem(YEDEK_ANAHTAR)
    const veri = ham ? JSON.parse(ham) : []
    mevcut = Array.isArray(veri) ? veri : []
  } catch {
    mevcut = []
  }
  mevcut.push(kayit)
  try {
    localStorage.setItem(YEDEK_ANAHTAR, JSON.stringify(mevcut))
  } catch {
    // Depolama tamamen kapalı: kayıt yalnızca bellekte kalır.
  }
}

/**
 * Sahada çevrimdışı yapılan işlemleri kuyruğa alır ve bağlantı gelince
 * sırayla gönderir.
 *
 * @param {object} secenekler
 * @param {Record<string, (kayit: object) => Promise<unknown>>} [secenekler.gondericiler] tür → gönderici
 * @param {{liste: Function, ekle: Function, guncelle: Function, sil: Function}} [secenekler.depo]
 */
export function useSahaKuyruk({ gondericiler = {}, depo = varsayilanDepo() } = {}) {
  const kuyruk = ref([])
  const yukleniyor = ref(false)
  const gonderiliyor = ref(false)
  const depolamaKisitli = ref(false)
  const sonHata = ref(null)
  let senkronEdiliyor = false

  /** Eski `raspel_saha_kuyrugu` kayıtlarını bir kez yeni depoya taşır. */
  async function eskiKuyruguTasi() {
    if (typeof localStorage === 'undefined') return
    let ham
    try {
      ham = localStorage.getItem(ESKI_ANAHTAR)
    } catch {
      return
    }
    if (!ham) return

    let eski
    try {
      eski = JSON.parse(ham)
    } catch {
      localStorage.removeItem(ESKI_ANAHTAR)
      return
    }
    if (Array.isArray(eski)) {
      let tasinamadi = 0
      for (const kayit of eski) {
        const donusen = eskidenYeni(kayit)
        if (!donusen) continue
        try {
          await depo.ekle(donusen)
        } catch {
          tasinamadi += 1
          depolamaKisitli.value = true
          yedegeYaz(donusen)
        }
      }
      // Taşınamayan kayıt varken eski anahtar silinmez: veri kaybı yerine
      // bir sonraki açılışta yeniden denenir.
      if (tasinamadi > 0) return
    }
    try {
      localStorage.removeItem(ESKI_ANAHTAR)
    } catch {
      // yoksay
    }
  }

  /** Depoya yazılamamış (kota/özel mod) kayıtları geri kazanır. */
  async function yedektenKurtar() {
    if (typeof localStorage === 'undefined') return
    let yedek = []
    try {
      const ham = localStorage.getItem(YEDEK_ANAHTAR)
      const veri = ham ? JSON.parse(ham) : []
      yedek = Array.isArray(veri) ? veri : []
    } catch {
      yedek = []
    }
    if (!yedek.length) return

    const kalanlar = []
    for (const kayit of yedek) {
      try {
        await depo.ekle(kayit)
      } catch {
        kalanlar.push(kayit)
      }
    }
    try {
      if (kalanlar.length) {
        localStorage.setItem(YEDEK_ANAHTAR, JSON.stringify(kalanlar))
      } else {
        localStorage.removeItem(YEDEK_ANAHTAR)
      }
    } catch {
      // yedek anahtarı yazılamıyorsa kayıtlar yedekte kalır
    }
  }

  async function yukle() {
    yukleniyor.value = true
    try {
      await eskiKuyruguTasi()
      await yedektenKurtar()
      const liste = await depo.liste()
      kuyruk.value = (liste || []).slice().sort((a, b) => {
        const sol = String(a?.olusturma || '')
        const sag = String(b?.olusturma || '')
        return sol.localeCompare(sag)
      })
    } catch (e) {
      sonHata.value = hataMesaji(e)
      kuyruk.value = []
    } finally {
      yukleniyor.value = false
    }
    return kuyruk.value
  }

  async function ekle(kayit) {
    const yeni = {
      id: kayit.id || yeniId(),
      clientKey: kayit.clientKey || yeniClientKey(),
      tur: kayit.tur,
      payload: kayit.payload && typeof kayit.payload === 'object' ? kayit.payload : {},
      dosyalar: Array.isArray(kayit.dosyalar) ? kayit.dosyalar : [],
      durum: DURUM.BEKLIYOR,
      deneme: 0,
      sonHata: null,
      olusturma: kayit.olusturma || new Date().toISOString(),
      // Kaydı oluşturan oturum: paylaşılan cihazda başka kullanıcı göndermesin.
      sirketId: kayit.sirketId ?? null,
      kullaniciId: kayit.kullaniciId ?? null
    }
    try {
      await depo.ekle(yeni)
      depolamaKisitli.value = false
    } catch {
      depolamaKisitli.value = true
      yedegeYaz(yeni)
    }
    kuyruk.value = [...kuyruk.value, yeni]
    return yeni.id
  }

  async function kaldir(id) {
    try {
      await depo.sil(id)
    } catch {
      // depo silinemese de bellekten çıkar
    }
    kuyruk.value = kuyruk.value.filter((k) => k.id !== id)
  }

  async function senkronize() {
    if (senkronEdiliyor) return { gonderilen: 0, basarisiz: 0, atlandi: true }
    senkronEdiliyor = true
    gonderiliyor.value = true
    let gonderilen = 0
    let basarisiz = 0

    try {
      for (const kayit of [...kuyruk.value]) {
        if (kayit.durum !== DURUM.BEKLIYOR) continue
        const gonderici = gondericiler[kayit.tur]
        if (typeof gonderici !== 'function') continue

        try {
          await gonderici(kayit, { guncelle: (guncel) => depo.guncelle(guncel) })
          await kaldir(kayit.id)
          gonderilen += 1
        } catch (e) {
          basarisiz += 1
          kayit.deneme = (kayit.deneme || 0) + 1
          kayit.sonHata = hataMesaji(e)
          if (kaliciHata(e)) {
            kayit.durum = DURUM.HATA
          }
          sonHata.value = kayit.sonHata
          try {
            await depo.guncelle(kayit)
          } catch {
            yedegeYaz(kayit)
          }
        }
      }
    } finally {
      senkronEdiliyor = false
      gonderiliyor.value = false
    }

    return { gonderilen, basarisiz }
  }

  /** Hatalı kaydı kullanıcı isteğiyle yeniden denenebilir yapar. */
  async function tekrarDene(id) {
    const kayit = kuyruk.value.find((k) => k.id === id)
    if (!kayit) return
    kayit.durum = DURUM.BEKLIYOR
    kayit.sonHata = null
    try {
      await depo.guncelle(kayit)
    } catch {
      // yoksay
    }
  }

  return {
    kuyruk,
    yukleniyor,
    gonderiliyor,
    depolamaKisitli,
    sonHata,
    yukle,
    ekle,
    kaldir,
    senkronize,
    tekrarDene
  }
}
