import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Tarayicinin cevrimici/cevrimdisi durumunu izler.
 *
 * `navigator.onLine` tek basina guvenilir degildir (ag kablosu takili ama
 * internet yok senaryosu); yine de PWA'da anlik geri bildirim icin en hizli
 * sinyaldir. Sunucuya ulasilabilirlik `api/client.js` icindeki
 * `networkStatus` tarafindan ayrica izlenir.
 *
 * @returns {{ cevrimici: import('vue').Ref<boolean> }}
 */
export function useCevrimici() {
  const oku = () => {
    if (typeof navigator === 'undefined') return true
    return navigator.onLine !== false
  }

  const cevrimici = ref(oku())

  const guncelle = () => {
    cevrimici.value = oku()
  }

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.addEventListener !== 'function') return
    // Mount aninda da oku: SSR/hydration veya test ortaminda deger degismis olabilir.
    guncelle()
    window.addEventListener('online', guncelle)
    window.addEventListener('offline', guncelle)
  })

  onBeforeUnmount(() => {
    if (typeof window === 'undefined' || typeof window.removeEventListener !== 'function') return
    window.removeEventListener('online', guncelle)
    window.removeEventListener('offline', guncelle)
  })

  return { cevrimici }
}

export default useCevrimici
