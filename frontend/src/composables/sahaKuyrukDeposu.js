/**
 * Saha çevrimdışı kuyruğu için depolama adaptörleri.
 *
 * Aynı arayüzü (`liste`, `ekle`, `guncelle`, `sil`) paylaşırlar; kuyruk
 * composable'ı hangi adaptörün verildiğini bilmez. Böylece tarayıcıda
 * IndexedDB, desteklenmeyen ortamda localStorage, testte bellek kullanılır.
 */

const DB_ADI = 'raspel-saha'
const DB_SURUM = 1
const MAGAZA = 'kuyruk'
const LS_ANAHTAR = 'raspel_saha_kuyruk_depo'

function kopyala(kayit) {
  return kayit ? { ...kayit, payload: { ...(kayit.payload || {}) }, dosyalar: [...(kayit.dosyalar || [])] } : kayit
}

/** Test ve SSR için: süreç belleğinde tutan depo. */
export function bellekDeposu(baslangic = []) {
  let kayitlar = baslangic.map(kopyala)
  return {
    async liste() {
      return kayitlar.map(kopyala)
    },
    async ekle(kayit) {
      kayitlar.push(kopyala(kayit))
    },
    async guncelle(kayit) {
      kayitlar = kayitlar.map((k) => (k.id === kayit.id ? kopyala(kayit) : k))
    },
    async sil(id) {
      kayitlar = kayitlar.filter((k) => k.id !== id)
    }
  }
}

/** IndexedDB kapalıysa (gizli sekme, eski tarayıcı) kullanılan yedek. */
export function localStorageDeposu() {
  const oku = () => {
    try {
      const ham = localStorage.getItem(LS_ANAHTAR)
      const veri = ham ? JSON.parse(ham) : []
      return Array.isArray(veri) ? veri : []
    } catch {
      return []
    }
  }
  const yaz = (kayitlar) => {
    localStorage.setItem(LS_ANAHTAR, JSON.stringify(kayitlar))
  }
  return {
    async liste() {
      return oku().map(kopyala)
    },
    async ekle(kayit) {
      const kayitlar = oku()
      kayitlar.push(kopyala(kayit))
      yaz(kayitlar)
    },
    async guncelle(kayit) {
      yaz(oku().map((k) => (k.id === kayit.id ? kopyala(kayit) : k)))
    },
    async sil(id) {
      yaz(oku().filter((k) => k.id !== id))
    }
  }
}

/** Asıl depo: sayfa kapanıp açılsa da kuyruk kalır. */
export function indexedDbDeposu() {
  if (typeof indexedDB === 'undefined') {
    return localStorageDeposu()
  }

  let dbSozu = null

  const veritabani = () => {
    if (!dbSozu) {
      dbSozu = new Promise((coz, reddet) => {
        const istek = indexedDB.open(DB_ADI, DB_SURUM)
        istek.onupgradeneeded = () => {
          const db = istek.result
          if (!db.objectStoreNames.contains(MAGAZA)) {
            db.createObjectStore(MAGAZA, { keyPath: 'id' })
          }
        }
        istek.onsuccess = () => coz(istek.result)
        istek.onerror = () => reddet(istek.error)
        istek.onblocked = () => reddet(new Error('IndexedDB bloklandı'))
      })
      dbSozu.catch(() => {
        dbSozu = null
      })
    }
    return dbSozu
  }

  const calistir = (mod, islem) =>
    veritabani().then(
      (db) =>
        new Promise((coz, reddet) => {
          const tx = db.transaction(MAGAZA, mod)
          const magaza = tx.objectStore(MAGAZA)
          let sonuc
          try {
            const istek = islem(magaza)
            if (istek) istek.onsuccess = () => { sonuc = istek.result }
          } catch (e) {
            reddet(e)
            return
          }
          tx.oncomplete = () => coz(sonuc)
          tx.onerror = () => reddet(tx.error)
          tx.onabort = () => reddet(tx.error)
        })
    )

  return {
    liste: () => calistir('readonly', (m) => m.getAll()).then((k) => (k || []).map(kopyala)),
    ekle: (kayit) => calistir('readwrite', (m) => m.put(kopyala(kayit))),
    guncelle: (kayit) => calistir('readwrite', (m) => m.put(kopyala(kayit))),
    sil: (id) => calistir('readwrite', (m) => m.delete(id))
  }
}

/** Varsayılan: IndexedDB, yoksa localStorage. */
export function varsayilanDepo() {
  if (typeof indexedDB === 'undefined') return localStorageDeposu()
  return indexedDbDeposu()
}
