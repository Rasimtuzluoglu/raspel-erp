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

/** Eski localStorage kayıtlarını yeni şemaya çevirir (NOT → ZIYARET). */
function eskidenYeni(eski) {
  if (!eski || !eski.tur) return null
  const { tur: eskiTur, clientKey, dosyalar, durum, deneme, sonHata, olusturma, id, payload, ...kalan } = eski
  return {
    id: id || yeniId(),
    clientKey: clientKey || yeniClientKey(),
    tur: eskiTur === 'NOT' ? 'ZIYARET' : eskiTur,
    payload: payload && typeof payload === 'object' ? payload : kalan,
    dosyalar: Array.isArray(dosyalar) ? dosyalar : [],
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
  if (typeof veri === 'string' && veri.trim()) return veri.trim()
  if (veri?.message) return String(veri.message)
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
      for (const kayit of eski) {
        const donusen = eskidenYeni(kayit)
        if (!donusen) continue
        try {
          await depo.ekle(donusen)
        } catch {
          depolamaKisitli.value = true
          yedegeYaz(donusen)
        }
      }
    }
    try {
      localStorage.removeItem(ESKI_ANAHTAR)
    } catch {
      // yoksay
    }
  }

  async function yukle() {
    yukleniyor.value = true
    try {
      await eskiKuyruguTasi()
      kuyruk.value = await depo.liste()
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
      olusturma: kayit.olusturma || new Date().toISOString()
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
          await gonderici(kayit)
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
