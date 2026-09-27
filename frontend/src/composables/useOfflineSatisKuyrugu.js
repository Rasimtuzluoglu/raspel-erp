import { ref } from 'vue'

const KUYRUK_KEY = 'raspel_offline_satis_kuyrugu'

function anahtarUret() {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  } catch {
    /* yoksay */
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function kuyruguOku() {
  try {
    return JSON.parse(localStorage.getItem(KUYRUK_KEY) || '[]')
  } catch {
    return []
  }
}

function kuyruguYaz(kuyruk) {
  localStorage.setItem(KUYRUK_KEY, JSON.stringify(kuyruk))
  bekleyen.value = kuyruk.length
}

/** Tum bilesenler ayni sayaci gorur (App.vue banner'i ile POS ekrani senkron kalsin). */
const bekleyen = ref(kuyruguOku().length)

export function useOfflineSatisKuyrugu() {
  /** Kuyrugu localStorage'dan yeniden okur (baska sekme/oturum degisikligi sonrasi). */
  function yenile() {
    bekleyen.value = kuyruguOku().length
  }

  /**
   * Satisi kuyruga ekler. Idempotency anahtari BURADA uretilir ve kayitla birlikte
   * saklanir; boylece her yeniden deneme ayni anahtarla gider ve sunucu mükerrer
   * fatura olusturmaz (ag tekrari/timeout sonrasi cift kayit engellenir).
   * `meta` (opsiyonel): fatura olustuktan sonra acilacak teslimat bilgileri
   * (driverId, teslimatAdresi, notlar, durum).
   */
  function ekle(satis, meta = null) {
    const kuyruk = kuyruguOku()
    kuyruk.push({
      id: anahtarUret(),
      anahtar: anahtarUret(),
      satis,
      meta,
      olusturma: Date.now()
    })
    kuyruguYaz(kuyruk)
    bekleyen.value = kuyruk.length
  }

  function hepsi() {
    return kuyruguOku()
  }

  function kaldir(id) {
    const kuyruk = kuyruguOku().filter((k) => k.id !== id)
    kuyruguYaz(kuyruk)
    bekleyen.value = kuyruk.length
  }

  /**
   * Kuyrugu sirayla gonderir. `gonder(satis, anahtar)` imzasi beklenir; anahtar
   * sabittir. Ag tekrar koparsa dongu durur; gonderilen kayitlar kuyruktan cikar.
   * `teslimatKaydet(meta, yanit)` verilirse, fatura olustuktan sonra teslimat
   * kaydi acilir; teslimat hatasi satisi kuyrukta birakmaz (mukerrer onlenir).
   */
  async function senkronizeEt(gonder, teslimatKaydet) {
    const kuyruk = kuyruguOku()
    if (kuyruk.length === 0) return 0
    let gonderilen = 0
    for (const k of kuyruk) {
      try {
        const yanit = await gonder(k.satis, k.anahtar)
        if (k.meta && typeof teslimatKaydet === 'function') {
          try {
            await teslimatKaydet(k.meta, yanit)
          } catch {
            /* teslimat kaydi basarisiz olsa da satis tamamlandi */
          }
        }
        kaldir(k.id)
        gonderilen++
      } catch {
        break // ağ tekrar koparsa dur
      }
    }
    return gonderilen
  }

  return { bekleyen, ekle, hepsi, kaldir, senkronizeEt, yenile }
}
