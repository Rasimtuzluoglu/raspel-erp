import { useToast } from 'primevue/usetoast'
import { useI18n } from 'vue-i18n'

// Ayni hata mesajinin kisa sure icinde tekrar gosterilmesini engeller.
// Global (App.vue) ve view-level toast'lar ayni kayit defterini paylastigi icin
// ayni API hatasi iki kez (farkli basliklarla) gorunmez.
const DEDUPE_MS = 1500
const sonHatalar = new Map()

function tekrarHataMi(mesaj) {
  const simdi = Date.now()
  for (const [k, zaman] of sonHatalar) {
    if (simdi - zaman > DEDUPE_MS) sonHatalar.delete(k)
  }
  const onceki = sonHatalar.get(mesaj)
  sonHatalar.set(mesaj, simdi)
  return onceki != null
}

export function useToastBildirim() {
  const toast = useToast()
  const { t } = useI18n()

  const basarili = (detay, summary = t('common.toastSuccess')) => {
    toast.add({ severity: 'success', summary, detail: detay, life: 3000 })
  }

  const hata = (detay, summary = t('common.toastError')) => {
    if (detay && tekrarHataMi(String(detay))) return
    toast.add({ severity: 'error', summary, detail: detay, life: 3000 })
  }

  const uyari = (detay, summary = t('common.toastWarning')) => {
    toast.add({ severity: 'warn', summary, detail: detay, life: 3000 })
  }

  const bilgi = (detay, summary = t('common.toastInfo')) => {
    toast.add({ severity: 'info', summary, detail: detay, life: 3000 })
  }

  return { basarili, hata, uyari, bilgi }
}
