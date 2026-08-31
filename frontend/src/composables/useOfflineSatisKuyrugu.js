import { ref } from 'vue'

const KUYRUK_KEY = 'raspel_offline_satis_kuyrugu'

function kuyruguOku() {
  try {
    return JSON.parse(localStorage.getItem(KUYRUK_KEY) || '[]')
  } catch {
    return []
  }
}

function kuyruguYaz(kuyruk) {
  localStorage.setItem(KUYRUK_KEY, JSON.stringify(kuyruk))
}

export function useOfflineSatisKuyrugu() {
  const bekleyen = ref(kuyruguOku().length)

  function ekle(satis) {
    const kuyruk = kuyruguOku()
    kuyruk.push({ id: Date.now().toString(36) + Math.random().toString(36).slice(2, 7), satis, olusturma: Date.now() })
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

  async function senkronizeEt(gonder) {
    const kuyruk = kuyruguOku()
    if (kuyruk.length === 0) return 0
    let gonderilen = 0
    for (const k of kuyruk) {
      try {
        await gonder(k.satis)
        kaldir(k.id)
        gonderilen++
      } catch {
        break // ağ tekrar koparsa dur
      }
    }
    return gonderilen
  }

  return { bekleyen, ekle, hepsi, kaldir, senkronizeEt }
}
