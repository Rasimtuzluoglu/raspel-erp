import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'

export function usePanoyaKopyala() {
  const toast = useToast()
const toastBildirim = useToastBildirim()

  const kopyala = async (metin, etiket = 'Kopyalandı') => {
    if (!metin) {
      toastBildirim.uyari('Kopyalanacak değer yok')
      return false
    }
    try {
      await navigator.clipboard.writeText(metin)
      toast.add({ severity: 'success', summary: etiket, detail: metin.length > 30 ? metin.slice(0, 30) + '...' : metin, life: 2000 })
      return true
    } catch {
      toastBildirim.hata('Panoya kopyalanamadı')
      return false
    }
  }

  return { kopyala }
}