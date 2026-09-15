import { ref } from 'vue'

/**
 * Bilgisayardaki bir klasöre yedek dosyası yazmak için File System Access API
 * (showDirectoryPicker) sarmalayıcısı. Seçilen klasör tanıtıcısı IndexedDB'de
 * saklanır; böylece sayfa yenilense de tercih hatırlanır. API desteklenmiyorsa
 * (Firefox/Safari) çağıran taraf klasik indirmeye düşer.
 */
const DB_NAME = 'raspel_yedek'
const STORE = 'tutamac'
const KEY = 'yedek-klasoru'

export const klasorSecimiDestekleniyor =
  typeof window !== 'undefined' && typeof window.showDirectoryPicker === 'function'

function idbAc() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') return reject(new Error('IDB_YOK'))
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE)
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function idbIslem(mod, fn) {
  const db = await idbAc()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, mod)
    const req = fn(tx.objectStore(STORE))
    tx.oncomplete = () => resolve(req?.result)
    tx.onerror = () => reject(tx.error)
  })
}

async function handleKaydet(handle) {
  try {
    await idbIslem('readwrite', (store) => store.put(handle, KEY))
  } catch {
    /* IndexedDB yoksa bellekte kalır */
  }
}

async function handleOku() {
  try {
    return (await idbIslem('readonly', (store) => store.get(KEY))) || null
  } catch {
    return null
  }
}

async function handleSil() {
  try {
    await idbIslem('readwrite', (store) => store.delete(KEY))
  } catch {
    /* yok say */
  }
}

export function useYedekKlasoru() {
  const klasorHandle = ref(null)
  const klasorAdi = ref('')

  const klasorSec = async () => {
    if (!klasorSecimiDestekleniyor) throw new Error('DESTEKLENMIYOR')
    const handle = await window.showDirectoryPicker({ mode: 'readwrite', id: 'raspel-yedek' })
    klasorHandle.value = handle
    klasorAdi.value = handle.name
    await handleKaydet(handle)
    return handle
  }

  const klasoruYukle = async () => {
    if (!klasorSecimiDestekleniyor) return null
    const handle = await handleOku()
    if (handle) {
      klasorHandle.value = handle
      klasorAdi.value = handle.name
    }
    return handle || null
  }

  const klasoruUnut = async () => {
    klasorHandle.value = null
    klasorAdi.value = ''
    await handleSil()
  }

  const izinVer = async (handle) => {
    if (!handle?.queryPermission) return true
    const opts = { mode: 'readwrite' }
    if ((await handle.queryPermission(opts)) === 'granted') return true
    return (await handle.requestPermission(opts)) === 'granted'
  }

  const yaz = async (dosyaAdi, blob) => {
    const handle = klasorHandle.value
    if (!handle) throw new Error('KLASOR_YOK')
    if (!(await izinVer(handle))) throw new Error('IZIN_YOK')
    const fileHandle = await handle.getFileHandle(dosyaAdi, { create: true })
    const writable = await fileHandle.createWritable()
    await writable.write(blob)
    await writable.close()
  }

  return {
    destekleniyor: klasorSecimiDestekleniyor,
    klasorHandle,
    klasorAdi,
    klasorSec,
    klasoruYukle,
    klasoruUnut,
    yaz
  }
}
