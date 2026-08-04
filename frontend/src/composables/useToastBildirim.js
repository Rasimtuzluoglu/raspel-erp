import { useToast } from 'primevue/usetoast'

export function useToastBildirim() {
  const toast = useToast()

  const basarili = (detay, summary = 'Başarılı') => {
    toast.add({ severity: 'success', summary, detail: detay, life: 3000 })
  }

  const hata = (detay, summary = 'Hata') => {
    toast.add({ severity: 'error', summary, detail: detay, life: 3000 })
  }

  const uyari = (detay, summary = 'Uyarı') => {
    toast.add({ severity: 'warn', summary, detail: detay, life: 3000 })
  }

  const bilgi = (detay, summary = 'Bilgi') => {
    toast.add({ severity: 'info', summary, detail: detay, life: 3000 })
  }

  return { basarili, hata, uyari, bilgi }
}
