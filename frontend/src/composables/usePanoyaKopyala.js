import { useToast } from 'primevue/usetoast'

export function usePanoyaKopyala() {
  const toast = useToast()

  const kopyala = async (metin, etiket = 'Kopyalandı') => {
    if (!metin) {
      toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Kopyalanacak değer yok', life: 2000 })
      return false
    }
    try {
      await navigator.clipboard.writeText(metin)
      toast.add({ severity: 'success', summary: etiket, detail: metin.length > 30 ? metin.slice(0, 30) + '...' : metin, life: 2000 })
      return true
    } catch {
      toast.add({ severity: 'error', summary: 'Hata', detail: 'Panoya kopyalanamadı', life: 3000 })
      return false
    }
  }

  return { kopyala }
}
